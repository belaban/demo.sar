package org.jboss;

import org.jboss.beans.metadata.api.annotations.*;
import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.ha.framework.interfaces.GroupMembershipListener;
import org.jboss.ha.framework.server.ClusterPartition;
import org.jgroups.ChannelException;
import org.jgroups.util.DefaultSocketFactory;
import org.jgroups.util.Util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * @author Bela Ban
 * @version $Id$
 */
public class Chat implements Runnable, GroupMembershipListener, ChatMBean {
    private ServerSocket     srv_sock;
    private Thread           runner;
    private volatile boolean running;
    private long             view_id=0;
    private ClusterPartition partition;
    private final List<Connection> connections=new LinkedList<Connection>();
    private final Set<String> users=new HashSet<String>();

    private static final String SERVICE_NAME="ChatDemo";
    private static final int SERVER_PORT=7888;


    public Chat() {
    }


    public ClusterPartition getPartition() {
        return partition;
    }

    @Inject(bean="HAPartition")
    public void setPartition(ClusterPartition partition) {
        this.partition=partition;
    }

    public String getMembers() {
        List<String> members=partition != null? partition.getCurrentView() : null;
        return members != null? members.toString() : "n/a";
    }

    public String getLocalAddress() {
        ClusterNode me=partition != null? partition.getClusterNode() : null;
        return me != null? me.toString() : "n/a";
    }

    @Create
    public void create() throws Exception {
        if(partition == null)
            throw new NullPointerException("partition is null");

        partition.registerGroupMembershipListener(this);
        partition.registerRPCHandler(SERVICE_NAME, this);
        srv_sock=Util.createServerSocket(new DefaultSocketFactory(), SERVICE_NAME, SERVER_PORT);
        System.out.println("listening on " + srv_sock.getLocalSocketAddress());
        runner=new Thread(this, "socket acceptor");
        running=true;
        runner.start();
        sendToAllClients(new Data(Data.VIEW, getMembers()));
    }


    @Start
    public void start() throws ChannelException {
    }

    @Stop
    public void stop() {
    }

    @Destroy
    public void destroy() {
        partition.unregisterRPCHandler(SERVICE_NAME, this);
        running=false;
        synchronized(connections) {
            for(Connection conn: connections)
                conn.destroy();
            connections.clear();
        }
        
        System.out.println("Closing socket " + srv_sock.getLocalSocketAddress());
        Util.close(srv_sock);
        users.clear();
    }

    public void receive(Data data) {
        switch(data.getType()) {
            case Data.MESSAGE:
                postMessage(data.getPayload());
                break;
            case Data.VIEW:
                break;
            case Data.JOIN:
                postMemberJoinedOrLeft(data.getPayload(), true);
                break;
            case Data.LEAVE:
                postMemberJoinedOrLeft(data.getPayload(), false);
                break;
        }
    }

    public void run() {
        while(running) {
            try {
                Socket sock=srv_sock.accept();
                System.out.println("accepted conn from " + sock.getRemoteSocketAddress());
                Connection conn=null;
                try {
                    conn=new Connection(sock);
                    conn.start();
                    synchronized(connections) {
                        connections.add(conn);
                    }
                    conn.send(new Data(Data.VIEW, getMembers()));
                }
                catch(Throwable t) {
                    if(conn != null)
                        conn.destroy();
                    Util.close(sock);
                }
            }
            catch(Exception e) {
                if(running)
                    e.printStackTrace();
            }
        }
    }

    public void postMessage(String msg) {
        try {
            partition.callAsynchMethodOnCluster(SERVICE_NAME, "receiveMessage", new Object[]{msg}, new Class<?>[]{String.class}, false);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void postMemberJoinedOrLeft(String user, boolean joined) {
        try {
            partition.callAsynchMethodOnCluster(SERVICE_NAME, "memberJoinedOrLeft", new Object[]{user, joined},
                                                new Class<?>[]{String.class, boolean.class}, false);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void receiveMessage(String msg) {
        sendToAllClients(new Data(Data.MESSAGE, msg));
    }

    public void memberJoinedOrLeft(String user, boolean joined) {
        Data data=new Data(joined? Data.JOIN : Data.LEAVE, user);
        if(joined)
            users.add(user);
        else
            users.remove(user);
        System.out.println("users = " + users);
        sendToAllClients(data);
    }


    public void membershipChanged(List<ClusterNode> clusterNodes, List<ClusterNode> clusterNodes1, List<ClusterNode> clusterNodes2) {
        if(partition != null && partition.getCurrentViewId() > view_id) {
            view_id=partition.getCurrentViewId();
            System.out.println("view change: " + getMembers());
            sendToAllClients(new Data(Data.VIEW, getMembers()));
        }
    }

    public void membershipChangedDuringMerge(List<ClusterNode> clusterNodes, List<ClusterNode> clusterNodes1, List<ClusterNode> clusterNodes2, List<List<ClusterNode>> lists) {
    }


    protected void sendToAllClients(Data data) {
        if(data == null)
            return;
        synchronized(connections) {
            for(Connection conn: connections) {
                try {
                    conn.send(data);
                }
                catch(Throwable t) {
                    t.printStackTrace(System.err);
                }
            }
        }
    }

    private class Connection implements Runnable {
        private final Socket           socket;
        private final DataOutputStream out;
        private final DataInputStream  in;
        private Thread                 thread;
        private String                 user=null;

        Connection(Socket socket) throws Exception {
            this.socket=socket;
            out=new DataOutputStream(socket.getOutputStream());
            in=new DataInputStream(socket.getInputStream());
        }

        void start() throws Exception {
            thread=new Thread(this, "connection handler " + socket.getRemoteSocketAddress());
            thread.start();
        }

        void destroy() {
            sendToAllClients(new Data(Data.LEAVE, user));
            Util.close(out);
            Util.close(in);
            Util.close(socket);
        }

        public boolean equals(Object obj) {
            if(!(obj instanceof Connection))
                return false;
            Connection other=(Connection)obj;
            return socket == other.socket;
        }

        void send(Data data) {
            try {
                Util.writeStreamable(data, out);
                out.flush();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            while(!socket.isClosed() && socket.isConnected()) {
                try {
                    Data data=(Data)Util.readStreamable(Data.class, in);

                    switch(data.getType()) {
                        case Data.JOIN:
                            user=data.getPayload();
                            break;
                        case Data.GET_USERS:
                            send(new Data(Data.USERS, null, users));
                            break;
                    }
                        
                    receive(data);
                }
                catch(IOException io_ex) {
                    break;
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("closing conn to " + socket.getRemoteSocketAddress());
            synchronized(connections) {
                connections.remove(this);
            }
            postMemberJoinedOrLeft(user, false);
            destroy();
        }
    }
}
