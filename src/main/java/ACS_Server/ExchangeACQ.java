package ACS_Server;

import ACS_Server.util.Ansi;
import ACS_Server.util.TokenBody;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


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
            SSLSocket sslsocket;
            try (SSLServerSocket sslserversocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(29180)) {
                sslsocket = (SSLSocket) sslserversocket.accept();
            }
            LOGGER.info(Ansi.GREEN + "Connexion à l'ACQ réussi !");
            InputStream inputstream = sslsocket.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            OutputStream outputstream = sslsocket.getOutputStream();
            OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream);
            BufferedWriter bufferedwriter = new BufferedWriter(outputstreamwriter);
            while (sslsocket.isConnected()|| sslsocket.isBound())
            {
                LOGGER.info(Ansi.CYAN  + "Reading ...");
                String messageFromACQ = bufferedreader.readLine();
                LOGGER.info("Message received: " + messageFromACQ);
                if (messageFromACQ.equals("@TokenVerif"))
                {
                    LOGGER.info(Ansi.CYAN  + "Reading TokenBody...");
                    String tokenBodyVerif = bufferedreader.readLine();
                    int verification = verifToken(tokenBodyVerif);
                    switch (verification) {
                        case -1 -> {
                            LOGGER.info(Ansi.RED + "ERROR - TOKENVERIF" + Ansi.SANE);
                            bufferedwriter.write("@Payement-Error");
                        }
                        case 0 -> {
                            LOGGER.info(Ansi.RED + "Token introuvable." + Ansi.SANE);
                            bufferedwriter.write("@Payement-InvalideToken");
                        }
                        case 1 -> {
                            LOGGER.info(Ansi.RED + "Token trouvé mais expiré." + Ansi.SANE);
                            bufferedwriter.write("@Payement-InvalideDate");
                        }
                        case 2 -> {
                            LOGGER.info(Ansi.GREEN + "Token vérifié correctement et valide!" + Ansi.SANE);
                            bufferedwriter.write("@Payement-OK");
                        }
                    }
                    bufferedwriter.flush();
                }
                else
                {
                    LOGGER.info(Ansi.RED + "InvalideMessageReception" +Ansi.SANE);
                }
            }


        } catch (Exception exception) {
            exception.printStackTrace();
            System.out.println(exception);
        }
    }

    public static int verifToken(String tokenVer)
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        File file = new File("src\\main\\java\\ACS_Server\\json\\token.json");
        TokenBody tmpRead = null;
        try {
            List<TokenBody> myObjectsRead = new ArrayList<>();
            if(file.length() == 0)
            {
                LOGGER.error("FICHIER JSON VIDE.");
                return 0;
            }
            else {
                myObjectsRead = mapper.readValue(file, new TypeReference<List<TokenBody>>() {
                });
                for (TokenBody en : myObjectsRead) {
                    if(en.getTokenBody().equals(tokenVer))
                    {
                        LOGGER.info(Ansi.CYAN + "------Token trouvé------" + Ansi.SANE);
                        LOGGER.info(Ansi.GREEN + "TokenUID: " + en.getTokenID().toString());
                        LOGGER.info(Ansi.GREEN + "IBAN: " + en.getIBAN());
                        LOGGER.info(Ansi.GREEN + "TokenBody: " + en.getTokenBody());
                        LOGGER.info(Ansi.GREEN + "TokenStart: " + en.getDateCrea().toString());
                        LOGGER.info(Ansi.GREEN + "TokenExpire: " + en.getDateExpi().toString()+ Ansi.SANE);
                        LOGGER.info(Ansi.CYAN + "------Vérification Expiration------" + Ansi.SANE);
                        LocalDate localDate = LocalDate.now();
                        Date dateNow = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                        if(en.getDateExpi().after(dateNow))
                        {
                            LOGGER.info(Ansi.GREEN + "Date Valide !" + Ansi.SANE);
                            return 2;
                        }
                        else
                        {
                            LOGGER.info(Ansi.CYAN + "Date Invalide" + Ansi.SANE);
                            return 1;
                        }
                    }
                    LOGGER.info(Ansi.BLUE + "-----------------------------------------------" + Ansi.SANE);
                    LOGGER.info(Ansi.GREEN + "TokenUID: " + en.getTokenID().toString());
                    LOGGER.info(Ansi.GREEN + "IBAN: " + en.getIBAN());
                    LOGGER.info(Ansi.GREEN + "TokenBody: " + en.getTokenBody());
                    LOGGER.info(Ansi.GREEN + "TokenStart: " + en.getDateCrea().toString());
                    LOGGER.info(Ansi.GREEN + "TokenExpire: " + en.getDateExpi().toString()+ Ansi.SANE);
                }
            }
        } catch (IOException e) {
            return -1;
        }
        return 0;
        // -1 error
        // 0 Introuvable
        // 1 Trouvé mais expiré
        // 2 Trouvé et valide
    }

}