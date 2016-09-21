/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.langs.transport;

import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.*;
import java.nio.channels.Channels;

public class NamedPipeConnection extends AbstractStreamConnection {
	
	private static String OS = System.getProperty("os.name").toLowerCase();

	private final String readFileName;
	private final String writeFileName;
	
	//used for windows
	private RandomAccessFile writeFile;
	private RandomAccessFile readFile;
	// used on POSIX
	private AFUNIXSocket writeSocket;
	private AFUNIXSocket readSocket;
	
	public NamedPipeConnection(String readFileName, String writeFileName) {
		this.readFileName = readFileName;
		this.writeFileName = writeFileName;
	}

	@Override
	public void close() {
		try {
			if (writeFile != null) 
				writeFile.close();
			if (readFile != null) 
				readFile.close();
			if(readSocket != null)
				readSocket.close();
			if(writeSocket != null )
				writeSocket.close();
		} catch (IOException e) {
			// TODO: handle exception
		}
	}

	@Override
	protected InputStream connectReadChannel() throws IOException{
		final File rFile = new File(readFileName);
		if(isWindows()){
			readFile = new RandomAccessFile(rFile, "rwd");
			return Channels.newInputStream(readFile.getChannel());
		}else{
			readSocket = AFUNIXSocket.newInstance();
			readSocket.connect(new AFUNIXSocketAddress(rFile));
			return readSocket.getInputStream();
		}
	}

	@Override
	protected OutputStream connectWriteChannel() throws IOException{
		final File wFile = new File(writeFileName);
		
		if(isWindows()){
			writeFile = new RandomAccessFile(wFile, "rwd");
			return Channels.newOutputStream(writeFile.getChannel());
		}else{
			writeSocket = AFUNIXSocket.newInstance();
			writeSocket.connect(new AFUNIXSocketAddress(wFile));
			return writeSocket.getOutputStream();
		}
	}
	
	private boolean isWindows() {
        return (OS.indexOf("win") >= 0);
	}

}
