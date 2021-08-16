/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8242258
 * @summary (jrtfs) Path::toUri throws AssertionError for malformed input
 * @run testng UriTests
 */

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.net.URI;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

public class UriTests {
    private FileSystem theFileSystem;

    @BeforeClass
    public void setup() {
       theFileSystem = FileSystems.getFileSystem(URI.create("jrt:/"));
    }

    @DataProvider(name = "problemStrings")
    private Object[][] problemStrings() {
        return new Object[][] {
            { "[", "jrt:/%5B" },
            { "]", "jrt:/%5D" },
            { "{", "jrt:/%7B" },
            { "}", "jrt:/%7D" },
            { "`", "jrt:/%60" },
            { "%", "jrt:/%25" },
            { " xyz", "jrt:/%20xyz" },
            { "xyz ", "jrt:/xyz%20" },
            { "xy z", "jrt:/xy%20z" },
        };
    }

    @Test(dataProvider = "problemStrings")
    public void testPathToURI(String pathSuffix, String uriStr) {
        URI uri = theFileSystem.getPath("/modules/" + pathSuffix).toUri();
        assertEquals(uri.toString(), uriStr);
    }

    @Test(dataProvider = "problemStrings")
    public void testURIToPath(String pathSuffix, String uriStr) {
        Path path = theFileSystem.provider().getPath(URI.create(uriStr));
        assertEquals(path.toString(), "/modules/" + pathSuffix);
    }
}
