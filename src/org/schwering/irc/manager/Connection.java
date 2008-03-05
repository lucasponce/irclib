package org.schwering.irc.manager;

import java.io.IOException;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCEventListener;
import org.schwering.irc.lib.IRCUser;
import org.schwering.irc.manager.event.CTCPListener;
import org.schwering.irc.manager.event.ConnectionListener;
import org.schwering.irc.manager.event.PrivateMessageListener;
import org.schwering.irc.manager.event.UnexpectedEventListener;

/**
 * A wrapper for <code>IRCConnection</code> and interface to various
 * administration tasks of an IRC connection.
 * <p>
 * This class manages the wrapped <code>IRCConnection</code> object and
 * a set of the joined channels. Additionally, a connection is the point where 
 * <code>ConnectionListener</code>s, <code>CTCPListener</code>s,
 * <code>PrivateMessageListener</code>s, and
 * <code>UnexpectedEventListener</code>s are registered.
 * @author Christoph Schwering &lt;schwering@gmail.com&gt;
 * @since 2.00
 * @version 1.00
 */
public class Connection {
	private IRCConnection conn;
	private SortedMap channels = new TreeMap();
	private boolean requestModes = true;
	private Collection connectionListeners = new LinkedList();
	private Collection ctcpListeners = new LinkedList();
	private Collection privateMessageListeners = new LinkedList();
	private Collection unexpectedEventListeners = new LinkedList();
	
	/**
	 * Initializes a new connection. This means that the internal
	 * <code>IRCConnection</code> is initalized with the connection data
	 * specified as constructor arguments and some other specific values.
	 * Further configuration can be done via the <code>Connection</code>
	 * class's methods.
	 */
	Connection(String host, int portMin, int portMax, String pass, String nick,
			String username, String realname) {
		conn = new IRCConnection(host, portMin, portMax, pass, nick, username, 
				realname);
		conn.setPong(true);
		conn.setColors(false);
		conn.addIRCEventListener(new BasicListener(this));
	}

	/**
	 * Initializes a new connection. This means that the internal
	 * <code>IRCConnection</code> is initalized with the connection data
	 * specified as constructor arguments and some other specific values.
	 * Further configuration can be done via the <code>Connection</code>
	 * class's methods.
	 */
	Connection(String host, int[] ports, String pass, String nick,
			String username, String realname) {
		conn = new IRCConnection(host, ports, pass, nick, username, realname);
		conn.setPong(true);
		conn.setColors(false);
		conn.addIRCEventListener(new BasicListener(this));
	}
	
	/**
	 * Returns the connected user's current nickname.
	 * This method simply calls <code>IRCConnection.getNick</code>.
	 * @see IRCConnection#getNick()
	 */
	public String getNickname() {
		return conn.getNick();
	}
	
	/**
	 * Returns the IRC server's hostname.
	 * This method simply calls <code>IRCConnection.getHost</code>.
	 * @see IRCConnection#getHost()
	 */
	public String getServerHostname() {
		return conn.getHost();
	}
	
	/**
	 * Returns the encoding of the connection.
	 * This method simply calls <code>IRCConnection.getEncoding</code>.
	 * @see IRCConnection#getEncoding()
	 */
	public String getEncoding() {
		return conn.getEncoding();
	}
	
	/**
	 * Sets the encoding of the connection.
	 * This method simply calls <code>IRCConnection.setEncoding</code>.
	 * @see IRCConnection#setEncoding(String)
	 */
	public void setEncoding(String encoding) {
		conn.setEncoding(encoding);
	}

	/**
	 * Returns the timeout of the connection.
	 * This method simply calls <code>IRCConnection.getTimeout</code>.
	 * @see IRCConnection#getTimeout()
	 */
	public int getTimeout() {
		return conn.getTimeout();
	}
	
	/**
	 * Sets the timeout of the connection.
	 * This method simply calls <code>IRCConnection.setTimeout</code>.
	 * @see IRCConnection#setTimeout(int)
	 */
	public void setTimeout(int millis) {
		conn.setTimeout(millis);
	}
	
