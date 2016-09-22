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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StdIoConnection extends AbstractStreamConnection {

	@Override
	protected InputStream connectReadChannel() throws IOException {
		String datafile = System.getenv("STDIO_DATA_FILE");
		if (datafile != null) {
			return new FileInputStream(datafile);
		}
		return System.in;
	}

	@Override
	protected OutputStream connectWriteChannel() throws IOException {
		return System.out;
	}

}
