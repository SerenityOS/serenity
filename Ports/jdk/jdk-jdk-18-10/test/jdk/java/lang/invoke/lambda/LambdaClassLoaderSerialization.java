/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004970
 * @summary Lambda serialization in the presence of class loaders
 * @run main LambdaClassLoaderSerialization
 * @author Peter Levart
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class LambdaClassLoaderSerialization {

    public interface SerializableRunnable extends Runnable, Serializable {}

    public static class MyCode implements SerializableRunnable {

        private byte[] serialize(Object o) {
            ByteArrayOutputStream baos;
            try (
                ObjectOutputStream oos =
                    new ObjectOutputStream(baos = new ByteArrayOutputStream())
            ) {
                oos.writeObject(o);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            return baos.toByteArray();
        }

        private <T> T deserialize(byte[] bytes) {
            try (
                ObjectInputStream ois =
                    new ObjectInputStream(new ByteArrayInputStream(bytes))
            ) {
                return (T) ois.readObject();
            }
            catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            System.out.println("                this: " + this);

            SerializableRunnable deSerializedThis = deserialize(serialize(this));
            System.out.println("    deSerializedThis: " + deSerializedThis);

            SerializableRunnable runnable = () -> {System.out.println("HELLO");};
            System.out.println("            runnable: " + runnable);

            SerializableRunnable deSerializedRunnable = deserialize(serialize(runnable));
            System.out.println("deSerializedRunnable: " + deSerializedRunnable);
        }
    }

    public static void main(String[] args) throws Exception {
        ClassLoader myCl = new MyClassLoader(
            LambdaClassLoaderSerialization.class.getClassLoader()
        );
        Class<?> myCodeClass = Class.forName(
            LambdaClassLoaderSerialization.class.getName() + "$MyCode",
            true,
            myCl
        );
        Runnable myCode = (Runnable) myCodeClass.newInstance();
        myCode.run();
    }

    static class MyClassLoader extends ClassLoader {
        MyClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.indexOf('.') < 0) {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> c = findLoadedClass(name);
                    if (c == null) {
                        c = findClass(name);
                    }
                    if (resolve) {
                        resolveClass(c);
                    }
                    return c;
                }
            } else {
                return super.loadClass(name, resolve);
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String path = name.replace('.', '/').concat(".class");
            try (InputStream is = getResourceAsStream(path)) {
                if (is != null) {
                    byte[] bytes = is.readAllBytes();
                    return defineClass(name, bytes, 0, bytes.length);
                } else {
                    throw new ClassNotFoundException(name);
                }
            }
            catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
