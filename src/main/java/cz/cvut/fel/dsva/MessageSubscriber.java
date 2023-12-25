package cz.cvut.fel.dsva;

import com.sun.messaging.ConnectionConfiguration;

import java.util.*;

import javax.jms.*;

public class MessageSubscriber {




	public static void main(String[] args) {
		Dictionary<String,String> dataStore = new Hashtable<>();
		Integer MEMBER_COUNT = 6;
		Integer MyRq = 0;
		Integer numberOfOperations = 0;
		Integer MaxRq = 0;
		Integer RpCnt = 0;
		String keyToWrite = null;
		String valueToStore = null;
		List<Boolean> Req = new ArrayList<>();

		boolean connected = false;

		Integer ID;
		if (args.length < 1) {
			ID = 0;
		} else {
			ID = Integer.valueOf(args[0]);
		}

		for (int i = 0; i < MEMBER_COUNT; i++) {
			Req.add(false);
		}

		while (!connected) {
			try {
				// #### administered object ####
				// This statement can be eliminated if JNDI is used.
				ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
				((com.sun.messaging.ConnectionFactory) connectionFactory).setProperty(ConnectionConfiguration.imqAddressList, "mq://192.168.18.44:7676,mq://192.168.18.44:7677");

				// Create a connection to the JMS
				Connection myConn = connectionFactory.createConnection();

				// Create a session within the connection.
				Session instructionSession = myConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
				Session topicSession = myConn.createSession(false, Session.AUTO_ACKNOWLEDGE);


				// Instantiate a JMS Queue Destination
				// This statement can be eliminated if JNDI is used.
				Destination topicOfInstructions = instructionSession.createTopic("TopicOfInstructions");
				//Topic for communication
				Destination topicRiAg = topicSession.createTopic("RiAgTopic");


				// #### Client ####
				// Create a message consumer.
				MessageConsumer instructionConsumer = instructionSession.createConsumer(topicOfInstructions);
				MessageConsumer topicConsumer = topicSession.createConsumer(topicRiAg);
				MessageProducer topicProducer = topicSession.createProducer(topicRiAg);

				// Start the Connection.
				myConn.start();
				connected = true;


				while (true) {
					// Receive a message
					Message message;
					String messageText;
					String[] parts = new String[0];

					if (keyToWrite == null){
						message = instructionConsumer.receiveNoWait();
						if (message != null) {
							TextMessage txtMsg = (TextMessage) message;
							messageText = txtMsg.getText();
							System.out.println("Consumed message from instructionTopic" + messageText +"&& set action");

							parts = messageText.split("\\|");
							keyToWrite = parts[1];
							valueToStore = parts[2];
						}
						else {
							message = topicConsumer.receiveNoWait();
							if (message != null) {
								TextMessage txtMsg = (TextMessage) message;
								messageText = txtMsg.getText();
								parts = messageText.split("\\|");
								System.out.println("Received message from topic " + messageText);
							}
						}
					}
					else {
						message = topicConsumer.receive();
						TextMessage txtMsg = (TextMessage) message;
						messageText = txtMsg.getText();
						parts = messageText.split("\\|");
						System.out.println("Received message from topic " + messageText);
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
								System.out.println("Sent Reply due to priority");
								topicProducer.send(replyText);
							}

						} else if(parts[0].equals("Reply") && Integer.valueOf(parts[1]).equals(ID)){
							RpCnt = RpCnt+1;
						}
					}

					if (Objects.equals(RpCnt, MEMBER_COUNT-1)) {


						dataStore.put(keyToWrite, valueToStore);
						System.out.println(MyRq + "Wrote to key number: " + keyToWrite + " value of:" + valueToStore);
						numberOfOperations +=1;
						keyToWrite = null;
						RpCnt = 0;
						Req.set(ID, false);
						for (int j = 0; j < MEMBER_COUNT; j++) {
							if(Req.get(j)){
								Req.set(j, false);
								TextMessage replyText = topicSession.createTextMessage();
								replyText.setText("Reply|" + j);
								topicProducer.send(replyText);
								System.out.println("Replied after message");

							}
						}
					}
				}


			} catch (JMSException e) {
				e.printStackTrace();
				connected = false;
				// Optionally add a delay before reconnecting
				try {
					Thread.sleep(10000); // Wait for 10 seconds before retrying
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				}
			}
		}

	}
}
