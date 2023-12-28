package cz.cvut.fel.dsva;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.concurrent.locks.ReentrantLock;

//For testing purposes only.
public class Main {
    private static int sharedCounter = 0;

    public static void main(String[] args) {
        System.out.println(Integer.valueOf("1168181114"));


    }
}

//public static void main(String[] args) {
//    String fileName = "number.txt"; // Replace with your file name
//    int incrementValue = 5; // Replace with the desired increment or decrement value
//
//    try {
//        // Step 1: Read the Number from the File
//        BufferedReader reader = new BufferedReader(new FileReader(fileName));
//        String line = reader.readLine();
//        int currentNumber = Integer.parseInt(line);
//        reader.close();
//
//        // Step 2: Perform Increment or Decrement
//        currentNumber += incrementValue;
//
//        // Step 3: Write the Updated Number Back to the File
//        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false)); // 'false' for overwriting
//        writer.write(Integer.toString(currentNumber));
//        writer.close();
//
//        System.out.println("Updated number: " + currentNumber);
//    } catch (IOException e) {
//        e.printStackTrace();
//    }
//}
//}
