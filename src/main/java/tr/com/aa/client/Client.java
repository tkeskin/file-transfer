package tr.com.aa.client;

import tr.com.aa.client.auth.UserCredentials;
import tr.com.aa.connection.Connection;

public abstract class Client {

  protected String host;
  protected int port;

  protected UserCredentials userCredentials = UserCredentials.ANONYMOUS;

  public void setCredentials(UserCredentials userCredentials) {

    this.userCredentials = userCredentials;
  }

  public void setCredentials(String userName, String password) {

    setCredentials(new UserCredentials(userName, password));
  }

  public void setHost(String host) {

    this.host = host;
  }

  public void setPort(int port) {

    this.port = port;
  }

  /**
   * Opens a connection to the given host and port. All activity and communication should be handled
   * using this connection.
   *
   * @return An active connection matching the protocol set by the client.
   */
  public abstract Connection connect();

  /**
   * The connection action is time-consuming, and the judgment method is added to eliminate the
   * repeated connection action.
   */
  public abstract boolean isConnect();

  public abstract void disconnect();
}