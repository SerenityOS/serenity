/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
package java.rmi.server;

import java.io.InvalidObjectException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.UnexpectedException;
import java.util.Map;
import java.util.WeakHashMap;
import sun.rmi.server.Util;
import sun.rmi.server.WeakClassHashMap;

/**
 * An implementation of the <code>InvocationHandler</code> interface for
 * use with Java Remote Method Invocation (Java RMI).  This invocation
 * handler can be used in conjunction with a dynamic proxy instance as a
 * replacement for a pregenerated stub class.
 *
 * <p>Applications are not expected to use this class directly.  A remote
 * object exported to use a dynamic proxy with {@link UnicastRemoteObject}
 * has an instance of this class as that proxy's invocation handler.
 *
 * @author  Ann Wollrath
 * @since   1.5
 **/
public class RemoteObjectInvocationHandler
    extends RemoteObject
    implements InvocationHandler
{
    private static final long serialVersionUID = 2L;

    /**
     * A weak hash map, mapping classes to weak hash maps that map
     * method objects to method hashes.
     **/
    private static final MethodToHash_Maps methodToHash_Maps =
        new MethodToHash_Maps();

    /**
     * Creates a new <code>RemoteObjectInvocationHandler</code> constructed
     * with the specified <code>RemoteRef</code>.
     *
     * @param ref the remote ref
     *
     * @throws NullPointerException if <code>ref</code> is <code>null</code>
     **/
    public RemoteObjectInvocationHandler(RemoteRef ref) {
        super(ref);
        if (ref == null) {
            throw new NullPointerException();
        }
    }

    /**
     * Processes a method invocation made on the encapsulating
     * proxy instance, <code>proxy</code>, and returns the result.
     *
     * <p><code>RemoteObjectInvocationHandler</code> implements this method
     * as follows:
     *
     * <p>If <code>method</code> is one of the following methods, it
     * is processed as described below:
     *
     * <ul>
     *
     * <li>{@link Object#hashCode Object.hashCode}: Returns the hash
     * code value for the proxy.
     *
     * <li>{@link Object#equals Object.equals}: Returns <code>true</code>
     * if the argument (<code>args[0]</code>) is an instance of a dynamic
     * proxy class and this invocation handler is equal to the invocation
     * handler of that argument, and returns <code>false</code> otherwise.
     *
     * <li>{@link Object#toString Object.toString}: Returns a string
     * representation of the proxy.
     * </ul>
     *
     * <p>If <code>method</code> overrides {@link Object#finalize Object.finalize},
     * it is ignored.
     *
     * <p>Otherwise, a remote call is made as follows:
     *
     * <ul>
     * <li>If <code>proxy</code> is not an instance of the interface
     * {@link Remote}, then an {@link IllegalArgumentException} is thrown.
     *
     * <li>Otherwise, the {@link RemoteRef#invoke invoke} method is invoked
     * on this invocation handler's <code>RemoteRef</code>, passing
     * <code>proxy</code>, <code>method</code>, <code>args</code>, and the
     * method hash (defined in section 8.3 of the "Java Remote Method
     * Invocation (RMI) Specification") for <code>method</code>, and the
     * result is returned.
     *
     * <li>If an exception is thrown by <code>RemoteRef.invoke</code> and
     * that exception is a checked exception that is not assignable to any
     * exception in the <code>throws</code> clause of the method
     * implemented by the <code>proxy</code>'s class, then that exception
     * is wrapped in an {@link UnexpectedException} and the wrapped
     * exception is thrown.  Otherwise, the exception thrown by
     * <code>invoke</code> is thrown by this method.
     * </ul>
     *
     * <p>The semantics of this method are unspecified if the
     * arguments could not have been produced by an instance of some
     * valid dynamic proxy class containing this invocation handler.
     *
     * @param proxy the proxy instance that the method was invoked on
     * @param method the <code>Method</code> instance corresponding to the
     * interface method invoked on the proxy instance
     * @param args an array of objects containing the values of the
     * arguments passed in the method invocation on the proxy instance, or
     * <code>null</code> if the method takes no arguments
     * @return the value to return from the method invocation on the proxy
     * instance
     * @throws  Throwable the exception to throw from the method invocation
     * on the proxy instance
     **/
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable
    {
        if (! Proxy.isProxyClass(proxy.getClass())) {
            throw new IllegalArgumentException("not a proxy");
        }

        if (Proxy.getInvocationHandler(proxy) != this) {
            throw new IllegalArgumentException("handler mismatch");
        }

        if (method.getDeclaringClass() == Object.class) {
            return invokeObjectMethod(proxy, method, args);
        } else if ("finalize".equals(method.getName()) && method.getParameterCount() == 0) {
            return null; // ignore
        } else {
            return invokeRemoteMethod(proxy, method, args);
        }
    }

    /**
     * Handles java.lang.Object methods.
     **/
    private Object invokeObjectMethod(Object proxy,
                                      Method method,
                                      Object[] args)
    {
        String name = method.getName();

        if (name.equals("hashCode")) {
            return hashCode();

        } else if (name.equals("equals")) {
            Object obj = args[0];
            InvocationHandler hdlr;
            return
                proxy == obj ||
                (obj != null &&
                 Proxy.isProxyClass(obj.getClass()) &&
                 (hdlr = Proxy.getInvocationHandler(obj)) instanceof RemoteObjectInvocationHandler &&
                 this.equals(hdlr));

        } else if (name.equals("toString")) {
            return proxyToString(proxy);

        } else {
            throw new IllegalArgumentException(
                "unexpected Object method: " + method);
        }
    }

    /**
     * Handles remote methods.
     **/
    private Object invokeRemoteMethod(Object proxy,
                                      Method method,
                                      Object[] args)
        throws Exception
    {
        try {
            if (!(proxy instanceof Remote)) {
                throw new IllegalArgumentException(
                    "proxy not Remote instance");
            }

            // Verify that the method is declared on an interface that extends Remote
            Class<?> decl = method.getDeclaringClass();
            if (!Remote.class.isAssignableFrom(decl)) {
                throw new RemoteException("Method is not Remote: " + decl + "::" + method);
            }

            return ref.invoke((Remote) proxy, method, args,
                              getMethodHash(method));
        } catch (Exception e) {
            if (!(e instanceof RuntimeException)) {
                Class<?> cl = proxy.getClass();
                try {
                    method = cl.getMethod(method.getName(),
                                          method.getParameterTypes());
                } catch (NoSuchMethodException nsme) {
                    throw (IllegalArgumentException)
                        new IllegalArgumentException().initCause(nsme);
                }
                Class<?> thrownType = e.getClass();
                for (Class<?> declaredType : method.getExceptionTypes()) {
                    if (declaredType.isAssignableFrom(thrownType)) {
                        throw e;
                    }
                }
                e = new UnexpectedException("unexpected exception", e);
            }
            throw e;
        }
    }

    /**
     * Returns a string representation for a proxy that uses this invocation
     * handler.
     **/
    private String proxyToString(Object proxy) {
        Class<?>[] interfaces = proxy.getClass().getInterfaces();
        if (interfaces.length == 0) {
            return "Proxy[" + this + "]";
        }
        String iface = interfaces[0].getName();
        if (iface.equals("java.rmi.Remote") && interfaces.length > 1) {
            iface = interfaces[1].getName();
        }
        int dot = iface.lastIndexOf('.');
        if (dot >= 0) {
            iface = iface.substring(dot + 1);
        }
        return "Proxy[" + iface + "," + this + "]";
    }

    /**
     * @throws InvalidObjectException unconditionally
     **/
    private void readObjectNoData() throws InvalidObjectException {
        throw new InvalidObjectException("no data in stream; class: " +
                                         this.getClass().getName());
    }

    /**
     * Returns the method hash for the specified method.  Subsequent calls
     * to "getMethodHash" passing the same method argument should be faster
     * since this method caches internally the result of the method to
     * method hash mapping.  The method hash is calculated using the
     * "computeMethodHash" method.
     *
     * @param method the remote method
     * @return the method hash for the specified method
     */
    private static long getMethodHash(Method method) {
        return methodToHash_Maps.get(method.getDeclaringClass()).get(method);
    }

    /**
     * A weak hash map, mapping classes to weak hash maps that map
     * method objects to method hashes.
     **/
    private static class MethodToHash_Maps
        extends WeakClassHashMap<Map<Method,Long>>
    {
        MethodToHash_Maps() {}

        protected Map<Method,Long> computeValue(Class<?> remoteClass) {
            return new WeakHashMap<Method,Long>() {
                public synchronized Long get(Object key) {
                    Long hash = super.get(key);
                    if (hash == null) {
                        Method method = (Method) key;
                        hash = Util.computeMethodHash(method);
                        put(method, hash);
                    }
                    return hash;
                }
            };
        }
    }
}
