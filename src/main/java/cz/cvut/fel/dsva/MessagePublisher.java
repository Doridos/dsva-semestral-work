package cz.cvut.fel.dsva;

import com.sun.messaging.ConnectionConfiguration;

import java.util.Random;

import javax.jms.*;

public class MessagePublisher {

	public static void main(String[] args) {
		boolean connected = false;
		// set default values
		int numOfMessages = 8;
		int producerId = 666;
		// reading input data from commandline
		if (args.length == 2) {
			System.out.println("Reading values from commandline ...");
			producerId = Integer.parseInt(args[0]);
			numOfMessages = Integer.parseInt(args[1]);
		} else
			System.out.println("Using default values ...");
		char[] letters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X'};
		int numOfLetter = 0;

		try {
			while (!connected) {

				ConnectionFactory myConnFactory;
				Topic myTopic;


				// #### administered object ####
				// This statement can be eliminated if JNDI is used.
				ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
				((com.sun.messaging.ConnectionFactory) connectionFactory).setProperty(ConnectionConfiguration.imqAddressList, "mq://192.168.18.44:7676,mq://192.168.18.44:7677");

				// Create a connection to the JMS
				Connection myConn = connectionFactory.createConnection();

				// Create a session within the connection.
				Session mySess = myConn.createSession(false, Session.AUTO_ACKNOWLEDGE);

				// Instantiate a JMS Queue Destination
				// This statement can be eliminated if JNDI is used.
				myTopic = mySess.createTopic("TopicOfInstructions");

				// #### Client ####
				// Create a message producer.
				MessageProducer myMsgProducer = mySess.createProducer(myTopic);

				// Create and send a message to the queue.
				for (int j = 0; j < 4; j++) {
					for (int i = 0; i < numOfMessages; i++) {
						if (numOfLetter == 24) {
							numOfLetter = 0;
						}
						TextMessage myTextMsg = mySess.createTextMessage();

						myTextMsg.setText("Change|" + i + "|" + letters[numOfLetter] + "|Message from producer-" + producerId);


						System.out.println("Sending Message: " + myTextMsg.getText());
						myMsgProducer.send(myTextMsg);
						numOfLetter += 1;


//				myTextMsg.setText("Message to topic " + i);
//				topicProducer.send(myTextMsg);

					}
					Thread.sleep(10000);
				}

				connected = true;


				// Close the session and connection resources.
				mySess.close();
				myConn.close();

			}
		}  catch (JMSException e) {
			e.printStackTrace();
			connected = false;
			// Optionally add a delay before reconnecting
			try {
				Thread.sleep(10000); // Wait for 10 seconds before retrying
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
