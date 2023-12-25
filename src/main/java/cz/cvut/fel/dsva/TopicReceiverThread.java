package cz.cvut.fel.dsva;

import com.sun.messaging.ConnectionConfiguration;

import javax.jms.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class TopicReceiverThread implements Runnable {
    private  MessageConsumer consumer;
    private MessageConsumer instructionConsumer;
    private String name;
    private final Integer MEMBER_COUNT = 2;

    private ConnectionFactory myConnFactory;
    private Connection myConn;
    private Session topicSession;
    private Destination topic;
    private MessageProducer topicProducer;

    Integer MyRq;
    Integer MaxRq;
    Integer RpCnt = 0;
    Integer ID;
    String action = null;
    List<Boolean> Req = new ArrayList<>();

    boolean free = true;

    public TopicReceiverThread(ConnectionFactory connectionFactory, MessageConsumer consumer, MessageConsumer instructionConsumer, String name, Integer ID) throws JMSException {
        this.myConnFactory = connectionFactory;
        this.myConn = myConnFactory.createConnection();
        this.topicSession = myConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        this.topic = topicSession.createTopic("GlobalTopic");
        this.topicProducer = topicSession.createProducer(topic);
        this.consumer = consumer;
        this.instructionConsumer = instructionConsumer;
        this.name = name;
        this.MaxRq = 0;
        this.MyRq = 0;
        this.ID = ID;
        for (int i = 0; i < 3; i++) {
            this.Req.add(false);
        }
    }


    @Override
    public void run() {
        try {
            while (true) {
                // Receive a message
                Message message;
                String messageText;
                String[] parts = new String[0];

                if (action == null){
                    message = instructionConsumer.receive();
                    if (message instanceof TextMessage) {
                        TextMessage txtMsg = (TextMessage) message;
                        messageText = txtMsg.getText();
                        System.out.println("Consumed message from instructionTopic" + messageText +"&& set action");

                        parts = messageText.split("\\|");
                        action = parts[1];
                        free = false;
                    }
                }
                else {
                    message = consumer.receive();
                    TextMessage txtMsg = (TextMessage) message;
                    messageText = txtMsg.getText();
                    parts = messageText.split("\\|");
                    System.out.println("Recieved message from topic " + messageText);
                }


                if (message instanceof TextMessage) {


                    if (parts[0].equals("Change")) {

                        MessageReceiverThread.setOccupied();
                        Req.set(ID, true);
                        MyRq = MaxRq + 1;
                        RpCnt = 0;
                        for (int j = 0; j < MEMBER_COUNT; j++) {
                            if (j != ID) {
                                TextMessage requestText = topicSession.createTextMessage();
                                requestText.setText("Request|" + MyRq + "|" + ID);
                                topicProducer.send(requestText);
                            }
                        }
                    } else if (parts[0].equals("Request") && !Integer.valueOf(parts[2]).equals(ID)) {
                        MaxRq = MaxRq > Integer.valueOf(parts[1]) ? MaxRq : Integer.valueOf(parts[1]);
                        if(Req.get(ID) && (Integer.valueOf(parts[1]) > MyRq || Integer.valueOf(parts[2]) > ID)){
                            Req.set(Integer.valueOf(parts[2]),true);
                        }
                        else {
                            TextMessage replyText = topicSession.createTextMessage();
                            replyText.setText("Reply|" + Integer.valueOf(parts[2]));
                            topicProducer.send(replyText);
                        }

                    } else if(parts[0].equals("Reply") && Integer.valueOf(parts[1]).equals(ID)){
                        RpCnt = RpCnt+1;
                    }
                }

                if (Objects.equals(RpCnt, MEMBER_COUNT-1)) {

                    String fileName = "src/main/java/cz/cvut/fel/dsva/number.txt"; // Replace with your file name

                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(fileName));
                        String line = reader.readLine();
                        int currentNumber = Integer.parseInt(line);
                        reader.close();

                        if(action.equals("Decrement")){
                            currentNumber -= 1;
                            System.out.println(MyRq + ". Decremented number: " + currentNumber);
                        }
                        else if(action.equals("Increment")){
                            currentNumber += 1;
                            System.out.println(MyRq + ". Incremented number: " + currentNumber);
                        }

                        action = null;
                        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
                        writer.write(Integer.toString(currentNumber));
                        writer.close();




                        Req.set(ID, false);
                        for (int j = 0; j < MEMBER_COUNT; j++) {
                            if(Req.get(j)){
                                Req.set(j, false);
                                TextMessage replyText = topicSession.createTextMessage();
                                replyText.setText("Reply|" + j);
                                topicProducer.send(replyText);
                            }
                        }


                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}