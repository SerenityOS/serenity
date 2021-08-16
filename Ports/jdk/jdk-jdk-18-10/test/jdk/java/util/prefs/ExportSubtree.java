/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.prefs.Preferences;

/**
 * @test
 * @bug 6203576 4700020 7197662 8217777
 * @summary checks if the output of exportSubtree() is identical to
 *          the output from previous release.
 * @run main/othervm -Djava.util.prefs.userRoot=. ExportSubtree
 */
public class ExportSubtree {
    private static final String LS = System.getProperty("line.separator");

    private static final String IMPORT_PREFS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<!DOCTYPE preferences SYSTEM \"http://java.sun.com/dtd/preferences.dtd\">" +
        "<preferences EXTERNAL_XML_VERSION=\"1.0\">" +
        "  <root type=\"user\">" +
        "    <map>" +
        "      <entry key=\"key1\" value=\"value1\"/>" +
        "    </map>" +
        "    <node name=\"testExportSubtree\">" +
        "      <map>" +
        "        <entry key=\"key2\" value=\"value2\"/>" +
        "      </map>" +
        "      <node name=\"test\">" +
        "        <map>" +
        "          <entry key=\"key3\" value=\"value3\"/>" +
        "        </map>" +
        "      </node>" +
        "    </node>" +
        "  </root>" +
        "</preferences>";

    private static final String EXPECTED_RESULT =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + LS +
        "<!DOCTYPE preferences SYSTEM \"http://java.sun.com/dtd/preferences.dtd\">" + LS +
        "<preferences EXTERNAL_XML_VERSION=\"1.0\">" + LS +
        "  <root type=\"user\">" + LS +
        "    <map/>" + LS +
        "    <node name=\"testExportSubtree\">" + LS +
        "      <map>" + LS +
        "        <entry key=\"key2\" value=\"value2\"/>" + LS +
        "      </map>" + LS +
        "      <node name=\"test\">" + LS +
        "        <map>" + LS +
        "          <entry key=\"key3\" value=\"value3\"/>" + LS +
        "        </map>" + LS +
        "      </node>" + LS +
        "    </node>" + LS +
        "  </root>" + LS +
        "</preferences>" + LS;

    public static void main(String[] args) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(IMPORT_PREFS.getBytes("utf-8"));
        Preferences.importPreferences(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Preferences.userRoot().node("testExportSubtree").exportSubtree(baos);
        Preferences.userRoot().node("testExportSubtree").removeNode();
        if (!EXPECTED_RESULT.equals(baos.toString())) {
            String errMsg = "Preferences::exportSubtree did not yield the expected result.";
            System.out.println(errMsg + LS +
                               "Actual:" + LS +
                               baos + LS +
                               "Expected:" + LS +
                               EXPECTED_RESULT);
            throw new RuntimeException(errMsg);
        }
    }
}
