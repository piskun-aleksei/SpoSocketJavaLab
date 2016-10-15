import entity.BasicInterface;
import entity.impl.Client;
import entity.impl.Server;

import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Created by Brotorias on 15.10.2016.
 */
public class Main {
    public static void main(String... args){
        BasicInterface actionMaker = null;
        boolean choiceIsCorrect = false;
        System.out.println("Choose your destiny: 1 for server, 2 for client ");
        while(!choiceIsCorrect) {
            int i = Integer.parseInt(new Scanner(System.in).nextLine());
            if (i == 1) {
                actionMaker = new Server();
                choiceIsCorrect = true;
            }
            if (i == 2) {
                actionMaker = new Client();
                choiceIsCorrect = true;
            }
            if (i != 1 && i != 2) {
                System.out.println("Please, specify correct number");
            }
        }
        choiceIsCorrect = false;

        try {
            actionMaker.start();
        } catch (IOException e) {
            System.out.println("Crashed. Oops.");
        }
    }
}
