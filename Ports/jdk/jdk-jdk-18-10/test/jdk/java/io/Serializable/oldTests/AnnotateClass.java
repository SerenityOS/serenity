/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary it is new version of old test which was
 *          /src/share/test/serialization/subtest.java
 *          This test verifies of invocation
 *          annotateClass/replaceObject methods
 */

import java.io.*;

public class AnnotateClass {
    public static void main (String argv[]) {
        System.err.println("\nRegression test for verification " +
                           "of invocation annotateClass/replaceObject " +
                           "methods \n");
        try {
            FileOutputStream ostream = new FileOutputStream("subtest1.tmp");
            try {
                TestOutputStream p = new TestOutputStream(ostream);
                p.writeObject(System.out);
                p.writeObject(System.err);
                p.writeObject(new PrintStream(ostream));
                p.flush();
            } finally {
                ostream.close();
            }

            FileInputStream istream = new FileInputStream("subtest1.tmp");
            try {
                TestInputStream q = new TestInputStream(istream);

                PrintStream out = (PrintStream)q.readObject();
                PrintStream err = (PrintStream)q.readObject();
                Object other = q.readObject();
                if (out != System.out) {
                    System.err.println(
                        "\nTEST FAILED: System.out not read correctly");
                    throw new Error();
                }
                if (err != System.err) {
                    System.err.println(
                        "\nTEST FAILED: System.err not read correctly");
                    throw new Error();
                }
                if (other != null) {
                    System.err.println(
                        "\nTEST FAILED: Non-system PrintStream should have " +
                        "been written/read as null");
                    throw new Error();
                }
            } finally {
                istream.close();
            }

            System.err.println("\nTEST PASSED");
        } catch (Exception e) {
            System.err.print("TEST FAILED: ");
            e.printStackTrace();
            throw new Error();
        }
    }
}



/** ObjectOutputStream is extended to test the annotateClass()
 * and replaceObject() subclassable methods.
 * In annotateClass a magic string is written to the stream
 * so that it can be verified in ObjectInputStream.
 * replaceObject is used to subsititute a handle object for
 * one of the standard PrintStreams (stdout or stderr).
 */
class TestOutputStream extends ObjectOutputStream {
    /* Construct a new test stream */
    TestOutputStream(OutputStream out)  throws IOException {
        super(out);
        enableReplaceObject(true);
    }

    /* When any class is written, add a "magic" string
     * that must be verified by the TestInputStream.
     */
    protected void annotateClass(Class<?> cl) throws IOException {
        this.writeUTF("magic");
    }

    /* For each object of type PrintStream, substitute
     * a StdStream handle object that encodes which
     * of the standard print streams is being written.
     * Other objects are written as themselves.
     */
    protected Object replaceObject(Object obj)
    {
        /* For PrintStreams, like stdout and stderr, encode */
        if (obj instanceof PrintStream) {
            return new StdStream((PrintStream)obj);
        }
        return obj;
    }
}

/** Reverse the effects of TestOutputStream.
 */
class TestInputStream extends ObjectInputStream {

    TestInputStream(InputStream in)  throws IOException  {
        super(in);
        enableResolveObject(true);
    }

    /** Verify that the magic string was written to the stream
     * Also use the default classname->class resolution.
     */
    protected Class<?> resolveClass(ObjectStreamClass classdesc)
        throws ClassNotFoundException, IOException
    {
        try {
            String s = readUTF();
            if (!(s.equals("magic"))) {
                System.err.println(
                    "\nTEST FAILED: Bad magic number");
                throw new Error();
            }
        } catch (IOException ee) {
            System.err.println(
                "\nTEST FAILED: I/O Exception");
            throw new Error();
        }
        return super.resolveClass(classdesc);
    }

    /** If the object in the stream is a StdStream,
     * get the mapping of it to the local System printstream and
     * return it.
     * Other objects are returned as themselves.
     */
    protected Object resolveObject(Object obj) {
        if (obj instanceof StdStream) {
            return ((StdStream)obj).getStream();
        }
        return obj;
    }
}

/* A holder class to map between standard print streams (stdout, stderr)
 * and a small integer.
 */
class StdStream implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int stream = 0;

    public StdStream(PrintStream s) {
        if (s == System.out) {
            stream = 1;
        } else if (s == System.err) {
            stream = 2;
        }
    }

    public PrintStream getStream() {
        if (stream == 1) {
            return System.out;
        } else if (stream == 2) {
            return System.err;
        } else {
            return null;
        }
    }
}
