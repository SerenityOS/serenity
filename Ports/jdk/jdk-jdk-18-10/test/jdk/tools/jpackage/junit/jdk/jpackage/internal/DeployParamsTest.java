/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jpackage.internal;

import java.nio.file.Path;
import java.io.IOException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/**
 * Test for JDK-8211285
 */
public class DeployParamsTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws IOException {
        testRoot = tempFolder.newFolder().toPath();
    }

    @Test
    public void testValidAppName() throws PackagerException {
        initParamsAppName();

        setAppNameAndValidate("Test");

        setAppNameAndValidate("Test Name");

        setAppNameAndValidate("Test - Name !!!");
    }

    @Test
    public void testInvalidAppName() throws PackagerException {
        initForInvalidAppNamePackagerException();
        initParamsAppName();
        setAppNameAndValidate("Test\nName");
    }

    @Test
    public void testInvalidAppName2() throws PackagerException {
        initForInvalidAppNamePackagerException();
        initParamsAppName();
        setAppNameAndValidate("Test\rName");
    }

    @Test
    public void testInvalidAppName3() throws PackagerException {
        initForInvalidAppNamePackagerException();
        initParamsAppName();
        setAppNameAndValidate("TestName\\");
    }

    @Test
    public void testInvalidAppName4() throws PackagerException {
        initForInvalidAppNamePackagerException();
        initParamsAppName();
        setAppNameAndValidate("Test \" Name");
    }

    private void initForInvalidAppNamePackagerException() {
        thrown.expect(PackagerException.class);

        String msg = "Error: Invalid Application name";

        // Unfortunately org.hamcrest.core.StringStartsWith is not available
        // with older junit, DIY

        // thrown.expectMessage(startsWith("Error: Invalid Application name"));
        thrown.expectMessage(new BaseMatcher() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(Object o) {
                if (o instanceof String) {
                    return ((String) o).startsWith(msg);
                }
                return false;
            }

            @Override
            public void describeTo(Description d) {
                d.appendText(msg);
            }
        });
    }

    // Returns deploy params initialized to pass all validation, except for
    // app name
    private void initParamsAppName() {
        params = new DeployParams();

        params.setOutput(testRoot);
        params.addBundleArgument(Arguments.CLIOptions.APPCLASS.getId(),
                "TestClass");
        params.addBundleArgument(Arguments.CLIOptions.MAIN_JAR.getId(),
                "test.jar");
        params.addBundleArgument(Arguments.CLIOptions.INPUT.getId(), "input");
    }

    private void setAppNameAndValidate(String appName) throws PackagerException {
        params.addBundleArgument(Arguments.CLIOptions.NAME.getId(), appName);
        params.validate();
    }

    private Path testRoot = null;
    private DeployParams params;
}
