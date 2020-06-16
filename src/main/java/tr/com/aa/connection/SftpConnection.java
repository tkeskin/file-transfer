package tr.com.aa.connection;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.aa.exception.FtpException;

@Slf4j
public class SftpConnection implements Connection {

  private static final String COULD_NOT_FIND_FILE_MESSAGE = "Could not find file: %s";
  private static final String DIRECTORY_DOES_NOT_EXIST_MESSAGE = "Directory %s does not exist.";
  private static final String FILE_LISTING_ERROR_MESSAGE = "Unable to list files in directory %s";
  private static final String FILE_SEPARATOR = "/";
  private static final int MILLIS = 1000;

  private ChannelSftp channel;

  public SftpConnection(ChannelSftp channel) {
    this.channel = channel;
  }

  @Override
  public String getWorkingDirectory() throws FtpException {

    try {
      return channel.pwd();
    } catch (SftpException e) {
      throw new FtpException("Unable to print the working directory", e);
    }
  }

  @Override
  public void changeDirectory(String directory) throws FtpException {

    try {
      channel.cd(directory);
    } catch (SftpException e) {
      throw new FtpException(String.format(DIRECTORY_DOES_NOT_EXIST_MESSAGE, directory), e);
    }
  }

  @Override
  public List<AaFtpFile> listFiles() throws FtpException {
    return listFiles(getWorkingDirectory());
  }

