package cz.cvut.fel.dsva;

import com.sun.messaging.ConnectionConfiguration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.jms.*;

public class MessageSubscriber {
	private static Logger logger = Logger.getLogger("MyLogger");
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
			String interfaceName = "enp0s1";
			String ipv4Address = null;

			try {
				NetworkInterface specificInterface = NetworkInterface.getByName(interfaceName);
				if (specificInterface != null) {
					Enumeration<InetAddress> interfaceAddresses = specificInterface.getInetAddresses();

					while (interfaceAddresses.hasMoreElements()) {
						InetAddress address = interfaceAddresses.nextElement();
						if (address.getHostAddress().length() > 0) {
							if (address instanceof java.net.Inet4Address) {
								ipv4Address = address.getHostAddress();
								break;
							}
						}
					}
				}
				String ipWithoutDots = ipv4Address.replace(".", "");
				int index = ipWithoutDots.length() - 9;
				ID = ipWithoutDots.substring(index);
				logger.info("Set ID from last nine numbers of IP address: " + ID);
			} catch (Exception e) {
				logger.severe("This node was unable to connect to JSM.");
			}
		} else {
			ID = args[0];
			logger.info("Set ID from program arguments: " + ID);
		}
		try {
			String nameOfFile = "subscriber_" + ID + ".log";
			FileHandler fileHandler = new FileHandler(nameOfFile);
			logger.addHandler(fileHandler);
			fileHandler.setFormatter(new SimpleFormatter());
		} catch (IOException e) {
			logger.severe("This node was unable to connect to JSM.");
		}
		Req.put(String.valueOf(ID), false);

		while (!connected) {
			try {
				ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
				((com.sun.messaging.ConnectionFactory) connectionFactory).setProperty(ConnectionConfiguration.imqAddressList, "mq://192.168.18.44:7676,mq://192.168.18.44:7677");
				logger.info("Tried to connect to JMS.");
				// Create a connection to the JMS
				Connection myConn = connectionFactory.createConnection();
				logger.info("Connected to JMS.");
				// Create a session within the connection.
				Session instructionSession = myConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
				Session topicSession = myConn.createSession(false, Session.AUTO_ACKNOWLEDGE);

				// Topic for instructions
				Destination topicOfInstructions = instructionSession.createTopic("TopicOfInstructions");
				// Topic for communication
				Destination topicRiAg = topicSession.createTopic("RiAgTopic");
				logger.info("Connected to JMS topics - TopicOfInstructions and RiAgTopic.");

				// Create a message consumer and producer.
				MessageConsumer instructionConsumer = instructionSession.createConsumer(topicOfInstructions);
				MessageConsumer topicConsumer = topicSession.createConsumer(topicRiAg);
				MessageProducer topicProducer = topicSession.createProducer(topicRiAg);
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					try {
						TextMessage byeMessage = topicSession.createTextMessage();
						logger.info("Said goodbye to JMS and disconnected.");
						byeMessage.setText("Goodbye|" + ID);
						topicProducer.send(byeMessage);
					} catch (JMSException e) {
						logger.severe("This node was unable to connect to JSM.");
					}

				}));
				// Start the connection
				myConn.start();
				connected = true;
//				System.out.println("Started node with ID: " + ID);
				logger.info("Started node with ID: " + ID);
				TextMessage helloMessage = topicSession.createTextMessage();
				helloMessage.setText("Start|" + ID);
				rebuild = false;
				topicProducer.send(helloMessage);
				logger.info("Sent hello message to nodes.");
