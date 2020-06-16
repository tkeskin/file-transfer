package tr.com.aa.connection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPCmd;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.io.CopyStreamListener;
import org.apache.commons.net.io.Util;
import tr.com.aa.exception.FtpException;
import tr.com.aa.filefilter.FileNameEqualFilter;
import tr.com.aa.util.MyDateUtils;

@Slf4j
public class FtpConnection implements Connection {

  public static final int FTP_FILE_NOT_FOUND = 550;
  public static final int FTP_PATH_CREATED = 257;
  /**
   * Default SO_SNDBUF and SO_RCVBUF size .
   */
  public static final int DEFAULT_TCP_BUFFER_SIZE = 128 * 1024;
  private static final String COULD_NOT_FIND_FILE_MESSAGE = "Could not find file: %s";
  private static final String FILE_DOWNLOAD_FAILURE_MESSAGE = "Unable to download file %s";
  private static final String FILE_STREAM_OPEN_FAIL_MESSAGE =
      "Unable to write to local directory %s";
  private static final String FILE_LISTING_ERROR_MESSAGE = "Unable to list files in directory %s";
  private static final String NO_SUCH_DIRECTORY_MESSAGE =
      "The directory %s doesn't exist on the remote server.";
  private static final String UNABLE_TO_CD_MESSAGE =
      "Remote server was unable to change directory.";
  //  private static final String FILE_SEPARATOR = "/";

  //  private String currentFileName = "";
  private FTPClient client;
  private int replyCode;
  //ftp MDTM Last modified time format returned by the command
  private String ftpModificationTimePattern = "yyyyMMddHHmmss";

  public FtpConnection(FTPClient client) {

    this.client = client;
  }

  /**
   * Returns the pathname of the current working directory .
   *
   * @return .
   * @throws FtpException .
   */

  @Override
  public String getWorkingDirectory() throws FtpException {

    try {
      return client.printWorkingDirectory();
    } catch (IOException e) {
      throw new FtpException("Unable to print the working directory", e);
    }
  }

  /**
   * Determine if ftp service is available .
   *
   * @return .
   */
  @Override
  public boolean isAvailable() {

    return client.isAvailable();
  }

  @Override
  public void changeDirectory(String directory) throws FtpException {

    try {
      boolean success = client.changeWorkingDirectory(directory);
      if (!success) {
        throw new FtpException(String.format(NO_SUCH_DIRECTORY_MESSAGE, directory));
      }
    } catch (IOException e) {
      throw new FtpException(UNABLE_TO_CD_MESSAGE, e);
    }
  }

  /**
   * List all files and folders in the current directory (can be a single file), repackaged as a
   * custom AaFtpFile
   *
   * @return .
   * @throws FtpException .
   */
  @Override
  public List<AaFtpFile> listFiles() throws FtpException {

    return listFiles(getWorkingDirectory());
  }

  /**
   * List all files and folders in the current directory (can be a single file), repackaged as a
   * custom AaFtpFile
   *
   * @param ftpFileFilter filter .
   * @return .
   * @throws FtpException .
   */
  @Override
  public List<AaFtpFile> listFiles(FTPFileFilter ftpFileFilter) throws FtpException {

    return listFiles(getWorkingDirectory(), ftpFileFilter);
  }

  /**
   * List file and folder information in the specified path (can be a single file), because there is
   * no file path information in org.apache.commons.net.ftp.FTPFile, so repackage it as a custom
   * FTPFile class
   *
   * @param remotePath .
   * @return .
   * @throws FtpException .
   */
  @Override
  public List<AaFtpFile> listFiles(String remotePath) throws FtpException {

    return listFiles(remotePath, null);
  }

