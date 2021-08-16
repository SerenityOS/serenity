/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.ReflectPermission;
import java.security.AccessControlException;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.SecurityPermission;
import java.util.*;

/*
 * @test
 * @bug 8004260 8189291
 * @summary Test proxy classes that implement non-public interface
 *
 * @build p.Foo
 * @run main/othervm -Djava.security.manager=allow NonPublicProxyClass grant
 * @run main/othervm -Djava.security.manager=allow NonPublicProxyClass deny
 * @run main/othervm NonPublicProxyClass
 */
public class NonPublicProxyClass {
    static final Policy DEFAULT_POLICY = Policy.getPolicy();

    public interface PublicInterface {
        void foo();
    }
    interface NonPublicInterface {
        void bar();
    }

    public static void main(String[] args) throws Exception {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        Class<?> zipConstantsClass = Class.forName("java.util.zip.ZipConstants", false, null);
        Class<?> fooClass = Class.forName("p.Foo");

        NonPublicProxyClass test1 =
            new NonPublicProxyClass(loader, PublicInterface.class, NonPublicInterface.class);
        NonPublicProxyClass test2 =
            new NonPublicProxyClass(loader, fooClass, PublicInterface.class);
        NonPublicProxyClass test3 =
            new NonPublicProxyClass(null, zipConstantsClass);

        if (args.length == 1) {
            switch (args[0]) {
                case "grant": Policy.setPolicy(new NewInstancePolicy(true));
                              break;
                case "deny" : Policy.setPolicy(new NewInstancePolicy(false));
                              break;
                default: throw new IllegalArgumentException(args[0]);
            }
            System.setSecurityManager(new SecurityManager());
        }

        test1.run();
        test2.run();
        test3.run();
        System.out.format("Test passed: security %s%n",
            (args.length == 0 ? "manager not installed" : Policy.getPolicy()));
    }

    private final ClassLoader loader;
    private final Class<?>[] interfaces;
    private final InvocationHandler handler = newInvocationHandler();
    private Class<?> proxyClass;
    public NonPublicProxyClass(ClassLoader loader, Class<?> ... intfs) {
        this.loader = loader;
        this.interfaces = intfs;
    }

    public void run() throws Exception {
        boolean hasAccess = loader != null || hasAccess();
        try {
            proxyClass = Proxy.getProxyClass(loader, interfaces);
            if (!hasAccess) {
                throw new RuntimeException("should have no permission to create proxy class");
            }
        } catch (AccessControlException e) {
            if (hasAccess) {
                throw e;
            }
            if (e.getPermission().getClass() != RuntimePermission.class ||
                    !e.getPermission().getName().equals("getClassLoader")) {
                throw e;
            }
            return;
        }

        if (Modifier.isPublic(proxyClass.getModifiers())) {
            throw new RuntimeException(proxyClass + " must be non-public");
        }
        newProxyInstance();
        newInstanceFromConstructor(proxyClass);
    }

    private boolean hasAccess() {
        if (System.getSecurityManager() == null) {
            return true;
        }
        NewInstancePolicy policy = NewInstancePolicy.class.cast(Policy.getPolicy());
        return policy.grant;
    }

    private static final String NEW_PROXY_IN_PKG = "newProxyInPackage.";
    private void newProxyInstance() {
        // expect newProxyInstance to succeed if it's in the same runtime package
        int i = proxyClass.getName().lastIndexOf('.');
        String pkg = (i != -1) ? proxyClass.getName().substring(0, i) : "";
        boolean hasAccess = pkg.isEmpty() || hasAccess();
        try {
            Proxy.newProxyInstance(loader, interfaces, handler);
            if (!hasAccess) {
                throw new RuntimeException("ERROR: Proxy.newProxyInstance should fail " + proxyClass);
            }
        } catch (AccessControlException e) {
            if (hasAccess) {
                throw e;
            }
            if (e.getPermission().getClass() != ReflectPermission.class ||
                    !e.getPermission().getName().equals(NEW_PROXY_IN_PKG + pkg)) {
                throw e;
            }
        }
    }

    private void newInstanceFromConstructor(Class<?> proxyClass)
        throws Exception
    {
        // expect newInstance to succeed if it's in the same runtime package
        boolean isSamePackage = proxyClass.getName().lastIndexOf('.') == -1;
        try {
            Constructor cons = proxyClass.getConstructor(InvocationHandler.class);
            cons.newInstance(newInvocationHandler());
            if (!isSamePackage) {
                throw new RuntimeException("ERROR: Constructor.newInstance should not succeed");
            }
        }  catch (IllegalAccessException e) {
            if (isSamePackage) {
                throw e;
            }
        }
    }

    private static InvocationHandler newInvocationHandler() {
        return new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
                Class<?>[] intfs = proxy.getClass().getInterfaces();
                System.out.println("Proxy for " + Arrays.toString(intfs)
                        + " " + method.getName() + " is being invoked");
                return null;
            }
        };
    }

    static class NewInstancePolicy extends Policy {
        final PermissionCollection permissions = new Permissions();
        final boolean grant;
        NewInstancePolicy(boolean grant) {
            this.grant = grant;
            permissions.add(new SecurityPermission("getPolicy"));
            if (grant) {
                permissions.add(new RuntimePermission("getClassLoader"));
                permissions.add(new ReflectPermission(NEW_PROXY_IN_PKG + "p"));
                permissions.add(new ReflectPermission(NEW_PROXY_IN_PKG + "java.util.zip"));
            }
        }
        public PermissionCollection getPermissions(ProtectionDomain domain) {
            return permissions;
        }

        public PermissionCollection getPermissions(CodeSource codesource) {
            return permissions;
        }

        public boolean implies(ProtectionDomain domain, Permission perm) {
            return permissions.implies(perm) ||
                    DEFAULT_POLICY.implies(domain, perm);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("policy: ");
            Enumeration<Permission> perms = permissions.elements();
            while (perms.hasMoreElements()) {
                sb.append("\n").append(perms.nextElement().toString());
            }
            return sb.toString();
        }
    }
}
