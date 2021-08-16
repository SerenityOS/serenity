/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.xml.internal.stream.events;

import java.util.List;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.NotationDeclaration;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author Neeraj Bajaj, Sun Microsystesm.
 *
 */
public class DTDEvent extends DummyEvent implements DTD {

    private String fDoctypeDeclaration;
    private List<NotationDeclaration> fNotations;
    private List<EntityDeclaration> fEntities;

    /**
     * Creates a new instance of DTDEvent
     */
    public DTDEvent() {
        init();
    }

    public DTDEvent(String doctypeDeclaration) {
        init();
        fDoctypeDeclaration = doctypeDeclaration;
    }

    public void setDocumentTypeDeclaration(String doctypeDeclaration) {
        fDoctypeDeclaration = doctypeDeclaration;
    }

    @Override
    public String getDocumentTypeDeclaration() {
        return fDoctypeDeclaration;
    }

    //xxx: we can change the signature if the implementation doesn't store the entities in List Datatype.
    //and then convert that DT to list format here. That way callee dont need to bother about conversion
    public void setEntities(List<EntityDeclaration> entites) {
        fEntities = entites;
    }

    @Override
    public List<EntityDeclaration> getEntities() {
        return fEntities;
    }

    //xxx: we can change the signature if the implementation doesn't store the entities in List Datatype.
    //and then convert that DT to list format here. That way callee dont need to bother about conversion
    public void setNotations(List<NotationDeclaration> notations) {
        fNotations = notations;
    }

    @Override
    public List<NotationDeclaration> getNotations() {
        return fNotations;
    }

    /**
     * Returns an implementation defined representation of the DTD. This method
     * may return null if no representation is available.
     *
     */
    @Override
    public Object getProcessedDTD() {
        return null;
    }

    protected final void init() {
        setEventType(XMLEvent.DTD);
    }

    @Override
    public String toString() {
        return fDoctypeDeclaration;
    }

    @Override
    protected void writeAsEncodedUnicodeEx(java.io.Writer writer)
            throws java.io.IOException {
        writer.write(fDoctypeDeclaration);
    }
}
