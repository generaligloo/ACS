package ACS_Server;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Scanner;
import ACS_Server.util.Ansi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.*;

public class ACS_Server_Main implements Runnable
{
    private final static Logger LOGGER = LoggerFactory.getLogger(ACS_Server_Main.class);
    static String command;
    static volatile boolean newCommand = false;
    static volatile boolean CommandEnd = true;
    static boolean End = true;

    public static void main(String[] args)
    {
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
                        String test_token = generateSafeToken();
                        LOGGER.info(Ansi.GREEN + "Test: " + test_token);
                        CommandEnd = true;
                        break;
                    case "2":
                        LOGGER.info(Ansi.BLUE + "--EN CONSTRUCTION--");
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

    private static String generateSafeToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[40];
        random.nextBytes(bytes);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(bytes);
    }

}
