package ACS_Server;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

import ACS_Server.util.Ansi;

import ACS_Server.util.TokenBody;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.validator.routines.checkdigit.IBANCheckDigit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.*;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.time.LocalDate;

public class ACS_Server_Main implements Runnable
{
    private final static Logger LOGGER = LoggerFactory.getLogger(ACS_Server_Main.class);
    static String command;
    static volatile boolean newCommand = false;
    static volatile boolean CommandEnd = true;
    static boolean End = true;

    public static void main(String[] args)
    {
        //init method
        System.setProperty("javax.net.ssl.keyStore","src\\main\\java\\ACS_Server\\store\\ServerACS.jks");
        System.setProperty("javax.net.ssl.keyStorePassword","123456ACS");
        System.setProperty("javax.net.ssl.trustStore","src\\main\\java\\ACS_Server\\store\\ServerACS.jks");
        System.setProperty("javax.net.ssl.trustStorePassword","123456ACS");

        LOGGER.info(Ansi.GREEN+ "Starting Main Thread ...");
        ACS_Server_Main ServerMain = new ACS_Server_Main();
        ExchangeACQ ExchangeACQ = new ExchangeACQ();
        Thread ExchangeACQThread = new Thread(ExchangeACQ);
        Thread ServerMainThread = new Thread(ServerMain);
        ExchangeACQThread.start();
        ServerMainThread.start();
        Scanner scanner = new Scanner(System.in);
        while(End)
        {
                if(CommandEnd) {
                    System.out.println(Ansi.BLUE + "Choisissez une commande :\n");
                    System.out.println(Ansi.CYAN + "- 1.Generate Token ");
                    System.out.println(Ansi.CYAN + "- 2.View tokens ");
                    System.out.println(Ansi.CYAN + "- 3.Verify token");
                    System.out.println(Ansi.CYAN + "- 4.Quitter ");
                    command = scanner.nextLine();
                    LOGGER.info("Input: " + command);
                    if(StringUtils.isNumeric(command))
                    {
                        newCommand = true;
                        CommandEnd = false;
                    }
                    else
                    {
                        System.out.println(Ansi.RED + "Command is not an integer ");
                    }
                }
            if (newCommand)
            {
                LOGGER.info(Ansi.GREEN+ "Listening command ...");
                LOGGER.info("command: " + command);
                switch (command) {
                    case "1" -> {
                        System.out.println("dd-mm-yyyy:");
                        String date = scanner.nextLine();
                        Date dateExpi;
                        try {
                            dateExpi = new SimpleDateFormat("dd-MM-yyyy").parse(date);
                        } catch (ParseException e) {
                            LOGGER.error(Ansi.RED + "erreur Date non valide");
                            CommandEnd = true;
                            break;
                        }
                        System.out.println("IBAN:");
                        String IBAN = scanner.nextLine();
                        boolean valid = new IBANCheckDigit().isValid(IBAN);
                        if (!valid) {
                            LOGGER.error(Ansi.RED + "erreur IBAN non valide");
                            CommandEnd = true;
                            break;
                        }
                        LOGGER.info(dateExpi.toString());
                        String tmp = AddTokenToJSON(dateExpi, IBAN);
                        LOGGER.info("Token:" + tmp);
                        CommandEnd = true;
                    }
                    case "2" -> {
                        AffichTokens();
                        CommandEnd = true;
                    }
                    case "3" -> {
                        System.out.println(Ansi.BLUE + "Choisissez un token à vérifier :\n" + Ansi.SANE);
                        String tokenAverif = scanner.nextLine();
                        int response = verifToken(tokenAverif);
                        switch (response) {
                            case -1 -> System.out.println(Ansi.RED + "ERROR - TOKENVERIF" + Ansi.SANE);
                            case 0 -> System.out.println(Ansi.RED + "Token introuvable." + Ansi.SANE);
                            case 1 -> System.out.println(Ansi.RED + "Token trouvé mais expiré." + Ansi.SANE);
                            case 2 ->
                                    System.out.println(Ansi.GREEN + "Token vérifié correctement et valide!" + Ansi.SANE);
                        }
                        CommandEnd = true;
                    }
                    case "4" -> {
                        LOGGER.info(Ansi.RED + "Fermeture du Thread de commande et sortie de la boucle1");
                        ServerMainThread.stop();
                        End = false;
                    }
                }
                newCommand = false;
            }
        }
    }

