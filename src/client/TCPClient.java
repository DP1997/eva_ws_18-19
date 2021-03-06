package client;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

import javafx.beans.property.SimpleBooleanProperty;
import shared_resources.datatypes.FileInformation;
import shared_resources.datatypes.ProgressStream;

import static client.ClientApplicationController.showAlert;
import static shared_resources.utils.MarshallingUtils.*;

public class TCPClient {
    
    public static String sharePath = null;
    public static Socket clientSocket = null;
    
    //indicates whether the client is connected to a server or not -> necessary for gui update
    public static SimpleBooleanProperty connectionStatus = new SimpleBooleanProperty(false);
    
    //global streams -> these are not allowed to be closed for the lifetime of a connection
    //which is why they are managed centrally
    private static BufferedOutputStream bos = null;
    public static BufferedOutputStream bos_fos = null;
    private static ProgressStream ps = null;
    
    public static ArrayList<FileInformation> fileInformation = null;
    
    public static void connectToServer(String serverIP, String serverPort) throws Exception {
	    	
    		//create new SocketAddress
    		SocketAddress sockaddr = new InetSocketAddress(serverIP, Integer.valueOf(serverPort));
    		clientSocket = new Socket();
    		// Connect with 2 s timeout
    		clientSocket.connect(sockaddr, 1000);
    		clientSocket.setSoTimeout(3000);
    		//set the socket timeout -> important for instances like pulling the lan-cable, turning of wifi, etc..
    		//-> throws an IOException after 3 seconds instead of waiting indefinitely 
    		clientSocket.setSoTimeout(3000);
    		//update connectionStatus
    		connectionStatus.set(true);
    		System.out.println("connection with server successfully established");
    		initializeStreams();
    }

    public static void setDownloadPath(String sharePath) throws AssertionError {	
    	assert(!(sharePath.equals("BITTE PFAD ANGEBEN"))); 
    	assert(sharePath != null);
        TCPClient.sharePath = sharePath; 	
    }
    
    //open the download directory in the explorer of the os
    //also checks whether it is supported on the os
    public static void showInExplorer() throws Exception, AssertionError {
    		assert(Desktop.isDesktopSupported());
    		assert(Desktop.getDesktop().isSupported(Desktop.Action.OPEN));
			String os = System.getProperty("os.name").toLowerCase();
			//unter Linux führt das Öffnen im Explorer zu Problemen
			assert((!(os.contains("nix") || os.contains("nux"))) == true);
        	Desktop.getDesktop().open(new File(sharePath.substring(0, sharePath.length()-1)));
    }
    
	//checks whether the connection is still live
	//if not, the connection is properly terminated
	private static void checkConnection(int i) throws IOException {
		if(i == -1 && !ClientApplicationController.downloadCanceled.get()) throw new IOException();
		return;
	}	

	public static boolean checkInternetConnection() {
		//Inet verbindung prüfen
    	try { 
            URL url = new URL("https://www.google.de/"); 
            URLConnection connection = url.openConnection(); 
            connection.connect();
            return true;
        } 
        catch (Exception e) { 
            showAlert("Warnung!","Sie haben keine Internetverbindung.", false);
            return false;
        }
	}
    
    //allocate resources needed for the connection
    public static void initializeStreams() {
    	//check for an active socket
    	try {
    		assert(clientSocket != null && !clientSocket.isClosed());
    	} catch (AssertionError ae) {
    		System.err.println("socket is null or closed - terminate connection");
    		//terminate connection
    		closeStreams();
    	}

    	//allocate streams
    	try {
			bos = new BufferedOutputStream(clientSocket.getOutputStream());
			ps = new ProgressStream(clientSocket.getInputStream());
			assert(ps != null && bos != null);
			System.out.println("streams have been successfully initialized");
		} catch (IOException | AssertionError e) {
			System.err.println("steams could not be initialized - terminating connection");
			e.printStackTrace();
			//terminate connection
			closeStreams();
		} 
    }
    
    //release all allocated resources
    public static void closeStreams() {
    	//release BufferedOutputStream
    	try {
        	assert(bos != null);
    		bos.close();
    		bos = null;
    		System.out.println("bos successfully released");
    	} catch (AssertionError ae) {
    		System.err.println("bos is already released");
    		ae.printStackTrace();
    	} catch (IOException e) {
    		System.err.println("bos could not be closed - setting bos null");
    		bos = null;
		}
    	
    	//release BufferedInputStream
    	try {
        	assert(ps != null);
    		ps.close();
    		ps = null;
    		System.out.println("ps successfully released");
    	} catch (AssertionError ae) {
    		System.err.println("ps is already released");
    		ae.printStackTrace();
    	} catch (IOException e) {
    		System.err.println("ps could not be closed - setting ps null");
    		ps = null;
		}
    	
    	//release socket
    	try {
    		assert(clientSocket != null);
    		clientSocket.close();
    		System.out.println("socket successfully closed - new connection can be established");
    	} catch (AssertionError ae) {
    		System.err.println("socket is already null");
    		ae.printStackTrace();
    	} catch (IOException ioe) {
    		System.err.println("socket could not be closed - setting socket null");
    		ioe.printStackTrace();
    		clientSocket = null;
    	}
    	connectionStatus.set(false);
    	ClientApplicationController.enableDownloading = true;
    }
    
