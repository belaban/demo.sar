package org.jboss.client;


import org.jboss.Data;
import org.jgroups.ReceiverAdapter;
import org.jgroups.util.Util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public abstract class ChatCore extends ReceiverAdapter {
    protected String             username=null;
    protected final Set<String>  users=new HashSet<String>();
    protected String             host;
    protected int                port;
    protected Socket             sock;
    protected DataOutputStream   out;



    public ChatCore(String host, int port, String user) {
        this.host=host;
        this.port=port;
        username=user;
        try {
            if(username == null)
                username=System.getProperty("user.name");
        }
        catch(Throwable t) {
        }
    }

    abstract void showMessage(String msg);
    abstract void memberJoined(String name);
    abstract void memberLeft(String name);
    abstract void newView(String view);
    abstract void usersReceived(Set<String> users);

    public void connect() throws Exception {
        sock=new Socket(host, port);
        out=new DataOutputStream(sock.getOutputStream());
        send(new Data(Data.JOIN, username));
    }

    public void sendGetUsers() {
        send(new Data(Data.GET_USERS, null));
    }

    public void destroy() {
        Util.close(out);
        Util.close(sock);
    }

    public void handleSocket() throws Exception {
        DataInputStream in=new DataInputStream(sock.getInputStream());
        while(!sock.isClosed()) {
            Data data=(Data)Util.readStreamable(Data.class, in);
            String payload=data.getPayload();

            switch(data.getType()) {
                case Data.MESSAGE:
                    showMessage(payload);
                    break;
                case Data.VIEW:
                    newView(payload);
                    break;
                case Data.JOIN:
                    memberJoined(payload);
                    break;
                case Data.LEAVE:
                    memberLeft(payload);
                    break;
                case Data.USERS:
                    usersReceived(data.getList());
                    break;
                default:
                    throw new IllegalArgumentException("type " + data.getType() + " unknown");
            }
        }
    }



    protected void send(String msg) {
        try {
            String tmp=username + ": " + msg;
            Data data=new Data(Data.MESSAGE, tmp);
            Util.writeStreamable(data, out);
            out.flush();
        }
        catch(Exception e) {
            System.err.println("Failed sending message: " + e);
        }
    }

    protected void send(Data data) {
        try {
            Util.writeStreamable(data, out);
            out.flush();
        }
        catch(Exception e) {
            System.err.println("Failed sending message: " + e);
        }
    }

}
