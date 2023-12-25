package cz.cvut.fel.dsva;

import com.sun.messaging.ConnectionConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.jms.*;

public class MessageSubscriber {

	public static void main(String[] args) {
		boolean connected = false;

		Integer ID;
		if (args.length < 1) {
			ID = 0;
		} else {
			ID = Integer.valueOf(args[0]);
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

//			Thread queueThread = new Thread(new MessageReceiverThread(queueConsumer, "QueueConsumer", ID));
				Thread topicThread = new Thread(new TopicReceiverThread(topicSession, topicProducer, topicConsumer, instructionConsumer, ID));

//			queueThread.start();
				topicThread.start();

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
