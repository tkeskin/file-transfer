package tr.com.aa.filefilter;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public class FileNamePrefixFilter implements FTPFileFilter {

  private List prefixList = Lists.newArrayList();

  public FileNamePrefixFilter(String[] prefixContains) {

    this.prefixList = Arrays.asList(prefixContains);
  }

  @Override
  public boolean accept(FTPFile ftpFile) {

    if (prefixList.isEmpty()) {
      return ftpFile.isFile();
    }

    return (ftpFile.isFile() && prefixList.contains(FilenameUtils.getPrefix(ftpFile.getName())));
  }
}