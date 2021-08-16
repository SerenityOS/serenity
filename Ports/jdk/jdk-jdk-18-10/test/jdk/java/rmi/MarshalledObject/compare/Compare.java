/*
 * Copyright (c) 1998, 2012, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4096312
 * @summary Codebase annotations on classes that are marshalled should not
 *          affect the behavior of MarshalledObject.equals.  Only the bytes
 *          not involved in location should be compared.
 * @author Ken Arnold
 *
 * @run main/othervm Compare 11 annotatedRef
 */

import java.rmi.MarshalledObject;
import java.io.*;

public class Compare {
    static class Node implements Serializable {
        int value = nextValue++;
        Node next;

        static int nextValue = 1;
    };

    private static MarshalledObject made;
    private static MarshalledObject read;

    public static void main(String[] args) throws Throwable {
        if (args.length == 1)
            writeObjToOut(Integer.parseInt(args[0]));
        else
            compareObjToFile(args[0], args[1]);
    }

    static void writeObjToOut(int listLength) throws Throwable {
        ObjectOutputStream out = new ObjectOutputStream(System.out);
        out.writeObject(marshalledList(listLength));
        out.close();
    }

    public static void compareHashCodes(String[] args) throws Throwable {
        File f = new File(System.getProperty("test.src", "."), args[1]);
        setupObjects(args[0], f);
        if (made.hashCode() != read.hashCode()) {
            throw new RuntimeException(
                "made.hashCode(){" + made.hashCode() + "} != " +
                "read.hashCode(){" + read.hashCode() + "}"
            );
        }
    }

    static void compareObjToFile(String lengthStr, String file0) throws Throwable
    {
        File f = new File(System.getProperty("test.src", "."), file0);
        setupObjects(lengthStr, f);
        if (!made.equals(read) || !read.equals(made)) {
            throw new RuntimeException(
                     "made.equals(read) = " + made.equals(read)
                + ", read.equals(made) = " + read.equals(made)
            );
        }
    }

    static MarshalledObject setupObjects(String lengthStr, File file)
        throws Throwable
    {
        int listLength = Integer.parseInt(lengthStr);
        made = marshalledList(listLength);
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
        read = (MarshalledObject) in.readObject();
        in.close();
        return read;
    }

    static MarshalledObject marshalledList(int length) throws Throwable {
        Node head = null;
        Node cur = null;
        for (int i = 0; i < length; i++) {
            if (head == null)
                cur = head = new Node();
            else
                cur = cur.next = new Node();
        }
        System.err.println("head = " + head);
        return new MarshalledObject(head);
    }
}
