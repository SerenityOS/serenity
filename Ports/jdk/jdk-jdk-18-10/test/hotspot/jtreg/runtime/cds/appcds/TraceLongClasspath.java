/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @summary ensure -Xlog:class+path showing entire expecting app classpath
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @compile test-classes/Super.java
 * @run driver TraceLongClasspath
 */

import java.io.File;
import jdk.test.lib.process.OutputAnalyzer;

public class TraceLongClasspath {

    final static String ps = File.pathSeparator;

    public static void main(String[] args) throws Exception {
        String appJar = JarBuilder.getOrCreateHelloJar();
        String dummyJar = JarBuilder.build("dummy", "Super");

        String longClassPath =
            dummyJar + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/abc/abc/modules/user-patch.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/abc/abc/modules/abc-startup.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/foobar_common/modules/features/com.foobar.db.jdbc7-dms.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/jdk/lib/tools.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/aaserver/server/lib/someapps.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/aaserver/../foobar_common/modules/net.xy.batcontrib_1.1.0.0_1-0b3/lib/bat-contrib.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/aaserver/modules/features/foobar.aas.common.kkkkkkkkkkk.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/abc/abc/modules/foobar.abc.common.adapters_11.1.1/foobar.abc.common.adapters.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/abc/abc/modules/foobar.plane.adapter_12.1.3/foobar.plane.adapter.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/abc/lib/ccccccccar-common.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/foobar_common/communications/modules/config.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/foobar_common/communications/modules/userprefs-config.jar" + ps +
            "/scratch/xxxx/yyyy/XXXXXX/aaaaaaaa/xxxxxxx/xxxxxxxx.us.foobar.com/CommonDomain/config/abc-infra" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/abc/abc/modules/qqqqqq-all-1.6.5.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/abc/abc/modules/foobar.abc.thread_11.1.1/foobar.abc.thread.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/abc/abc/modules/foobar.abc.thread_11.1.1/thread-rrrrrrr-ext-aas.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/abc/abc/modules/foobar.abc.adapter_11.1.1/foobar.abc.adapter.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/abc/abc/modules/foobar.abc.ccc_11.1.1/foobar.abc.ccc.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/bbb/lib/commons-configuration.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/bbb/lib/commons-lang.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/bbb/lib/commons-logging.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/foobar_common/modules/foobar.wccore/foobar-ppppppp-api.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/foobar_common/modules/foobar.ooo_12.1.3/ooo-manifest.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/foobar_common/modules/internal/features/rrr_aaxyxx_foobar.rrr.aas.classpath.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/abc/abc/modules/foobar.abc.thread_11.1.1/rrrrrrrr-api.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/abc/abc/modules/commons-xxx-1.1.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/abc/abc/modules/foobar.abc.mgmt_11.1.1/abc-infra-mgmt.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/foobar_common/eee/archives/eee-eee.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/aaserver/common/march/lib/marchnet.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/aaserver/common/march/lib/marchclient.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/aaserver/common/march/lib/march.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/wwcontent/cde/iii/jlib/iiiloader.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/wwcontent/cde/iii/components/xxxxxxyyzzzzz/classes-xxxxxxyyzzzzz.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/wwcontent/cde/iii/components/mmmmmmm/lib/abc_core.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/wwcontent/cde/iii/components/mmmmmmm/lib/abc_codec.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/wwcontent/cde/iii/components/mmmmmmm/lib/abc_imageio.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/jdk/lib/tools.jar" + ps +
            "/scratch/xxxx/yyyy/ZZZZZZ/aaaaaaaaaa/xx/foobar_common/modules/foobar.ooo_12.1.3/ooo-manifest.jar";

        String dumpCP = longClassPath + ps + appJar;
        // Dump an archive with a specified JAR file in -classpath
        TestCommon.testDump(dumpCP, TestCommon.list("Hello"));

        // Then try to execute the archive with a different classpath and with -Xlog:class+path.
        // The diagnostic "expecting" app classpath trace should show the entire classpath (excluding any non-existent dump-time paths).
        String recordedCP = dummyJar + ps + appJar;
        TestCommon.run(
            "-Xlog:class+path", "-Xlog:cds",
            "-cp", appJar,
            "Hello")
            .assertAbnormalExit(output -> {
                output.shouldContain("Unable to use shared archive");
                output.shouldContain("shared class paths mismatch");
                // the "expecting" app classpath from -Xlog:class+path should not
                // be truncated
                output.shouldContain(recordedCP);
              });
    }
}
