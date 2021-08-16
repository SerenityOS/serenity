/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import java.util.Hashtable;

/*
 * @test
 * @bug 8051422
 * @summary Make sure java.applet.Applet is not used as a source of
 *          configuration parameters for an InitialContext
 */
public class AppletIsNotUsed {

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws NamingException {

        testWith(Context.APPLET);
        testWith("java.naming.applet");

    }

    private static void testWith(String appletProperty) throws NamingException {
        Hashtable<Object, Object> env = new Hashtable<>();
        // Deliberately put java.lang.Object rather than java.applet.Applet
        // if an applet was used we would see a ClassCastException down there
        env.put(appletProperty, new Object());
        // It's ok to instantiate InitialContext with no parameters
        // and be unaware of it right until you try to use it
        Context ctx = new InitialContext(env);
        boolean threw = true;
        try {
            ctx.lookup("whatever");
            threw = false;
        } catch (NoInitialContextException e) {
            String m = e.getMessage();
            if (m == null || m.contains("applet"))
                throw new RuntimeException("The exception message is incorrect", e);
        } catch (Throwable t) {
            throw new RuntimeException(
                    "The test was supposed to catch NoInitialContextException" +
                            " here, but caught: " + t.getClass().getName(), t);
        } finally {
            ctx.close();
        }

        if (!threw)
            throw new RuntimeException("The test was supposed to catch NoInitialContextException here");
    }
}
