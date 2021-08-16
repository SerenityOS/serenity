/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6345338
 * @modules java.security.jgss/sun.security.jgss
 * @run main/othervm GssNPE
 * @summary GSS throws NPE when the JAAS config file does not exist
 */

import sun.security.jgss.*;

public class GssNPE {

    public static void main(String[] argv) throws Exception {
        System.setProperty("java.security.auth.login.config",
                "this.file.does.not.exist");
        // When JAAS login config file is lost, the original JGSS throws a
        // SecurityException which holds an IOException saying the file does
        // not exist. New caller-enabled JGSS changed this. this bug fix will
        // revert to the old behavior.
        try {
            GSSUtil.login(GSSCaller.CALLER_INITIATE, GSSUtil.GSS_KRB5_MECH_OID);
        } catch (SecurityException se) {
            if (se.getCause() instanceof java.io.IOException) {
                // what had been and should be...
            } else {
                throw se;
            }
        }
    }
}