  /**
   * List file and folder information in the specified path (can be a single file), because there is
   * no file path information in org.apache.commons.net.ftp.FTPFile, so repackage it as a custom
   * FTPFile class
   *
   * @param remotePath    .
   * @param ftpFileFilter filter .
   * @return .
   * @throws FtpException .
   */
  @Override
  public List<AaFtpFile> listFiles(String remotePath, FTPFileFilter ftpFileFilter)
      throws FtpException {

    List<AaFtpFile> files = new ArrayList<AaFtpFile>();

    try {

      if (existsDirectory(remotePath)) {
        changeDirectory(remotePath);
      } else {
        throw new FtpException(String.format(NO_SUCH_DIRECTORY_MESSAGE, remotePath));
      }

      String newWorkingDirectory = getWorkingDirectory();
      String originalWorkingDirectory = getWorkingDirectory();

      FTPFile[] ftpFiles;

      if (ftpFileFilter == null) {
        ftpFiles = client.listFiles(newWorkingDirectory);
      } else {
        ftpFiles = client.listFiles(newWorkingDirectory, ftpFileFilter);
      }

      for (FTPFile file : ftpFiles) {
        files.add(toFtpFile(file, newWorkingDirectory));
      }

      //Restore the current working directory attributes of ftpClient
      changeDirectory(originalWorkingDirectory);

    } catch (IOException e) {

      throw new FtpException(String.format(FILE_LISTING_ERROR_MESSAGE, remotePath), e);
    }

    return files;
  }

  /**
   * Download to the specified directory, the file name remains unchanged, time stamps are not
   * compared, and download progress is not recorded .
   *
   * @param remoteFilePath     path of the file on the server
   * @param localDirectoryPath path of directory where the file will be stored
   * @throws IOException if any network or IO error occurred
   */
  public void downloadFile(String remoteFilePath, String localDirectoryPath) throws FtpException {

    downloadFile(remoteFilePath, localDirectoryPath, null, false, false);
  }

  /**
   * Download to the specified directory, the file name remains unchanged, and the download progress
   * is recorded
   *
   * @param remoteFilePath     path of the file on the server
   * @param localDirectoryPath path of directory where the file will be stored
   * @param logProcess         log Whether to show download progress in
   * @throws IOException if any network or IO error occurred.
   */
  @Override
  public void downloadFile(String remoteFilePath, String localDirectoryPath, boolean compareTime,
                           boolean logProcess) throws FtpException {

    downloadFile(remoteFilePath, localDirectoryPath, null, compareTime, logProcess);
  }

