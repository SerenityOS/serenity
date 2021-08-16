/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6268315
 * @bug 6273812
 * @modules jdk.security.auth
 * @summary Configuration should be provider-based
 * @build GetInstanceConfigSpi GetInstanceProvider
 * @run main/othervm -Djava.security.auth.login.config==${test.src}${/}GetInstance.config GetInstance
 */

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.URIParameter;
import java.io.File;
import java.net.URI;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

public class GetInstance {

    private static final String JAVA_CONFIG = "JavaLoginConfig";

    private static final String MOD0 = "com.foo.Module";
    private static final String MOD1 = "com.bar.Module";
    private static final String MOD2 = "com.foobar.Module";
    private static final String MOD3 = "Module";

    private static class BadParam implements Configuration.Parameters { }

    public static void main(String[] args) throws Exception {

        int testnum = 1;
        GetInstance gi = new GetInstance();

        testnum = gi.testDefault(testnum);
        testnum = gi.testStringProvider(testnum);
        testnum = gi.testProvider(testnum);
        testnum = gi.testCustomImpl(testnum);
        testnum = gi.testURIParam(testnum);
        testnum = gi.testException(testnum);
    }

    private int testDefault(int testnum) throws Exception {
        // get an instance of the default ConfigSpiFile
        Configuration c = Configuration.getInstance(JAVA_CONFIG, null);
        doTest(c, testnum++);

        // get an instance of FooConfig
        try {
            c = Configuration.getInstance("FooConfig", null);
            throw new SecurityException("test " + testnum++ + " failed");
        } catch (NoSuchAlgorithmException nsae) {
            // good
            System.out.println("test " + testnum++ + " passed");
        }

        return testnum;
    }

    private int testStringProvider(int testnum) throws Exception {
        // get an instance of JavaLoginConfig from SUN
        Configuration c = Configuration.getInstance(JAVA_CONFIG, null, "SUN");
        doTest(c, testnum++);

        // get an instance of JavaLoginConfig from SunRsaSign
        try {
            c = Configuration.getInstance(JAVA_CONFIG, null, "SunRsaSign");
            throw new SecurityException("test " + testnum++ + " failed");
        } catch (NoSuchAlgorithmException nsae) {
            // good
            System.out.println("test " + testnum++ + " passed");
        }

        // get an instance of JavaLoginConfig from FOO
        try {
            c = Configuration.getInstance(JAVA_CONFIG, null, "FOO");
            throw new SecurityException("test " + testnum++ + " failed");
        } catch (NoSuchProviderException nspe) {
            // good
            System.out.println("test " + testnum++ + " passed");
        }

        return testnum;
    }

    private int testProvider(int testnum) throws Exception {
        // get an instance of JavaLoginConfig from SUN
        Configuration c = Configuration.getInstance(JAVA_CONFIG,
                                null,
                                Security.getProvider("SUN"));
        doTest(c, testnum++);

        // get an instance of JavaLoginConfig from SunRsaSign
        try {
            c = Configuration.getInstance(JAVA_CONFIG,
                                null,
                                Security.getProvider("SunRsaSign"));
            throw new SecurityException("test " + testnum++ + " failed");
        } catch (NoSuchAlgorithmException nsae) {
            // good
            System.out.println("test " + testnum++ + " passed");
        }

        return testnum;
    }

    private int testCustomImpl(int testnum) throws Exception {
        Provider customProvider = new GetInstanceProvider();
        Configuration c = Configuration.getInstance("GetInstanceConfigSpi",
                                null,
                                customProvider);
        doCustomTest(c, testnum++, customProvider);
        return testnum;
    }

    private int testURIParam(int testnum) throws Exception {
        // get an instance of JavaLoginConfig
        // from SUN and have it read from the URI

        File file = new File(System.getProperty("test.src", "."),
                                "GetInstance.configURI");
        URI uri = file.toURI();
        URIParameter uriParam = new URIParameter(uri);
        Configuration c = Configuration.getInstance(JAVA_CONFIG, uriParam);
        doTestURI(c, uriParam, testnum++);

        return testnum;
    }

    private int testException(int testnum) throws Exception {
        // get an instance of JavaLoginConfig
        // from SUN and have it read from the bad URI

        File file = new File(System.getProperty("test.src", "."),
                                "GetInstance.bad.configURI");
        URI uri = file.toURI();
        URIParameter uriParam = new URIParameter(uri);

        try {
            Configuration c = Configuration.getInstance(JAVA_CONFIG, uriParam);
            throw new SecurityException("expected IOException");
        } catch (NoSuchAlgorithmException nsae) {
            if (nsae.getCause() instanceof java.io.IOException) {
                System.out.println("exception test passed: " +
                                nsae.getCause().getMessage());
            } else {
                throw new SecurityException("expected IOException");
            }
        }

        // pass bad param
        try {
            Configuration c = Configuration.getInstance(JAVA_CONFIG,
                                new BadParam());
            throw new SecurityException("test " + testnum++ + " failed");
        } catch (IllegalArgumentException iae) {
            // good
            System.out.println("test " + testnum++ + " passed");
        }

        try {
            Configuration c = Configuration.getInstance(JAVA_CONFIG,
                                new BadParam(),
                                "SUN");
            throw new SecurityException("test " + testnum++ + " failed");
        } catch (IllegalArgumentException iae) {
            // good
            System.out.println("test " + testnum++ + " passed");
        }

        try {
            Configuration c = Configuration.getInstance(JAVA_CONFIG,
                                new BadParam(),
                                Security.getProvider("SUN"));
            throw new SecurityException("test " + testnum++ + " failed");
        } catch (IllegalArgumentException iae) {
            // good
            System.out.println("test " + testnum++ + " passed");
        }

        return testnum;
    }

