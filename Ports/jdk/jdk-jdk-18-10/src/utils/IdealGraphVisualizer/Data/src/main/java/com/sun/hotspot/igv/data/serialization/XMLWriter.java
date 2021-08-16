/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.data.serialization;

import com.sun.hotspot.igv.data.Properties;
import com.sun.hotspot.igv.data.Property;
import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

/**
 *
 * @author Thomas Wuerthinger
 */
public class XMLWriter extends Writer {

    private Writer inner;
    private Stack<String> elementStack;

    public XMLWriter(Writer inner) {
        this.inner = inner;
        elementStack = new Stack<>();
    }

    @Override
    public void write(char[] arr) throws IOException {
        write(arr, 0, arr.length);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        for (int i = off; i < off + len; i++) {
            char c = cbuf[i];
            if (c == '>') {
                inner.write("&gt;");
            } else if (c == '<') {
                inner.write("&lt;");
            } else if (c == '&') {
                inner.write("&amp;");
            } else {
                inner.write(c);
            }
        }
    }

    @Override
    public void flush() throws IOException {
        inner.flush();
    }

    @Override
    public void close() throws IOException {
        inner.close();
    }

    public void endTag() throws IOException {
        inner.write("</" + elementStack.pop() + ">\n");
    }

    public void startTag(String name) throws IOException {
        inner.write("<" + name + ">\n");
        elementStack.push(name);
    }

    public void simpleTag(String name) throws IOException {
        inner.write("<" + name + "/>\n");
    }

    public void startTag(String name, Properties attributes) throws IOException {
        inner.write("<" + name);
        elementStack.push(name);

        for (Property p : attributes) {
            inner.write(" " + p.getName() + "=\"");
            write(p.getValue().toCharArray());
            inner.write("\"");
        }

        inner.write(">\n");
    }

    public void simpleTag(String name, Properties attributes) throws IOException {
        inner.write("<" + name);

        for (Property p : attributes) {
            inner.write(" " + p.getName() + "=\"");
            write(p.getValue().toCharArray());
            inner.write("\"");
        }

        inner.write("/>\n");
    }

    public void writeProperties(Properties props) throws IOException {
        if (props.iterator().hasNext() == false) {
            return;
        }

        startTag(Parser.PROPERTIES_ELEMENT);

        for (Property p : props) {
            startTag(Parser.PROPERTY_ELEMENT, new Properties(Parser.PROPERTY_NAME_PROPERTY, p.getName()));
            this.write(p.getValue().toCharArray());
            endTag();
        }

        endTag();
    }
}
