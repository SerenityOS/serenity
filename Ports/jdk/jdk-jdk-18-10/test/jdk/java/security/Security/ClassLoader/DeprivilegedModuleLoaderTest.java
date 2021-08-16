/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8159964
 * @summary Classes from deprivileged modules should get loaded through
 *          Platform Classloader.
 * @modules java.xml.crypto
 *          jdk.security.auth
 *          jdk.security.jgss
 * @run main DeprivilegedModuleLoaderTest
 */

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.security.auth.kerberos.KeyTab;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import com.sun.security.auth.callback.TextCallbackHandler;
import com.sun.security.jgss.AuthorizationDataEntry;

public class DeprivilegedModuleLoaderTest {

    public static void main(String[] args) {

        boolean pass = true;
        List<Class<?>> classes = getDeprivilegedClasses();
        for (Class<?> cls : classes) {
            try {
                pass &= testPlatformClassLoader(cls);
            } catch (Exception exc) {
                exc.printStackTrace(System.out);
                pass = false;
            }
        }

        if (!pass) {
            throw new RuntimeException("Atleast one test failed.");
        }
    }

    private static List<Class<?>> getDeprivilegedClasses() {

        List<Class<?>> classes = new ArrayList<Class<?>>();
        // Test from java.xml.crypto/javax/xml/crypto/dsig package
        classes.add(XMLSignatureFactory.class);
        // Test from java.xml.crypto/javax/xml/crypto package
        classes.add(KeySelectorException.class);
        // Test From java.security.jgss/javax/security/auth/kerberos package
        classes.add(KeyTab.class);
        // Test from jdk.security.jgss/com/sun/security/jgss package
        classes.add(AuthorizationDataEntry.class);
        // Test from jdk.security.auth/com/sun/security/auth/callback package
        classes.add(TextCallbackHandler.class);
        return classes;
    }

    private static boolean testPlatformClassLoader(Class<?> cls) {

        ClassLoader loader = cls.getClassLoader();
        if (loader == null) {
            throw new RuntimeException(String.format(
                    "Loaded through Bootstrap Classloader: '%s'", cls));
        } else if (!loader.toString().contains("PlatformClassLoader")) {
            throw new RuntimeException(String.format(
                    "Not loaded through Platform ClassLoader: '%s'", cls));
        }
        System.out.println(String.format(
                "Pass: '%s' get loaded through PlatformClassLoader", cls));
        return true;
    }
}
