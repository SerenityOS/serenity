/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6802846 8172529 8227758 8260960
 * @summary jarsigner needs enhanced cert validation(options)
 * @library /test/lib
 * @run main/timeout=240 ConciseJarsigner
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.List;

public class ConciseJarsigner {

    static OutputAnalyzer kt(String cmd) throws Exception {
        // Choose 2048-bit RSA to make sure it runs fine and fast. In
        // fact, every keyalg/keysize combination is OK for this test.
        return SecurityTools.keytool("-storepass changeit -keypass changeit "
                + "-keystore ks -keyalg rsa -keysize 2048 " + cmd);
    }

    static void gencert(String owner, String cmd) throws Exception {
        kt("-certreq -alias " + owner + " -file tmp.req");
        kt("-gencert -infile tmp.req -outfile tmp.cert " + cmd);
        kt("-import -alias " + owner + " -file tmp.cert");
    }

    static OutputAnalyzer js(String cmd) throws Exception {
        return SecurityTools.jarsigner("-debug " + cmd);
    }

    public static void main(String[] args) throws Exception {

        Files.write(Path.of("A1"), List.of("a1"));
        Files.write(Path.of("A2"), List.of("a2"));
        Files.write(Path.of("A3"), List.of("a3"));
        Files.write(Path.of("A4"), List.of("a4"));
        Files.write(Path.of("A5"), List.of("a5"));
        Files.write(Path.of("A6"), List.of("a6"));

        String year = "" + Calendar.getInstance().get(Calendar.YEAR);

        // ==========================================================
        // First part: output format
        // ==========================================================

        kt("-genkeypair -alias a1 -dname CN=a1 -validity 366");
        kt("-genkeypair -alias a2 -dname CN=a2 -validity 366");

        // a.jar includes 8 unsigned, 2 signed by a1 and a2, 2 signed by a3
        SecurityTools.jar("cvf a.jar A1 A2");
        js("-keystore ks -storepass changeit a.jar a1");
        SecurityTools.jar("uvf a.jar A3 A4");
        js("-keystore ks -storepass changeit a.jar a2");
        SecurityTools.jar("uvf a.jar A5 A6");

        // Verify OK
        js("-verify a.jar").shouldHaveExitValue(0);

        // 4(chainNotValidated)+16(hasUnsignedEntry)
        js("-verify a.jar -strict").shouldHaveExitValue(20);

        // 16(hasUnsignedEntry)
        js("-verify a.jar -strict -keystore ks -storepass changeit")
                .shouldHaveExitValue(16);

        // 16(hasUnsignedEntry)+32(notSignedByAlias)
        js("-verify a.jar a1 -strict -keystore ks -storepass changeit")
                .shouldHaveExitValue(48);

        // 16(hasUnsignedEntry)
        js("-verify a.jar a1 a2 -strict -keystore ks -storepass changeit")
                .shouldHaveExitValue(16);

        // 12 entries all together
        Asserts.assertTrue(js("-verify a.jar -verbose")
                .asLines().stream()
                .filter(s -> s.contains(year))
                .count() == 12);

        // 12 entries all listed
        Asserts.assertTrue(js("-verify a.jar -verbose:grouped")
                .asLines().stream()
                .filter(s -> s.contains(year))
                .count() == 12);

        // 5 groups: MANIFEST, signature related entries, directory entries,
        // signed entries, and unsigned entries.
        Asserts.assertTrue(js("-verify a.jar -verbose:summary")
                .asLines().stream()
                .filter(s -> s.contains(year))
                .count() == 5);

        // still 5 groups, but MANIFEST group and directiry entry group
        // have no other file
        Asserts.assertTrue(js("-verify a.jar -verbose:summary")
                .asLines().stream()
                .filter(s -> s.contains("more)"))
                .count() == 3);

        // 6 groups: MANIFEST, signature related entries, directory entries,
        // signed entries by a1/a2, signed entries by a2, and unsigned entries.
        Asserts.assertTrue(js("-verify a.jar -verbose:summary -certs")
                .asLines().stream()
                .filter(s -> s.contains(year))
                .count() == 6);

        // 2 for MANIFEST, 2*2 for A1/A2, 2 for A3/A4
        Asserts.assertTrue(js("-verify a.jar -verbose -certs")
                .asLines().stream()
                .filter(s -> s.contains("[certificate"))
                .count() == 8);

        // a1,a2 for MANIFEST, a1,a2 for A1/A2, a2 for A3/A4
        Asserts.assertTrue(js("-verify a.jar -verbose:grouped -certs")
                .asLines().stream()
                .filter(s -> s.contains("[certificate"))
                .count() == 5);

        // a1,a2 for MANIFEST, a1,a2 for A1/A2, a2 for A3/A4
        Asserts.assertTrue(js("-verify a.jar -verbose:summary -certs")
                .asLines().stream()
                .filter(s -> s.contains("[certificate"))
                .count() == 5);

        // still 6 groups, but MANIFEST group and directory entry group
        // have no other file
        Asserts.assertTrue(js("-verify a.jar -verbose:summary -certs")
                .asLines().stream()
                .filter(s -> s.contains("more)"))
                .count() == 4);

        // ==========================================================
        // Second part: exit code 2, 4, 8.
        // 16 and 32 already covered in the first part
        // ==========================================================

        kt("-genkeypair -alias ca -dname CN=ca -ext bc -validity 365");
        kt("-genkeypair -alias expired -dname CN=expired");
        gencert("expired", "-alias ca -startdate -10m");
        kt("-genkeypair -alias notyetvalid -dname CN=notyetvalid");
        gencert("notyetvalid", "-alias ca -startdate +1m");
        kt("-genkeypair -alias badku -dname CN=badku");
        gencert("badku", "-alias ca -ext KU=cRLSign -validity 365");
        kt("-genkeypair -alias badeku -dname CN=badeku");
        gencert("badeku", "-alias ca -ext EKU=sa -validity 365");
        kt("-genkeypair -alias goodku -dname CN=goodku");
        gencert("goodku", "-alias ca -ext KU=dig -validity 365");
        kt("-genkeypair -alias goodeku -dname CN=goodeku");
        gencert("goodeku", "-alias ca -ext EKU=codesign -validity 365");

        js("-strict -keystore ks -storepass changeit a.jar expired")
                .shouldHaveExitValue(4);

        js("-strict -keystore ks -storepass changeit a.jar notyetvalid")
                .shouldHaveExitValue(4);

        js("-strict -keystore ks -storepass changeit a.jar badku")
                .shouldHaveExitValue(8);

        js("-strict -keystore ks -storepass changeit a.jar badeku")
                .shouldHaveExitValue(8);

        js("-strict -keystore ks -storepass changeit a.jar goodku")
                .shouldHaveExitValue(0);

        js("-strict -keystore ks -storepass changeit a.jar goodeku")
                .shouldHaveExitValue(0);

        // badchain signed by ca1, but ca1 is removed later
        kt("-genkeypair -alias badchain -dname CN=badchain -validity 365");
        kt("-genkeypair -alias ca1 -dname CN=ca1 -ext bc -validity 365");
        gencert("badchain", "-alias ca1 -validity 365");

        // save ca1.cert for easy replay
        kt("-exportcert -file ca1.cert -alias ca1");
        kt("-delete -alias ca1");

        js("-strict -keystore ks -storepass changeit a.jar badchain")
                .shouldHaveExitValue(4);

        js("-verify a.jar").shouldHaveExitValue(0);

        // ==========================================================
        // Third part: -certchain test
        // ==========================================================

        // altchain signed by ca2
        kt("-genkeypair -alias altchain -dname CN=altchain -validity 365");
        kt("-genkeypair -alias ca2 -dname CN=ca2 -ext bc -validity 365");
        kt("-certreq -alias altchain -file altchain.req");
        Files.write(Path.of("certchain"), List.of(
                kt("-gencert -alias ca2 -validity 365 -rfc -infile altchain.req")
                        .getOutput(),
                kt("-exportcert -alias ca2 -rfc").getOutput()));

        // Self-signed cert does not work
        js("-strict -keystore ks -storepass changeit a.jar altchain")
                .shouldHaveExitValue(4);

        // -certchain works
        js("-strict -keystore ks -storepass changeit -certchain certchain "
                + "a.jar altchain")
                .shouldHaveExitValue(0);

        // if ca2 is removed and cert is imported, -certchain won't work
        // because this certificate entry is not trusted
        // save ca2.cert for easy replay
        kt("-exportcert -file ca2.cert -alias ca2");
        kt("-delete -alias ca2");
        kt("-importcert -file certchain -alias altchain -noprompt");
        js("-strict -keystore ks -storepass changeit "
                + "-certchain certchain a.jar altchain")
                .shouldHaveExitValue(4);

        js("-verify a.jar").shouldHaveExitValue(0);

        // ==========================================================
        // 8172529
        // ==========================================================

        kt("-genkeypair -alias ee -dname CN=ee");
        kt("-genkeypair -alias caone -dname CN=caone -ext bc:c");
        kt("-genkeypair -alias catwo -dname CN=catwo -ext bc:c");

        kt("-certreq -alias ee -file ee.req");
        kt("-certreq -alias catwo -file catwo.req");

        // This certchain contains a cross-signed weak catwo.cert
        Files.write(Path.of("ee2"), List.of(
                kt("-gencert -alias catwo -rfc -infile ee.req").getOutput(),
                kt("-gencert -alias caone -sigalg MD5withRSA -rfc "
                        + "-infile catwo.req").getOutput()));

        kt("-importcert -alias ee -file ee2");

        SecurityTools.jar("cvf a.jar A1");
        js("-strict -keystore ks -storepass changeit a.jar ee")
                .shouldHaveExitValue(0);
        js("-strict -keystore ks -storepass changeit -verify a.jar")
                .shouldHaveExitValue(0);
    }
}
