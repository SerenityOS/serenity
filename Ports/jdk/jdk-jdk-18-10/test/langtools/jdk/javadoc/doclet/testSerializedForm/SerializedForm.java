/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;

public class SerializedForm implements Serializable {

    /**
     * @serialField name String a test
     * @serialField longs Long[] the longs
     * @serialField i int an int
     * @serialField  m double[][] the doubles
     * @serialField next SerializedForm a linked reference
     * @see TestSerializedForm
     */
    @Deprecated
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("i", int.class),
        new ObjectStreamField("m", double[][].class),
        new ObjectStreamField("count", Integer.TYPE),
        new ObjectStreamField("name", String.class),
        new ObjectStreamField("longs", Long[].class),
        new ObjectStreamField("next", SerializedForm.class)
    };

    /**
     * The entry point of the test.
     * @param args the array of command line arguments.
     */

    /**
     * @param s ObjectInputStream.
     * @throws IOException when there is an I/O error.
     * @serial
     */
    private void readObject(ObjectInputStream s) throws IOException {}

    /**
     * @param s ObjectOutputStream.
     * @throws IOException when there is an I/O error.
     * @serial
     */
    private void writeObject(ObjectOutputStream s) throws IOException {}

    /**
     * @throws IOException when there is an I/O error.
     * @serialData This is a serial data comment.
     * @return an object.
     */
    protected Object readResolve() throws IOException {return null;}

    /**
     * @throws IOException when there is an I/O error.
     * @serialData This is a serial data comment.
     * @return an object.
     */
    protected Object writeReplace() throws IOException {return null;}

    /**
     * @throws IOException when there is an I/O error.
     * @serialData This is a serial data comment.
     * @return an object.
     */
    protected Object readObjectNoData() throws IOException {
        return null;
    }
}
