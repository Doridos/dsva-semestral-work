package cz.cvut.fel.dsva;

import com.sun.messaging.ConnectionConfiguration;

import java.util.*;

import javax.jms.*;

public class MessageSubscriber {

	private static MessageSubscriber instance;
	private static HashMap<String, Boolean> Req = new HashMap<>();
	private static String ID;
	private static boolean rebuild = false;
	private static Timer timer = null;


	public static void main(String[] args) {
		Dictionary<String, String> dataStore = new Hashtable<>();
		Integer MyRq = 0;
		Integer numberOfOperations = 0;
		Integer MaxRq = 0;
		Integer RpCnt = 0;
		String keyToWrite = null;
		String valueToStore = null;


		boolean connected = false;


		if (args.length < 1) {
			ID = String.valueOf(0);
		} else {
			ID = args[0];
		}

		Req.put(String.valueOf(ID), false);

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

				TextMessage helloMessage = topicSession.createTextMessage();
				helloMessage.setText("Start|" + ID);
				topicProducer.send(helloMessage);
				while (true) {
					// Receive a message
					Message message;
					String messageText;
					String[] parts = new String[0];

					if (keyToWrite == null) {
						message = instructionConsumer.receiveNoWait();
						if (message != null) {
							TextMessage txtMsg = (TextMessage) message;
							messageText = txtMsg.getText();
							System.out.println("Consumed message from instructionTopic" + messageText + "&& set action");

							parts = messageText.split("\\|");
							keyToWrite = parts[1];
							valueToStore = parts[2];

							if(timer != null){
								timer.cancel();
							}
							timer = new Timer();
							timer.schedule(new RebuildTopology(), 5000);

						} else {
							message = topicConsumer.receiveNoWait();
							if (message != null) {
								TextMessage txtMsg = (TextMessage) message;
								messageText = txtMsg.getText();
								parts = messageText.split("\\|");
								System.out.println("Received message from topic " + messageText);
							}
						}
					} else {

						message = topicConsumer.receive();
						TextMessage txtMsg = (TextMessage) message;
						messageText = txtMsg.getText();
						parts = messageText.split("\\|");
						System.out.println("Received message from topic " + messageText);
					}


					if (message instanceof TextMessage) {


						if (parts[0].equals("Change")) {
							Req.put(ID, true);
							MyRq = MaxRq + 1;
							RpCnt = 0;

							TextMessage requestText = topicSession.createTextMessage();
							requestText.setText("Request|" + MyRq + "|" + ID);
							topicProducer.send(requestText);

						} else if (parts[0].equals("Request") && !parts[2].equals(ID)) {
							MaxRq = MaxRq > Integer.valueOf(parts[1]) ? MaxRq : Integer.valueOf(parts[1]);
							if (Req.get(ID) && (Integer.valueOf(parts[1]) > MyRq || (Objects.equals(Integer.valueOf(parts[1]), MyRq) && Integer.valueOf(parts[2]) > Integer.valueOf(ID)))) {
								Req.put(parts[2], true);
							} else {
								TextMessage replyText = topicSession.createTextMessage();
								replyText.setText("Reply|" + Integer.valueOf(parts[2]));
								System.out.println("Sent Reply due to priority");
								topicProducer.send(replyText);
							}

						} else if (parts[0].equals("Reply") && parts[1].equals(ID)) {
							RpCnt = RpCnt + 1;
						} else if (parts[0].equals("Start") && !parts[1].equals(ID)) {
							if (!Req.containsKey(parts[1])) {
								Req.put(parts[1], false);
								TextMessage replyText = topicSession.createTextMessage();
								replyText.setText("Confirm|" + ID);
								topicProducer.send(replyText);
								System.out.println("I confirmed");
							}

						} else if (parts[0].equals("Confirm") && !parts[1].equals(ID)) {
							Req.put(parts[1], false);
							System.out.println(Req.size());
						}
					}

					if (Objects.equals(RpCnt, Req.size() - 1) && keyToWrite != null && !rebuild) {
						dataStore.put(keyToWrite, valueToStore);
						System.out.println(MyRq + "Wrote to key number: " + keyToWrite + " value of:" + valueToStore);
						numberOfOperations += 1;
						keyToWrite = null;
						RpCnt = 0;
						Req.put(ID, false);
						for (String key : Req.keySet()) {
							if (Req.get(key) == true) {
								Req.put(key, false);
								TextMessage replyText = topicSession.createTextMessage();
								replyText.setText("Reply|" + key);
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

	static class RebuildTopology extends TimerTask {
		@Override
		public void run() {
			System.out.println("REBUILDING");
			// Clear the Req HashMap
			Req.clear();
			Req.put(String.valueOf(ID), false);
			rebuild = true;
			// Send a different message to the topic
			try {
				// #### administered object ####
				// This statement can be eliminated if JNDI is used.
				ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
				((com.sun.messaging.ConnectionFactory) connectionFactory).setProperty(ConnectionConfiguration.imqAddressList, "mq://192.168.18.44:7676,mq://192.168.18.44:7677");
				// Create a connection to the JMS
				Connection myConn = connectionFactory.createConnection();
				// Create a session within the connection.
				Session topicSession = myConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
				// Instantiate a JMS Queue Destination
				// This statement can be eliminated if JNDI is used.
				//Topic for communication
				Destination topicRiAg = topicSession.createTopic("RiAgTopic");
				// #### Client ####
				// Create a message consumer.
				MessageProducer topicProducer = topicSession.createProducer(topicRiAg);
				TextMessage helloMessage = topicSession.createTextMessage();
				helloMessage.setText("Start|" + ID);
				topicProducer.send(helloMessage);
				rebuild = false;

			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}
}

