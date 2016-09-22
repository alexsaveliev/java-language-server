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
package org.jboss.tools.vscode.java.internal.handlers;

import org.eclipse.jdt.core.ITypeRoot;
import org.jboss.tools.langs.Hover;
import org.jboss.tools.langs.TextDocumentPositionParams;
import org.jboss.tools.langs.base.LSPMethods;
import org.jboss.tools.vscode.internal.ipc.RequestHandler;
import org.jboss.tools.vscode.java.internal.HoverInfoProvider;
import org.jboss.tools.vscode.java.internal.JDTUtils;
import org.jboss.tools.vscode.java.internal.JavaClientConnection;

import java.io.File;

public class HoverHandler implements RequestHandler<TextDocumentPositionParams, Hover>{

	// SOURCEGRAPH: env
	private static boolean MODE_SOURCEGRAPH = System.getenv("SOURCEGRAPH") != null;

	// SOURCEGRAPH: connection object to share workspace root
	private JavaClientConnection connection;

	public HoverHandler(JavaClientConnection connection) {
		this.connection = connection;
	}

	@Override
	public boolean canHandle(String request) {
		return LSPMethods.DOCUMENT_HOVER.getMethod().equals(request);
	}

	@Override
	public Hover handle(TextDocumentPositionParams param) {

		String uri = param.getTextDocument().getUri();
		if (MODE_SOURCEGRAPH) {
			// SOURCEGRAPH: URI is expected to be in form file:///foo/bar,
			// but we need to construct absolute file URI
			uri = new File(connection.getWorkpaceRoot(), uri.substring(8)).toURI().toString();
		}

		ITypeRoot unit = JDTUtils.resolveTypeRoot(uri);
		
		String hover = computeHover(unit ,param.getPosition().getLine().intValue(),
				param.getPosition().getCharacter().intValue());
		Hover $ = new Hover();
		if (hover != null && hover.length() > 0) {
			return $.withContents(hover);
		}
		return $;
	}


	public String computeHover(ITypeRoot unit, int line, int column) {
		HoverInfoProvider provider = new HoverInfoProvider(unit);
		return provider.computeHover(line,column);
	}	

}
