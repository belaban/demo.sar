package org.jboss;

/**
 * @author Bela Ban
 * @version $Id$
 */
public interface ChatMBean {
    void postMessage(String msg);
    String getLocalAddress();
    String getMembers();
}
