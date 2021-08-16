/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verifies security checks are performed before existence checks
 *          in pre-defined body processors APIs
 * @run testng/othervm SecurityBeforeFile
 * @run testng/othervm/java.security.policy=nopermissions.policy SecurityBeforeFile
 */

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static java.nio.file.StandardOpenOption.*;
import static org.testng.Assert.*;

public class SecurityBeforeFile {

    static final boolean hasSecurityManager = System.getSecurityManager() != null;
    static final boolean hasNoSecurityManager = !hasSecurityManager;

    @Test
    public void BodyPublishersOfFile() {
        Path p = Paths.get("doesNotExist.txt");
        if (hasNoSecurityManager && Files.exists(p))
            throw new AssertionError("Unexpected " + p);

        try {
            BodyPublishers.ofFile(p);
            fail("UNEXPECTED, file " + p.toString() + " exists?");
        } catch (SecurityException se) {
            assertTrue(hasSecurityManager);
            out.println("caught expected security exception: " + se);
        } catch (FileNotFoundException fnfe) {
            assertTrue(hasNoSecurityManager);
            out.println("caught expected file not found exception: " + fnfe);
        }
    }

    @DataProvider(name = "handlerOpenOptions")
    public Object[][] handlerOpenOptions() {
        return new Object[][] {
                { new OpenOption[] {               } },
                { new OpenOption[] { CREATE        } },
                { new OpenOption[] { CREATE, WRITE } },
        };
    }

    @Test(dataProvider = "handlerOpenOptions")
    public void BodyHandlersOfFileDownload(OpenOption[] openOptions) {
        Path p = Paths.get("doesNotExistDir");
        if (hasNoSecurityManager && Files.exists(p))
            throw new AssertionError("Unexpected " + p);

        try {
            BodyHandlers.ofFileDownload(p, openOptions);
            fail("UNEXPECTED, file " + p.toString() + " exists?");
        } catch (SecurityException se) {
            assertTrue(hasSecurityManager);
            out.println("caught expected security exception: " + se);
        } catch (IllegalArgumentException iae) {
            assertTrue(hasNoSecurityManager);
            out.println("caught expected illegal argument exception: " + iae);
        }
    }
}
