/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8232357 8221801 8244674
 * @summary Make sure version info is consistent between code and license
 * @run testng Versions
 */

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Versions {

    Path rootPath = Path.of(System.getProperty("test.root"), "../..");
    Path legalPath = Path.of(System.getProperty("test.jdk"), "legal");

    @DataProvider(name = "data")
    public Object[][] createData() {
        return new Object[][]{
                {"src/java.xml.crypto/share/classes/org/jcp/xml/dsig/internal/dom/XMLDSigRI.java",
                        Pattern.compile("// Apache Santuario XML Security for Java, version (?<n>\\S+)"),
                        "src/java.xml.crypto/share/legal/santuario.md",
                        Pattern.compile("## Apache Santuario v(?<n>\\S+)"),
                        "java.xml.crypto/santuario.md"},
                {"make/data/publicsuffixlist/VERSION",
                        Pattern.compile("list/(?<n>[0-9a-f]+)/public_suffix_list.dat"),
                        "src/java.base/share/legal/public_suffix.md",
                        Pattern.compile("list/(?<n>[0-9a-f]+)/public_suffix_list.dat"),
                        "java.base/public_suffix.md"},
                {"src/java.smartcardio/unix/native/libj2pcsc/MUSCLE/pcsclite.h",
                        Pattern.compile("#define PCSCLITE_VERSION_NUMBER +\"(?<n>[0-9\\.]+)\""),
                        "src/java.smartcardio/unix/legal/pcsclite.md",
                        Pattern.compile("## PC/SC Lite v(?<n>[0-9\\.]+)"),
                        "java.smartcardio/pcsclite.md"}
        };
    }

    @Test(dataProvider = "data")
    public void Test(String src, Pattern sp, String legal, Pattern lp,
                     String legalInBuild) throws IOException {

        Path pSrc = rootPath.resolve(src);
        Path pLegal = rootPath.resolve(legal);

        Assert.assertEquals(fetch(pSrc, sp), fetch(pLegal, lp));

        Path pLegalInBuild = legalPath.resolve(legalInBuild);
        if (!Files.exists(pLegalInBuild)) {
            System.out.println("Not an image build, or file platform dependent");
        } else {
            Assert.assertEquals(Files.mismatch(pLegal, pLegalInBuild), -1);
        }
    }

    // Find a match in path and return the extracted named group
    static String fetch(Path path, Pattern pattern)
            throws IOException  {
        return Files.lines(path)
                .map(pattern::matcher)
                .filter(Matcher::find)
                .findFirst()
                .map(m -> m.group("n"))
                .get();
    }
}
