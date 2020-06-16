package tr.com.aa.client;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.aa.connection.Connection;
import tr.com.aa.connection.ConnectionFactory;
import tr.com.aa.exception.FtpException;

/**
 * No method was found to output the executed command in the console.
 */
@Slf4j
public class SftpClient extends Client {

  private static final String SFTP = "sftp";
  private static final String CONNECTION_ERROR_MESSAGE = "Unable to connect to host %s on port %d";
  private static Connection connection;
  private JSch jsch;
  private ConnectionFactory connectionFactory;
  private Session session;
  private Channel channel;

  /**
   * sftp client .
   */
  public SftpClient() {
    this.jsch = new JSch();
    this.connectionFactory = new ConnectionFactory();
  }

  /**
   *
   * @return .
   */
  public boolean isConnect() {
    if (channel != null) {
      return channel.isConnected();
    }
    return false;
  }

  /**
   *
   * @return .
   */
  public Connection connect() {
    session = null;
    channel = null;

    try {

      configureSessionAndConnect();
      openChannelFromSession();

    } catch (JSchException e) {
      throw new FtpException(String.format(CONNECTION_ERROR_MESSAGE, host, port), e);
    }

    if (connection != null && isConnect()) {
      return connection;
    } else {
      return connectionFactory.createSftpConnection(channel);
    }
  }

  /**
   * disconnect .
   */
  public void disconnect() {

    if (channel != null) {
      if (channel.isConnected()) {
        channel.disconnect();
        log.info("logged out.");
      } else if (channel.isClosed()) {
        log.info("sftp is closed already");
      }
    }

    if (session != null) {
      if (session.isConnected()) {
        session.disconnect();
        log.info("logged out.");
      } else {
        log.info("sshSession is disconnected already.");
      }
    }

  }

  private void configureSessionAndConnect() throws JSchException {

    log.info("Connected to " + host + ":" + port + " via SFTP ...");
    session = jsch.getSession(userCredentials.getUsername(), host, port);
    session.setConfig("StrictHostKeyChecking", "no");
    session.setPassword(userCredentials.getPassword());
    session.connect();
  }

  private void openChannelFromSession() throws JSchException {

    channel = session.openChannel(SFTP);
    channel.connect();
  }
}