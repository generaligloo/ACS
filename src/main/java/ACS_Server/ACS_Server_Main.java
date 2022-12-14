package ACS_Server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

import ACS_Server.util.Ansi;

import ACS_Server.util.TokenBody;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.*;
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
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        File file = new File("src\\main\\java\\ACS_Server\\json\\token.json");
        LOGGER.info(Ansi.GREEN+ "Starting Main Thread ...");
        ACS_Server_Main ServerMain = new ACS_Server_Main();
        Thread ServerMainThread = new Thread(ServerMain);
        ServerMainThread.start();
        while(End)
        {
            //server listening ici "normalement"
            if (newCommand)
            {
                LOGGER.info(Ansi.GREEN+ "Listening command ...");
                LOGGER.info("command: " + command);
                switch (command)
                {
                    case "1":
                        System.out.println("dd-mm-yyyy");
                        Scanner scanner = new Scanner(System.in);
                        String date = scanner.nextLine();
                        Date dateExpi= null;
                        try {
                            dateExpi = new SimpleDateFormat("dd-MM-yyyy").parse(date);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                        LOGGER.info(dateExpi.toString());
                        LocalDate localDate = LocalDate.now();
                        Date dateStart = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                        TokenBody tmpWrite= new TokenBody(dateStart, dateExpi);
                        LOGGER.info(Ansi.GREEN + "TokenUID: " + tmpWrite.getTokenID().toString());
                        LOGGER.info(Ansi.GREEN + "TokenBody: " + tmpWrite.getTokenBody());
                        LOGGER.info(Ansi.GREEN + "TokenStart: " + tmpWrite.getDateCrea().toString());
                        LOGGER.info(Ansi.GREEN + "Test: " + tmpWrite.getDateExpi().toString());
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
                        CommandEnd = true;
                        break;
                    case "2":
                        TokenBody tmpRead = null;
                        try {
                            List<TokenBody> myObjectsRead = new ArrayList<>();
                            if(file.length() == 0)
                            {
                                LOGGER.error("FICHIER JSON VIDE.");
                            }
                            else {
                                myObjectsRead = mapper.readValue(file, new TypeReference<List<TokenBody>>() {
                                });
                                for (TokenBody en : myObjectsRead) {
                                    LOGGER.info(Ansi.BLUE + "-----------------------------------------------");
                                    LOGGER.info(Ansi.GREEN + "TokenUID: " + en.getTokenID().toString());
                                    LOGGER.info(Ansi.GREEN + "TokenBody: " + en.getTokenBody());
                                    LOGGER.info(Ansi.GREEN + "TokenStart: " + en.getDateCrea().toString());
                                    LOGGER.info(Ansi.GREEN + "TokenExpire: " + en.getDateExpi().toString());
                                }
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        CommandEnd = true;
                        break;
                    case "3":
                        LOGGER.info(Ansi.RED + "Fermeture du Thread de commande et sortie de la boucle1");
                        ServerMainThread.stop();
                        End = false;
                        break;
                }
                newCommand = false;
            }
        }
    }

    @Override
    public void run()
    {
        Scanner scanner = new Scanner(System.in);
        while(true)
        {
            if(CommandEnd) {
                System.out.println(Ansi.BLUE + "Choisissez une commande :\n");
                System.out.println(Ansi.CYAN + "- 1.Generate Token ");
                System.out.println(Ansi.CYAN + "- 2.View tokens ");
                System.out.println(Ansi.CYAN + "- 3.Quitter ");
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
        }
    }



}
