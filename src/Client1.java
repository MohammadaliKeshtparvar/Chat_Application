import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Client1 implements Runnable {

    private static int serverPort;
    private static String serverAddress;
    private static Socket clientSocket;
    private static ObjectOutputStream outputData ;
    private static ObjectInputStream inputData;
    private static BufferedReader inputClient ;
    //private static BufferedInputStream bufferedInputStream ;
    private static boolean isOpen;
    private static ArrayList<String> onlineUsers;


    public static void main(String[] args) {
        serverPort = 1234;
        serverAddress = "127.0.0.1";
        onlineUsers = new ArrayList<>();
        isOpen = true;

        System.out.println("Default Server: " + serverAddress + ", Default Port: " + serverPort);

        try {
            clientSocket = new Socket(serverAddress, serverPort);
            inputClient = new BufferedReader(new InputStreamReader(System.in));
            outputData = new ObjectOutputStream(clientSocket.getOutputStream());
            inputData = new ObjectInputStream(clientSocket.getInputStream());
        } catch (UnknownHostException e) {
            System.err.println("Unknown " + serverAddress + ":" + serverPort);
        } catch (IOException e) {
            System.err.println("No Server found.");
        }

        if (clientSocket != null && outputData != null && inputData != null) {
            new Thread(new Client1()).start();
            SendClient s = new SendClient(outputData);
            s.run();
        }
    }
    public void run() {

        String responseLine;
        String filename = null;
        byte[] ipfile = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        File directory_name = null;
        String full_path;
        String dir_name = "downloads_client1";
        directory_name = new File((String) dir_name);
        if (!directory_name.exists()) {
            directory_name.mkdir();
            System.out.println("Create a new directory for saving receiving file.");
        }

        try {
            while ((responseLine = (String) inputData.readObject()) != null)  {
                if (responseLine.startsWith("Sending_File")) {
                    try {
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH_mm_ss");
                        LocalDateTime now = LocalDateTime.now();
                        filename = dtf.format(now) +"."+ responseLine.split(":")[1];
                        full_path = directory_name.getAbsolutePath()+"/"+filename;
                        ipfile = (byte[]) inputData.readObject();
                        fos = new FileOutputStream(full_path);
                        bos = new BufferedOutputStream(fos);
                        bos.write(ipfile);
                        bos.flush();
                        System.out.println("File Received.");
                    }
                    finally {
                        if (fos != null) fos.close();
                        if (bos != null) bos.close();
                    }
                }

                else {
                    System.out.println(responseLine);
                }
            }

            isOpen = false;
            System.exit(0);

        } catch (IOException e) {
            System.exit(0);
        }catch (ClassNotFoundException ee) {
            System.err.println("YOU LEFT THE CHAT ROOM.");
        }
    }
}
