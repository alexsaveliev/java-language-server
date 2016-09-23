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
package org.jboss.tools.vscode.java.internal;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.jboss.tools.langs.transport.AbstractStreamConnection;
import org.jboss.tools.langs.transport.Connection;
import org.jboss.tools.langs.transport.NamedPipeConnection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class JavaLanguageServerPlugin implements BundleActivator {

	public static final String PLUGIN_ID = "org.jboss.tools.vscode.java";
	private static BundleContext context;
	static LanguageServer languageServer;

	private Collection<JavaClientConnection> connections = new LinkedList<>();
	private  ServerSocket socket;

	static BundleContext getContext() {
		return context;
	}

	public static LanguageServer getLanguageServer() {
		return languageServer;
	}
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		JavaLanguageServerPlugin.context = bundleContext;

		WorkingCopyOwner.setPrimaryBufferProvider(new WorkingCopyOwner() {
			@Override
			public IBuffer createBuffer(ICompilationUnit workingCopy) {
				ICompilationUnit original= workingCopy.getPrimary();
				IResource resource= original.getResource();
				if (resource instanceof IFile)
					return new DocumentAdapter(workingCopy, (IFile)resource);
				return DocumentAdapter.Null;
			}
		});

		new CreateConnectionsThread().start();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		JavaLanguageServerPlugin.context = null;
		for (JavaClientConnection connection : connections) {
			connection.disconnect();
		}
		connections.clear();
		if (socket != null) {
			socket.close();
		}
	}

	public static void log(IStatus status) {
		Platform.getLog(JavaLanguageServerPlugin.context.getBundle()).log(status);
	}

	public static void logError(String message) {
		log(new Status(IStatus.ERROR, context.getBundle().getSymbolicName(), message));
	}

	public static void logInfo(String message) {
		log(new Status(IStatus.INFO, context.getBundle().getSymbolicName(), message));
	}

	public static void logException(String message, Throwable ex) {
		log(new Status(IStatus.ERROR, context.getBundle().getSymbolicName(), message, ex));
	}

	protected Iterable<Connection> getConnections() throws IOException {
		if (System.getenv("STDIO_MODE") != null) {
			return Collections.singleton(new AbstractStreamConnection() {

				@Override
				protected InputStream connectReadChannel() throws IOException {
					String dataFile = System.getenv("STDIO_DATA_FILE");
					if (dataFile == null) {
						return System.in;
					} else {
						return new FileInputStream(dataFile);
					}
				}

				@Override
				protected OutputStream connectWriteChannel() throws IOException {
					return System.out;
				}
			});
		} if (System.getenv("SOCKET_MODE") != null) {
			int port = Integer.parseInt(System.getenv("SOCKET_MODE"));
			String host = System.getenv("SOCKET_HOST");
			InetAddress address = host == null ? InetAddress.getLocalHost() : InetAddress.getByName(host);
			socket = new ServerSocket();
			socket.bind(new InetSocketAddress(address, port));
			return new Iterable<Connection>() {
				@Override
				public Iterator<Connection> iterator() {

					return new Iterator<Connection>() {

						private Socket next;
						private boolean done;

						@Override
						public boolean hasNext() {
							return getNext() != null;
						}

						@Override
						public Connection next() {
							Socket socket = getNext();
							next = null;
							if (socket == null) {
								return null;
							}
							return new AbstractStreamConnection() {

								@Override
								protected InputStream connectReadChannel() throws IOException {
									return socket.getInputStream();
								}

								@Override
								protected OutputStream connectWriteChannel() throws IOException {
									return socket.getOutputStream();
								}
							};
						}

						private Socket getNext() {
							if (done) {
								return null;
							}
							if (next != null) {
								return next;
							}
							try {
								next = socket.accept();
								return next;
							} catch (IOException e) {
								logException("Unable to accept incoming connection", e);
								done = true;
								return null;
							}
						}
					};
				}
			};
		} else {
			final String stdInName = System.getenv("STDIN_PIPE_NAME");
			final String stdOutName = System.getenv("STDOUT_PIPE_NAME");
			if (stdInName == null || stdOutName == null) {
				//XXX temporary hack to let unit tests run
				System.err.println("Unable to connect to named pipes");
				return Collections.emptyList();
			}
			return Collections.singleton(new NamedPipeConnection(stdOutName, stdInName));
		}
	}

	private class CreateConnectionsThread extends Thread {

		CreateConnectionsThread() {
			super("Create connections");
			setDaemon(true);
		}

		@Override
		public void run () {
			try {
				for (Connection connection : getConnections()) {
					if (connection == null) {
						break;
					}
					JavaClientConnection javaClientConnection = new JavaClientConnection(connection);
					connections.add(javaClientConnection);
					javaClientConnection.connect();
				}
			} catch (IOException e) {
				logException("Unable to create connection", e);
			}

		}
	}
}
