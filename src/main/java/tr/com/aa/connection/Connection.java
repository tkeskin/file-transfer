package tr.com.aa.connection;

import java.io.IOException;
import java.util.List;
import org.apache.commons.net.ftp.FTPFileFilter;
import tr.com.aa.exception.FtpException;

/**
 * String,File .
 * commons ftp,org.apache.commons.net.ftp.FTPFile,File
 */
public interface Connection {

  /**
   * @return .
   * @throws FtpException .
   */
  String getWorkingDirectory() throws FtpException;

  /**
   * @param directory .
   * @throws FtpException .
   */
  void changeDirectory(String directory) throws FtpException;

  /**
   * AaFtpFile
   *
   * @return .
   * @throws FtpException .
   */
  List<AaFtpFile> listFiles() throws FtpException;

  /**
   * AaFtpFile
   *
   * @param ftpFileFilter .
   * @return .
   * @throws FtpException .
   */
  List<AaFtpFile> listFiles(FTPFileFilter ftpFileFilter) throws FtpException;

  /**
   * AaFtpFile
   *
   * @param remotePath .
   * @return .
   * @throws FtpException .
   */
  List<AaFtpFile> listFiles(String remotePath) throws FtpException;

  /**
   * AaFtpFile
   *
   * @param remotePath .
   * @param ftpFileFilter .
   * @return .
   * @throws FtpException .
   */
  List<AaFtpFile> listFiles(String remotePath, FTPFileFilter ftpFileFilter) throws FtpException;

  /**
   * Upload a single file to the FTP server .
   *
   * @param localFilePath       Path of the file on local computer .
   * @param remoteDirectoryPath path of directory where the file will be stored .
   * @param logProcess          log .
   */
  void uploadFile(String localFilePath, String remoteDirectoryPath, boolean logProcess)
      throws FtpException;

  /**
   * @param localDirectoryPath  Path of the local directory being uploaded.
   * @param remoteDirectoryPath Path of the current directory on the server
   * @param logProcess          log
   * @throws FtpException .
   */
  void uploadDirectory(String localDirectoryPath, String remoteDirectoryPath, boolean logProcess)
      throws FtpException;

  /**
   * Download a single file from the FTP server
   *
   * @param remoteFilePath     path of the file on the server
   * @param localDirectoryPath path of directory where the file will be stored
   * @param localFileName      ()
   * @param compareTime        true false
   * @param logProcess         log
   * @throws IOException if any network or IO error occurred .
   */
  void downloadFile(String remoteFilePath, String localDirectoryPath, String localFileName,
                    boolean compareTime, boolean logProcess) throws FtpException;

  /**
   * Download a single file from the FTP server，
   * Resume resume download, if the specified directory does not exist
   *
   * @param remoteFilePath     path of the file on the server
   * @param localDirectoryPath path of directory where the file will be stored
   * @param logProcess         log
   * @throws IOException if any network or IO error occurred.
   */
  void downloadFile(String remoteFilePath, String localDirectoryPath, boolean compareTime,
                    boolean logProcess) throws FtpException;

  /**
   * @param remoteFilePath     .
   * @param localDirectoryPath .
   * @throws FtpException .
   */
  void downloadFile(String remoteFilePath, String localDirectoryPath) throws FtpException;

  /**
   * @param remoteDirectoryPath path of remote directory will be downloaded.
   * @param localDirectoryPath  path of local directory will be saved.
   * @throws IOException if any network or IO error occurred.
   */
  public void downloadDirectory(String remoteDirectoryPath, String localDirectoryPath);

  /**
   * @param remoteDirectoryPath path of remote directory will be downloaded.
   * @param localDirectoryPath  path of local directory will be saved.
   * @param logProcess          log
   * @throws IOException if any network or IO error occurred.
   */
  void downloadDirectory(String remoteDirectoryPath, String localDirectoryPath, boolean compareTime,
                         boolean logProcess);

  /**
   * This method calculates total number of sub directories, files and size of a remote directory.
   *
   * @param remoteDirectoryPath path of remote directory
   * @return An array of long numbers in which: - the 1st number is total directories. - the 2nd
   * number is total files. - the 3rd number is total size.
   * @throws IOException If any I/O error occurs.
   */
  long[] directoryInfo(String remoteDirectoryPath) throws FtpException;

  /**
   * directory：Removes a directory by delete all its sub files and , sub directories recursively.
   * And finally remove the directory. file : remove the file only
   *
   * @param remoteFileOrDirectoryPath Path of the parent directory of the current directory on the
   *                                  server (used by recursive calls).
   * @throws IOException .
   */
  void removeFileOrDirectory(String remoteFileOrDirectoryPath) throws FtpException;

  /**
   * Determines whether a file exists or not false
   *
   * @param remoteFilePath .
   * @return true if exists, false otherwise .
   * @throws IOException thrown if any I/O error occurred.
   */
  boolean existsFile(String remoteFilePath) throws FtpException;

  /**
   * Determines whether a directory exists or not false
   *
   * @param remoteDirPath .
   * @return true if exists, false otherwise .
   * @throws IOException thrown if any I/O error occurred.
   */
  boolean existsDirectory(String remoteDirPath) throws FtpException;

  /**
   * stackoverflow.com/questions/29425471
   * /how-to-compare-ftpclient-files-last-modification-with-a-local-file
   * There is no standard way to know ftp server timezone,
   * but what you can do is to upload a file and then calculate the time difference between
   * the file time reported by FTP and locally.
   * This must be a method running as a first step of your program to
   * initialize the timezone logic in every client application of yours.
   */

  /**
   * @param remoteFilePath .
   * @param localFilePath  .
   * @return .
   */
  boolean isSync(String remoteFilePath, String localFilePath);

  /**
   * ftp .
   *
   * @return .
   */
  boolean isAvailable();
}