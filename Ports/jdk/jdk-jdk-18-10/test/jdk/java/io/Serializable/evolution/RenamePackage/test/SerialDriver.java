/*
 * Copyright (c) 1998, 2011, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @bug 4087295
 * @build install/SerialDriver.java test/SerialDriver.java extension/ExtendedObjectInputStream.java
 * @summary Enable resolveClass() to accommodate package renaming.
 * This fix enables one to implement a resolveClass method that maps a
 * Serializable class within a serialization stream to the same class
 * in a different package within the JVM runtime. See run shell script
 * for instructions on how to run this test.
 */

package test;

import java.io.*;
import extension.ExtendedObjectInputStream;

public class SerialDriver implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    SerialDriver next;

    public SerialDriver() {
        name = "<terminator>";
        next = null;
    }

    public SerialDriver(String name, SerialDriver next) {
        this.name = name;
        this.next = next;
    }

    static boolean serialize = false;
    static boolean deserialize = false;

    public static void main(String args[])  throws Exception  {
        SerialDriver obj = new SerialDriver("SerialDriver_1", new SerialDriver());
        SerialDriver[] array = new SerialDriver[5];
        for (int i = 0; i < array.length; i++)
            array[i] = new SerialDriver("SerialDriver_1_" + i, new SerialDriver());

        /*
         * see if we are serializing or deserializing.
         * The ability to deserialize or serialize allows
         * us to see the bidirectional readability and writeability
         */
        if (args.length == 1) {
            if (args[0].equals("-d")) {
                deserialize = true;
            } else if (args[0].equals("-s")) {
                serialize = true;
            } else {
                usage();
                throw new Exception("incorrect command line arguments");
            }
        } else {
            usage();
            throw new Exception("incorrect command line arguments");
        }

        File f = new File("stream.ser");
        if (serialize) {
            // Serialize the subclass
            try (FileOutputStream fo = new FileOutputStream(f);
                 ObjectOutputStream so = new ObjectOutputStream(fo))
            {
                so.writeObject(obj);
                /* Comment out since renaming arrays does not work
                   since it changes the serialVersionUID. */
                so.writeObject(array);
            } catch (Exception e) {
                System.out.println(e);
                throw e;
            }
        }
        if (deserialize) {
            // Deserialize the subclass
            try (FileInputStream fi = new FileInputStream(f);
                 ExtendedObjectInputStream si = new ExtendedObjectInputStream(fi))
            {
                si.addRenamedClassName("install.SerialDriver",
                                       "test.SerialDriver");
                si.addRenamedClassName("[Linstall.SerialDriver;",
                                       "[Ltest.SerialDriver");
                obj = (SerialDriver) si.readObject();
                array = (SerialDriver[]) si.readObject();
            } catch (Exception e) {
                System.out.println(e);
                throw e;
            }
            System.out.println();
            System.out.println("Printing deserialized class: ");
            System.out.println();
            System.out.println(obj.toString());
            System.out.println();
        }
    }


    public String toString() {
        String nextString = next != null ? next.toString() : "<null>";
        return "name =" + name + " next = <" + nextString + ">";
    }

    /**
     * Prints out the usage
     */
    static void usage() {
        System.out.println("Usage:");
        System.out.println("      -s (in order to serialize)");
        System.out.println("      -d (in order to deserialize)");
    }
}
