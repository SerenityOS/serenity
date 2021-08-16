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
 * @bug 5087448
 * @summary Verify that property.save saves comments correctly
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

public class SaveComments {

    public static void main(String[] argv) throws IOException {
        String ls = System.getProperty("line.separator");
        String[] input = new String[] {
          "Comments with \u4e2d\u6587\u6c49\u5b57 included",
          "Comments with \n Second comments line",
          "Comments with \n# Second comments line",
          "Comments with \n! Second comments line",
          "Comments with last character is \n",
          "Comments with last character is \r\n",
          "Comments with last two characters are \n\n",
          "Comments with last four characters are \r\n\r\n",
          "Comments with \nkey4=value4",
          "Comments with \n#key4=value4"};

        String[] output = new String[] {
          "#Comments with \\u4E2D\\u6587\\u6C49\\u5B57 included" + ls,
          "#Comments with " + ls + "# Second comments line" + ls,
          "#Comments with " + ls + "# Second comments line" + ls,
          "#Comments with " + ls + "! Second comments line" + ls,
          "#Comments with last character is " + ls+"#"+ls,
          "#Comments with last character is " + ls+"#"+ls,
          "#Comments with last two characters are " + ls+"#"+ls+"#"+ls,
          "#Comments with last four characters are " + ls+"#"+ls+"#"+ls};

        Properties props = new Properties();
        boolean failed = false;
        int i = 0;
        for (i = 0; i < output.length; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(200);
            props.store(baos, input[i]);
            String result = baos.toString("iso8859-1");
            if (result.indexOf(output[i]) == -1) {
                System.out.println("Wrong \"store()\" output:");
                System.out.println(result);
                failed = true;
            }
        }

        props.put("key1", "value1");
        props.put("key2", "value2");
        props.put("key3", "value3");
        for (; i < input.length; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(200);
            props.store(baos, input[i]);
            Properties propsNew = new Properties();
            propsNew.load(new ByteArrayInputStream(baos.toByteArray()));
            /*
            Set<Map.Entry<Object, Object>> kvsetNew = propsNew.entrySet();
            Set<Map.Entry<Object, Object>> kvset = props.entrySet();
            if (!kvsetNew.containsAll(kvset) || !kvset.containsAll(kvsetNew)) {
            */
            if (!props.equals (propsNew)) {
                System.out.println("Wrong output:");
                System.out.println(baos.toString("iso8859-1"));
                failed = true;
            }
        }
        if (failed)
            throw new RuntimeException("Incorrect Properties Comment Output.");
    }
}
