package tr.com.aa.connection;

import java.time.LocalDateTime;
import tr.com.aa.util.MyDateUtils;

/**
 * ，org.apache.commons.net.ftp.FTPFile,ftp ,AaFtpFile
 */
public class AaFtpFile {

  private String name;
  private long size;
  private String absolutePath;
  private LocalDateTime lastModified;
  private boolean directory;

  /**
   * @param name         .
   * @param size         .
   * @param absolutePath .
   * @param modTime      .
   * @param isDirectory  .
   */
  public AaFtpFile(String name, long size, String absolutePath, long modTime, boolean isDirectory) {

    this.name = name;
    this.size = size;
    this.absolutePath = absolutePath;
    this.lastModified = MyDateUtils.asLocalDateTime(modTime);
    this.directory = isDirectory;
  }

  public String getName() {

    return name;
  }

  public long getSize() {

    return size;
  }

  public String getAbsolutePath() {

    return absolutePath;
  }

  public LocalDateTime getLastModified() {

    return lastModified;
  }

  public boolean isDirectory() {

    return directory;
  }
}