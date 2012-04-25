/**
 * 
 */
package org.javarosa.j2me.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import org.javarosa.core.reference.Reference;

/**
 * A J2ME File reference is a reference type which refers to a 
 * FileConnection on a j2me phone. It is assumed that the
 * file reference is provided with a local file root
 * which is valid on the device (For which helper utilities
 * can be found in the J2meFileSystemProperties definition).
 * 
 * Note: J2ME File Connections must be managed carefully 
 * (Multiple connections to a single file cannot exist),
 * and this object cannot guarantee (yet) thread safety
 * on access to a single connection.
 * 
 * @author ctsims
 *
 */
public class J2meFileReference implements Reference
{
	//We really shouldn't need a lot of these
	private static final int MAX_CONNECTIONS = 5;
	
	private static Hashtable<String, FileConnection> connections = new Hashtable<String, FileConnection>();
	
	//Queue for existing connections to allow for good caching
	private static Vector<String> connectionList = new Vector<String>();
	
	String localPart;
	String referencePart;
	
	/**
	 * Creates a J2ME file reference of the format 
	 * 
	 * "jr://file"+referencePart"
	 * 
	 * which refers to the URI
	 * 
	 * "file:///" + localPart + referencePart
	 * 
	 * @param localPart
	 * @param referencePart
	 */
	public J2meFileReference(String localPart, String referencePart) {
		this.localPart = localPart;
		this.referencePart = referencePart;
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.reference.Reference#getStream()
	 */
	public boolean doesBinaryExist() throws IOException {
		//We do this a lot for many different things, 
		//no need to cache purely based on this
		return connector(false).exists();
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.reference.Reference#getStream()
	 */
	public InputStream getStream() throws IOException {
		return connector().openInputStream();
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.reference.Reference#getURI()
	 */
	public String getURI() {
		return "jr://file" + referencePart;
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.reference.Reference#isReadOnly()
	 */
	public boolean isReadOnly() {
		return false;
	}
	

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.reference.Reference#getOutputStream()
	 */
	public OutputStream getOutputStream() throws IOException {
		FileConnection connector = connector();
		if(!connector.exists()) {
			connector.create();
			if(!connector.exists()) {
				throw new IOException("File still doesn't exist at  " + this.getLocalURI() + " after create worked succesfully. Reference is probably incorrect");
			}
		} else {
			//TODO: Delete exist file, maybe? Probably....
		}
		return connector.openOutputStream();
	}
	
	protected FileConnection connector() throws IOException {
		return connector(true);
	}
	
	protected FileConnection connector(boolean cache) throws IOException {
		return connector(getLocalURI(), cache);
	}
	
	protected FileConnection connector(String uri, boolean cache) throws IOException {
		synchronized (connections) {
			// We only want to allow one connection to a file at a time.
			// Otherwise we can get into trouble when we want to remove it.
			if (connections.containsKey(uri)) {
				return connections.get(uri);
			} else {
				FileConnection connection = (FileConnection) Connector.open(uri);
				
				if(cache) {
					//These connections aren't cheap, we can't afford to keep all that many around
					if(connectionList.size() == MAX_CONNECTIONS) {
						//FIFO, make room for one more
						clearReferenceConnection(connectionList.elementAt(0));
					}
					
					// Store the newly opened connection for reuse.
					connections.put(uri, connection);
					
					//Add it to the end of the queue for management
					connectionList.addElement(uri);
				}
				return connection;
			}
		}
	}
	
	private void clearReferenceConnection(String ref) {
		synchronized(connectionList) {
			connectionList.removeElement(ref);
			connections.remove(ref);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.reference.Reference#remove()
	 */
	public void remove() throws IOException {
		FileConnection con = connector();
		
		//TODO: this really needs to be written better, but 
		//for now avoiding deadlock is better than ensuring
		//thread safety
		
		//Take a lock on the connection so that nothing tries
		//to access it while this happens
		synchronized(con) {
			con.delete();
			con.close();
		}
		
		//Take a lock on the connections so that
		//nothing can retrieve the connection
		synchronized(connections) {
			//Remove the local connection now that it's 
			//closed.
			clearReferenceConnection(getLocalURI());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.reference.Reference#getLocalURI()
	 */
	public String getLocalURI() {
		return "file:///" + localPart + referencePart;
	}
	
	public Reference[] probeAlternativeReferences() {
		//showtime
		String local = this.getLocalURI();
		String folder = local.substring(0,local.lastIndexOf('/') + 1);
		
		String folderPart = folder.substring(("file:///" + localPart).length(),folder.length());
		
		int finalIndex = local.length();
		if(local.lastIndexOf('.') != -1 && local.lastIndexOf('/') < local.lastIndexOf('.')) {
			finalIndex = local.lastIndexOf('.');
		}
		String fileNoExt = local.substring(local.lastIndexOf('/') + 1, finalIndex);
		
		try {
			Vector<Reference> results = new Vector<Reference>();
			for(Enumeration en = connector(folder, false).list(fileNoExt + ".*", true) ; en.hasMoreElements() ; ) {
				String file = (String)en.nextElement();
				String referencePart = folderPart + file;
				if(!referencePart.equals(this.referencePart)) {
					results.addElement(new J2meFileReference(localPart, referencePart));
				}
			}
			
			Reference[] refs = new Reference[results.size()];
			for(int i = 0 ; i < refs.length ; ++i) {
				refs[i] = results.elementAt(i);
			}
			
			return refs;
		} catch (IOException e) {
			return new Reference[0];
		}
	}
	
	/**
	 * Cleanup task in case it becomes necessary. Not sure yet if it will
	 */
	public static void clearConnectionCache() {
		synchronized(connections) {
			connections.clear();
			connectionList.removeAllElements();
		}
	}
}