	/**
	 * Starts the connection.
	 * This method simply calls <code>IRCConnection.connect</code>.
	 * @throws IOException If an I/O error occurs.
	 * @throws SocketException If the connect method was already invoked.
	 */
	public void connect() throws IOException, SocketException {
		conn.connect();
	}
	
	/**
	 * Indicates whether the connection is alive. This is the case if
	 * <code>connect</code> has been invoked. Note that <code>isConnected</code>
	 * being <code>true</code> does not necessarily require 
	 * <code>ConnectionListener.connectionEstablished</code> to be fired.
	 * This method simply calls <code>IRCConnection.isConnected</code>.
	 * @see #connect()
	 * @see IRCConnection#isConnected()
	 */
	public boolean isConnected() {
		return conn.isConnected();
	}

	/**
	 * Indicates whether directly after joining a channel, the channel modes
	 * are requested automatically.
	 * By default, this is <code>true</code>.
	 */
	public boolean getRequestModes() {
		return requestModes;
	}
	
	/**
	 * Indicates whether directly after joining a channel, the channel modes
	 * are requested automatically.
	 * By default, this is <code>true</code>.
	 */
	public void setRequestModes(boolean requestModes) {
		this.requestModes = requestModes;
	}
	
	/**
	 * Returns a set of <code>Channel</code> objects that the connection
	 * participates in.
	 */
	public Set getChannels() {
		return Collections.unmodifiableSet(channels.keySet());
	}
	
	/**
	 * Returns the <code>Channel</code> object corresponding to the channel
	 * with the specified channel name if connection participates in this
	 * channel. Otherwise <code>null</code> is returned.
	 * TODO Remove either this or resolveChannel().
	 */
	public Channel getChannel(String channelName) {
		return (Channel)channels.get(channelName);
	}
	
	/**
	 * Returns a <code>Channel</code> object that contains all known 
	 * information about the channel. This is rather empty if the connection
	 * has not joined the respective channel.
	 * TODO Remove either this or resolveChannel().
	 */
	public Channel resolveChannel(String channelName) {
		if (channelName == null) {
			throw new IllegalArgumentException();
		}
		Channel channel = (Channel)channels.get(channelName);
		return channel != null ? channel : new Channel(channelName);
	}
	
	/**
	 * Returns a <code>User</code> object that contains all known 
	 * information about a user. The <code>User</code> object may contain 
	 * further information like the user's host, username and away status.
	 * <p>
	 * First, the method searches a <code>User</code> object in the joined
	 * channels. If this fails, a new <code>User</code> is created.
	 */
	public User resolveUser(String nick) {
		if (nick == null) {
			throw new IllegalArgumentException();
		}
		for (Iterator it = getChannels().iterator(); it.hasNext(); ) {
			Channel channel = (Channel)it.next();
			User user = channel.getUser(nick);
			if (user != null) {
				return user;
			}
		}
		return new User(nick);
	}
	
	/**
	 * Returns a <code>User</code> object that contains all known 
	 * information about a user. The <code>User</code> object may contain 
	 * further information like the user's host, username and away status.
	 * <p>
	 * First, the method searches a <code>User</code> object in the joined
	 * channels. If this fails, a new <code>User</code> is created.
	 */
	public User resolveUser(IRCUser ircUser) {
		if (ircUser == null) {
			throw new IllegalArgumentException();
		}
		for (Iterator it = getChannels().iterator(); it.hasNext(); ) {
			Channel channel = (Channel)it.next();
			User user = channel.getUser(ircUser.getNick());
			if (user != null) {
				user.update(ircUser);
				return user;
			}
		}
		return new User(ircUser);
	}
	
	/**
	 * Adds a pure <code>IRCEventListener</code> to the pure
	 * <code>IRCConnection</code>.
	 */
	public void addIRCEventListener(IRCEventListener listener) {
		conn.addIRCEventListener(listener);
	}
	
