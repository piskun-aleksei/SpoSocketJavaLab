package entity.impl;

import entity.BasicInterface;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
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

    private boolean expired = false;
    private boolean uploadComplete = false;
    private String clientFileName;

    private int currentPacket;
    private File file;

    public void start() throws IOException {
        String input;
        String output;
        byte[] fileInput = new byte[8 * 1024];
        while (true) {
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Specify ip: ");
            String ip = userInput.readLine();
            System.out.println("Specify port: ");
            int port = Integer.parseInt(userInput.readLine());
            this.socket = new Socket(ip, port);
            DataOutputStream clientOutput = new DataOutputStream(this.socket.getOutputStream());
            clientInput = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            InputStream in = socket.getInputStream();
            System.out.println(clientInput.readLine());
            while (!isClosed) {
                input = userInput.readLine();
                clientOutput.writeBytes(input + "\n");
                String clientCommand = input.split(" ")[0].toUpperCase();
                if (!clientCommand.equals("DOWNLOAD") && !clientCommand.equals("UPLOAD")) {
                    output = clientInput.readLine();
                    System.out.println("Received: " + output);
                } else if (clientCommand.equals("DOWNLOAD")) {
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
                    try {
                        out = new FileOutputStream(receivedFile, append);
                    } catch (FileNotFoundException ex) {
                        System.out.println("File not found. ");
                    }

                    int count = 0;
                    int packet = 0;
                    boolean written = false;
                    try {
                        while ((count = in.read(fileInput)) > 0) {
                            written = false;
                            System.out.println("Packet: " + packet + " out of " + numPackets);
                            out.write(fileInput, 0, count);
                            written = true;
                            clientOutput.writeBytes("got" + "\n");
                            System.out.println("Packet " + packet + " got");
                            packet++;
                            if (packet == numPackets) {
                                break;
                            }
                        }
                    } finally {
                        System.out.println("BAM BAM BAM!");
                        if (!written) {
                            out.write(fileInput, 0, count);
                        }
                    }
                    System.out.println("File got.");
                    out.close();
                    System.out.println("File closed.");
                } else {
                    String fileName = input.split(" ")[1];
                        if (clientFileName != null && !expired && clientFileName.equals(fileName) && !uploadComplete) {
                            clientOutput.writeBytes("Continue");
                            in.close();
                            in = new FileInputStream(file);
                            //currentPacket--;
                            in.skip(currentPacket * 8 * 1024);
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
                        byte[] byteArray = new byte[8 * 1024];
                        int numPackets = (int) Math.ceil(((double) file.length() / byteArray.length)) - currentPacket;
                        try {
                            out = socket.getOutputStream();
                        } catch (FileNotFoundException ex) {
                            System.out.println("File not found. ");
                        }
                        clientOutput.writeBytes(Integer.toString(numPackets) + "\n");
                        clientOutput.flush();
                        // Get the size of the file
                        long length = file.length();
                        int count;
                        String answer;
                        while ((count = in.read(byteArray)) > 0) {
                            System.out.println("Packet: " + currentPacket);
                            try {
                                TimeUnit.MILLISECONDS.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            out.write(byteArray, 0, count);
                            answer = clientInput.readLine();
                            if (answer.equals("got")) {
                                currentPacket++;
                            }
                        }
                        System.out.println("File sent.");

                        System.out.println("File closed.");
                        currentPacket = 0;
                        uploadComplete = true;
                    } else {
                        String result = "File is not found/available";
                        clientOutput.writeBytes(result);
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
