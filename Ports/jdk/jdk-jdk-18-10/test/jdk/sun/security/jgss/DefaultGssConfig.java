/*
 * Copyright (c) 2006, 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6483218
 * @summary Provide a default login configuration
 * @modules java.security.jgss/sun.security.jgss
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.URIParameter;
import javax.security.auth.login.Configuration;
import sun.security.jgss.GSSUtil;
import sun.security.jgss.GSSCaller;
import sun.security.jgss.LoginConfigImpl;

public class DefaultGssConfig {

    public static void main(String[] argv) throws Exception {

        // 1. Make sure the FileNotFoundException is hidden
        try {
            Configuration.getInstance("JavaLoginConfig", new URIParameter(new URI("file:///no/such/file")));
        } catch (NoSuchAlgorithmException nsae) {
            if (nsae.getCause() instanceof IOException &&
                    !(nsae.getCause() instanceof FileNotFoundException)) {
                // ignore
            } else {
                throw nsae;
            }
        }

        // 2. Make sure there's always a Configuration even if no config file exists
        Configuration.getConfiguration();

        // 3. Make sure there're default entries for GSS krb5 client/server
        LoginConfigImpl lc = new LoginConfigImpl(GSSCaller.CALLER_INITIATE, GSSUtil.GSS_KRB5_MECH_OID);
        if (lc.getAppConfigurationEntry("").length == 0) {
            throw new Exception("No default config for GSS krb5 client");
        }
        lc = new LoginConfigImpl(GSSCaller.CALLER_ACCEPT, GSSUtil.GSS_KRB5_MECH_OID);
        if (lc.getAppConfigurationEntry("").length == 0) {
            throw new Exception("No default config for GSS krb5 server");
        }
    }
}
