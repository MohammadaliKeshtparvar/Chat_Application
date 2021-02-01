import java.io.*;

public class SendClient implements Runnable {


    private ObjectOutputStream outputData;
    private BufferedReader inputClient;

    public SendClient(ObjectOutputStream outputData) {
        this.outputData = outputData;
        inputClient = new BufferedReader(new InputStreamReader(System.in));
    }


    @Override
    public void run() {
        BufferedInputStream bufferedInputStream ;
        boolean isOpen = true;
        try {
            while (isOpen) {
                String message = inputClient.readLine().trim();
                String[] input = message.split(" ", 2);
                if ((input[0].split(":").length > 1)) {
                    if (input[0].split(":")[input[0].split(":").length - 1].startsWith("sendfile")) {
                        File sendingFile = new File(input[1]);
                        if (!sendingFile.exists()) {
                            System.out.println("File Doesn't exist!!");
                            continue;
                        }
                        byte [] fileToByte  = new byte [(int)sendingFile.length()];
                        FileInputStream fis = new FileInputStream(sendingFile);
                        bufferedInputStream = new BufferedInputStream(fis);
                        while (bufferedInputStream.read(fileToByte,0,fileToByte.length) >= 0) {
                            bufferedInputStream.read(fileToByte,0,fileToByte.length);
                        }
                        outputData.writeObject(message);
                        outputData.writeObject(fileToByte);
                        outputData.flush();
                    } else {
                        outputData.writeObject(message);
                        outputData.flush();
                    }

                } else if (message.startsWith("sendfile")) {

                    File sendingFile = new File(input[1]);

                    if (!sendingFile.exists()) {
                        System.out.println("File Doesn't exist!!");
                        continue;
                    }

                    byte [] fileToByte  = new byte [(int)sendingFile.length()];
                    FileInputStream fis = new FileInputStream(sendingFile);
                    bufferedInputStream = new BufferedInputStream(fis);
                    while (bufferedInputStream.read(fileToByte,0,fileToByte.length) >= 0) {
                        bufferedInputStream.read(fileToByte,0,fileToByte.length);
                    }
                    outputData.writeObject(message);
                    outputData.writeObject(fileToByte);
                    outputData.flush();
                } else {
                    outputData.writeObject(message);
                    outputData.flush();
                }
            }
            outputData.close();
        } catch (IOException e) {
            isOpen = false;
            System.err.println("IOException:  " + e);
        }
    }
}
