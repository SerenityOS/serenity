/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4884913
 * @summary Tests that default protocols are loaded correctly
 * @author Eamonn McManus
 *
 * @run clean DefaultProviderTest
 * @run build DefaultProviderTest
 * @run main DefaultProviderTest
 */

import java.net.*;
import java.util.*;
import javax.management.remote.*;

/**
 * Create an environment where the providers for the default protocols
 * (e.g. rmi) are not visible to the loader that is used to load
 * non-default providers.  Check that we can load the default
 * protocols nevertheless.
 *
 * If JMX Remote is part of J2SE, this test becomes meaningless,
 * because the default protocol providers are visible to the bootstrap
 * class loader.
 */
public class DefaultProviderTest {
    public static void main(String[] args) throws Exception {
        URLClassLoader emptyLoader = new URLClassLoader(new URL[0], null);
        Thread.currentThread().setContextClassLoader(emptyLoader);
        Map env = new HashMap();
        env.put(JMXConnectorFactory.PROTOCOL_PROVIDER_CLASS_LOADER,
                emptyLoader);
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///bogus");
        JMXConnector conn = JMXConnectorFactory.newJMXConnector(url, env);
        System.out.println("Successfully created RMI connector in hostile " +
                           "class-loading environment");
    }
}
