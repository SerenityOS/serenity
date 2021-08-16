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

import java.io.Writer;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.ProcessingInstruction;

/** Implements Processing Instruction Event
 *
 *@author Neeraj Bajaj, Sun Microsystems.
 *
 */


public class ProcessingInstructionEvent extends DummyEvent
implements ProcessingInstruction {

    /** Processing Instruction Name */
    private String fName;
    /** Processsing instruction content */
    private String fContent;

    public ProcessingInstructionEvent() {
        init();
    }

    public ProcessingInstructionEvent(String targetName, String data) {
        this(targetName,data,null);
    }

    public ProcessingInstructionEvent(String targetName, String data,Location loc) {
        init();
        this.fName = targetName;
        fContent = data;
        setLocation(loc);
    }

    protected void init() {
        setEventType(XMLStreamConstants.PROCESSING_INSTRUCTION);
    }

    public String getTarget() {
        return fName;
    }

    public void setTarget(String targetName) {
        fName = targetName;
    }

    public void setData(String data) {
        fContent = data;
    }

    public String getData() {
        return fContent;
    }

    public String toString() {
        if(fContent != null && fName != null)
            return "<?" + fName + " " + fContent + "?>";
        if(fName != null)
            return "<?" + fName + "?>";
        if(fContent != null)
            return "<?" + fContent + "?>";
        else
            return "<??>";
    }

    protected void writeAsEncodedUnicodeEx(java.io.Writer writer)
    throws java.io.IOException
    {
        writer.write(toString());
    }

}