    @Override
    public void run()
    {
        try
        {
            SSLServerSocketFactory sslserversocketfactory =
                    (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket sslserversocket =
                    (SSLServerSocket) sslserversocketfactory.createServerSocket(29170);
            SSLSocket sslsocket = (SSLSocket) sslserversocket.accept();
            LOGGER.info(Ansi.GREEN + "Connexion au client console réussi !");
            InputStream inputstream = sslsocket.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            OutputStream outputstream = sslsocket.getOutputStream();
            OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream);
            BufferedWriter bufferedwriter = new BufferedWriter(outputstreamwriter);
            LOGGER.info(Ansi.CYAN  + "Reading ...");
            String IBANBF = bufferedreader.readLine();
            LOGGER.info("IBAN:" +IBANBF);
            String DateBF = bufferedreader.readLine();
            LOGGER.info("DATE_EXPI:" +DateBF);
            Date dateExpi = new SimpleDateFormat("dd-MM-yyyy").parse(DateBF);
            String responseTosend = AddTokenToJSON(dateExpi, IBANBF);
            LOGGER.info("TokenBody:" +responseTosend);
            bufferedwriter.write(responseTosend + '\n');
            bufferedwriter.flush();
            sslsocket.close();

        } catch (Exception exception) {
            exception.printStackTrace();
            LOGGER.error(exception.toString());
        }
    }

    public static String AddTokenToJSON(Date date, String IBAN)
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        File file = new File("src\\main\\java\\ACS_Server\\json\\token.json");

        LocalDate localDate = LocalDate.now();
        Date dateStart = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        TokenBody tmpWrite= new TokenBody(dateStart, date , IBAN);
        LOGGER.info(Ansi.GREEN + "TokenUID: " + tmpWrite.getTokenID().toString());
        LOGGER.info(Ansi.GREEN + "IBAN: " + tmpWrite.getIBAN());
        LOGGER.info(Ansi.GREEN + "TokenBody: " + tmpWrite.getTokenBody());
        LOGGER.info(Ansi.GREEN + "TokenStart: " + tmpWrite.getDateCrea().toString());
        LOGGER.info(Ansi.GREEN + "Date Expiration: " + tmpWrite.getDateExpi().toString());
        List<TokenBody> myObjects = new ArrayList<>();
        try {
            if(file.length() !=0)
            {
                myObjects = mapper.readValue(file, new TypeReference<List<TokenBody>>() {
                });
            }
            myObjects.add(tmpWrite);
            PrintWriter writer = new PrintWriter(file);
            writer.print("");
            writer.close();
            mapper.writeValue(file, myObjects);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tmpWrite.getTokenBody();
    }

    public static int verifToken(String tokenVer)
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        File file = new File("src\\main\\java\\ACS_Server\\json\\token.json");
        try {
            List<TokenBody> myObjectsRead;
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

    public static void AffichTokens()
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        File file = new File("src\\main\\java\\ACS_Server\\json\\token.json");
        try {
            List<TokenBody> myObjectsRead;
            if (file.length() == 0) {
                LOGGER.error("FICHIER JSON VIDE.");
            } else {
                myObjectsRead = mapper.readValue(file, new TypeReference<List<TokenBody>>() {
                });
                for (TokenBody en : myObjectsRead) {
                    LOGGER.info(Ansi.BLUE + "-----------------------------------------------" + Ansi.SANE);
                    LOGGER.info(Ansi.GREEN + "TokenUID: " + en.getTokenID().toString());
                    LOGGER.info(Ansi.GREEN + "IBAN: " + en.getIBAN());
                    LOGGER.info(Ansi.GREEN + "TokenBody: " + en.getTokenBody());
                    LOGGER.info(Ansi.GREEN + "TokenStart: " + en.getDateCrea().toString());
                    LOGGER.info(Ansi.GREEN + "TokenExpire: " + en.getDateExpi().toString() + Ansi.SANE);
                }
            }
        } catch (IOException e) {
            LOGGER.error(Ansi.RED + e + Ansi.SANE);
            throw new RuntimeException(e);
        }
    }
}
