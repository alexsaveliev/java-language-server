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

package org.jboss.tools.langs;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class CompletionList {

    /**
     * This list it not complete. Further typing should result in recomputing
     * 
     * this list.
     * 
     */
    @SerializedName("isIncomplete")
    @Expose
    private Boolean isIncomplete;
    /**
     * The completion items.
     * 
     */
    @SerializedName("items")
    @Expose
    private List<CompletionItem> items = new ArrayList<CompletionItem>();

    /**
     * This list it not complete. Further typing should result in recomputing
     * 
     * this list.
     * 
     * @return
     *     The isIncomplete
     */
    public Boolean getIsIncomplete() {
        return isIncomplete;
    }

    /**
     * This list it not complete. Further typing should result in recomputing
     * 
     * this list.
     * 
     * @param isIncomplete
     *     The isIncomplete
     */
    public void setIsIncomplete(Boolean isIncomplete) {
        this.isIncomplete = isIncomplete;
    }

    public CompletionList withIsIncomplete(Boolean isIncomplete) {
        this.isIncomplete = isIncomplete;
        return this;
    }

    /**
     * The completion items.
     * 
     * @return
     *     The items
     */
    public List<CompletionItem> getItems() {
        return items;
    }

    /**
     * The completion items.
     * 
     * @param items
     *     The items
     */
    public void setItems(List<CompletionItem> items) {
        this.items = items;
    }

    public CompletionList withItems(List<CompletionItem> items) {
        this.items = items;
        return this;
    }

}
