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

/*
 * @test
 * @bug 4423074
 * @summary Need to rebase all the duplicated classes from Merlin.
 *          This test will check out http POST
 * @modules java.base/sun.net.www.protocol.https
 */
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import sun.net.www.protocol.https.HttpsURLConnectionImpl;

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
            if (debug)
                System.out.println("comparing "+this +" against: "+obj);
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
            StringBuffer sb = new StringBuffer(name+"(");
            for (int i = 0; i < paramTypes.length; i++) {
                sb.append(paramTypes[i].getName()+",");
                if (i == (paramTypes.length -1))
                    sb.deleteCharAt(sb.length()-1);
            }
            sb.append(")");
            return sb.toString();
        }
    }

    // check HttpsURLConnectionImpl contain all public and protected methods
    // defined in HttpURLConnection and URLConnection
    public static void main(String[] args) {
        ArrayList allMethods = new ArrayList(
            Arrays.asList(HttpURLConnection.class.getDeclaredMethods()));
        allMethods.addAll(Arrays.asList(URLConnection.class.getDeclaredMethods()));
        ArrayList allMethodSignatures = new ArrayList();
        for (Iterator itr = allMethods.iterator(); itr.hasNext(); ) {
            Method m = (Method)itr.next();
            // don't include static and private methods
            if (!Modifier.isStatic(m.getModifiers()) &&
                (Modifier.isPublic(m.getModifiers()) ||
                 Modifier.isProtected(m.getModifiers()))) {
                allMethodSignatures.add(
                    new MethodSignature(m.getName(), m.getParameterTypes()));
            }
        }

        // testing HttpsURLConnectionImpl
        List httpsMethods =
            Arrays.asList(HttpsURLConnectionImpl.class.getDeclaredMethods());

        ArrayList httpsMethodSignatures = new ArrayList();
        for (Iterator itr = httpsMethods.iterator(); itr.hasNext(); ) {
            Method m = (Method)itr.next();
            if (!Modifier.isStatic(m.getModifiers())) {
                httpsMethodSignatures.add(
                 new MethodSignature(m.getName(), m.getParameterTypes()));
            }
        }

        if (!httpsMethodSignatures.containsAll(allMethodSignatures)) {
            throw new RuntimeException("Method definition test failed on HttpsURLConnectionImpl");
        }

        // testing for non static public field
        ArrayList allFields = new ArrayList(
            Arrays.asList(URLConnection.class.getFields()));
        allFields.addAll(Arrays.asList(HttpURLConnection.class.getFields()));

        for (Iterator itr = allFields.iterator(); itr.hasNext(); ) {
            Field f = (Field) itr.next();
            if (!Modifier.isStatic(f.getModifiers())) {
                throw new RuntimeException("Non static Public fields" +
                                           " declared in superclasses");
            }
        }
    }
}
