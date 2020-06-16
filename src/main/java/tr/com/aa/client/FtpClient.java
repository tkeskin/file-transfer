package tr.com.aa.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import tr.com.aa.connection.Connection;
import tr.com.aa.connection.ConnectionFactory;
import tr.com.aa.exception.FtpException;

@Slf4j
public class FtpClient extends Client {

  private static final int FIVE_MINUTES = 300;
  private static final String UNABLE_TO_LOGGERIN_MESSAGE = "Unable to login for user %s";
  private static final String CONNECTION_ERROR_MESSAGE = "Unable to connect to host %s on port %d";
  private static final String STATUS_ERROR_MESSAGE =
      "The host %s on port %d returned a bad status code.";
  private static Connection connection;
  protected FTPClient ftpClient;
  private ConnectionFactory connectionFactory = new ConnectionFactory();

  public FtpClient() {

    ftpClient = new FTPClient();
  }

  /**
   * @return .
   */
  public boolean isConnect() {

    if (ftpClient != null) {
      return ftpClient.isConnected();
    }

    return false;
  }

  /**
   * @return .
   */
  public Connection connect() {

    try {
      connectClientAndCheckStatus();
      setSpecificModesOnClient();
      login();

    } catch (IOException e) {
      throw new FtpException(String.format(CONNECTION_ERROR_MESSAGE, host, port), e);
    }

    if (connection != null && isConnect()) {
      return connection;
    } else {
      return connectionFactory.createFtpConnection(ftpClient);
    }
  }

  /**
   * disconnect .
   */
  public void disconnect() {

    try {

      if (null == ftpClient) {
        throw new FtpException("The underlying client was null.");
      }

      if (ftpClient.isConnected()) {
        log.debug("Disconnecting...");
        ftpClient.disconnect();
        log.debug("Disconnected");
      }

    } catch (IOException e) {
      throw new FtpException("There was an unexpected error while trying to disconnect.", e);
    } finally {

      if (ftpClient.isConnected()) {
        try {
          log.debug("Disconnecting...");
          ftpClient.disconnect();
          log.debug("Disconnected");
        } catch (IOException ioe) {
          // do nothing
        }
      }
    }

  }

  private void connectClientAndCheckStatus() throws SocketException, IOException, FtpException {

    log.info("Connected to " + host + ":" + port + "  via ftp ...");
    ftpClient.addProtocolCommandListener(
        new PrintCommandListener(new PrintWriter(System.out), true)); // Print the executed command
    ftpClient.connect(host, port);

    if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
      throw new FtpException(String.format(STATUS_ERROR_MESSAGE, host, port));
    }
  }

  private void login() throws IOException, FtpException {

    boolean hasLoggedIn = ftpClient
        .login(userCredentials.getUsername(), userCredentials.getPassword());

    if (!hasLoggedIn) {
      ftpClient.disconnect();
      throw new FtpException(
          String.format(UNABLE_TO_LOGGERIN_MESSAGE, userCredentials.getUsername()));
    }
    ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
  }

  private void setSpecificModesOnClient() throws IOException {

    ftpClient.enterLocalPassiveMode();
    ftpClient.setControlKeepAliveTimeout(FIVE_MINUTES);
  }
}
