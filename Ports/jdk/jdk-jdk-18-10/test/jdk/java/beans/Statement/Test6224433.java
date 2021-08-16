/*
 * Copyright (c) 2005, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6224433
 * @summary Tests class loader lookup problem in Statement
 * @run main/othervm -Djava.security.manager=allow Test6224433
 * @author Jeff Nisewanger
 */

import java.beans.Expression;

public class Test6224433 {
    public static void main(String[] args) {
        try {
            System.setSecurityManager(new SecurityManager());
            Class target = Test6224433.class;
            String method = "forName";
            String[] params = {"sun.security.x509.X509CertInfo"};
            if (null != new Expression(target, method, params).getValue())
                throw new Error("failure: bug exists");

            throw new Error("unexpected condition");
        }
        catch (ClassNotFoundException exception) {
            throw new Error("expected class missing", exception);
        }
        catch (SecurityException exception) {
            // expected
        }
        catch (Exception exception) {
            throw new Error("unexpected condition", exception);
        }
    }
}
