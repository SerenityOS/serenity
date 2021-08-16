/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4205440
 * @summary When ObjectInputStream.inputClassDescriptor calls its protected
 * resolveClass, if a NoClassDefFoundError is thrown, that Error should be
 * propagated to the caller, instead of being trapped and transformed into
 * a ClassNotFoundException for the class being resolved.
 * @author Peter Jones
 *
 * @build NoClassDefFoundErrorTrap
 * @run main NoClassDefFoundErrorTrap
 */

import java.io.*;

public class NoClassDefFoundErrorTrap {

    private static NoClassDefFoundError ncdfe;

    public interface Bar {}
    public static class Foo implements Bar, java.io.Serializable {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Test subclass of ObjectInputStream that overrides resolveClass
     * to throw a NoClassDefFoundError if our test class "Foo" is to
     * be resolved.
     */
    public static class TestObjectInputStream extends ObjectInputStream {

        public TestObjectInputStream(InputStream in)
            throws IOException
        {
            super(in);
        }

        protected Class<?> resolveClass(ObjectStreamClass desc)
            throws IOException, ClassNotFoundException
        {
            String name = desc.getName();

            if (name.equals(Foo.class.getName())) {
                ncdfe = new NoClassDefFoundError("Bar");
                throw ncdfe;
            } else {
                return super.resolveClass(desc);
            }
        }
    }

    public static void main(String[] args) {

        System.err.println("\nRegression test for bug 4205440\n");

        try {
            /*
             * Serialize a Foo instance to a byte array.
             */
            Foo foo = new Foo();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bout);
            out.writeObject(foo);
            byte[] stream = bout.toByteArray();

            /*
             * Deserialize the Foo instance using our test subclass of
             * ObjectInputStream that will throw NoClassDefFoundError.
             */
            ByteArrayInputStream bin = new ByteArrayInputStream(stream);
            ObjectInputStream in = new TestObjectInputStream(bin);

            /*
             * The test succeeds if we get the NoClassDefFoundError.
             */
            try {
                in.readObject();
            } catch (NoClassDefFoundError e) {
                if (e == ncdfe) {
                    System.err.println("TEST PASSED: " + e.toString());
                } else {
                    throw e;
                }
            }

        } catch (Exception e) {
            System.err.println("\nTEST FAILED:");
            e.printStackTrace();
            throw new RuntimeException("TEST FAILED: " + e.toString());
        }
    }
}
