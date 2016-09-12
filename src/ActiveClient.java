import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by mikkel on 12-09-2016.
 */
public class ActiveClient extends Thread {
    // the socket where to listen/talk
    private Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private int id;
    private String username;
    private Date connectedDate;
    private Server connectedServer;
    private Timer aliveTimer;

    public ActiveClient(Socket socket, int id, Server connectedServer) {
        this.id = id;
        this.socket = socket;
        this.connectedServer = connectedServer;
                try
        {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream  = new ObjectInputStream(socket.getInputStream());
            username = (String) inputStream.readObject();
            connectedServer.display(username + " connected.");
            connectedDate = new Date();

        }
        catch (IOException e) {

            connectedServer.display("Error. Could not create IO streams: " + e);
            return;
        } catch (ClassNotFoundException e) {
            connectedServer.display("Error. Could not create IO streams: " + e);
            return;
        }

    }

    public synchronized void run() {
        boolean connected = true;
        Message message;
        while(connected) {
            try {
                message = (Message) inputStream.readObject();
            }
            catch (IOException ex) {
                connectedServer.display(username + " Error reading Streams: " + ex);
                break;
            }
            catch(ClassNotFoundException ex2) {
                connectedServer.display(username + " Error reading Streams: " + ex2);
                break;
            }

            switch(message.getType()) {

                case Message.DATA:
                    connectedServer.broadcast(message);
                    break;
                case Message.QUIT:
                    connectedServer.broadcast(new Message(username, Message.DATA ," disconnected by own will"));
                    connected = false;
                    break;
                case Message.ALVE:
                    alive();
                    break;
                case Message.JOIN:
                    for(ActiveClient activeClient: connectedServer.getClientList()){
                        //Run them for each name.... but then it is already kinda accepted
                    }
                    break;
                default:
                    break;
            }
        }
        connectedServer.removeClient(id);
        close();
    }

    public void close() {
        try {
            if(outputStream != null) outputStream.close();
            if(inputStream != null) inputStream.close();
            if(socket != null) socket.close();
        }
        catch(Exception e) {
        }
    }

    public boolean writeToThisClient(Message message) {
        if(!socket.isConnected()) {
            close();
            return false;
        }
        try {
            outputStream.writeObject(message);
        }
        catch(IOException e) {
            connectedServer.display("Error sending message to " + username);
            connectedServer.display(e.toString());
        }
        return true;
    }

    private void alive(){
        aliveTimer.cancel();
        aliveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                close();
                connectedServer.broadcast(new Message(username, Message.DATA ," disconnected by dropout"));
            }
        }, 0, 70000);
    }

    public String getUsername() {
        return username;
    }

}