package cz.cvut.fel.dsva;

import javax.jms.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class MessageReceiverThread implements Runnable {
    private final MessageConsumer consumer;
    private final String name;
    private final ConnectionFactory myConnFactory = new com.sun.messaging.ConnectionFactory();
    private final Connection myConn = myConnFactory.createConnection();
    private final Session topicSession = myConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
    private final Destination topic = topicSession.createTopic("GlobalTopic");
    private final MessageProducer topicProducer = topicSession.createProducer(topic);
    private Integer IDofThisNode;
    private static boolean free = true;
    public MessageReceiverThread(MessageConsumer consumer, String name, Integer ID) throws JMSException {
        this.consumer = consumer;
        this.name = name;
        this.IDofThisNode = ID;
    }

    public static void setFree(){
        free = true;
    }
    public static void setOccupied(){
        free = false;
    }


    @Override
    public void run() {
        try {
            while (true) {
                // Receive a message
                if(free){
                    Message message = consumer.receive();

                    if (message instanceof TextMessage) {
                        free = false;
                        TextMessage txtMsg = (TextMessage) message;
                        String messageText = txtMsg.getText();
                        if(messageText.contains("|")){

                            String[] parts = messageText.split("\\|");
                            TextMessage myTextMsg = topicSession.createTextMessage();
                            myTextMsg.setText(parts[0] +"|"+IDofThisNode+"|"+parts[1]);
                            System.out.println(name + " received: " + parts[0] +"|"+IDofThisNode+"|"+parts[1]);
                            topicProducer.send(myTextMsg);
                        }
                    }

                }
                }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

//Critical section
//String fileName = "number.txt"; // Replace with your file name
//int incrementValue = 5; // Replace with the desired increment or decrement value
//
//        try {
//// Step 1: Read the Number from the File
//BufferedReader reader = new BufferedReader(new FileReader(fileName));
//String line = reader.readLine();
//int currentNumber = Integer.parseInt(line);
//            reader.close();
//
//// Step 2: Perform Increment or Decrement
//currentNumber += incrementValue;
//
//// Step 3: Write the Updated Number Back to the File
//BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false)); // 'false' for overwriting
//            writer.write(Integer.toString(currentNumber));
//        writer.close();
//
//            System.out.println("Updated number: " + currentNumber);