    //schickt dem Server einen String anhand dieser entscheidet, welche Aktion er auszufÃ¼hren hat
    // refresh -> shareDirInformation oder fileName -> sendFileToClient
    public static void contactServer(String action){
		try {
			assert(action != null);
			//tell server how long our message will be (in bytes)
			bos.write(marshalling(action.getBytes().length));
			//write our message
			bos.write(action.getBytes());
			bos.flush();
			System.out.println("contacting server with request: "+action+", length in bytes: "+action.getBytes().length);
		} catch (AssertionError e) {
			System.err.println("action is null");
			e.printStackTrace();
			showAlert("Die angeforderte Aktion ist nicht verfügbar!", "Bitte wählen Sie eine andere.", false);
		} catch (IOException e) {
			System.err.println("error occured while writing in streams - terminating connection");
			e.printStackTrace();
			showAlert("Verbindungsfehler", "Die Verbindung zum Server wurde unterbrochen", false);
			closeStreams();
		}
    }
    
    //lese Datei von Server aus InputStream
    public static void downloadFileFromServer(String fileName) {
    	String filePath = sharePath + fileName;
    	
    	System.out.println("downloading...");
    	try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
    			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            assert(bos != null && baos != null);
    		// read fileLength from Client in order to avoid read-blocking and closing the socket on serverside (EOF)
            // marshalling
            byte[] fileLengthInBytes = new byte[4];
            checkConnection(ps.read(fileLengthInBytes));
            int fileLength = unmarshalling(fileLengthInBytes);
            ClientApplicationController.enableDownloading = false;
            // set fileLength of ProgressStream
            ProgressStream.setFileLength(fileLength);
            ProgressStream.resetProgressBar();
            // read data
        	//byte[] file = new byte[fileLength];
        	byte[] aByte = new byte[1];
        	int bytesRead = 0;
        	
        	while (bytesRead < fileLength) {
            	checkConnection(ps.read(aByte, 0, 1));
            	++bytesRead;
            	baos.write(aByte);
        	}
            
            // write data in boas to put it on the disk
            bos.write(baos.toByteArray());
            bos.flush();   
            System.out.println("file downloaded");
		
        } catch (AssertionError assErr) {
			System.err.println("streams for writing on disk could not be initialized");
			assErr.printStackTrace();
			showAlert("Fehler beim Download", "Einige - für den Download relevante - Streams konnten nicht allokiert werden. Das System muss beendet werden.", true);
		} catch (SocketException se) {
			throw new NullPointerException();
		} catch (IOException e) {
        	System.err.println("connection to server lost");
			e.printStackTrace();
			 if (!ClientApplicationController.downloadCanceled.get())showAlert("Verbindungsfehler!", "Die Verbindung zum Server wurde unterbrochen", false);
			closeStreams();
		} 
    }

    // empfange Share-Ordner Informationen von Server
    public static void receiveDirInformation() { 
			try {
				System.out.println("receiving server-directory information...");
				fileInformation = new ArrayList<FileInformation>();
				
				//retrieve how many fileInformation-sets will be send
	            byte[] fileCountBBuffer = new byte[4];
	            checkConnection(ps.read(fileCountBBuffer));
	            int fileCount = unmarshalling(fileCountBBuffer);
	            System.out.println(fileCount+" fileInformation-Sets expected");
	            
	            //retrieve header information of fileInformation sets
	        	byte[] lengthOfDataBBuffer = new byte[4];
	        	int lengthOfData;
	        	byte[] dataBBuffer;
	        	String fileName;
	        	String fileLength;
				for(int i = 0; i < fileCount; i++) {
					//read length of incoming message in bytes
		            checkConnection(ps.read(lengthOfDataBBuffer));
		            //convert length in bytes to int
		            lengthOfData = unmarshalling(lengthOfDataBBuffer);
		            System.out.print("incoming msg expected to be "+lengthOfData+" bytes long - ");
		            //initialize byte array which will contain the received message as bytes
		            dataBBuffer = new byte[lengthOfData];
		            //read message as bytes and write it in our buffer
		            checkConnection(ps.read(dataBBuffer));
		            //convert recieved data to a String
		            fileName = new String(dataBBuffer);
		            System.out.println("file "+fileName+" recieved");
		            
		            //do the same for the fileLength
		            checkConnection(ps.read(lengthOfDataBBuffer));
		            lengthOfData = unmarshalling(lengthOfDataBBuffer);
		            System.out.print("incoming msg expected to be "+lengthOfData+" bytes long - ");
		            dataBBuffer = new byte[lengthOfData];
		            checkConnection(ps.read(dataBBuffer));
		            fileLength = new String(dataBBuffer);
		            System.out.println("fileLength recieved: "+fileLength);
		            //create fileInformation object
					fileInformation.add(new FileInformation(fileName, fileLength));
				}
	        	System.out.println("directory content recieved");
			} catch (IOException e) {
				System.out.println("error occured while reading from streams - terminating connection");
				e.printStackTrace();
				showAlert("Verbindungsfehler!", "Die Verbindung zum Server ist abgebrochen.", false);
				closeStreams();
			}
			ProgressStream.resetProgressBar();
			

    }
    
}

