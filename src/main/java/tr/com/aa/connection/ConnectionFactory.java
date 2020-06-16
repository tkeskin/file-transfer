package tr.com.aa.connection;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.net.ftp.FTPClient;

public class ConnectionFactory {

  public SftpConnection createSftpConnection(Channel channel) {

    return new SftpConnection((ChannelSftp) channel);
  }

  public FtpConnection createFtpConnection(FTPClient client) {

    return new FtpConnection(client);
  }
}