//				timer = new Timer();
//				timer.scheduleAtFixedRate(new RebuildTopology(),0,10000);
				while (true) {
					// Receive a message
					Message message;
					String messageText;
					String[] parts = new String[0];

					if (keyToWrite == null) {
						if (timer == null) {
							timer = new Timer();
							timer.scheduleAtFixedRate(new RebuildTopology(), 20000, 20000);
						}

						message = instructionConsumer.receiveNoWait();
						// Process the message
						if (message != null) {
							if (timer != null) {
								timer.cancel();
								timer = null;
							}

							TextMessage txtMsg = (TextMessage) message;
							messageText = txtMsg.getText();
//							System.out.println("Consumed message from instructionTopic" + messageText + "&& set action");
							logger.info("Consumed message from instructionTopic" + messageText + "&& set action");
							parts = messageText.split("\\|");
							keyToWrite = parts[1];
							valueToStore = parts[2];


						} else {
							message = topicConsumer.receiveNoWait();
							if (message != null) {
								TextMessage txtMsg = (TextMessage) message;
								messageText = txtMsg.getText();
								parts = messageText.split("\\|");
//								System.out.println("Received message from topic " + messageText);
								logger.info("Received message from topic " + messageText);
							}
						}
					} else {
						message = topicConsumer.receive();
						TextMessage txtMsg = (TextMessage) message;
						messageText = txtMsg.getText();
						parts = messageText.split("\\|");
//						System.out.println("Received message from topic " + messageText);
						logger.info("Received message from topic " + messageText);
					}


					if (message instanceof TextMessage) {
//						Thread.sleep(500);

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
								topicProducer.send(replyText);
							}

						} else if (parts[0].equals("Reply") && parts[1].equals(ID)) {
							RpCnt = RpCnt + 1;
						}else if (parts[0].equals("GetValue")) {
							logger.info("Read value stored on node:" + ID + " with key "+parts[1]+" value = "+dataStore.get(parts[1]));
						}  else if (parts[0].equals("Goodbye")) {
							Req.remove(parts[1]);
//							System.out.println("Received goodbye message from: " + parts[1]);
							logger.info("Received goodbye message from: " + parts[1]);
						} else if (parts[0].equals("Start") && !parts[1].equals(ID)) {
							if (!Req.containsKey(parts[1])) {
								Req.put(parts[1], false);
							} else {
								boolean previousNodeState = Req.get(parts[1]);
								Req.put(parts[1], previousNodeState);
							}
							TextMessage replyText = topicSession.createTextMessage();
							replyText.setText("Confirm|" + ID);
							topicProducer.send(replyText);
//							System.out.println("Total number of members is now: " + Req.size());
							logger.info("Total number of members is now: " + Req.size());
//								System.out.println("I confirmed");

						} else if (parts[0].equals("Confirm")) {
							if (!parts[1].equals(ID)) {
								Req.put(parts[1], false);
//								System.out.println("Total number of members is now: " + Req.size());
								logger.info("Total number of members is now: " + Req.size());
							}
						}
					}

					if (Objects.equals(RpCnt, Req.size() - 1) && keyToWrite != null && !rebuild) {

//						System.out.println("-------------------------");
//						System.out.println("Entered critical section.");
						logger.info("Entered critical section.");
						dataStore.put(keyToWrite, valueToStore);
//						System.out.println("Key " + keyToWrite + " = " + valueToStore);
						logger.info("Key " + keyToWrite + " = " + valueToStore);
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
							}
						}
						logger.info("Left critical section.");
//						System.out.println("Left critical section.");
//						System.out.println("-------------------------");
					} else {
						if (timer == null) {
							timer = new Timer();
							timer.scheduleAtFixedRate(new RebuildTopology(), 20000, 20000);
						}
					}

				}


			} catch (JMSException e) {
				logger.severe("This node was unable to connect to JSM.");
				connected = false;
				rebuild = true;
				// Optionally add a delay before reconnecting
				try {
					Thread.sleep(3000); // Wait for 3 seconds before retrying
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				}
			}
//			catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
        }

	}

	static class RebuildTopology extends TimerTask {
		@Override
		public void run() {
			if(!rebuild){
//				System.out.println("REBUILDING");
				logger.info("Rebuilding topology.");
				// Clear the Req HashMap
				Req.clear();
				Req.put(String.valueOf(ID), false);
				rebuild = true;
				// Send a different message to the topic
				try {
					ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
					((com.sun.messaging.ConnectionFactory) connectionFactory).setProperty(ConnectionConfiguration.imqAddressList, "mq://192.168.18.44:7676,mq://192.168.18.44:7677");
					// Create a connection to the JMS
					Connection myConn = connectionFactory.createConnection();
					// Create a session within the connection
					Session topicSession = myConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
					// Topic for communication
					Destination topicRiAg = topicSession.createTopic("RiAgTopic");
					// Create a message producer
					MessageProducer topicProducer = topicSession.createProducer(topicRiAg);
					TextMessage helloMessage = topicSession.createTextMessage();
					helloMessage.setText("Start|" + ID);
					topicProducer.send(helloMessage);
					rebuild = false;
					myConn.close();
//				System.out.println("FINISHED REBUILDING");
					logger.info("Topology was rebuilt.");
				} catch (JMSException e) {
					logger.severe("This node was unable to connect to JSM.");
				}
			}
		}
	}
}