  @Override
  public List<AaFtpFile> listFiles(FTPFileFilter ftpFileFilter) throws FtpException {
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<AaFtpFile> listFiles(String remotePath) throws FtpException {

    try {

      List<AaFtpFile> files = new ArrayList<AaFtpFile>();
      String originalWorkingDirectory = getWorkingDirectory();
      changeDirectory(remotePath);
      String newWorkingDirectory = getWorkingDirectory();
      Vector<LsEntry> lsEntries = channel.ls(newWorkingDirectory);
      for (LsEntry entry : lsEntries) {
        files.add(toFtpFile(entry, newWorkingDirectory));
      }
      changeDirectory(originalWorkingDirectory);
      return files;
    } catch (SftpException e) {
      throw new FtpException(String.format(FILE_LISTING_ERROR_MESSAGE, remotePath), e);
    }
  }

  @Override
  public List<AaFtpFile> listFiles(String remotePath, FTPFileFilter ftpFileFilter)
      throws FtpException {
    return null;
  }

  @Override
  public void downloadFile(String remoteFilePath, String localDirectoryPath, boolean compareTime,
                           boolean logProcess) throws FtpException {
    downloadFile(remoteFilePath, localDirectoryPath, null, compareTime, logProcess);

  }

  @Override
  public void downloadFile(String remoteFilePath, String localDirectoryPath) throws FtpException {

  }

  @Override
  public void downloadFile(String remoteFilePath, String localDirectoryPath, String localFileName,
                           boolean compareTime, boolean logProcess) throws FtpException {

    if (!existsFile(remoteFilePath)) {
      return;
    }

    //Local folder does not exist, recursively created
    Path targetPath = Paths.get(localDirectoryPath);
    if (!Files.exists(targetPath)) {
      Paths.get(localDirectoryPath).toFile().mkdirs();
    }

    try {
      if (localFileName == null) {
        channel.get(remoteFilePath, localDirectoryPath);
      } else {
        channel.get(remoteFilePath, localDirectoryPath + File.separator + localFileName);
      }

    } catch (SftpException e) {
      throw new FtpException("Unable to download file " + remoteFilePath, e);
    }

    log.info(
        "download file '" + remoteFilePath + "'  to  localDirectory '" + localDirectoryPath
            + FILE_SEPARATOR + StringUtils.substringAfterLast(remoteFilePath, "/") + "' succeed.");

  }

  @Override
  public void downloadDirectory(String remoteDirectoryPath, String localDirectoryPath,
                                boolean compareTime, boolean logProcess) throws FtpException {

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
              localDirectoryPath + FILE_SEPARATOR + jubFile.getName(), compareTime, logProcess);
        } else {
          // download the file
          downloadFile(jubFile.getAbsolutePath(), localDirectoryPath, compareTime, logProcess);
        }
      }
    }

  }

  @Override
  public void downloadDirectory(String remoteDirectoryPath, String localDirectoryPath) {
    downloadDirectory(remoteDirectoryPath, localDirectoryPath, false, false);
  }

  @Override
  public void uploadFile(String localFilePath, String remoteDirectoryPath, boolean logProcess)
      throws FtpException {
    if (!Paths.get(localFilePath).toFile().exists()) {
      throw new FtpException("Unable to upload file, file does not exist :  " + localFilePath);
    }

    if (!existsDirectory(remoteDirectoryPath)) {
      createDirectory(remoteDirectoryPath);
    }

    try {
      channel.put(localFilePath, remoteDirectoryPath);
    } catch (SftpException e) {
      e.printStackTrace();
      throw new FtpException("Unable to upload file :  " + localFilePath);
    }

    log.info("upload file succeed : " + localFilePath);
  }

  @Override
  public void uploadDirectory(String localDirectoryPath, String remoteDirectoryPath,
                              boolean logProcess) throws FtpException {

    log.info("Listing directory: " + localDirectoryPath);
    File[] localSubFiles = Paths.get(localDirectoryPath).toFile().listFiles();
    if (localSubFiles != null && localSubFiles.length > 0) {
      for (File localFile : localSubFiles) {
        if (localFile.isFile()) {
          uploadFile(localFile.getAbsolutePath(), remoteDirectoryPath, logProcess);
        } else {
          uploadDirectory(localFile.getAbsolutePath(),
              remoteDirectoryPath + FILE_SEPARATOR + localFile.getName(), logProcess);
        }
      }
    }
    log.info("upload local Directory " + localDirectoryPath + " succeed.");
  }

  @Override
  public long[] directoryInfo(String remoteDirectoryPath) throws FtpException {
    return new long[0];
  }

  @Override
  public void removeFileOrDirectory(String remoteFileOrDirectoryPath) throws FtpException {

  }

  @Override
  public boolean existsFile(String remoteFilePath) {

    try {
      // System.out.println(channel.realpath(remoteFilePath));
      SftpATTRS attrs = channel.stat(remoteFilePath);
      return attrs.isReg();
    } catch (SftpException e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public boolean existsDirectory(String remoteDirectoryPath) {

    try {
      // System.out.println(channel.realpath(remoteFilePath));
      SftpATTRS attrs = channel.stat(remoteDirectoryPath);
      return attrs.isDir();
    } catch (SftpException e) {
      // e.printStackTrace();
      return false;
    }
  }

  @Override
  public boolean isSync(String remoteFilePath, String localFilePath) {
    return false;
  }

  @Override
  public boolean isAvailable() {
    return channel.isConnected();
  }

  private void createDirectory(String remoteDirectoryPath) {

    String originalWorkingDirectory = getWorkingDirectory();
    String[] folders = remoteDirectoryPath.split("/");
    for (String folder : folders) {
      if (folder.length() > 0) {
        try {
          channel.cd(folder);
        } catch (SftpException e) {
          try {
            channel.mkdir(folder);
            channel.cd(folder);
          } catch (SftpException e1) {
            e1.printStackTrace();
          }
        }
      }
    }
    log.info("create remote Directory '" + remoteDirectoryPath + "' succeed.");
    changeDirectory(originalWorkingDirectory);
  }

  private AaFtpFile toFtpFile(LsEntry lsEntry, String filePath) throws SftpException {
    String name = lsEntry.getFilename();
    long fileSize = lsEntry.getAttrs().getSize();
    String fullPath = String.format("%s%s%s", filePath, "/", lsEntry.getFilename());
    //   String fullPath = channel.realpath(filePath);
    int modTime = lsEntry.getAttrs().getMTime();
    boolean directory = lsEntry.getAttrs().isDir();
    return new AaFtpFile(name, fileSize, fullPath, (long) modTime * MILLIS, directory);
  }
}