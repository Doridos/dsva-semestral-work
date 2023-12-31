package cz.cvut.fel.dsva;

import com.sun.messaging.ConnectionConfiguration;

import java.util.Random;

import javax.jms.*;

public class MessagePublisher {

	public static void main(String[] args) {
		boolean connected = false;
		int numOfMessages = 8;
		int producerId = 777;
		String action = null;
		if (args.length > 0) {
			System.out.println("Reading values from commandline ...");

			if(args.length ==1){
				action = args[0];
			}else {
				producerId = Integer.parseInt(args[0]);
				action = args[1];
			}

		} else{
			System.out.println("Using default values ...");
			 action = "write";
		}

		char[] letters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X'};
		int numOfLetter = 0;

		try {
			while (!connected) {

				Topic myTopic;

				ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
				((com.sun.messaging.ConnectionFactory) connectionFactory).setProperty(ConnectionConfiguration.imqAddressList, "mq://192.168.18.44:7676,mq://192.168.18.44:7677");

				// Create a connection to the JMS
				Connection myConn = connectionFactory.createConnection();

				// Create a session within the connection.
				Session mySess = myConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
				Session topicSession = myConn.createSession(false, Session.AUTO_ACKNOWLEDGE);
				connected = true;
				// Topic for instructions
				// Topic for communication
				Destination topicRiAg = topicSession.createTopic("RiAgTopic");
				// Instantiate a JMS Queue Destination
				myTopic = mySess.createTopic("TopicOfInstructions");

				// Create a message producer.
				MessageProducer topicProducer = mySess.createProducer(topicRiAg);
				MessageProducer instructionProducer = mySess.createProducer(myTopic);

					for (int j = 0; j < 3; j++) {
					if(action.equals("write")) {
						for (int i = 0; i < numOfMessages; i++) {
							if (numOfLetter == 24) {
								numOfLetter = 0;
							}
							TextMessage myTextMsg = mySess.createTextMessage();

							myTextMsg.setText("Change|" + i + "|" + letters[numOfLetter] + "|Message from producer-" + producerId);


							System.out.println("Sending Message: " + myTextMsg.getText());
							instructionProducer.send(myTextMsg);
							numOfLetter += 1;


//				myTextMsg.setText("Message to topic " + i);
//				topicProducer.send(myTextMsg);

						}
					}else if (action.equals("read")) {

						for (int i = 0; i < numOfMessages; i++) {

							TextMessage myTextMsg = mySess.createTextMessage();

							myTextMsg.setText("GetValue|" + i);
							System.out.println("Sending Message: " + myTextMsg.getText());
							topicProducer.send(myTextMsg);
						}

					}
					// Create and send a message to the topic
					Thread.sleep(10000);
				}



				// Close the session and connection resources
				mySess.close();
				myConn.close();

			}
		}  catch (JMSException e) {
			e.printStackTrace();
			connected = false;
			try {
				Thread.sleep(10000);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
