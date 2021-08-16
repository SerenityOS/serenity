/*
 * Copyright (c) 2004, 2010, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvmstat.perfdata.monitor;

import sun.jvmstat.monitor.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Class for monitoring a PerfData String instrument.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class PerfStringMonitor extends PerfByteArrayMonitor
       implements StringMonitor {

    private static Charset defaultCharset = Charset.defaultCharset();

    /**
     * Constructor to create a StringMonitor object for the string instrument
     * represented by the data in the given buffer.
     *
     * @param name the name of the string instrument
     * @param v the variability attribute
     * @param supported support level indicator
     * @param bb the buffer containing the string instrument data.
     */
    public PerfStringMonitor(String name, Variability v, boolean supported,
                             ByteBuffer bb) {
        this(name, v, supported, bb, bb.limit());
    }

    /**
     * Constructor to create a StringMonitor object for the string instrument
     * represented by the data in the given buffer.
     *
     * @param name the name of the string instrument
     * @param v the variability attribute
     * @param supported support level indicator
     * @param bb the buffer containing the string instrument data.
     * @param maxLength the maximum length of the string data.
     */
    public PerfStringMonitor(String name, Variability v, boolean supported,
                             ByteBuffer bb, int maxLength) {
        super(name, Units.STRING, v, supported, bb, maxLength);
    }

    /**
     * {@inheritDoc}
     * The object returned contains a String with a copy of the current
     * value of the StringInstrument.
     *
     * @return Object - a copy of the current value of the StringInstrument.
     *                  The return value is guaranteed to be of type String.
     */
    public Object getValue() {
        return stringValue();
    }

    /**
     * Return the current value of the StringInstrument as a String.
     *
     * @return String - a copy of the current value of the StringInstrument.
     */
    public String stringValue() {
        String str = "";
        byte[] b = byteArrayValue();

        // catch null strings
        if ((b == null) || (b.length <= 1) || (b[0] == (byte)0)) {
            return str;
        }

        int i;
        for (i = 0; i < b.length && b[i] != (byte)0; i++);

        return new String(b, 0, i, defaultCharset);
    }
}
