/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verifies basic correct functioning of proxy serialization.
 * @key randomness
 */

import java.io.*;
import java.lang.reflect.*;
import java.util.Random;

// proxy interfaces
interface Foo { int foo(); }
interface Bar { float bar(); }

// dummy invocation handler
class Handler implements InvocationHandler, Serializable {
    private static final long serialVersionUID = 1L;

    static Method fooMethod, barMethod;
    static {
        try {
            fooMethod = Foo.class.getDeclaredMethod("foo", new Class<?>[0]);
            barMethod = Bar.class.getDeclaredMethod("bar", new Class<?>[0]);
        } catch (NoSuchMethodException ex) {
            throw new Error();
        }
    }

    int foo;
    float bar;

    Handler(int foo, float bar) {
        this.foo = foo;
        this.bar = bar;
    }

    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable
    {
        if (method.equals(fooMethod)) {
            return foo;
        } else if (method.equals(barMethod)) {
            return bar;
        } else {
            throw new UnsupportedOperationException();
        }
    }
}

// ObjectInputStream incapable of resolving proxy classes
class ProxyBlindInputStream extends ObjectInputStream {

    ProxyBlindInputStream(InputStream in) throws IOException { super(in); }

    protected Class<?> resolveProxyClass(String[] interfaces)
        throws IOException, ClassNotFoundException
    {
        throw new ClassNotFoundException();
    }
}

public class Basic {
    public static void main(String[] args) throws Exception {
        ClassLoader loader = Basic.class.getClassLoader();
        Class<?>[] interfaces = { Foo.class, Bar.class };
        Random rand = new Random();
        int foo = rand.nextInt();
        float bar = rand.nextFloat();
        ObjectOutputStream oout;
        ObjectInputStream oin;
        ByteArrayOutputStream bout;
        Object proxy;


        // test simple proxy write + read
        bout = new ByteArrayOutputStream();
        oout = new ObjectOutputStream(bout);
        oout.writeObject(Proxy.newProxyInstance(
            loader, interfaces, new Handler(foo, bar)));
        oout.close();

        oin = new ObjectInputStream(
            new ByteArrayInputStream(bout.toByteArray()));
        proxy = oin.readObject();
        if ((((Foo) proxy).foo() != foo) || (((Bar) proxy).bar() != bar)) {
            throw new Error();
        }


        // test missing proxy class ClassNotFoundException
        oin = new ProxyBlindInputStream(
            new ByteArrayInputStream(bout.toByteArray()));
        try {
            oin.readObject();
            throw new Error();
        } catch (ClassNotFoundException ex) {
        }
    }
}
