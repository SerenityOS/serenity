/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.xml.internal.stream.events ;

import javax.xml.stream.events.EntityReference;
import java.io.Writer;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.XMLEvent;

/** Implements EntityReference event.
 *
 *@author Neeraj Bajaj, Sun Microsystems,
 */
public class EntityReferenceEvent extends DummyEvent
implements EntityReference {
    private EntityDeclaration fEntityDeclaration ;
    private String fEntityName;

    public EntityReferenceEvent() {
        init();
    }

    public EntityReferenceEvent(String entityName , EntityDeclaration entityDeclaration) {
        init();
        fEntityName = entityName;
        fEntityDeclaration = entityDeclaration ;
    }

    public String getName() {
        return fEntityName;
    }

    public String toString() {
        String text = fEntityDeclaration.getReplacementText();
        if(text == null)
            text = "";
        return "&" + getName() + ";='" + text + "'";
    }

    protected void writeAsEncodedUnicodeEx(java.io.Writer writer)
    throws java.io.IOException
    {
        writer.write('&');
        writer.write(getName());
        writer.write(';');
    }

    public EntityDeclaration getDeclaration(){
        return fEntityDeclaration ;
    }

    protected void init() {
        setEventType(XMLEvent.ENTITY_REFERENCE);
    }


}
