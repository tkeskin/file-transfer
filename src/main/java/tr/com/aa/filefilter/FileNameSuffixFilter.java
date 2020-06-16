package tr.com.aa.filefilter;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public class FileNameSuffixFilter implements FTPFileFilter {

  private List suffixList = Lists.newArrayList();

  public FileNameSuffixFilter(String[] suffixs) {

    this.suffixList = Arrays.asList(suffixs);
  }

  @Override
  public boolean accept(FTPFile ftpFile) {

    if (suffixList.isEmpty()) {
      return ftpFile.isFile();
    }

    return (ftpFile.isFile() && suffixList.contains(FilenameUtils.getExtension(ftpFile.getName())));
  }
}