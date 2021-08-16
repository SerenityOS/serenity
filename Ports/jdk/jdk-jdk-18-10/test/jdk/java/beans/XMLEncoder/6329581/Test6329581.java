/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6329581
 * @summary Tests encoding of a class with custom ClassLoader
 * @author Sergey Malenkov
 */

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;

public class Test6329581 extends URLClassLoader implements ExceptionListener {
    public static final class Bean {
    }

    public static void main(String[] args) throws Exception {
        new Test6329581().decode(new Test6329581().encode(Bean.class.getName()));
    }

    private Test6329581() {
        super(new URL[] {
                Test6329581.class.getProtectionDomain().getCodeSource().getLocation()
        });
    }

    @Override
    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (c == null) {
            if (Bean.class.getName().equals(name)) {
                c = findClass(name);
            }
            else try {
                c = getParent().loadClass(name);
            }
            catch (ClassNotFoundException exception) {
                c = findClass(name);
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    public void exceptionThrown(Exception exception) {
        throw new Error("unexpected exception", exception);
    }

    private void validate(Object object) {
        if (!object.getClass().getClassLoader().equals(this)) {
            throw new Error("Bean is loaded with unexpected class loader");
        }
    }

    private byte[] encode(String name) throws Exception {
        Object object = loadClass(name).newInstance();
        validate(object);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(out);
        encoder.setExceptionListener(this);
        encoder.writeObject(object);
        encoder.close();
        return out.toByteArray();
    }

    private Object decode(byte[] array) {
        ByteArrayInputStream in = new ByteArrayInputStream(array);
        XMLDecoder decoder = new XMLDecoder(in, null, this, this);
        Object object = decoder.readObject();
        validate(object);
        decoder.close();
        return object;
    }
}
