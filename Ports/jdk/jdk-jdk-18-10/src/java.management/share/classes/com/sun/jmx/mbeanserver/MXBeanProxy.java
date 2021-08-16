/*
 * Copyright (c) 2005, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.jmx.mbeanserver;

import static com.sun.jmx.mbeanserver.Util.*;

import java.lang.reflect.Method;
import java.util.Map;

import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

/**
   <p>Helper class for an {@link InvocationHandler} that forwards methods from an
   MXBean interface to a named
   MXBean in an MBean Server and handles translation between the
   arbitrary Java types in the interface and the Open Types used
   by the MXBean.</p>

   @since 1.6
*/
public class MXBeanProxy {
    public MXBeanProxy(Class<?> mxbeanInterface) {

        if (mxbeanInterface == null)
            throw new IllegalArgumentException("Null parameter");

        final MBeanAnalyzer<ConvertingMethod> analyzer;
        try {
            analyzer =
                MXBeanIntrospector.getInstance().getAnalyzer(mxbeanInterface);
        } catch (NotCompliantMBeanException e) {
            throw new IllegalArgumentException(e);
        }
        analyzer.visit(new Visitor());
    }

    private class Visitor
            implements MBeanAnalyzer.MBeanVisitor<ConvertingMethod> {
        public void visitAttribute(String attributeName,
                                   ConvertingMethod getter,
                                   ConvertingMethod setter) {
            if (getter != null) {
                getter.checkCallToOpen();
                Method getterMethod = getter.getMethod();
                handlerMap.put(getterMethod,
                               new GetHandler(attributeName, getter));
            }
            if (setter != null) {
                // return type is void, no need for checkCallToOpen
                Method setterMethod = setter.getMethod();
                handlerMap.put(setterMethod,
                               new SetHandler(attributeName, setter));
            }
        }

        public void visitOperation(String operationName,
                                   ConvertingMethod operation) {
            operation.checkCallToOpen();
            Method operationMethod = operation.getMethod();
            String[] sig = operation.getOpenSignature();
            handlerMap.put(operationMethod,
                           new InvokeHandler(operationName, sig, operation));
        }
    }

    private static abstract class Handler {
        Handler(String name, ConvertingMethod cm) {
            this.name = name;
            this.convertingMethod = cm;
        }

        String getName() {
            return name;
        }

        ConvertingMethod getConvertingMethod() {
            return convertingMethod;
        }

        abstract Object invoke(MBeanServerConnection mbsc,
                               ObjectName name, Object[] args) throws Exception;

        private final String name;
        private final ConvertingMethod convertingMethod;
    }

    private static class GetHandler extends Handler {
        GetHandler(String attributeName, ConvertingMethod cm) {
            super(attributeName, cm);
        }

        @Override
        Object invoke(MBeanServerConnection mbsc, ObjectName name, Object[] args)
                throws Exception {
            assert(args == null || args.length == 0);
            return mbsc.getAttribute(name, getName());
        }
    }

    private static class SetHandler extends Handler {
        SetHandler(String attributeName, ConvertingMethod cm) {
            super(attributeName, cm);
        }

        @Override
        Object invoke(MBeanServerConnection mbsc, ObjectName name, Object[] args)
                throws Exception {
            assert(args.length == 1);
            Attribute attr = new Attribute(getName(), args[0]);
            mbsc.setAttribute(name, attr);
            return null;
        }
    }

    private static class InvokeHandler extends Handler {
        InvokeHandler(String operationName, String[] signature,
                      ConvertingMethod cm) {
            super(operationName, cm);
            this.signature = signature;
        }

        Object invoke(MBeanServerConnection mbsc, ObjectName name, Object[] args)
                throws Exception {
            return mbsc.invoke(name, getName(), args, signature);
        }

        private final String[] signature;
    }

    public Object invoke(MBeanServerConnection mbsc, ObjectName name,
                         Method method, Object[] args)
            throws Throwable {

        Handler handler = handlerMap.get(method);
        ConvertingMethod cm = handler.getConvertingMethod();
        MXBeanLookup lookup = MXBeanLookup.lookupFor(mbsc);
        MXBeanLookup oldLookup = MXBeanLookup.getLookup();
        try {
            MXBeanLookup.setLookup(lookup);
            Object[] openArgs = cm.toOpenParameters(lookup, args);
            Object result = handler.invoke(mbsc, name, openArgs);
            return cm.fromOpenReturnValue(lookup, result);
        } finally {
            MXBeanLookup.setLookup(oldLookup);
        }
    }

    private final Map<Method, Handler> handlerMap = newMap();
}
