package tr.com.aa;

import tr.com.aa.client.Client;
import tr.com.aa.client.ClientFactory;
import tr.com.aa.client.auth.UserCredentials;
import tr.com.aa.connection.Connection;

public class FtpExamples {

  /**
   *
   * @param arg .
   */
  public static void main(String[] arg) {

    int ftpport = 21;
    String ftpHostName = "files.000webhost.com";

    String ftpUsername = "mikrorek";
    String ftpPassword = "Keskyn2408";

    Client client = new ClientFactory().createClient(ClientFactory.Protocol.FTP);
    client.setHost(ftpHostName);
    client.setPort(ftpport);
    client.setCredentials(new UserCredentials(ftpUsername, ftpPassword));

    Connection connection = client.connect();

    System.out.println("working directory : " + connection.getWorkingDirectory());

    connection.uploadDirectory("/home/tkeskin/has/", "/mp4", true);

    client.disconnect();
  }
}