package tr.com.aa.filefilter;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public class FileNameEqualFilter implements FTPFileFilter {

  private String fullFileName = new String();

  public FileNameEqualFilter(String fullFileName) {

    this.fullFileName = fullFileName;
  }

  @Override
  public boolean accept(FTPFile ftpFile) {

    if (fullFileName.isEmpty()) {
      return ftpFile.isFile();
    }

    return (ftpFile.isFile() && ftpFile.getName().equals(fullFileName));
  }
}