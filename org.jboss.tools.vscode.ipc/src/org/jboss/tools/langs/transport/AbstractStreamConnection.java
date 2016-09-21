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

import org.jboss.tools.vscode.internal.ipc.IPCPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public abstract class AbstractStreamConnection extends AbstractConnection {

	private InputStream inputStream;
	private OutputStream outputStream;

	private class ReaderThread extends Thread {

		private final InputStream stream;

		ReaderThread(InputStream input ) {
			super("LSP_ReaderThread");
			this.stream = input;
		}

		@Override
		public void run() {
			startDispatcherThread();
			try {
				startWriterThread();
			} catch (IOException e1) {
				//TODO: write failed to connect.
			}

			while(true){
				TransportMessage message;
				try {
					message = TransportMessage.fromStream(stream, DEFAULT_CHARSET);
					if(message == null ){
						//Stream disconnected exit reader thread
						IPCPlugin.logError("Empty message read");
						break;
					}
					inboundQueue.add(message);
				} catch (IOException e) {
					//continue
				}
			}
		}
	}

	private class WriterThread extends Thread {

		private final OutputStream stream;

		WriterThread(OutputStream output) {
			super("LSP_WriterThread");
			this.stream = output;
		}

		@Override
		public void run() {
			while (true) {
				try {
					TransportMessage message = outboundQueue.take();
					message.send(stream, DEFAULT_CHARSET);
					stream.flush();
				} catch (InterruptedException e) {
					break;//exit
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	@Override
	public void send(TransportMessage message) {
		if(message != null)
			outboundQueue.add(message);
	}

	@Override
	public void start() throws IOException {
		this.inputStream = connectReadChannel();
		ReaderThread readerThread = new ReaderThread(inputStream);
		readerThread.setDaemon(true);
		readerThread.start();
	}
	
	public void startWriterThread() throws IOException{
		this.outputStream = connectWriteChannel();
		WriterThread writerThread = new WriterThread(outputStream);
		writerThread.setDaemon(true);
		writerThread.start();
	}
	
	@Override
	public void close() {
		try {
			inputStream.close();
		} catch (IOException e) {
			// TODO
		}
		try {
			outputStream.close();
		} catch (IOException e) {
			// TODO
		}
	}
	
	protected abstract InputStream connectReadChannel() throws IOException;
	protected abstract OutputStream connectWriteChannel() throws IOException;


}
