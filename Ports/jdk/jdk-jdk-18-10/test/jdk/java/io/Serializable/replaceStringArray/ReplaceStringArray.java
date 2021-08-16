/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4099013
 * @summary Enable substitution of String and Array by ObjectStreams.
 */

import java.io.*;
import java.lang.reflect.Array;

class A implements Serializable {
    private static final long serialVersionUID = 1L;

    String stringA;
    String stringB;
    String stringC;
    String[] arrayOfString;

    A() {
        stringA = "hello";
        stringB = "goodbye";
        stringC = stringA;
        arrayOfString = new String[2];
        for (int i = 0; i < arrayOfString.length; i++)
            arrayOfString[i] = new String("array element " + i);
    }

    void report() {
            System.out.println("stringA = " + stringA);
            System.out.println("stringB = " + stringB);
            System.out.println("stringC = " + stringC);
            System.out.println("length of arrayOfString = " +
                               arrayOfString.length);
            for (int i = 0; i < arrayOfString.length; i++)
                System.out.println("arrayOfString[" + i + "]= " +
                                   arrayOfString[i]);
    }
}

class SubstituteObjectOutputStream extends ObjectOutputStream {
    public int numStringsReplaced = 0;
    public int numArraysCounted = 0;

    public SubstituteObjectOutputStream(OutputStream out) throws IOException {
        super(out);
        enableReplaceObject(true);
    }

    protected Object replaceObject(Object obj) throws IOException {
        if (obj instanceof String) {
            numStringsReplaced++;
            return obj + "_WriteReplaced";
        }
        if (obj.getClass().isArray()) {
            Object[] array = (Object[]) obj;
            /* Double the array.
             * Initialize new array elements with original array. */
            Class<?> arrayComponentType = array.getClass().getComponentType();
            Object[] newarray =
                (Object[])Array.newInstance(arrayComponentType,
                                            array.length * 2);
            for (int i = 0; i < array.length; i++)
                newarray[i] = array[i];
            for (int ni = array.length; ni < 2* array.length; ni++)
                newarray[ni] = array[ni - array.length];
            numArraysCounted++;
            obj = newarray;
        }
        return obj;
    }
}

class SubstituteObjectInputStream extends ObjectInputStream {
    public int numStringsReplaced = 0;
    public int numArraysCounted = 0;

    public SubstituteObjectInputStream(InputStream in) throws IOException {
        super(in);
        enableResolveObject(true);
    }

    protected Object resolveObject(Object obj) throws IOException {
        if (obj instanceof String) {
            numStringsReplaced++;
            return obj + "_ReadResolved";
        }
        if (obj.getClass().isArray()) {
            Object[] array = (Object[])obj;

            /* Double the array.
             * Initialize new array elements with original array. */
            Class<?> arrayComponentType = array.getClass().getComponentType();
            Object[] newarray =
                (Object[])Array.newInstance(arrayComponentType,
                                            array.length * 2);
            for (int i = 0; i < array.length; i++)
                newarray[i] = array[i];
            for (int ni = array.length; ni < 2* array.length; ni++)
                newarray[ni] = array[ni - array.length];
            numArraysCounted++;
            obj = newarray;
        }
        return obj;
    }
}

public class ReplaceStringArray {
    public static void main(String args[]) throws IOException, ClassNotFoundException {
        boolean verbose = false;
        if (args.length >= 1 && args[0].compareTo("verbose") == 0)
            verbose = true;


        A a = new A();
        if (verbose) {
            System.out.println("Value of Class A");
            a.report();
            System.out.println("");
        }


        /* Serialize object a to bytestream. */
        if (verbose) {
            System.out.println("Serialize A to SubstituteObjectOutputStream");
            System.out.println("");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SubstituteObjectOutputStream out =   new SubstituteObjectOutputStream(baos);
        out.writeObject(a);
        out.close();
        a = null;

        /* Validate that writeReplace called by SubstituteObjectOutputStream.*/
        boolean expectedResult = (out.numStringsReplaced == 4);
        if (!expectedResult)
            throw new Error("Expected " + 4 + " strings to be replaced during serialization;" +
                            " only " + out.numStringsReplaced + " strings were replaced.");
        if (out.numArraysCounted != 1)
            throw new Error("Expected 1 array during serialization; only " +
                            out.numArraysCounted + " arrays");

        if (verbose) {
            System.out.println("DeSerialize A from SubstituteObjectInputStream");
            System.out.println("");
        }
        SubstituteObjectInputStream in =
            new SubstituteObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        a = (A)in.readObject();
        in.close();

        /* Validate that readResolve called by SubstituteObjectInputStream.*/
        if (in.numStringsReplaced != 4)
            throw new Error("Expected 4 strings to be resolved during deserialization;" +
                            " only " + in.numStringsReplaced + " strings were resolved.");
        if (in.numArraysCounted != 1)
            throw new Error("Expected 1 array during deserialization; only " +
                            out.numArraysCounted + " arrays");
        if (a.arrayOfString.length != 8)
            throw new Error("Expected a.arrayOfString.length to be 8, observed " +
                            a.arrayOfString.length);
        if (verbose) {
            System.out.println("Value of Class A after serialize/deserialize with writeReplace/readResolve");
            a.report();
        }
    }
}
