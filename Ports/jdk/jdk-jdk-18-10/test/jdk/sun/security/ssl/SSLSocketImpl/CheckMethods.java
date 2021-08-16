/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4791676
 * @summary various pass through methods missing in SSLSocketImpl
 */
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

public class CheckMethods {
    static boolean debug = false;
    static class MethodSignature {
        String name;
        Class[] paramTypes;
        MethodSignature(String name, Class[] paramTypes) {
            this.name = name;
            this.paramTypes = paramTypes;
        }

        public boolean equals(Object obj) {
            if (debug) {
                System.out.println("comparing " + this + " against: " + obj);
            }
            if (!(obj instanceof MethodSignature)) {
                if (debug)
                    System.out.println(false);
                return false;
            }
            MethodSignature ms = (MethodSignature) obj;
            Class[] types = ms.paramTypes;
            try {
                for (int i = 0; i < types.length; i++) {
                    if (!types[i].equals(paramTypes[i])) {
                        if (debug)
                            System.out.println(false);
                        return false;
                    }
                }
            } catch (Exception e) {
                if (debug)
                    System.out.println(false);
                return false;
            }
            boolean result = this.name.equals(ms.name);
            if (debug)
                System.out.println(result);
            return result;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer(name + "(");
            for (int i = 0; i < paramTypes.length; i++) {
                sb.append(paramTypes[i].getName() + ",");
                if (i == (paramTypes.length - 1))
                    sb.deleteCharAt(sb.length() - 1);
            }
            sb.append(")");
            return sb.toString();
        }
    }

    // check that SSLSocket contains all public and protected
    // methods defined in Socket
    public static void main(String[] args) throws Exception {
        ArrayList allMethods = new ArrayList(
                Arrays.asList(Socket.class.getDeclaredMethods()));

        ArrayList allMethodSignatures = new ArrayList();
        for (Iterator itr = allMethods.iterator(); itr.hasNext();) {
            Method m = (Method) itr.next();
            // don't include static and private methods
            if (!Modifier.isStatic(m.getModifiers()) &&
                    (Modifier.isPublic(m.getModifiers()) ||
                    Modifier.isProtected(m.getModifiers()))) {
                allMethodSignatures.add( new MethodSignature(m.getName(),
                        m.getParameterTypes()));
            }
        }

        // testing Socket
        Class sslSI = Class.forName(
            "sun.security.ssl.SSLSocketImpl");
        Class baseSSLSI = Class.forName(
            "sun.security.ssl.BaseSSLSocketImpl");

        ArrayList sslSocketMethods =
                new ArrayList(Arrays.asList(sslSI.getDeclaredMethods()));

        sslSocketMethods.addAll( new ArrayList(
                Arrays.asList(baseSSLSI.getDeclaredMethods())));

        ArrayList sslSocketMethodSignatures = new ArrayList();
        for (Iterator itr = sslSocketMethods.iterator(); itr.hasNext();) {
            Method m = (Method) itr.next();
            if (!Modifier.isStatic(m.getModifiers())) {
                sslSocketMethodSignatures.add(
                        new MethodSignature(m.getName(),
                        m.getParameterTypes()));
            }
        }

        if (!sslSocketMethodSignatures.containsAll(allMethodSignatures)) {
            throw new RuntimeException(
                "Method definition test failed on SSLSocketImpl");
        }

        // testing for non static public field
        ArrayList allFields =
                new ArrayList(Arrays.asList(Socket.class.getFields()));

        for (Iterator itr = allFields.iterator(); itr.hasNext();) {
            Field f = (Field) itr.next();
            if (!Modifier.isStatic(f.getModifiers())) {
                throw new RuntimeException("Non static Public fields" +
                        " declared in superclasses");
            }
        }
    }
}
