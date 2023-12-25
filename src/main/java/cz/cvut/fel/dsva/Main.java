package cz.cvut.fel.dsva;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    private static int sharedCounter = 0;

    public static void main(String[] args) {
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 100000; i++)
            synchronized (Main.class){
                sharedCounter++; // Increment sharedCounter without synchronization
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 100000; i++)
                synchronized (Main.class){
                sharedCounter--; // Decrement sharedCounter without synchronization
            }
        });

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Final value of sharedCounter: " + sharedCounter);
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
