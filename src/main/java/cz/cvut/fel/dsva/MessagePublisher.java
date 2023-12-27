package cz.cvut.fel.dsva;

import com.sun.messaging.ConnectionConfiguration;

import java.util.Random;

import javax.jms.*;

public class MessagePublisher {

	public static void main(String[] args) {
		boolean connected = false;
		// set default values
		int numOfMessages = 10;
		int producerId = 666;
		// reading input data from commandline
		if (args.length == 2) {
			System.out.println("Reading values from commandline ...");
			producerId = Integer.parseInt(args[0]);
			numOfMessages = Integer.parseInt(args[1]);
		} else
			System.out.println("Using default values ...");

		try {
			while (!connected) {

				ConnectionFactory myConnFactory;
				Topic myTopic;
				Random rnd = new Random();

				// #### administered object ####
				// This statement can be eliminated if JNDI is used.
				ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
				((com.sun.messaging.ConnectionFactory) connectionFactory).setProperty(ConnectionConfiguration.imqAddressList, "mq://localhost:7676,mq://192.168.18.44:7677");

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
				for (int i = 0; i < numOfMessages; i++) {

					TextMessage myTextMsg = mySess.createTextMessage();
					if (i % 2 == 0) {
						myTextMsg.setText("Change|"+ 0 +"|alfa|"+i+"|Message from producer-" + producerId);
					} else {
						myTextMsg.setText("Change|"+ 0 +"|beta|"+i+"|Message from producer-" + producerId);
					}

					System.out.println("Sending Message: " + myTextMsg.getText());
					myMsgProducer.send(myTextMsg);
//				myTextMsg.setText("Message to topic " + i);
//				topicProducer.send(myTextMsg);

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
        }
    }
}