  /**
   * Download a single file from the FTP server，Support breakpoint resume，If the specified directory
   * does not exist，Created automatically commons net ftp Downloaded file，Last modified and created
   * date of the file，Both are current time，Does not retain the time of the source file。 In this
   * method, the time when the downloaded file is modified is the last modification time of the
   * source file, which is convenient for monitoring whether the server-side file changes.
   *
   * @param remoteFilePath     path of the file on the server
   * @param localDirectoryPath path of directory where the file will be stored
   * @param localFileName      The name of the file downloaded to the local (in some requirements,
   *                           you need to rename the remote file) , null To keep the original file
   *                           name unchanged
   * @param compareTime        When the files are the same size：Whether to compare timestamps true
   *                           Same timestamp，Do not download false Different timestamp，download
   * @param logProcess         log Whether to show download progress in
   * @throws IOException if any network or IO error occurred.
   */
  @Override
  public void downloadFile(String remoteFilePath, String localDirectoryPath, String localFileName,
                           boolean compareTime, boolean logProcess) throws FtpException {

    try {

      //Check if remote file exists
      if (!existsFile(remoteFilePath)) {
        throw new FtpException("Unable to download file : " + remoteFilePath + " does not exist.");
      }

      //If the local folder does not exist, it can be created recursively
      String localFilePath;

      // If the local folder does not exist, it can be created recursively
      // mkdirs Can create folders recursively
      if (!Files.exists(Paths.get(localDirectoryPath))) {
        Paths.get(localDirectoryPath).toFile().mkdirs();
      }

      if (localFileName == null) {
        localFilePath = localDirectoryPath + File.separator + FilenameUtils.getName(remoteFilePath);
      } else {
        localFilePath = localDirectoryPath + File.separator + localFileName;
      }

      log.info("localFilePath={}", localFilePath);

      FTPFile[] files = client.listFiles(
          new String(remoteFilePath.getBytes(StandardCharsets.UTF_8), "iso-8859-1"));
      File localFile = Paths.get(localFilePath).toFile();
      long localSize = localFile.length();
      long remoteSize = files[0].getSize();

      if (localSize == 0) {
        log.info("Local file does not exist, ready to download...");
      }

      if (remoteSize == 0) {
        log.info("Remote ftp file does not exist, exit...");
        return;
      }

      if (remoteSize == localSize) {
        if (compareTime) {
          if (!isSync(remoteFilePath, localFilePath)) {
            log.info(
                "The server file and the local file have the same size "
                    + "and the same timestamp: {} B = {} B, exit the download...",
                remoteSize, localSize);
            return;
          }
        } else {
          log.info(
              "The server file is the same size as the local file, "
                  + "and does not compare timestamps: {} B = {} B, exit the download...",
              remoteSize, localSize);
          return;
        }
      }

      if (remoteSize < localSize) {
        log.info(
            "The local file is larger than the server file, "
                + "with errors: {} B <-> {} B, exit the download...",
            remoteSize, localSize);
        return;
      }

      if (localSize > 0 && remoteSize > localSize) {
        log.info("Local file already exists, ready to resume "
                + ":{}B <--> {}B...", remoteSize,
            localSize);
      }

      //Set passive mode
      //client.enterLocalPassiveMode();
      //Settings are transmitted in binary mode
      //client.setFileType(FTP.BINARY_FILE_TYPE);
      FileOutputStream out = new FileOutputStream(localFile, true);
      client.setRestartOffset(localFile.length());
      InputStream in = client
          .retrieveFileStream(new String(remoteFilePath.getBytes("GBK"), "iso-8859-1"));

      if (logProcess) {
        /* org.apache.commons.net.io.Util.copyStream method*/
        final long step = remoteSize / 100;
        final long[] process = {localSize / step};
        final long[] readbytes = {localSize};

        // Listener  org.apache.commons.net.io.CopyStreamListener
        CopyStreamListener listener = new org.apache.commons.net.io.CopyStreamAdapter() {
          @Override
          public void bytesTransferred(long totalBytesTransferred, int bytesTransferred,
                                       long streamSize) {

            readbytes[0] += bytesTransferred;
            if (readbytes[0] / step != process[0]) {
              process[0] = readbytes[0] / step;
              log.info("Download file completion progress:" + process[0] + " %");
              //TODO Report upload status
            }
            /*System.out.println(" totalBytesTransferred :" + totalBytesTransferred
                + " , bytesTransferred :" + bytesTransferred + " ,streamSize :" + streamSize);*/
          }
        };

        Util.copyStream(
            in, out, DEFAULT_TCP_BUFFER_SIZE, client.getBufferSize(), listener, true);

      } else {
        // Use IOUtils Re-implementation, is efficiency better? Otherwise use if Statements in
        //When greater than 2G
        if (remoteSize - localSize >= 2 * FileUtils.ONE_GB) {
          IOUtils.copyLarge(in, out);
        } else {
          IOUtils.copy(in, out);
        }

      }

      out.flush();
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
      client.completePendingCommand();

      // Last modified date of modified downloaded file is ftp File time
      localFile.setLastModified(MyDateUtils.asLong(getModificationTime(remoteFilePath)));
      log.info("Download completed {} <--> {}B...", localFile.getAbsolutePath(),
          localFile.length());

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Override
  public void downloadDirectory(String remoteDirectoryPath, String localDirectoryPath) {

    downloadDirectory(
        remoteDirectoryPath, localDirectoryPath, false, false);
  }

  /**
   * Download the files under the specified folder .
   *
   * @param remoteDirectoryPath Path of the current directory being downloaded .
   * @param localDirectoryPath  path of directory where the whole remote directory will be .
   *                            downloaded and saved.
   * @param logProcess          log Whether to show download progress in .
   */

  @Override
  public void downloadDirectory(String remoteDirectoryPath, String localDirectoryPath,
                                boolean compareTime, boolean logProcess) {

    List<AaFtpFile> subFiles = listFiles(remoteDirectoryPath);

    if (subFiles != null && subFiles.size() > 0) {
      for (AaFtpFile jubFile : subFiles) {
        if (jubFile.getName().equals(".") || jubFile.getName().equals("..")) {
          // skip parent directory and the directory itself
          continue;
        }
        if (jubFile.isDirectory()) {
          // download the sub directory
          downloadDirectory(jubFile.getAbsolutePath(),
              localDirectoryPath + File.separator + jubFile.getName(), compareTime, logProcess);
        } else {
          // download the file
          downloadFile(
              jubFile.getAbsolutePath(), localDirectoryPath, null, compareTime, logProcess);
        }
      }
    }

  }

  /**
   * Upload a single file to the FTP server .
   *
   * @param localFilePath       Path of the file on local computer
   * @param remoteDirectoryPath path of directory where the file will be stored
   * @param logProcess          log
   */
  @Override
  public void uploadFile(String localFilePath, String remoteDirectoryPath, boolean logProcess)
      throws FtpException {

    try {
      //verilen pathe gore file olustur
      File localFile = Paths.get(localFilePath).toFile();

      //verilen pathe göre file olustu mu kontrol et
      if (!localFile.exists()) {
        throw new FtpException("Unable to download file : " + localFilePath + " does not exist.");
      }

      // upload icin verilen path uzakta var mi yok mu bak,yoksa olustur
      String remoteFilePath = getRemoteUploadDirectoryPath(localFilePath, remoteDirectoryPath);

      //upload edilecek dosya uzakta var mi gorelim
      log.info("[Upload] Check if the file exists on the server...");
      // verilen pathdeki dosya listesini al
      FTPFile[] files = client.listFiles(remoteFilePath);

      //localdeki dosya size
      long localSize = localFile.length();

      //uzaktaki dosya size
      long remoteSize = 0;

      /*OutputStream out = client.appendFileStream(
          new String(remoteFilePath.getBytes(StandardCharsets.UTF_8),
              StandardCharsets.ISO_8859_1));*/
      OutputStream out = client.appendFileStream(remoteFilePath);//After parsing
      //localdeki dosyayi oku
      InputStream in = new FileInputStream(localFile);

      // devam edebilir upload icin mod acildi Set passive mode
      //client.enterLocalPassiveMode();
      // Settings are transmitted in binary mode
      //client.setFileType(FTP.BINARY_FILE_TYPE);
      //client.setControlEncoding("UTF_8");

      // yuklenen dosyanin ilerleme durumunu goster
      long step = localFile.length() / 100;
      long process = remoteSize / step;
      // yuklenecek dosya size
      long readbytes = 0L;

      if (files.length == 1) { // yuklenecek dosya zaten var
        log.info("[Upload Resume] The file already exists on the server, ready to resume...");

        remoteSize = files[0].getSize();
        readbytes = remoteSize;

        if (remoteSize == localSize) {
          log.info(
              "[Upload and resume] The server file and the local file are the same size "
                  + ":: {} B-{} B, exit the upload...",
              remoteSize, localSize);
          return;
        } else if (remoteSize > localSize) {
          log.info(
              "[Upload and resume] The server file is larger than the local file, "
                  + "and there is an error: {} B <-> {} B, exit the upload...",
              remoteSize, localSize);
          return;
        }

        //Resume upload and record status
        log.info(
            "[Upload Resume] Preparation for resuming the transfer is complete, "
                + "start preparing the cursor...");
        in.skip(remoteSize);
        client.setRestartOffset(remoteSize);

      } else {
        in.skip(0);
        client.setRestartOffset(0);
        log.info("File not exists. New upload");
      }

      log.info("Start uploading file");
      if (logProcess) {
        byte[] bytes = new byte[DEFAULT_TCP_BUFFER_SIZE];
        int c;
        while ((c = in.read(bytes)) != -1) {
          out.write(bytes, 0, c);
          readbytes += c;
          if (readbytes / step != process) {
            process = readbytes / step;
            log.info("Upload file completion progress:" + process + " %");
            //TODO Report upload status
          }
        }
      } else {
        // dosya 2gb buyukse
        if (localSize - remoteSize >= 2 * FileUtils.ONE_GB) {
          IOUtils.copyLarge(in, out);
        } else {
          IOUtils.copy(in, out);
        }
      }
      out.flush();
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
      if (client.completePendingCommand()) {
        log.info("The file is uploaded successfully.");
      }
    } catch (IOException e) {
      throw new FtpException("Upload may not have completed.", e);
    }
  }

  /**
   * @param localDirectoryPath  Path of the local directory being uploaded .
   * @param remoteDirectoryPath Path of the current directory on the server .
   * @param logProcess          log .
   * @throws FtpException .
   */
  @Override
  public void uploadDirectory(String localDirectoryPath, String remoteDirectoryPath,
                              boolean logProcess) throws
      FtpException {

    log.info("Listing directory: " + localDirectoryPath);

    File[] localSubFiles = Paths.get(localDirectoryPath).toFile().listFiles();

    if (localSubFiles != null && localSubFiles.length > 0) {
      for (File localFile : localSubFiles) {

        if (localFile.isFile()) {

          uploadDirectory(localFile.getAbsolutePath(), remoteDirectoryPath, logProcess);

        } else {

          uploadDirectory(localFile.getAbsolutePath(),
              remoteDirectoryPath + File.separator + localFile.getName(), logProcess);
        }
      }
    }

  }

  /**
   * This method calculates total number of sub directories, files and size of a remote directory .
   *
   * @param remoteDirectoryPath path of remote directory .
   * @return An array of long numbers in which
   * @throws IOException If any I/O error occurs .
   */
  @Override
  public long[] directoryInfo(String remoteDirectoryPath) throws FtpException {

    return new long[0];
  }

  /**
   * Create a directory and all missing parent-directories . Set to private ,No need to just create
   * folders on the server .
   *
   * @param remoteDirectoryPath .
   * @throws IOException .
   */
  private void createDirectory(String remoteDirectoryPath) throws IOException {

    log.info("Create Directory: {}", remoteDirectoryPath);
    int createDirectoryStatus = client.mkd(remoteDirectoryPath); // makeDirectory...
    log.debug("Create Directory Status: {}", createDirectoryStatus);

    //  remoteDirectoryPath ="/2/2/2/1"    Format that should be validated
    if (createDirectoryStatus == FTP_FILE_NOT_FOUND) {
      int sepIdx = remoteDirectoryPath.lastIndexOf('/');
      if (sepIdx > -1) {
        String parentPath = remoteDirectoryPath.substring(0, sepIdx);
        createDirectory(parentPath);
        log.debug("2'nd Create Directory: {}", remoteDirectoryPath);
        createDirectoryStatus = client.mkd(remoteDirectoryPath); // makeDirectory...
        log.debug("2'nd Create Directory Status: {}", createDirectoryStatus);
      }
    }
  }

  /**
   * directory Removes a directory by delete all its sub files and , sub directories recursively .
   * And finally remove the directory. file : remove the file only .
   *
   * @param remoteFileOrDirectoryPath Path of the destination directory on the server .
   * @throws IOException .
   */
  @Override
  public void removeFileOrDirectory(String remoteFileOrDirectoryPath) throws FtpException {

    log.info("Delete directory: {}", remoteFileOrDirectoryPath);

    try {

      if (existsFile(remoteFileOrDirectoryPath)) {
        client.deleteFile(remoteFileOrDirectoryPath);//If it is a file, delete it
        return;
      }

      List<AaFtpFile> ftpFiles = listFiles(remoteFileOrDirectoryPath);

      if (ftpFiles != null && ftpFiles.size() > 0) {
        for (AaFtpFile jubFtpFile : ftpFiles) {
          if (jubFtpFile.getName().equals(".") || jubFtpFile.getName().equals("..")) {
            // skip parent directory and the directory itself
            continue;
          }

          if (jubFtpFile.isDirectory()) {
            removeFileOrDirectory(jubFtpFile.getAbsolutePath());
          } else {
            client.deleteFile(jubFtpFile.getAbsolutePath());
          }

          client.removeDirectory(
              jubFtpFile.getAbsolutePath());  //Removes a directory on the FTP server (if empty).
        }

        client.removeDirectory(remoteFileOrDirectoryPath);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  /**
   * Determines whether a file exists or not false .
   *
   * @param remoteFilePath .
   * @return true if exists, false otherwise .
   * @throws IOException thrown if any I/O error occurred.
   */
  @Override
  public boolean existsFile(String remoteFilePath) throws FtpException {

    FTPFile[] ftpFiles = new FTPFile[1];
    try {
      ftpFiles = client.listFiles(remoteFilePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return ftpFiles.length == 1;
  }

  /**
   * Determines whether a directory exists or not false com.enterprisedt.net.ftp.FTPClient.java .
   *
   * @param remoteDirectoryPath .
   * @return true if exists, false otherwise .
   * @throws IOException thrown if any I/O error occurred .
   */
  @Override
  public boolean existsDirectory(String remoteDirectoryPath) throws FtpException {

    try {
      changeDirectory(remoteDirectoryPath);
    } catch (FtpException e) {
      e.printStackTrace();
    }

    replyCode = client.getReplyCode();
    if (replyCode == 550) {
      return false;
    }

    String originalWorkingDirectory = getWorkingDirectory();
    changeDirectory(originalWorkingDirectory);
    return true;
  }

  /**
   * According to the above analysis, the solution for monitoring
   * file changes on the ftp server using commons net ftp is:
   * 0. Assuming the server supports MDTM commands, that is,
   * commons net ftp can read the last modified time of the file
   * 1. The monitored file must be downloaded from the server.
   * After downloading, modify the last modified time of the downloaded
   * file to be the last modified time of the server-side source file to make them the same.
   * 2. After a certain period of time, compare whether
   * the last modification time of the server file and
   * the local file is the same (the server has not changed,
   * the local download file has been modified
   * in step 1 and is the same as the server)
   * 3. Plus, compare file sizes (already tested:
   * the file size returned by the commons net ftp command is the same as the local file.
   * This method is not accurate, such as a character a in a file becomes a character b,
   * the size does not change)
   */

  /**
   *
   * @param remoteFilePath server-side file .
   * @param localFilePath local file .
   * @return true files are different and need to be synchronized .
   */

  /**
   * https://github.com/JAGFin1/auto-ftp
   * http://stackoverflow.com/questions/29909233/calculate-file-checksum-in-ftp-server-using-apache-ftpclient
   * few FTP servers support server side CRC generation.
   * If the server doesn't support XCRC, BC will download the entire file and calulate the CRC locally.
   */
  @Override
  public boolean isSync(String remoteFtpFilePath, String localFilePath) {

    log.info("compare remoteFtpFile ({}) and localFile ({}) . ", remoteFtpFilePath, localFilePath);

    File localTempFile = Paths.get(localFilePath).toFile();

    if (!localTempFile.exists()) {
      throw new FtpException("local file : " + localFilePath + " does not exist.");
    }

    LocalDateTime localTimeStamp = MyDateUtils.asLocalDateTime(localTempFile.lastModified());

    FTPFile[] ftpFiles = new FTPFile[1];
    try {
      ftpFiles = client.listFiles(remoteFtpFilePath);
    } catch (IOException e) {
      e.printStackTrace();
    }

    //Check if remote file exists
    if (ftpFiles.length == 0) {
      throw new FtpException("remote file : " + remoteFtpFilePath + " does not exist.");
    }

    LocalDateTime serverTimeStamp = getModificationTime(remoteFtpFilePath);

    boolean time = localTimeStamp.getSecond() == serverTimeStamp.getSecond();
    if (!time) {
      log.info("File creation time is inconsistent and needs to be synchronized...");
      return true;
    }
    boolean size = localTempFile.length() == ftpFiles[0].getSize();
    if (!size) {
      log.info("File sizes are inconsistent and need to be synchronized...");
      return true;
    }
    log.info("The files are the same and do not need to be synchronized...");
    return false;

  }

  /**
   * Due to the ftp protocol standard problem, the last modification time of the file in
   * yyyyMMddHHmmss format can only be returned through the MDTM command to the nearest second. In
   * addition, LIST in ftp protocol can return similar time.But because different ftp servers have
   * different support for these two commands, the return time is different, and you need to test
   * both commands before you can determine.In commons net ftp, there are two methods to directly
   * get the last modification time of the file, but both can only be accurate to the second String
   * ts = client.getModificationTime (remoteFilePath) Check the commons net ftp source code and
   * learn that it is the last modification time obtained using the MDTM command
   * client.doCommandAsStrings (FTPCmd.MDTM.getCommand (), remoteFilePath) Use DMTM commands
   * directly The two returned strings are different client.getModificationTime returns
   * 20140220090225 client.doCommandAsStrings returns 213 20140220090225 Need to do the
   * corresponding string parsing Also,In commons ftp, the client.list method, the time of the
   * FTPFile obtained, uses GregorianCalendar, there is no second attribute.      
   *
   * @param remoteFilePath destination file path
   * @return .
   */
  private LocalDateTime getModificationTime(String remoteFilePath) {

    try {
      //  Long ftpServerTimeStamp = ftpFile.getLastModified().getMillis();
      // commons net ftp
      String ts = client.getModificationTime(remoteFilePath).trim();
      // log.info("ftp file time ({})", ts);
      /**
       * ftp server
       * ftp MDTM  GMT yyyyMMddHHmmss
       */
      return MyDateUtils.parseLocalDateTime(ts, ftpModificationTimePattern);

    } catch (IOException e) {
      e.printStackTrace();
      throw new FtpException(String.format("getTimeDiff exception"), e);
    }
  }

  /**
   * @param localFilePath       .
   * @param remoteDirectoryPath .
   * @return .
   */
  private String getRemoteUploadDirectoryPath(String localFilePath, String remoteDirectoryPath) {

    if (!existsDirectory(remoteDirectoryPath)) {
      try {
        createDirectory(remoteDirectoryPath);

      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    Path targetPath = Paths.get(remoteDirectoryPath);
    String safePath = targetPath.toString();
    String fileName = Paths.get(localFilePath).getFileName().toString();

    return safePath + File.separator + fileName;
  }

  /**
   * FTPFile JFtpFile
   *
   * @param ftpFile             .
   * @param remoteDirectoryPath .
   * @return .
   * @throws IOException .
   */
  private AaFtpFile toFtpFile(FTPFile ftpFile, String remoteDirectoryPath) throws IOException {

    String name = ftpFile.getName();
    long fileSize = ftpFile.getSize();
    String fullPath = String.format("%s%s%s", remoteDirectoryPath, "/",
        ftpFile.getName()); // ftp "/"  File.pathSeparator
    long modTime = ftpFile.getTimestamp().getTime().getTime();
    /*log.info(" 1.  getTimestamp : " + new DateTime(modTime).withZone(DateTimeZone.forTimeZone(
        TimeZone.getDefault())));
    log.info(" 2.  getTimestamp : " + ftpFile.getTimestamp().getTimeInMillis());*/

    return new AaFtpFile(name, fileSize, fullPath, modTime, ftpFile.isDirectory());
  }

  private long getTimeDiff() {

    Path path = null;
    //ftp MDTM
    //String pattern = "yyyyMMddHHmmss";

    try {
      path = Files.createTempFile("temp", ".txt");
      // File loaclTempFile = path.toFile();

      File loaclTempFile = Paths.get("D:\\download\\edtftpj.zip").toFile();

      String localFilePath = loaclTempFile.getAbsolutePath();
      // FileUtils.writeStringToFile(
      // loaclTempFile,"this is test string, for get time diff of local and remote");

      LocalDateTime localTimeStamp = MyDateUtils.asLocalDateTime(loaclTempFile.lastModified());

      String remoteFilePath = getRemoteUploadDirectoryPath(localFilePath, "temp_test_time");
      InputStream inputStream = null;
      try {
        inputStream = new FileInputStream(new File(localFilePath));
        boolean hasUploaded = client.storeFile(remoteFilePath, inputStream);
        if (!hasUploaded) {
          throw new FtpException("Upload failed.");
        }

        log.info("upload a file to: " + remoteFilePath);
      } catch (FileNotFoundException e) {

        throw new FtpException(String.format(COULD_NOT_FIND_FILE_MESSAGE, localFilePath), e);
      } catch (IOException e) {
        throw new FtpException("Upload may not have completed.", e);
      } finally {
        IOUtils.closeQuietly(inputStream);

      }

      client.setModificationTime(remoteFilePath,
          MyDateUtils.parseLocalDateTime(localTimeStamp, ftpModificationTimePattern));

      List<AaFtpFile> ftpFiles = this
          .listFiles("/temp_test_time/", new FileNameEqualFilter(loaclTempFile.getName()));
      AaFtpFile ftpFile = ftpFiles.get(0);
      String ts = client.getModificationTime("/temp_test_time/" + ftpFile.getName());

      LocalDateTime serverTimeStamp = MyDateUtils
          .parseLocalDateTime(ts, ftpModificationTimePattern);
      return localTimeStamp.getSecond() - serverTimeStamp.getSecond();

    } catch (IOException e) {
      e.printStackTrace();
      throw new FtpException(String.format("getTimeDiff exception"), e);
    }
  }
}