    private int doCommon(Configuration c, int testnum) throws Exception {

        AppConfigurationEntry[] entries = c.getAppConfigurationEntry("EMPTY");
        if (entries == null) {
            System.out.println("test " + testnum + ".1 passed");
        } else {
            throw new SecurityException("test " + testnum + ".1 failed");
        }

        entries = c.getAppConfigurationEntry("one");
        if (entries.length == 1 &&
            MOD0.equals(entries[0].getLoginModuleName()) &&
            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED ==
                entries[0].getControlFlag()) {
            System.out.println("test " + testnum + ".2 passed");
        } else {
            throw new SecurityException("test " + testnum + ".2 failed");
        }

        entries = c.getAppConfigurationEntry("two");
        if (entries.length == 2 &&
            MOD0.equals(entries[0].getLoginModuleName()) &&
            AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT ==
                entries[0].getControlFlag() &&
            MOD1.equals(entries[1].getLoginModuleName()) &&
            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED ==
                entries[1].getControlFlag()) {
            System.out.println("test " + testnum + ".3 passed");
        } else {
            throw new SecurityException("test " + testnum + ".3 failed");
        }

        entries = c.getAppConfigurationEntry("three");
        if (entries.length == 3 &&
            MOD0.equals(entries[0].getLoginModuleName()) &&
            AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT ==
                entries[0].getControlFlag() &&
            MOD1.equals(entries[1].getLoginModuleName()) &&
            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED ==
                entries[1].getControlFlag() &&
            MOD2.equals(entries[2].getLoginModuleName()) &&
            AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL ==
                entries[2].getControlFlag()) {
            System.out.println("test " + testnum + ".4 passed");
        } else {
            throw new SecurityException("test " + testnum + ".4 failed");
        }

        return testnum;
    }

    private void doCustomTest(Configuration c,
                        int testnum,
                        Provider custom) throws Exception {

        testnum = doCommon(c, testnum);

        // test getProvider
        if (custom == c.getProvider() &&
            "GetInstanceProvider".equals(c.getProvider().getName())) {
            System.out.println("test " + testnum + " (getProvider) passed");
        } else {
            throw new SecurityException
                        ("test " + testnum + " (getProvider) failed");
        }

        // test getType
        if ("GetInstanceConfigSpi".equals(c.getType())) {
            System.out.println("test " + testnum + "(getType) passed");
        } else {
            throw new SecurityException("test " + testnum +
                        " (getType) failed");
        }
    }

    private void doTest(Configuration c, int testnum) throws Exception {
        testnum = doCommon(c, testnum);

        // test getProvider
        if ("SUN".equals(c.getProvider().getName())) {
            System.out.println("test " + testnum + " (getProvider) passed");
        } else {
            throw new SecurityException("test " + testnum +
                        " (getProvider) failed");
        }

        // test getType
        if (JAVA_CONFIG.equals(c.getType())) {
            System.out.println("test " + testnum + " (getType) passed");
        } else {
            throw new SecurityException("test " + testnum +
                        " (getType) failed");
        }
    }

    private void doTestURI(Configuration c,
                        Configuration.Parameters uriParam,
                        int testnum) throws Exception {

        AppConfigurationEntry[] entries = c.getAppConfigurationEntry("four");
        if (entries.length == 4 &&
            MOD0.equals(entries[0].getLoginModuleName()) &&
            AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT ==
                entries[0].getControlFlag() &&
            MOD1.equals(entries[1].getLoginModuleName()) &&
            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED ==
                entries[1].getControlFlag() &&
            MOD2.equals(entries[2].getLoginModuleName()) &&
            AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL ==
                entries[2].getControlFlag() &&
            MOD3.equals(entries[3].getLoginModuleName()) &&
            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED ==
                entries[3].getControlFlag()) {
            System.out.println("test " + testnum + ".1 passed");
        } else {
            throw new SecurityException("test " + testnum + ".1 failed");
        }

        // test getProvider
        if ("SUN".equals(c.getProvider().getName())) {
            System.out.println("test " + testnum + " (getProvider) passed");
        } else {
            throw new SecurityException("test " + testnum +
                        " (getProvider) failed");
        }

        // test getType
        if (JAVA_CONFIG.equals(c.getType())) {
            System.out.println("test " + testnum + " (getType) passed");
        } else {
            throw new SecurityException("test " + testnum +
                        " (getType) failed");
        }

        // test getParameters
        if (uriParam.equals(c.getParameters())) {
            System.out.println("test " + testnum + " (getParameters) passed");
        } else {
            throw new SecurityException("test " + testnum +
                        " (getParameters) failed");
        }
    }
}
