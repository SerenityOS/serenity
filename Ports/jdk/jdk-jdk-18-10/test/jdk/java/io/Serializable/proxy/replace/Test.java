/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Ensure that serialization invokes writeReplace/readResolve methods
 *          on dynamic proxies, just as with normal objects.
 */

import java.io.*;
import java.lang.reflect.*;

public class Test implements InvocationHandler, Serializable {
    private static final long serialVersionUID = 1L;

    static ClassLoader loader = Test.class.getClassLoader();

    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable
    {
        String methName = method.getName();
        if (methName.equals("writeReplace")) {
            return Proxy.newProxyInstance(
                loader, new Class<?>[] { ReadResolve.class }, this);
        } else if (methName.equals("readResolve")) {
            return Proxy.newProxyInstance(
                loader, new Class<?>[] { Resolved.class }, this);
        } else if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        } else {
            throw new Error();
        }
    }

    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(Proxy.newProxyInstance(
            loader, new Class<?>[] { WriteReplace.class }, new Test()));
        oout.close();
        ObjectInputStream oin = new ObjectInputStream(
            new ByteArrayInputStream(bout.toByteArray()));
        if (!(oin.readObject() instanceof Resolved)) {
            throw new Error();
        }
    }
}
