/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 */

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.management.MBeanServer;
import javax.management.remote.MBeanServerForwarder;

public class MBeanServerForwarderInvocationHandler
    implements InvocationHandler {

    public static MBeanServerForwarder newProxyInstance() {

        final InvocationHandler handler =
            new MBeanServerForwarderInvocationHandler();

        final Class[] interfaces =
            new Class[] {MBeanServerForwarder.class};

        Object proxy = Proxy.newProxyInstance(
                             MBeanServerForwarder.class.getClassLoader(),
                             interfaces,
                             handler);

        return MBeanServerForwarder.class.cast(proxy);
    }

    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {

        final String methodName = method.getName();

        if (methodName.equals("getMBeanServer")) {
            return mbs;
        }

        if (methodName.equals("setMBeanServer")) {
            if (args[0] == null)
                throw new IllegalArgumentException("Null MBeanServer");
            if (mbs != null)
                throw new IllegalArgumentException("MBeanServer object " +
                                                   "already initialized");
            mbs = (MBeanServer) args[0];
            return null;
        }

        if (methodName.equals("getAttribute") && exception != null) {
            throw exception;
        }

        return method.invoke(mbs, args);
    }

    public void setGetAttributeException(Exception exception) {
        this.exception = exception;
    }

    private Exception exception;
    private MBeanServer mbs;
}
