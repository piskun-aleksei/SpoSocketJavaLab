package entity.impl;

import entity.BasicInterface;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.Time;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;


public class Client implements BasicInterface {
    private static String receivedFile;
    private static boolean isClosed = false;
    private static BufferedReader clientInput = null;
    private InputStream in = null;
    private OutputStream out = null;
    private Socket socket;
    private int sentPackages;

    private int whole = 10000;

    private boolean expired = false;
    private boolean uploadComplete = false;
    private String clientFileName;

    private int currentPacket;
    private File file;

    public void start() throws IOException {
        String input;
        String output;
        byte[] fileInput = new byte[ 1024];
        while (true) {
            isClosed = false;
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Specify ip: ");
            String ip = userInput.readLine();
            System.out.println("Specify port: ");
            int port = Integer.parseInt(userInput.readLine());
            this.socket = new Socket(ip, port);
            DataOutputStream clientOutput = new DataOutputStream(this.socket.getOutputStream());
            clientInput = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            in = socket.getInputStream();
            out = socket.getOutputStream();
            socket.setKeepAlive(true);
            System.out.println(clientInput.readLine());
            while (!isClosed) {
                input = userInput.readLine();
                clientOutput.writeBytes(input + "\n");
                String clientCommand = input.split(" ")[0].toUpperCase();
                if(clientCommand.equals("CLOSE")){

                    isClosed=true;
                }
                else if (!clientCommand.equals("DOWNLOAD") && !clientCommand.equals("UPLOAD")) {
                    output = clientInput.readLine();
                    System.out.println("Received: " + output);
                }
                else if (clientCommand.equals("DOWNLOAD")) {
                    socket.setSoTimeout(15000);
                    receivedFile = "downloaded_" + input.split(" ")[1];
                    String isContinue = clientInput.readLine();
                    System.out.println(isContinue);
                    boolean append = false;
                    if (isContinue.equals("Continue")) {
                        System.out.println("Continuing!");
                        append = true;
                    }
                    String inputFromServer = clientInput.readLine();
                    System.out.println(inputFromServer);
                    int numPackets = tryParse(inputFromServer);
                    inputFromServer = clientInput.readLine();
                    int fileLength = tryParse(inputFromServer);
                    FileOutputStream outFile = null;
                    try {
                        outFile = new FileOutputStream(receivedFile, append);
                    } catch (FileNotFoundException ex) {
                        System.out.println("File not found. ");
                    }

                    int count;
                    int packet = 0;
                    outFile.flush();
                    long startTime = System.currentTimeMillis();

                    while (whole < fileLength  && (count = in.read(fileInput)) != -1) {
                        try {
                            outFile.write(fileInput, 0, count);
                            int temp = 0;
                            whole += count;
                            temp += count;
                            System.out.println("Got " + temp + " bytes");
                        }
                        catch (SocketException | SocketTimeoutException e){
                            isClosed = true;
                            break;
                        }
                    }
                    long elapsedTimeNs = System.currentTimeMillis() - startTime;
                    System.out.println("TIME." + elapsedTimeNs);
                    System.out.println("File got.");
                    outFile.close();
                    System.out.println("File closed.");
                } else {
                    String fileName = input.split(" ")[1];
                    if (clientFileName != null && !expired && clientFileName.equals(fileName) && !uploadComplete) {
                        clientOutput.writeBytes("Continue" + "\n");
                        in.close();
                        in = new FileInputStream(file);
                        //currentPacket--;
                        in.skip(whole);
                    } else {
                        uploadComplete = false;
                        clientFileName = fileName;
                        file = new File(fileName);
                        clientOutput.writeBytes("NewFile" + "\n");
                        currentPacket = 0;
                        //in.close();
                        in = new FileInputStream(file);
                    }
                    if (file.isFile() & file.canRead()) {
                        byte[] byteArray = new byte[ 1024];
                        int numPackets = (int) Math.ceil(((double) file.length() / byteArray.length)) - currentPacket;
                        clientOutput.writeBytes(Integer.toString(numPackets) + "\n");
                        clientOutput.writeBytes(Long.toString(file.length())+ "\n");
                        int count;
                        while ((count = in.read(byteArray)) > 0) {
                            System.out.println("Packet: " + currentPacket);
                            out.write(byteArray, 0, count);
                            currentPacket++;
                        }
                        System.out.println("File sent.");

                        System.out.println("File closed.");
                        currentPacket = 0;
                        uploadComplete = true;
                    } else {
                        String result = "File is not found/available";
                        clientOutput.writeBytes(result + "\n");
                    }
                }
            }
            this.socket.close();
        }
    }

    private static Integer tryParse(String text) {

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean checkServer() throws IOException {
        boolean result = true;
        try {
            clientInput.readLine();
        } catch (SocketException e) {
            result = false;
        }
        return result;
    }
}
