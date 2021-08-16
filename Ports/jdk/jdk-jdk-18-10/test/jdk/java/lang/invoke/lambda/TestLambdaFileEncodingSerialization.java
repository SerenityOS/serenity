/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.*;
import java.util.concurrent.Callable;

/*
 * Testcase for verifying deserializeLambda containing a non-ASCII mappable char
 * is correctly handled as UTF-8
 */
public class TestLambdaFileEncodingSerialization {
        public static class ABCÃ¢ implements Serializable {
                public String msg;
                public ABCÃ¢() {
                        msg = "Hello world";
                }
                public static Callable<ABCÃ¢> getHello() {
                        return (Callable<ABCÃ¢> & Serializable) () -> {
                                return new ABCÃ¢();
                        };
                }
        }

    // Re-serialize the object containing the lambda
    private static <T> T reserialize(T o) throws IOException {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);

                oos.writeObject(o);

                oos.close();

                ObjectInputStream iis = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));

                try {
                        o = (T)iis.readObject();
                } catch (ClassNotFoundException e) {
                        throw new IOException(e);
                }
                iis.close();
                return o;
    }

    public static void main(String args[]) throws Exception{
        System.out.println("Default charset = "+Charset.defaultCharset());

        // Construct class containing suitable UTF-8 char
        Callable<ABCÃ¢> foo = ABCÃ¢.getHello();
        ABCÃ¢ hello = new ABCÃ¢();

        // re-serialize hello
        ABCÃ¢ rh = reserialize(hello);
        System.out.println(rh.msg);

        // re-serialize foo and call()
        reserialize(foo).call();
    }
}

