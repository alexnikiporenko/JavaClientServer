import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Scanner;

public class Client {

	// Initialising and declaring class variables
	private String message = ""; // used to store the text string
	private int SN; // sequence number of the transferred packets
	private int base; // sequence number of the oldest unacknowledged packet 
	private DatagramSocket socket = null; // Client's socket
	private InetAddress IPAddress = null; // Server's IP address
	private int portNum; // Server's port number

	// Method for joining the sequence number with the text string
	private String formatMessage(int sequence, String text) {
		return String.format("%04d", sequence) + text;
	}

	// Method for converting byte array to integer
	private int bytesToInt(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getInt();
	}

	// Method for creating an output string confirming window size
	private String confirmWindow(int windSize) {
		return "Window size = " + String.format("%d", windSize);
	}
	
	// Method for creating an output string confirming sends
	private String confirmSend(int seqNo) {
		return "Sent datagram #" + String.format("%d", seqNo);
	}

	// Method for creating an output string confirming received acknowledgements
	private String ACKtoString(int seqNo) {
		return "ACK from server: received datagram #" + String.format("%d", seqNo);
	}

	// Method ensuring that the user runs the server first
	private void go() {
		
		// Scanner used to read user input
		Scanner keyboard = new Scanner(System.in);
		String command = "";

		System.out.println("Please ensure that the server is listening. Run 'Server.java' first.");
		System.out.println("Once the server is listening, type GO to begin the communication:");

		do {
			command = keyboard.nextLine();
			if (command.equalsIgnoreCase("GO") == false) {
				System.out.println();
				System.out.println("Please type GO to begin the communication:");
			}
		} while (command.equalsIgnoreCase("GO") == false);

		keyboard.close();
	}

	// Method converting the .txt file into the text string
	private String txtImport() throws Exception {
		Path filePath = Paths.get("text.txt");
		String text = Files.readString(filePath);
		return text;
	}
	
	// Method for setting up the session of communication
	private void setUp(String address, int port) throws Exception {
		
		System.out.println();
		System.out.println("######################################");
		System.out.println("CLIENT: starting communication session");
		
		// Initialising Server's IP address and port number
		IPAddress = InetAddress.getByName(address);
		portNum = port;
		
		// Opening Client's datagram socket
		socket = new DatagramSocket();

		// importing the .txt file
		message = txtImport();
		System.out.println();
		System.out.println("Text string imported from text.txt");
		System.out.println("Sending string: " + message);
		System.out.println();
	}
	
	// Method for closing the session of communication 
	private void close() throws Exception {
		
		// Sending Server the signal that the communication session is complete
		byte[] sendEndMessage = new byte[16];
		String fin = "FIN";
		sendEndMessage = fin.getBytes();
		DatagramPacket sendFin = new DatagramPacket(sendEndMessage, sendEndMessage.length, IPAddress, portNum);
		socket.send(sendFin);
		
		// closing Client's socket
		System.out.println("CLIENT: communication session completed");
		System.out.println("######################################");
		socket.close();
	}
	
	// Client communication method
	private void clientSession(int windowSize) throws Exception {
		
		System.out.println("++++++++++++++++++++++++++++++++++++++");
		System.out.println("CLIENT: beginning the transfer");
		System.out.println();
		System.out.println(confirmWindow(windowSize));
		System.out.println();

		// Initialising the sequence number and the base
		SN = 0;
		base = 0;
		
		// Handshake - resetting the sequence number of the packet the Server is expected to receive
		byte[] sendHandshake = new byte[16];
		String syn = "SYN";
		sendHandshake = syn.getBytes();
		DatagramPacket sendSyn = new DatagramPacket(sendHandshake, sendHandshake.length, IPAddress, portNum);
		socket.send(sendSyn);
		System.out.println("Handshake sent: server has been sent the sequence number");
		System.out.println();
		
		// 10 datagrams will be sent to demonstrate the program
		while (SN < 10) { 

			// while loop used to enforce the window size
			while (SN < (base + windowSize)) {

				// Random number generator used to simulate packet loss
				Random rand = new Random();
				int n = rand.nextInt(100);

				// change the zero below to integer x, representing x% packet loss
				if (n >= 0) {

					// declaring the byte array that will store the message
					byte[] sendData = new byte[16];

					// creating a string containing the sequence number and the message
					String formattedMessage = formatMessage(SN, message);

					// converting the string into a byte array and sending it inside a datagram
					sendData = formattedMessage.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, portNum);
					socket.send(sendPacket);
					System.out.println(confirmSend(SN));
				}
				
				// SN is incremented after every sent packet.
				SN++;
			}

			// once the number of packets permitted by the window size has been sent, the client begins waiting for acknowledgements
			System.out.println();
			System.out.println("Waiting for ACK(s)");
			System.out.println();

			// while loop used to wait for acknowledgements from the Server
			while (base < SN) {
				byte[] receiveData = new byte[8];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				socket.receive(receivePacket);

				// reading the sequence number from the received acknowledgement 
				int ACKseqNo = bytesToInt(receivePacket.getData());
				System.out.println(ACKtoString(ACKseqNo));    

				// if the sequence number is in-order (matching the base), we increment the base
				if (ACKseqNo == (base)) {
					System.out.println("Sequence number matches the expected");
					System.out.println();
					base++;
				} else {

					// if the sequence number is unexpected, we reset the SN to the sequence number of the oldest unacknowledged packet
					System.out.println("Sequence number does not match the expected!");
					System.out.println();
					SN = base;
				}   
			}
		}
		
		System.out.println("CLIENT: transfer completed");
		System.out.println("++++++++++++++++++++++++++++++++++++++");
		System.out.println();
	}

	public static void main(String[] args) throws Exception {

		Client c = new Client();

		c.go(); // Starting the communication process, and ensuring that the user runs the server first
		
		c.setUp("127.0.0.1", 12123); // setting up the communication session
		
		c.clientSession(1); // STOP and WAIT protocol (window size = 1)

		c.clientSession(5); // GO-BACK-N protocol (window size = 5)
		
		c.close(); // closing the communication session
	}
}
