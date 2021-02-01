import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import java.net.ServerSocket;


public class Server {

    private static ServerSocket serverSocket = null;
    private static Socket clientSocket = null;
    public static ArrayList<ClientHandler> clients = new ArrayList<>();
    public static ArrayList<Record> records = new ArrayList<>();
    public static File information = new File("information.txt");


    public static void main(String args[]) {

        int portNumber = 1234;
        System.out.println("SERVER IS RUNNING ON PORT " + portNumber + " ...");
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            System.out.println("SERVER SOCKET CAN BE CREATES!!!");
        }

        int clientNumber = 1;
        while (true) {
            try {
                clientSocket = serverSocket.accept();
                ClientHandler newClient =  new ClientHandler(clientSocket);
                newClient.start();
                System.out.println("Client "  + clientNumber + " is connected!");
                clientNumber++;
            } catch (IOException e) {
                System.out.println("Client could not be connected");
            }
        }
    }
}


class ClientHandler extends Thread {

    private String clientName = null;
    private ObjectInputStream inputData = null;
    private ObjectOutputStream outputData = null;
    private Socket clientSocket = null;
    private String name;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }


    public void run() {
        try {
            inputData = new ObjectInputStream(clientSocket.getInputStream());
            outputData = new ObjectOutputStream(clientSocket.getOutputStream());

            while (true) {
                synchronized(this) {
                    this.outputData.writeObject("<SERVER> PLEASE ENTER YOUR USERNAME : ");
                    this.outputData.flush();
                    name = ((String) this.inputData.readObject()).trim();

                    if ((name.indexOf('#') == -1) || (name.indexOf(' ') == -1)) {
                        boolean flag = true;
                        for (ClientHandler c : Server.clients) {
                            if (c != this) {
                                if (c.clientName.equals("#"+name)) {
                                    flag = false;
                                    break;
                                }
                            }
                        }
                        if (flag) {
                            outputData.writeObject("<SERVER> SUCCESSFULLY LOGIN IN CHAT APPLICATION ...");
                            clientName = "#"+name;
                            Server.clients.add(this);
                            break;
                        }else {
                            outputData.writeObject("<SERVER> THIS NAME HAS BEEN USED ...");
                        }
                    } else {
                        this.outputData.writeObject("<SERVER> USERNAME SHOULD NOT CONTAIN '#' AND 'SPACE' CHARACTER.");
                        this.outputData.flush();
                    }
                }
            }
            Record newUser = new Record(clientName, true, clientSocket.getLocalAddress().getHostName(), clientSocket.getPort());
            Server.records.add(newUser);
            FileWriter fileWriter = new FileWriter(Server.information);
            for (Record r : Server.records) {
                fileWriter.write(r.toString());
                fileWriter.write("\n");
            }

            fileWriter.close();
            System.out.println("Client Name is " + name);

            synchronized(this) {
                for (ClientHandler curr_client : Server.clients) {
                    if (curr_client != null && curr_client != this) {
                        curr_client.outputData.writeObject("<SERVER> "+name + " has joined");
                        curr_client.outputData.flush();
                    }
                }
            }


            while (true) {
                this.outputData.writeObject("<SERVER> PLEASE ENTER YOUR COMMAND :");
                this.outputData.flush();
                String input = (String) inputData.readObject();
                if (input.startsWith("#exit")) {
                    break;
                }else if(input.startsWith("#username")) {
                    changUsername(input);
                }else if (input.startsWith("#online")) {
                    onlineUsers();
                }else if (input.startsWith("#except")) {
                    exceptSending(input);
                }else if (input.startsWith("#group")) {
                    sendingGroup(input);
                } else if (input.startsWith("#")) {
                    sendPrivate(input);
                }else {
                    broadcast(input);
                }
            }

            Record leftRecord = new Record(name, false, clientSocket.getRemoteSocketAddress().toString(), clientSocket.getPort());
            Server.records.add(leftRecord);
            FileWriter fileWriter1 = new FileWriter(Server.information);
            for (Record r : Server.records) {
                fileWriter1.write(r.toString());
                fileWriter1.write("\n");
            }
            fileWriter1.close();

            this.outputData.writeObject("<SERVER> YUO LEFT THE CHAT ROOM.");
            this.outputData.flush();
            System.out.println(name + " left the chat room..");
            Server.clients.remove(this);

            synchronized(this) {
                if (!Server.clients.isEmpty()) {
                    for (ClientHandler c : Server.clients) {
                        if (c != null && c != this && c.clientName != null) {
                            c.outputData.writeObject("<SERVER> LEFT " + name + " CHAT ROOM.");
                            c.outputData.flush();
                        }
                    }
                }
            }

            this.inputData.close();
            this.outputData.close();
            clientSocket.close();

        } catch (IOException e) {
            System.out.println("User Connection terminated.");
            Record leftRecord = new Record(name, false, clientSocket.getRemoteSocketAddress().toString(), clientSocket.getPort());
            Server.records.add(leftRecord);
            Server.clients.remove(this);
            try {
                FileWriter fileWriter1 = new FileWriter(Server.information);
                for (Record r : Server.records) {
                    fileWriter1.write(r.toString());
                    fileWriter1.write("\n");
                }
                fileWriter1.close();
            }catch (IOException ee) {
                System.out.println(ee);
            }
            for (ClientHandler c : Server.clients) {
                try {
                    if (c != null) {
                        c.outputData.writeObject("<SERVER> "+name + " LEFT THE CHAT ROOM.");
                        c.outputData.flush();
                    }
                } catch (SocketException ex) {
                    //ex.printStackTrace();
                } catch (IOException ex) {
                    //ex.printStackTrace();
                }
            }

        } catch (ClassNotFoundException e) {
            System.out.println("Class Not Found");
        }
    }

    void changUsername(String input) {
        String[] in = input.split(":");
        boolean flag = true;
        if (in.length >= 2) {
            for (ClientHandler c: Server.clients) {
                if (c.name.equals(in[1])) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                name = in[1];
                for (ClientHandler c : Server.clients) {
                    if (c == this) {
                        c.name = in[1];
                    }
                }
            }else {
                try {
                    outputData.writeObject("<SERVER> THIS USERNAME HAS BEEN USED!!!");
                    outputData.flush();
                }catch (IOException e) {

                }
            }
        }else {
            try {
                outputData.writeObject("<SERVER> WRONG INPUT.");
                outputData.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    void sendPrivate(String input) throws IOException, ClassNotFoundException {
        String[] in = input.split(":", 2);
        String[] out = input.split("\\.");
        if (in[1].split(" ")[0].toLowerCase().equals("sendfile")) {
            boolean flag = false;
            for (ClientHandler c : Server.clients) {
                if (c != null && c != this && c.clientName.equals(in[0])) {
                    flag = true;
                }
            }
            if (flag) {
                byte[] fileData = (byte[]) inputData.readObject();
                for (ClientHandler c : Server.clients) {
                    if (c != null && c != this && c.clientName != null
                            && c.clientName.equals(in[0])) {
                        //c.outputData.writeObject("Sending_File:" + in[1].split(" ", 2)[1].substring(in[1].split("\\s", 2)[1].lastIndexOf(File.separator) + 1));
                        c.outputData.writeObject("Sending_File:" + out[1]);
                        c.outputData.writeObject(fileData);
                        c.outputData.flush();
                        System.out.println(this.clientName.substring(1) + " transferred a private file to client " + c.clientName.substring(1));
                        this.outputData.writeObject("<SERVER> SEND FILE TO <" + c.clientName.substring(1) + ">");
                        this.outputData.flush();
                        break;
                    }
                }
            }else {
                outputData.writeObject("<SERVER> THIS USERNAME IS NOT ONLINE!!!");
                outputData.flush();
            }
        } else {
            try {
                boolean flag = false;
                for (ClientHandler c : Server.clients) {
                    if (c != null && c != this && c.clientName.equals(in[0])) {
                        flag = true;
                    }
                }
                in[1] = in[1].trim();
                if (flag) {
                    if (!in[1].isEmpty()) {
                        for (ClientHandler c : Server.clients) {
                            if (c != null && c != this && c.clientName != null
                                    && c.clientName.equals(in[0])) {
                                c.outputData.writeObject("<" + name + "> " + in[1]);
                                c.outputData.flush();
                                System.out.println(this.clientName.substring(1) + " transferred a private message to client " + c.clientName.substring(1));
                                this.outputData.writeObject("<SERVER> SEND MESSAGE TO " + c.clientName.substring(1));
                                this.outputData.flush();
                                break;
                            }
                        }
                    }
                }else  {
                    outputData.writeObject("<SERVER> THIS USERNAME IS NOT ONLINE!!!");
                    outputData.flush();
                }
            }catch (NullPointerException e) {
                outputData.writeObject("<SERVER> YOUR INPUT INPUT IS WRONG!");
                outputData.flush();
            }
        }
    }

    void exceptSending(String input) throws IOException, ClassNotFoundException {
        String[] in = input.split(" ", 2);
        String[] temp = in[0].split(":");
        String[] exceptClients = new String[temp.length - 2];
        String[] out = input.split("\\.");
        for (int i = 1; i < temp.length - 1; i++) {
            exceptClients[i - 1] = temp[i];
        }

        if (temp[temp.length - 1].toLowerCase().equals("sendfile")) {
            byte[] fileData = (byte[]) inputData.readObject();
            synchronized(this) {
                for (ClientHandler c : Server.clients) {
                    boolean flag = true;
                    for (String s : exceptClients) {
                        if ( c != this  && c.clientName.equals("#"+s)) {
                            flag = false;
                        }
                        if (c.clientName.equals(this.clientName)) {
                            flag = false;
                        }
                    }
                    if (flag) {
                        c.outputData.writeObject("Sending_File:" + out[1]);
                        c.outputData.writeObject(fileData);
                        c.outputData.flush();
                    }
                }
            }
        } else {
            synchronized (this) {
                for (ClientHandler c : Server.clients) {
                    boolean flag = true;
                    for (String s : temp) {
                        if (c.clientName.equals("#"+s)) {
                            flag = false;
                        }
                        if (c.clientName.equals(this.clientName)) {
                            flag = false;
                        }
                    }
                    if (flag) {
                        c.outputData.writeObject("<"+name + "> "+in[1]);
                    }
                }
            }
        }
    }

    void broadcast(String input) throws IOException, ClassNotFoundException {
        String[] out = input.split("\\.");
        if (input.split(" ")[0].toLowerCase().startsWith("sendfile")) {
            byte[] fileData = (byte[]) inputData.readObject();
            synchronized(this) {
                for (ClientHandler c : Server.clients) {
                    if (!c.clientName.equals(this.clientName)) {
                        c.outputData.writeObject("Sending_File:" + out[1]);
                        c.outputData.writeObject(fileData);
                        c.outputData.flush();
                    }
                }
            }
            this.outputData.writeObject("<SERVER> SEND MESSAGE TO ALL USERS SUCCESSFULLY.");
            this.outputData.flush();
            System.out.println("Send  " + this.clientName.substring(1));
        } else {
            synchronized (this) {
                for (ClientHandler c : Server.clients) {
                    if (!c.clientName.equals(this.clientName)) {
                        c.outputData.writeObject("<" + name + "> " + input);
                        c.outputData.flush();
                    }
                }
            }
            this.outputData.writeObject("<SERVER> SEND MESSAGE TO ALL USERS SUCCESSFULLY.");
            this.outputData.flush();
            System.out.println("SEND MESSAGE TO ALL USERS BY " + this.clientName.substring(1));
        }
    }

    void sendingGroup(String input) throws IOException , ClassNotFoundException {
        String[] in = input.split(" ", 2);
        String[] temp = in[0].split(":");
        String[] exceptClients = new String[temp.length - 2];
        String[] out = input.split("\\.");
        for (int i = 1; i < temp.length - 1; i++) {
            exceptClients[i - 1] = temp[i];
        }

        if (temp[temp.length - 1].toLowerCase().equals("sendfile")) {
            byte[] fileData = (byte[]) inputData.readObject();
            synchronized(this) {
                for (ClientHandler c : Server.clients) {
                    boolean flag = false;
                    for (String s : exceptClients) {
                        if ( c != this  && c.clientName.equals("#"+s)) {
                            flag = true;
                        }
                        if (c.clientName.equals(this.clientName)) {
                            flag = false;
                        }
                    }
                    if (flag) {
                        c.outputData.writeObject("Sending_File:" + out[1]);
                        c.outputData.writeObject(fileData);
                        c.outputData.flush();
                    }
                }
            }
        } else {
            synchronized (this) {
                for (ClientHandler c : Server.clients) {
                    boolean flag = true;
                    for (String s : temp) {
                        if (c.clientName.equals("#"+s)) {
                            flag = false;
                        }
                        if (c.clientName.equals(this.clientName)) {
                            flag = false;
                        }
                    }
                    if (flag) {
                        c.outputData.writeObject("<"+name + "> "+in[1]);
                    }
                }
            }
        }
    }

    void onlineUsers() {
        int counter = 1;
        for (ClientHandler c : Server.clients) {
            if (c != this) {
                try {
                    this.outputData.writeObject(counter+") "+c.name);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            outputData.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}