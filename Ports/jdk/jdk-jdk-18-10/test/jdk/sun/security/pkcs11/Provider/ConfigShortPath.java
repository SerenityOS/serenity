/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
/**
 * @test
 * @bug 6581254 6986789 7196009 8062170
 * @summary Allow '~', '+', and quoted paths in config file
 * @author Valerie Peng
 */

import java.security.*;
import java.io.*;
import java.lang.reflect.*;

public class ConfigShortPath {

    private static final String[] winConfigNames = {
        "csp.cfg", "cspSpace.cfg", "cspQuotedPath.cfg"
    };
    private static final String[] solConfigNames = {
        "cspPlus.cfg"
    };

    public static void main(String[] args) throws Exception {
        Provider p = Security.getProvider("SunPKCS11");
        if (p == null) {
            System.out.println("Skipping test - no PKCS11 provider available");
            return;
        }

        String osInfo = System.getProperty("os.name", "");
        String[] configNames = (osInfo.contains("Windows")?
            winConfigNames : solConfigNames);

        String testSrc = System.getProperty("test.src", ".");
        for (int i = 0; i < configNames.length; i++) {
            String configFile = testSrc + File.separator + configNames[i];

            System.out.println("Testing against " + configFile);
            try {
                p.configure(configFile);
            } catch (InvalidParameterException ipe) {
                ipe.printStackTrace();
                Throwable cause = ipe.getCause();
                // Indicate failure if due to parsing config
                if (cause.getClass().getName().equals
                        ("sun.security.pkcs11.ConfigurationException")) {
                    // Error occurred during parsing
                    if (cause.getMessage().indexOf("Unexpected") != -1) {
                        throw (ProviderException) cause;
                    }
                }
            } catch (ProviderException pe) {
                pe.printStackTrace();
                if (pe.getCause() instanceof IOException) {
                    // Thrown when the directory does not exist which is ok
                    System.out.println("Pass: config parsed ok");
                    continue;
                }
            }
        }
    }
}
