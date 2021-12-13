import java.net.*;
import java.nio.ByteBuffer;

public class Server {

	// Initialising and declaring class variables
	private InetAddress IPAddress = null; // Client's IP address
	private int port; // Client's port number
	private int expectedSeqNum; // expected sequence number
	private int SN; // Sequence number of the received packet
	private boolean completed = false; // Server will be listening while this boolean is false

	// Method for detaching the sequence number from the received message
	private int detachSequence(String text) {
		return Integer.parseInt(text.substring(0, 4));
	}

	// Method for formatting the received message
	private String detachAndFormatMessage(String text) {
		return "Datagram message: " + text.substring(4);
	}

	// Method for generating an output string confirming the receipt of a datagram
	private String generateACK(int seqNo) {
		return "Received datagram #" + String.format("%d", seqNo);
	}

	// Method for converting an integer to byte array
	private byte[] intToBytes(int seqNo) {
		ByteBuffer bb = ByteBuffer.allocate(4); 
		bb.putInt(seqNo); 
		return bb.array();
	}

	// Method for creating an output string confirming sent acknowledgements
	private String confirmSend(int seqNo) {
		return "Sent ACK #" + String.format("%d", seqNo);
	}
	
	// Server communication method
	private void serverSession() throws Exception {

		// initialising the socket
		DatagramSocket socket = new DatagramSocket(12123);

		System.out.println("SERVER: listening");
		System.out.println();

		// server is listing and sending acknowledgements until FIN is received
		while (completed == false) {

			//  declaring the byte arrays that will store the sent and received messages
			byte[] receiveData = new byte[16];
			byte[] sendData = new byte[8];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			socket.receive(receivePacket);
			String message = new String(receivePacket.getData());

			// checking if the message is the handshake. If it is, resetting the expected sequence number to zero
			if (message.substring(0, 3).equals("SYN")) {
				
				System.out.println("######################################");
				System.out.println("Handshake received: sequence number reset");
				System.out.println();
				expectedSeqNum = 0;
				
			// checking if the communication session is completed
			} else if (message.substring(0, 3).equals("FIN")) {
				
				// if it is, closing the socket and setting the boolean to true
				System.out.println("SERVER: communication session completed");
				System.out.println("######################################");
				socket.close();
				completed = true;
				
			} else {

				// if the message is not FIN or SYN, we detach the sequence number
				SN = detachSequence(message);
				System.out.println(generateACK(SN));

				// we check if the sequence number is in-order
				if (SN == expectedSeqNum) {
					
					// if it is in order, we display the received text string
					System.out.println("Sequence number matches the expected");
					System.out.println(detachAndFormatMessage(message));

					// and increment the expected sequence number
					expectedSeqNum++;
				} else {
					System.out.println("Sequence number does not match the expected!");
				}

				// we get the Client's IP address and port from the received packet
				IPAddress = receivePacket.getAddress();
				port = receivePacket.getPort();

				// sequence number is converted into a byte array and sent to the client as an acknowledgement
				sendData = intToBytes(SN);
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
				socket.send(sendPacket);

				System.out.println(confirmSend(SN));
				System.out.println();
			}
		} 
	}
	
	public static void main(String[] args) throws Exception {
		
		Server s = new Server();
		
		s.serverSession(); // server communication session
	}
}
