/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.util.xml.impl;

import java.io.Reader;

/**
 * A parsed entity input state.
 *
 * This class represents a parsed entity input state. The parser uses
 * an instance of this class to manage input.
 */

public class Input {

    /** The entity public identifier or null. */
    public String pubid;
    /** The entity systen identifier or null. */
    public String sysid;
    /** The encoding from XML declaration or null */
    public String xmlenc;
    /** The XML version from XML declaration or 0x0000 */
    public char xmlver;
    /** The entity reader. */
    public Reader src;
    /** The character buffer. */
    public char[] chars;
    /** The length of the character buffer. */
    public int chLen;
    /** The index of the next character to read. */
    public int chIdx;
    /** The next input in a chain. */
    public Input next;

    /**
     * Constructor.
     *
     * @param buffsize The input buffer size.
     */
    public Input(int buffsize) {
        chars = new char[buffsize];
        chLen = chars.length;
    }

    /**
     * Constructor.
     *
     * @param buff The input buffer.
     */
    public Input(char[] buff) {
        chars = buff;
        chLen = chars.length;
    }

    /**
     * Constructor.
     */
    public Input() {
    }
}
