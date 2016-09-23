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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.tools.vscode.internal.ipc.IPCPlugin;

public abstract class AbstractConnection implements Connection {

	private MessageListener listener;

	protected final BlockingQueue<TransportMessage> inboundQueue = new LinkedBlockingQueue<TransportMessage>();
	protected final BlockingQueue<TransportMessage> outboundQueue = new LinkedBlockingQueue<TransportMessage>();

	protected DispatcherThread dispatcherThread;

	/**
	 * Dispatches the messages received
	 */
	protected class DispatcherThread extends Thread {

		private boolean stopped;

		public DispatcherThread() {
			super("LSP_DispatcherThread");
		}

		@Override
		public void run() {
			TransportMessage message;
			try {
				while (!stopped) {
					message = inboundQueue.poll(1, TimeUnit.SECONDS);
					if (message == null) {
						continue;
					}
					if (listener != null) {
						try {
							IPCPlugin.logInfo("Dispatch incoming" + message.getContent());
							listener.messageReceived(message);
						} catch (Exception e) {
							IPCPlugin.logException("Exception on incoming message dispatcher", e);
						}
					}
				}
			} catch (InterruptedException e) {
				// stop the dispatcher thread
			}
		}

		public void shutdown() {
			stopped = true;
		}
	}

	@Override
	public void setMessageListener(MessageListener listener) {
		if (this.listener != null && this.listener == listener) {
			throw new IllegalStateException("Can not set listener multiple times");
		}
		this.listener = listener;
	}
	
	
	/**
	 * Must be called by implementers to start dispatching of incoming messages.
	 */
	protected void startDispatcherThread() {
		dispatcherThread = new DispatcherThread();
		dispatcherThread.setDaemon(true);
		dispatcherThread.start();
	}
}
