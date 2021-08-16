/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6204853
 * @summary tests PropertyResourceBundle(Reader) constructor.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PropertyResourceBundle;

public final class Bug6204853 {

    public Bug6204853() {
        String srcDir = System.getProperty("test.src", ".");
        try (FileInputStream fis8859_1 =
                 new FileInputStream(new File(srcDir, "Bug6204853.properties"));
             FileInputStream fisUtf8 =
                 new FileInputStream(new File(srcDir, "Bug6204853_Utf8.properties"));
             InputStreamReader isrUtf8 = new InputStreamReader(fisUtf8, "UTF-8"))
        {
            PropertyResourceBundle bundleUtf8 = new PropertyResourceBundle(isrUtf8);
            PropertyResourceBundle bundle = new PropertyResourceBundle(fis8859_1);

            String[] arrayUtf8 = createKeyValueArray(bundleUtf8);
            String[] array = createKeyValueArray(bundle);

            if (!Arrays.equals(arrayUtf8, array)) {
                throw new RuntimeException("PropertyResourceBundle constructed from a UTF-8 encoded property file is not equal to the one constructed from ISO-8859-1 encoded property file.");
            }
        } catch (FileNotFoundException fnfe) {
            throw new RuntimeException(fnfe);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private final String[] createKeyValueArray(PropertyResourceBundle b) {
        List<String> keyValueList = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();

        for (String key : b.keySet()) {
            sb.setLength(0);
            sb.append(key);
            sb.append(" = ");
            sb.append(b.getString(key));
            keyValueList.add(sb.toString());
        }

        String[] keyValueArray = keyValueList.toArray(new String[0]);
        Arrays.sort(keyValueArray);
        return keyValueArray;
    }

    public static final void main(String[] args) {
        new Bug6204853();
    }
}
