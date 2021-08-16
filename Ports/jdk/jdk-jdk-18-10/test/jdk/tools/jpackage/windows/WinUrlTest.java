/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Annotations.Parameters;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import jdk.jpackage.test.PackageType;

/**
 * Test all possible combinations of --about-url, --win-update-url and
 * --win-help-url parameters.
 */

/*
 * @test
 * @summary jpackage with --about-url, --win-update-url and --win-help-url
 *          parameters
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @build WinUrlTest
 * @requires (os.family == "windows")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @run main/othervm/timeout=360 -Xmx512m  jdk.jpackage.test.Main
 *  --jpt-run=WinUrlTest
 */
public class WinUrlTest {

    static enum URL {
        About("--about-url"),
        Update("--win-update-url"),
        Help("--win-help-url");

        URL(String cliOption) {
            this.cliOption = cliOption;
        }

        final String cliOption;
    }

    public WinUrlTest(Boolean withAboutURL, Boolean withUpdateURL,
            Boolean withHelpURL) {
        urls = Stream.of(
                withAboutURL ? URL.About : null,
                withUpdateURL ? URL.Update : null,
                withHelpURL ? URL.Help : null
        ).filter(Objects::nonNull).toList();
    }

    @Parameters
    public static List<Object[]> data() {
        List<Object[]> data = new ArrayList<>();
        for (var withAboutURL : List.of(Boolean.TRUE, Boolean.FALSE)) {
            for (var withUpdateURL : List.of(Boolean.TRUE, Boolean.FALSE)) {
                for (var withHelpURL : List.of(Boolean.TRUE, Boolean.FALSE)) {
                    var args = new Object[]{withAboutURL, withUpdateURL, withHelpURL};
                    if (Stream.of(args).anyMatch(Boolean.TRUE::equals)) {
                        data.add(args);
                    }
                }
            }
        }

        return data;
    }

    @Test
    public void test() {
        PackageTest test = new PackageTest()
                .forTypes(PackageType.WINDOWS)
                .configureHelloApp();

        test.addInitializer(JPackageCommand::setFakeRuntime);
        test.addInitializer(this::setPackageName);

        urls.forEach(url -> {
            test.addInitializer(cmd -> cmd.addArguments(url.cliOption,
                    "http://localhost/" + url.name().toLowerCase()));
        });

        test.run();
    }

    private void setPackageName(JPackageCommand cmd) {
        StringBuilder sb = new StringBuilder(cmd.name());
        sb.append("With");
        urls.forEach(url -> sb.append(url.name()));
        cmd.setArgumentValue("--name", sb.toString());
    }

    private final List<URL> urls;
}
