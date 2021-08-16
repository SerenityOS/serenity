/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @test
 * @bug 8005899
 * @build TestLogger testlogger.MyResource
 * @run main/othervm TestLogger
 * @run main/othervm -Djava.security.manager=allow -Dsecurity=on TestLogger
 **/
public class TestLogger {

    public static final String RESOURCE_BUNDLE = "testlogger.MyResource";
    public static final String ORG_LOGGER = "org";
    public static final String FOO_LOGGER = ORG_LOGGER + ".foo.Foo";
    public static final String BAR_LOGGER = ORG_LOGGER + ".bar.Bar";
    public static final String GEE_LOGGER = ORG_LOGGER + ".gee.Gee";
    public static final String GEE_GEE_LOGGER = GEE_LOGGER+".Gee";

    public static void main(String[] args) {
        final String security = System.getProperty("security", "off");
        System.out.println("Security is " + security);
        if ("on".equals(security)) {
           System.setSecurityManager(new SecurityManager());
        }

        newLogger(FOO_LOGGER, RESOURCE_BUNDLE);
        newLogger(FOO_LOGGER);
        newLogger(BAR_LOGGER);
        newLogger(BAR_LOGGER, RESOURCE_BUNDLE);
        newLogger(GEE_LOGGER, null);
        newLogger(GEE_LOGGER, RESOURCE_BUNDLE);
        newLogger(ORG_LOGGER);
        newLogger(GEE_GEE_LOGGER);

        for (String log : new String[] { FOO_LOGGER, BAR_LOGGER, GEE_LOGGER }) {
            if (!RESOURCE_BUNDLE.equals(Logger.getLogger(log).getResourceBundleName())) {
                throw new RuntimeException("Shouldn't allow to reset the resource bundle for " + log);
            }
            try {
                Logger logger = Logger.getLogger(log, null);
                if (!RESOURCE_BUNDLE.equals(logger.getResourceBundleName())) {
                    throw new RuntimeException("Shouldn't allow to reset the resource bundle for " + log);
                }
                throw new RuntimeException("Expected IllegalArgumentException not thrown for " + log);
            } catch (IllegalArgumentException e) {
                System.out.println("Got expected exception for " + log +": " + e);
            }
        }
        for (String log : new String[] { ORG_LOGGER, GEE_GEE_LOGGER }) {
            if (Logger.getLogger(log).getResourceBundleName() != null) {
                throw new RuntimeException("Resource bundle is not null for log: "
                           + Logger.getLogger(log).getResourceBundleName());
            }
            try {
                Logger logger = Logger.getLogger(log, null);
                if (logger.getResourceBundleName() != null) {
                    throw new RuntimeException("Resource bundle is not null for log: "
                               + logger.getResourceBundleName());
                }
                System.out.println("Success calling Logger.getLogger(\""+log+"\", null)");
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Unexpected exception for " + log +": " + e, e);
            }
        }
    }

    private static List<Logger> strongRefs = new ArrayList<>();
    private static void newLogger(String name) {
        strongRefs.add(Logger.getLogger(name));
    }
    private static void newLogger(String name, String resourceBundleName) {
        strongRefs.add(Logger.getLogger(name, resourceBundleName));
    }
}