	/**
	 * Removes a pure <code>IRCEventListener</code> from the pure
	 * <code>IRCConnection</code>.
	 */
	public void removeIRCEventListener(IRCEventListener listener) {
		conn.removeIRCEventListener(listener);
	}
	
	/* ConnectionListener methods */
	
	public void addConnectionListener(ConnectionListener listener) {
		connectionListeners.add(listener);
	}
	
	public void removeConnectionListener(ConnectionListener listener) {
		connectionListeners.remove(listener);
	}
	
	void fireConnectionEstablished() {
		for (Iterator it = connectionListeners.iterator(); it.hasNext(); ) {
			((ConnectionListener)it.next()).connectionEstablished();
		}
	}
	
	void fireConnectionLost() {
		for (Iterator it = connectionListeners.iterator(); it.hasNext(); ) {
			((ConnectionListener)it.next()).connectionLost();
		}
	}
	
	void fireErrorReceived(Message msg) {
		for (Iterator it = connectionListeners.iterator(); it.hasNext(); ) {
			((ConnectionListener)it.next()).errorReceived(msg);
		}
	}
	
	void fireMotdReceived(String[] motd) {
		for (Iterator it = connectionListeners.iterator(); it.hasNext(); ) {
			((ConnectionListener)it.next()).motdReceived(motd);
		}
	}
	
	void firePingReceived(Message msg) {
		for (Iterator it = connectionListeners.iterator(); it.hasNext(); ) {
			((ConnectionListener)it.next()).pingReceived(msg);
		}
	}
	
	void fireChannelJoined(Channel channel) {
		for (Iterator it = connectionListeners.iterator(); it.hasNext(); ) {
			((ConnectionListener)it.next()).channelJoined(channel);
		}
	}
	
	void fireChannelLeft(Channel channel) {
		for (Iterator it = connectionListeners.iterator(); it.hasNext(); ) {
			((ConnectionListener)it.next()).channelLeft(channel);
		}
	}
	
	void fireInvited(Channel channel, User user) {
		for (Iterator it = connectionListeners.iterator(); it.hasNext(); ) {
			((ConnectionListener)it.next()).invited(channel, user);
		}
	}
	
	/* PrivateMessageListener methods */
	
	public void addPrivateMessageListener(PrivateMessageListener listener) {
		privateMessageListeners.add(listener);
	}
	
	public void removevPrivateMessageListener(PrivateMessageListener listener) {
		privateMessageListeners.remove(listener);
	}
	
	void firePrivmsgReceived(User user, Message msg) {
		for (Iterator it = privateMessageListeners.iterator(); it.hasNext(); ) {
			((PrivateMessageListener)it.next()).privmsgReceived(user, msg);
		}
	}
	
	void fireNoticeReceived(User user, Message msg) {
		for (Iterator it = privateMessageListeners.iterator(); it.hasNext(); ) {
			((PrivateMessageListener)it.next()).noticeReceived(user, msg);
		}
	}
	
	/* CTCP listener methods (TODO: fireEvent() methods!) */
	
	public void addCTCPListener(CTCPListener listener) {
		ctcpListeners.add(listener);
	}
	
	public void removeCTCPListener(CTCPListener listener) {
		ctcpListeners.remove(listener);
	}
	
	/* UnexpectedEventListener methods */
	
	public void addUnexpectedEventListener(UnexpectedEventListener listener) {
		unexpectedEventListeners.add(listener);
	}
	
	public void removeUnexpectedEventListener(UnexpectedEventListener listener) {
		unexpectedEventListeners.remove(listener);
	}
	
	void fireUnexpectedEventReceived(String event, Object[] args) {
		for (Iterator it = privateMessageListeners.iterator(); it.hasNext(); ) {
			((UnexpectedEventListener)it.next()).unexpectedEventReceived(event, args);
		}
	}
	
}