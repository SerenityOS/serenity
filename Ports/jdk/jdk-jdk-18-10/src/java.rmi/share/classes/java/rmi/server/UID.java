/*
 * Copyright (c) 1996, 2011, Oracle and/or its affiliates. All rights reserved.
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
package java.rmi.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.security.SecureRandom;

/**
 * A <code>UID</code> represents an identifier that is unique over time
 * with respect to the host it is generated on, or one of 2<sup>16</sup>
 * "well-known" identifiers.
 *
 * <p>The {@link #UID()} constructor can be used to generate an
 * identifier that is unique over time with respect to the host it is
 * generated on.  The {@link #UID(short)} constructor can be used to
 * create one of 2<sup>16</sup> well-known identifiers.
 *
 * <p>A <code>UID</code> instance contains three primitive values:
 * <ul>
 * <li><code>unique</code>, an <code>int</code> that uniquely identifies
 * the VM that this <code>UID</code> was generated in, with respect to its
 * host and at the time represented by the <code>time</code> value (an
 * example implementation of the <code>unique</code> value would be a
 * process identifier),
 *  or zero for a well-known <code>UID</code>
 * <li><code>time</code>, a <code>long</code> equal to a time (as returned
 * by {@link System#currentTimeMillis()}) at which the VM that this
 * <code>UID</code> was generated in was alive,
 * or zero for a well-known <code>UID</code>
 * <li><code>count</code>, a <code>short</code> to distinguish
 * <code>UID</code>s generated in the same VM with the same
 * <code>time</code> value
 * </ul>
 *
 * <p>An independently generated <code>UID</code> instance is unique
 * over time with respect to the host it is generated on as long as
 * the host requires more than one millisecond to reboot and its system
 * clock is never set backward.  A globally unique identifier can be
 * constructed by pairing a <code>UID</code> instance with a unique host
 * identifier, such as an IP address.
 *
 * @author      Ann Wollrath
 * @author      Peter Jones
 * @since       1.1
 */
public final class UID implements Serializable {

    private static int hostUnique;
    private static boolean hostUniqueSet = false;

    private static final Object lock = new Object();
    private static long lastTime = System.currentTimeMillis();
    private static short lastCount = Short.MIN_VALUE;

    /** indicate compatibility with JDK 1.1.x version of class */
    private static final long serialVersionUID = 1086053664494604050L;

    /**
     * number that uniquely identifies the VM that this <code>UID</code>
     * was generated in with respect to its host and at the given time
     * @serial
     */
    private final int unique;

    /**
     * a time (as returned by {@link System#currentTimeMillis()}) at which
     * the VM that this <code>UID</code> was generated in was alive
     * @serial
     */
    private final long time;

    /**
     * 16-bit number to distinguish <code>UID</code> instances created
     * in the same VM with the same time value
     * @serial
     */
    private final short count;

    /**
     * Generates a <code>UID</code> that is unique over time with
     * respect to the host that it was generated on.
     */
    public UID() {

        synchronized (lock) {
            if (!hostUniqueSet) {
                hostUnique = (new SecureRandom()).nextInt();
                hostUniqueSet = true;
            }
            unique = hostUnique;
            if (lastCount == Short.MAX_VALUE) {
                boolean interrupted = Thread.interrupted();
                boolean done = false;
                while (!done) {
                    long now = System.currentTimeMillis();
                    if (now == lastTime) {
                        // wait for time to change
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            interrupted = true;
                        }
                    } else {
                        // If system time has gone backwards increase
                        // original by 1ms to maintain uniqueness
                        lastTime = (now < lastTime) ? lastTime+1 : now;
                        lastCount = Short.MIN_VALUE;
                        done = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            time = lastTime;
            count = lastCount++;
        }
    }

    /**
     * Creates a "well-known" <code>UID</code>.
     *
     * There are 2<sup>16</sup> possible such well-known ids.
     *
     * <p>A <code>UID</code> created via this constructor will not
     * clash with any <code>UID</code>s generated via the no-arg
     * constructor.
     *
     * @param   num number for well-known <code>UID</code>
     */
    public UID(short num) {
        unique = 0;
        time = 0;
        count = num;
    }

    /**
     * Constructs a <code>UID</code> given data read from a stream.
     */
    private UID(int unique, long time, short count) {
        this.unique = unique;
        this.time = time;
        this.count = count;
    }

    /**
     * Returns the hash code value for this <code>UID</code>.
     *
     * @return  the hash code value for this <code>UID</code>
     */
    public int hashCode() {
        return (int) time + (int) count;
    }

    /**
     * Compares the specified object with this <code>UID</code> for
     * equality.
     *
     * This method returns <code>true</code> if and only if the
     * specified object is a <code>UID</code> instance with the same
     * <code>unique</code>, <code>time</code>, and <code>count</code>
     * values as this one.
     *
     * @param   obj the object to compare this <code>UID</code> to
     *
     * @return  <code>true</code> if the given object is equivalent to
     * this one, and <code>false</code> otherwise
     */
    public boolean equals(Object obj) {
        if (obj instanceof UID) {
            UID uid = (UID) obj;
            return (unique == uid.unique &&
                    count == uid.count &&
                    time == uid.time);
        } else {
            return false;
        }
    }

    /**
     * Returns a string representation of this <code>UID</code>.
     *
     * @return  a string representation of this <code>UID</code>
     */
    public String toString() {
        return Integer.toString(unique,16) + ":" +
            Long.toString(time,16) + ":" +
            Integer.toString(count,16);
    }

    /**
     * Marshals a binary representation of this <code>UID</code> to
     * a <code>DataOutput</code> instance.
     *
     * <p>Specifically, this method first invokes the given stream's
     * {@link DataOutput#writeInt(int)} method with this <code>UID</code>'s
     * <code>unique</code> value, then it invokes the stream's
     * {@link DataOutput#writeLong(long)} method with this <code>UID</code>'s
     * <code>time</code> value, and then it invokes the stream's
     * {@link DataOutput#writeShort(int)} method with this <code>UID</code>'s
     * <code>count</code> value.
     *
     * @param   out the <code>DataOutput</code> instance to write
     * this <code>UID</code> to
     *
     * @throws  IOException if an I/O error occurs while performing
     * this operation
     */
    public void write(DataOutput out) throws IOException {
        out.writeInt(unique);
        out.writeLong(time);
        out.writeShort(count);
    }

    /**
     * Constructs and returns a new <code>UID</code> instance by
     * unmarshalling a binary representation from an
     * <code>DataInput</code> instance.
     *
     * <p>Specifically, this method first invokes the given stream's
     * {@link DataInput#readInt()} method to read a <code>unique</code> value,
     * then it invoke's the stream's
     * {@link DataInput#readLong()} method to read a <code>time</code> value,
     * then it invoke's the stream's
     * {@link DataInput#readShort()} method to read a <code>count</code> value,
     * and then it creates and returns a new <code>UID</code> instance
     * that contains the <code>unique</code>, <code>time</code>, and
     * <code>count</code> values that were read from the stream.
     *
     * @param   in the <code>DataInput</code> instance to read
     * <code>UID</code> from
     *
     * @return  unmarshalled <code>UID</code> instance
     *
     * @throws  IOException if an I/O error occurs while performing
     * this operation
     */
    public static UID read(DataInput in) throws IOException {
        int unique = in.readInt();
        long time = in.readLong();
        short count = in.readShort();
        return new UID(unique, time, count);
    }
}
