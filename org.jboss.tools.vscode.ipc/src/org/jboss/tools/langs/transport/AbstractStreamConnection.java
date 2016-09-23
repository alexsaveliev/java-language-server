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
import java.util.concurrent.TimeUnit;

public abstract class AbstractStreamConnection extends AbstractConnection {

	private InputStream inputStream;
	private OutputStream outputStream;

	private ReaderThread readerThread;
	private WriterThread writerThread;

	private class ReaderThread extends Thread {

		private final InputStream stream;

		private boolean stopped;

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

			LineReader reader = new LineReader(this.stream);

			while(!stopped){
				TransportMessage message;
				try {
					// SOURCEGRAPH: replaced TransportMessage.fromStream with TransportMessage.fromReader
					// to reuse reader
					message = TransportMessage.fromReader(reader, DEFAULT_CHARSET);
					if(message == null ){
						//Stream disconnected exit reader thread
						break;
					}
					inboundQueue.add(message);
				} catch (IOException e) {
					//continue
				}
			}
		}

		public void shutdown() {
			stopped = true;
			try {
				stream.close();
			} catch (IOException e) {
				// TODO
			}
		}
	}

	private class WriterThread extends Thread {

		private final OutputStream stream;

		private boolean stopped;

		WriterThread(OutputStream output) {
			super("LSP_WriterThread");
			this.stream = output;
		}

		@Override
		public void run() {
			while (true) {
				try {
					TransportMessage message = outboundQueue.poll(1, TimeUnit.SECONDS);
					if (message == null) {
						if (stopped) {
							break;
						}
						continue;
					}
					message.send(stream, DEFAULT_CHARSET);
					stream.flush();
				} catch (InterruptedException e) {
					break;//exit
				} catch (IOException e) {
					// NOOP
				}
			}
			try {
				stream.close();
			} catch (IOException e) {
				// TODO
			}
		}

		public void shutdown() {
			this.stopped = true;
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
		readerThread = new ReaderThread(inputStream);
		readerThread.setDaemon(true);
		readerThread.start();
	}

	public void startWriterThread() throws IOException{
		this.outputStream = connectWriteChannel();
		writerThread = new WriterThread(outputStream);
		writerThread.setDaemon(true);
		writerThread.start();
	}

	@Override
	public void close() {
		dispatcherThread.shutdown();
		readerThread.shutdown();
		writerThread.shutdown();
	}

	protected abstract InputStream connectReadChannel() throws IOException;
	protected abstract OutputStream connectWriteChannel() throws IOException;


}
