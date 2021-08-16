/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvmstat.monitor;

/**
 * Interface for Monitoring ByteArrayInstrument objects.
 *
 * This interface is provided to support the StringMonitor interface. No
 * instrumentation objects of this direct type can currently be created
 * or monitored.
 *
 * @author Brian Doherty
 * @since 1.5
 * @see sun.jvmstat.instrument.ByteArrayInstrument
 */
public interface ByteArrayMonitor extends Monitor {

    /**
     * Get a copy of the current values of the elements of the
     * ByteArrayInstrument object.
     *
     * @return byte[] - a copy of the bytes in the associated
     *                  instrumenattion object.
     */
    public byte[] byteArrayValue();

    /**
     * Get the current value of an element of the ByteArrayInstrument object.
     *
     * @return byte - the byte value at the specified index in the
     *                associated instrumentation object.
     */
    public byte byteAt(int index);
}
