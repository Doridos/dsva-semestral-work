package cz.cvut.fel.dsva;

import javax.jms.*;
import java.io.*;
import java.util.*;

class TopicReceiverThread implements Runnable {
    private  MessageConsumer consumer;
    private MessageConsumer instructionConsumer;
    private Dictionary<String,String> dataStore = new Hashtable<>();
    private final Integer MEMBER_COUNT = 3;
    private Session topicSession;
    private MessageProducer topicProducer;

    Integer MyRq;
    Integer MaxRq;
    Integer RpCnt = 0;
    Integer ID;
    String keyToWrite = null;
    String valueToStore = null;
    List<Boolean> Req = new ArrayList<>();



    public TopicReceiverThread(Session topicSession,MessageProducer topicProducer, MessageConsumer consumer, MessageConsumer instructionConsumer, Integer ID) throws JMSException {
        this.topicSession = topicSession;
        this.topicProducer = topicProducer;
        this.consumer = consumer;
        this.instructionConsumer = instructionConsumer;
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

                if (keyToWrite == null){
                    message = instructionConsumer.receive();
                    if (message instanceof TextMessage) {
                        TextMessage txtMsg = (TextMessage) message;
                        messageText = txtMsg.getText();
                        System.out.println("Consumed message from instructionTopic" + messageText +"&& set action");

                        parts = messageText.split("\\|");
                        keyToWrite = parts[1];
                        valueToStore = parts[2];
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
                        Req.set(ID, true);
                        MyRq = MaxRq + 1;
                        RpCnt = 0;

                        TextMessage requestText = topicSession.createTextMessage();
                        requestText.setText("Request|" + MyRq + "|" + ID);
                        topicProducer.send(requestText);

                    } else if (parts[0].equals("Request") && !Integer.valueOf(parts[2]).equals(ID)) {
                        MaxRq = MaxRq > Integer.valueOf(parts[1]) ? MaxRq : Integer.valueOf(parts[1]);
                        if(Req.get(ID) && (Integer.valueOf(parts[1]) > MyRq ||(Objects.equals(Integer.valueOf(parts[1]), MyRq) && Integer.valueOf(parts[2]) > ID))){
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


                    dataStore.put(keyToWrite, valueToStore);
                    System.out.println(MyRq + "Wrote to key number: " + keyToWrite + " value of:" + valueToStore);

                    keyToWrite = null;

                    Req.set(ID, false);
                    for (int j = 0; j < MEMBER_COUNT; j++) {
                        if(Req.get(j)){
                            Req.set(j, false);
                            TextMessage replyText = topicSession.createTextMessage();
                            replyText.setText("Reply|" + j);
                            topicProducer.send(replyText);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}