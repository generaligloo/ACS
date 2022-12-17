package ACS_Server;

import ACS_Server.util.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;

public class ExchangeACQ implements Runnable{

    private final static Logger LOGGER = LoggerFactory.getLogger(ACS_Server_Main.class);

    @Override
    public void run() {
        //init method
        System.setProperty("javax.net.ssl.keyStore","src\\main\\java\\ACS_Server\\store\\ServerACS.jks");
        System.setProperty("javax.net.ssl.keyStorePassword","123456ACS");
        System.setProperty("javax.net.ssl.trustStore","src\\main\\java\\ACS_Server\\store\\ServerACS.jks");
        System.setProperty("javax.net.ssl.trustStorePassword","123456ACS");
        try
        {
            SSLServerSocketFactory sslserversocketfactory =
                    (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket sslserversocket =
                    (SSLServerSocket) sslserversocketfactory.createServerSocket(29180);
            SSLSocket sslsocket = (SSLSocket) sslserversocket.accept();
            LOGGER.info(Ansi.GREEN + "Connexion à l'ACQ réussi !");
            InputStream inputstream = sslsocket.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            OutputStream outputstream = sslsocket.getOutputStream();
            OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream);
            BufferedWriter bufferedwriter = new BufferedWriter(outputstreamwriter);
            LOGGER.info(Ansi.CYAN  + "Reading ...");
            String messageFromACQ = bufferedreader.readLine();
            LOGGER.info("Message received: " + messageFromACQ);
            String response = "Your message was: " + messageFromACQ;
            bufferedwriter.write(response + '\n');
            bufferedwriter.flush();
            sslsocket.close();

        } catch (Exception exception) {
            exception.printStackTrace();
            System.out.println(exception);
        }
    }
}
