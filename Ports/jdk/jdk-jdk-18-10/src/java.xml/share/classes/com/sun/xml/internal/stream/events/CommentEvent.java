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

import javax.xml.stream.events.Comment;
import javax.xml.stream.events.XMLEvent;

/**
 * This class contains information about Comment event.
 *
 * @author Neeraj Bajaj, Sun Microsystems.
 */
public class CommentEvent extends DummyEvent implements Comment {

    /* String data for this event */
    private String fText ;

    public CommentEvent() {
        init();
    }

    public CommentEvent(String text) {
        init();
        fText = text;
    }

    protected void init() {
        setEventType(XMLEvent.COMMENT);
    }

    /**
     * @return String String representation of this event
     */
    public String toString() {
        return "<!--" + getText() + "-->";
    }


    /** Return the string data of the comment, returns empty string if it
     * does not exist
     * @return String
     */
    public String getText() {
        return fText ;
    }

    protected void writeAsEncodedUnicodeEx(java.io.Writer writer)
    throws java.io.IOException
    {
        writer.write("<!--" + getText() + "-->");
    }

}
