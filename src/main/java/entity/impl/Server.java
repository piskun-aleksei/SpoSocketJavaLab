package entity.impl;


import entity.BasicInterface;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Server implements BasicInterface{

    private static boolean isClosed = false;
    private static String receivedFile;
    private DataOutputStream serverOutput;
    private BufferedReader serverInput;
    private InputStream in;
    private OutputStream out;

    private int currentPacket = 0;

    private File file;

    private Socket socket;
    private String clientAddres;
    private String newClientAddress;
    private String clientFileName;

    private boolean firstClient = true;
    private boolean expired = false;

    @Override
    public void start() throws IOException {
        String receivedData;
        ServerSocket serverSocket = new ServerSocket(6790);

        while (true) {
            socket = serverSocket.accept();
            newClientAddress = socket.getRemoteSocketAddress().toString().substring(0, socket.getRemoteSocketAddress().toString().lastIndexOf(":"));

            serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            serverOutput = new DataOutputStream(socket.getOutputStream());
            out = socket.getOutputStream();
            while (!isClosed) {
                try {
                    System.out.println("Waiting for command... ");
                    receivedData = serverInput.readLine();
                    System.out.println("Received:" + receivedData);
                    execute(receivedData);
                } catch (SocketTimeoutException | SocketException e) {
                    firstClient = false;
                    clientAddres = newClientAddress;
                    System.out.println("Lost connection to client. Waiting...");
                    // Get current time
                    long start = System.currentTimeMillis();
                    socket = serverSocket.accept();
                    long elapsedTimeMillis = System.currentTimeMillis() - start;
                    float elapsedTimeSec = elapsedTimeMillis / 1000F;
                    if (elapsedTimeSec < 30) {
                        expired = false;
                    } else {
                        expired = true;
                    }
                    newClientAddress = socket.getRemoteSocketAddress().toString().substring(0, socket.getRemoteSocketAddress().toString().lastIndexOf(":"));

                    serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    serverOutput = new DataOutputStream(socket.getOutputStream());
                    out = socket.getOutputStream();

                    /*try {
                        System.out.println("Lost connection to client. Waiting...");
                        TimeUnit.SECONDS.sleep(30);
                        if (!main.checkClient()) {
                            System.out.println("Server timeout");
                            main.socket.close();
                            isClosed = true;
                        }
                    } catch (InterruptedException e2) {
                        e2.printStackTrace();
                    }*/
                }
            }
            isClosed = false;
        }
    }

    private void execute(String data) throws IOException {
        String[] command = data.split(" ");
        String result = "";

        if (command[0].toUpperCase().equals("ECHO")) {
            for (int i = 1; i < command.length; i++)
                result += command[i] + " ";
            sendData(result);
        } else if (command[0].toUpperCase().equals("TIME")) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            result = dateFormat.format(date);
            sendData(result);
        } else if (command[0].toUpperCase().equals("CLOSE")) {
            isClosed = true;
        } else if (command[0].toUpperCase().equals("DOWNLOAD")) {
            if(!firstClient) {
                System.out.println(clientAddres);
                System.out.println(newClientAddress);
                if (clientAddres.equals(newClientAddress) && !expired && clientFileName.equals(command[1])) {
                    sendData("Continue");
                } else {
                    System.out.println(newClientAddress);
                    clientFileName = command[1];
                    file = new File(clientFileName);
                    sendData("NewFile");
                    currentPacket = 0;
                    in.close();
                    in = new FileInputStream(file);
                }
            }
            else {
                System.out.println(newClientAddress);
                clientFileName = command[1];
                file = new File(clientFileName);
                sendData("NewFile");
                currentPacket = 0;
                in = new FileInputStream(file);
            }

            if (file.isFile() & file.canRead()) {
                byte[] byteArray = new byte[8*1024];

                int numPackets = (int) Math.ceil(((double) file.length() / byteArray.length)) - currentPacket;
                sendData(Integer.toString(numPackets));
                // Get the size of the file
                long length = file.length();
                int count;

                while ((count = in.read(byteArray)) > 0) {
                    System.out.println("Packet: " + currentPacket);
                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    out.write(byteArray, 0, count);
                    currentPacket++;
                }
                System.out.println("File sent.");

                System.out.println("File closed.");

                /*
                int sentPackages = 0;
                if(this.clientAddres == socket.getRemoteSocketAddress().toString() && this.clientFileName == command[1]){
                    sentPackages = Integer.parseInt(serverInput.readLine());
                }
                int numPackets = (int)Math.ceil(((double)file.length()/byteArray.length)) - sentPackages;
                this.clientFileName = command[1];
                sendData(Integer.toString(numPackets));
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                DataInputStream bis = new DataInputStream(fis);
                for(int i = 0; i < numPackets -1; i++) {
                    try {
                        bis.read(byteArray, byteArray.length * sentPackages, byteArray.length);
                        TimeUnit.MILLISECONDS.sleep(5);
                        serverOutput.write(byteArray, 0, byteArray.length);
                } catch (SocketTimeoutException | SocketException | InterruptedException e) {
                    try {
                        System.out.println("Lost connection to client. Waiting...");
                        boolean userConnected = false;
                        for(int check = 0; check < 30 || userConnected; check++){
                            TimeUnit.SECONDS.sleep(1);
                            if (this.checkClient()){
                                userConnected = true;
                            }

                        }
                        if (!this.checkClient()) {
                            System.out.println("Server timeout");
                            socket.close();
                            isClosed = true;
                        }
                    }
                    catch (InterruptedException e2){
                        e2.printStackTrace();
                    }
                }
                }
                int lastPackage = (int)(file.length() - byteArray.length*(numPackets-1));
                bis.read(byteArray, 0, lastPackage );
                serverOutput.write(byteArray, 0, lastPackage);
                bis.close();*/
            } else {
                result = "File is not found/available";
                sendData(result);
            }
             /*catch (SocketTimeoutException | SocketException e) {


                System.out.println("Lost connection to client. Waiting...");
                // Get current time
                long start = System.currentTimeMillis();


                long elapsedTimeMillis = System.currentTimeMillis() - start;
                float elapsedTimeSec = elapsedTimeMillis / 1000F;


                    /*boolean userConnected = false;
                    for(int check = 0; check < 30 || userConnected; check++){
                        TimeUnit.SECONDS.sleep(1);
                        if (this.checkClient()){
                            userConnected = true;
                        }

                    }
                    if (!this.checkClient()) {
                        System.out.println("Server timeout");
                        socket.close();
                        isClosed = true;
                    }


            }*/
        } else if (command[0].toUpperCase().equals("UPLOAD")) {
            char[] fileInput = new char[1024];
            File file = new File(receivedFile);
            file.createNewFile();
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(receivedFile));
            String inputFromClient = serverInput.readLine();
            Integer numPackets = tryParse(inputFromClient);
            if (numPackets != null) {
                for (int i = 0; i < numPackets; i++) {
                    serverInput.read(fileInput);
                    bos.write(String.valueOf(fileInput).trim().getBytes());
                    Arrays.fill(fileInput, (char) 0);
                }
                bos.flush();
                bos.close();
                System.out.println("Received file");
            } else {
                System.out.println("Wrong argument:" + inputFromClient);
            }
        } else

        {
            result = command[0] + " is not a command.";
            sendData(result);
        }

    }

    private void sendData(String outputData) throws IOException {
        this.serverOutput.writeBytes(outputData + "\n");
        serverOutput.flush();
    }

    private boolean checkClient() throws IOException {
        boolean result = true;
        try {
            serverInput.readLine();
        } catch (SocketException e) {
            result = false;
        }
        return result;
    }

    private static Integer tryParse(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
