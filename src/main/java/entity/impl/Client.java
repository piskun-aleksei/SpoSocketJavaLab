package entity.impl;

import entity.BasicInterface;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;



public class Client implements BasicInterface{
    private static String receivedFile;
    private static boolean isClosed = false;
    private static BufferedReader clientInput = null;
    private InputStream in = null;
    private OutputStream out = null;
    private Socket socket;
    private int sentPackages;


    public void start() throws IOException{
        String input;
        String output;
        byte[] fileInput = new byte[8*1024];
        while(true) {
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
                if(!clientCommand.equals("DOWNLOAD") && !clientCommand.equals("UPLOAD")) {
                    output = clientInput.readLine();
                    System.out.println("Received: " + output);
                }
                else if(clientCommand.equals("DOWNLOAD")){
                    receivedFile = "downloaded_" + input.split(" ")[1];
                    String isContinue = clientInput.readLine();
                    System.out.println(isContinue);
                    boolean append = false;
                    if(isContinue.equals("Continue"))
                    {
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
                    }
                    finally {
                        System.out.println("BAM BAM BAM!");
                        if(!written) {
                            out.write(fileInput, 0, count);
                        }
                    }
                    System.out.println("File got.");
                    out.close();
                    System.out.println("File closed.");
                    //DataInputStream dataInputStream = new DataInputStream(this.socket.getInputStream());
                    /*File file = new File(receivedFile);
                    file.createNewFile();
                    Integer numPackets = 0;
                    if(numPackets != 0){
                        clientOutput.write(this.sentPackages);
                    }
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(receivedFile));
                    String inputFromServer = clientInput.readLine();
                    numPackets = tryParse(inputFromServer);
                    if(numPackets != null) {
                        System.out.println("Number of packages is " + numPackets);
                        for (int i = 0; i < numPackets - 1; i++) {
                            try {
                                clientInput.read(fileInput);
                                System.out.println("Received package number " + i);
                                bos.write(String.valueOf(fileInput).getBytes());
                                Arrays.fill(fileInput, (char) 0);
                            } catch (SocketTimeoutException | SocketException e) {
                                try {
                                    System.out.println("Lost connection to client. Waiting...");
                                    TimeUnit.SECONDS.sleep(30);
                                    if (!this.checkServer()) {
                                        this.sentPackages = i;
                                        System.out.println("Server timeout");
                                        this.socket.close();
                                        isClosed = true;
                                    }
                                }
                                catch (InterruptedException e2){
                                    e2.printStackTrace();
                                }
                            }
                        }
                        clientInput.read(fileInput);
                        bos.write(String.valueOf(fileInput).trim().getBytes());
                        bos.flush();
                        bos.close();
                        System.out.println("Received file ");
                    }
                    else{
                        System.out.println(inputFromServer);
                    }*/
                }
                else {
                    File file = new File(input.split(" ")[1]);
                    if(file.isFile() & file.canRead()) {
                        byte[] byteArray = new byte[1024];
                        Arrays.fill(byteArray, (byte)0);
                        int numPackets = (int)Math.ceil(((double)file.length()/byteArray.length));
                        clientOutput.writeBytes(Integer.toString(numPackets) + "\n");
                        clientOutput.flush();
                        FileInputStream fis = null;
                        try {
                            fis = new FileInputStream(file);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        DataInputStream bis = new DataInputStream(fis);
                        for(int i = 0; i < numPackets - 1; i++) {
                            bis.read(byteArray, 0, byteArray.length);
                            clientOutput.write(byteArray, 0, byteArray.length);
                            clientOutput.flush();
                        }
                        int lastPackage = (int)(file.length() - byteArray.length*(numPackets-1));
                        bis.read(byteArray, 0, lastPackage );
                        clientOutput.write(byteArray, 0, lastPackage);
                        System.out.println("File was send");
                        bis.close();
                    }
                    else {
                        String result = "File is not found/available";
                        clientOutput.writeBytes(result + "\n");
                        clientOutput.flush();
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
    private boolean checkServer() throws IOException{
        boolean result = true;
        try {
            clientInput.readLine();
        }
        catch (SocketException e){
            result = false;
        }
        return result;
    }
}
