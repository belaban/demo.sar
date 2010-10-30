package org.jboss;

import org.jgroups.util.Streamable;
import org.jgroups.util.Util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Bela Ban
 * @version $Id$
 */
public class Data implements Streamable {
    public static final int MESSAGE   = 1; // client to server, server to client
    public static final int VIEW      = 2; // server to client
    public static final int JOIN      = 3; // client to server
    public static final int LEAVE     = 4; // client to server
    public static final int GET_USERS = 5; // client to server
    public static final int USERS     = 6; // server to client

    private int          type;
    private String       payload=null;
    private Set<String>  list;

    public Data() {}

    public Data(int type, String payload) {
        this.type=type;
        this.payload=payload;
    }

    public Data(int type, String payload, Set<String> list) {
        this.type=type;
        this.payload=payload;
        this.list=list;
    }

    public String getPayload() {
        return payload;
    }

    public int getType() {
        return type;
    }

    public Set<String> getList() {
        return list;
    }

    public String toString() {
        return typeToString(type) + (payload != null? ": " + payload : "") + (list != null? ", list=" + list : "");
    }

    private static String typeToString(int type) {
        switch(type) {
            case MESSAGE:   return "MESSAGE";
            case VIEW:      return "VIEW";
            case JOIN:      return "JOIN";
            case LEAVE:     return "LEAVE";
            case GET_USERS: return "GET_USERS";
            case USERS:     return "USERS";
            default:        return "n/a";
        }
    }

    public void writeTo(DataOutputStream out) throws IOException {
        out.writeInt(type);
        Util.writeString(payload, out);
        if(list == null || list.isEmpty()) {
            out.writeInt(0);
        }
        else {
            out.writeInt(list.size());
            for(String el: list)
                out.writeUTF(el);
        }
    }

    public void readFrom(DataInputStream in) throws IOException, IllegalAccessException, InstantiationException {
        type=in.readInt();
        payload=Util.readString(in);
        int length=in.readInt();
        if(length > 0) {
            list=new HashSet<String>();
            for(int i=0; i < length; i++)
                list.add(in.readUTF());
        }
    }
}
