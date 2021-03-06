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

public class Server implements BasicInterface {

    private static boolean isClosed = false;
    private static String receivedFile;
    private DataOutputStream serverOutput;
    private BufferedReader serverInput;
    private InputStream in;

    private OutputStream out;

    int whole;
    private int currentPacket = 0;

    private File file;

    private Socket socket;
    private String clientAddres;
    private String newClientAddress;
    private String clientFileName;

    private boolean firstClient = true;
    private boolean expired = false;
    private boolean downloadComplete = false;

    @Override
    public void start() throws IOException {
        String receivedData;
        ServerSocket serverSocket = new ServerSocket(6790);

        while (true) {
            isClosed = false;
            socket = serverSocket.accept();

            newClientAddress = socket.getRemoteSocketAddress().toString().substring(0, socket.getRemoteSocketAddress().toString().lastIndexOf(":"));

            serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            serverOutput = new DataOutputStream(socket.getOutputStream());
            out = socket.getOutputStream();
            sendData("Connected to server");
            while (!isClosed) {
                try {
                    System.out.println("Waiting for command... ");
                    receivedData = serverInput.readLine();
                    System.out.println("Received:" + receivedData);
                    execute(receivedData);
                } catch (SocketTimeoutException | SocketException e) {
                    //currentPacket++;
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
                    sendData("Connected to server");

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
            if (!firstClient) {
                System.out.println(clientAddres);
                System.out.println(newClientAddress);
                if (clientAddres.equals(newClientAddress) && clientFileName.equals(command[1]) && !downloadComplete) {
                    sendData("Continue");
                    in = new FileInputStream(file);
                    //currentPacket--;
                    in.skip(whole);
                } else {
                    downloadComplete = false;
                    System.out.println(newClientAddress);
                    clientFileName = command[1];
                    file = new File(clientFileName);
                    whole = 0;
                    sendData("NewFile");
                    currentPacket = 0;
                    in = new FileInputStream(file);
                }
            } else {
                downloadComplete = false;
                System.out.println(newClientAddress);
                clientFileName = command[1];
                file = new File(clientFileName);
                whole = 0;
                sendData("NewFile");
                currentPacket = 0;
                in = new FileInputStream(file);
            }

            if (file.isFile() & file.canRead()) {
                byte[] byteArray = new byte[8 * 1024];
                int numPackets = (int) Math.ceil(((double) file.length() / byteArray.length)) - currentPacket;
                sendData(Integer.toString(numPackets));
                sendData(Long.toString(file.length() - whole));
                int count;
                while ((count = in.read(byteArray)) > 0) {
                    System.out.println("Packet: " + currentPacket);
                    out.write(byteArray, 0, count);
                    whole+=count;
                }
                System.out.println("File sent.");
                in.close();
                System.out.println("File closed.");
                currentPacket = 0;
                downloadComplete = true;
            } else {
                result = "File is not found/available";
                sendData(result);
            }
        } else if (command[0].toUpperCase().equals("UPLOAD")) {
            byte[] byteArray = new byte[8 * 1024];
            receivedFile = "downloaded_" + command[1];
            String isContinue = serverInput.readLine();
            System.out.println(isContinue);
            boolean append = false;
            if (isContinue.equals("Continue")) {
                System.out.println("Continuing!");
                append = true;
            }
            String inputFromServer = serverInput.readLine();
            System.out.println(inputFromServer);
            int numPackets = tryParse(inputFromServer);
            inputFromServer = serverInput.readLine();
            int fileLength = tryParse(inputFromServer);
            try {
                out = new FileOutputStream(receivedFile, append);
            } catch (FileNotFoundException ex) {
                System.out.println("File not found. ");
            }

            int count;
            int packet = 0;
            while ((count = in.read(byteArray, 0, byteArray.length)) > 0) {
                out.write(byteArray, 0, count);
                int temp = 0;
                whole += count;
                temp += count;
                while (temp < (((packet + 1) < numPackets) ? byteArray.length : fileLength - packet * byteArray.length)) {
                    count = in.read(byteArray, 0, byteArray.length - temp);
                    temp += count;
                    whole += count;
                    System.out.println(temp);
                    out.write(byteArray, 0, count);
                }
                System.out.println("Got " + temp + " bytes");
                System.out.println("Packet " + packet + " got, and in total " + whole + " from " + fileLength);
                packet++;
                if (packet == numPackets) {
                    break;
                }
            }
            System.out.println("File got.");
            out.close();
            System.out.println("File closed.");
        } else

        {
            result = command[0] + " is not a command.";
            sendData(result);
        }

    }

    private void sendData(String outputData) throws IOException {
        serverOutput.writeBytes(outputData + "\n");
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
