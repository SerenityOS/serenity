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
 * @bug 4219644
 * @summary Escaping of spaces required only for leading spaces in the value
 *          part of the property.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class EscapeSpace {

    static String props =
        "key1=\\ \\ Value1, has leading and trailing spaces\\  \n" +
        "key2=Value2,\\ does not have\\ leading or trailing\\ spaces\n" +
        "key3=Value3,has,no,spaces\n" +
        "key4=Value4, does not have leading spaces\\  \n" +
        "key5=\\t\\ \\ Value5, has leading tab and no trailing spaces\n" +
        "key6=\\ \\ Value6,doesnothaveembeddedspaces\\ \\ \n" +
        "\\ key1\\ test\\ =key1, has leading and trailing spaces  \n" +
        "key2\\ test=key2, does not have leading or trailing spaces\n" +
        "key3test=key3,has,no,spaces\n" +
        "key4\\ test\\ =key4, does not have leading spaces  \n" +
        "\\t\\ key5\\ test=key5, has leading tab and no trailing spaces\n" +
        "\\ \\ key6\\ \\ =\\  key6,doesnothaveembeddedspaces  ";
    static void load(Properties p, String file) throws Exception
    {
        FileInputStream     fis = null;
        BufferedInputStream bis = null;

        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream( fis );

            p.load(bis);
        }
        catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            if (fis != null)
                fis.close();
        }
    }

    static void store(Properties p, String file) throws Exception
    {

        FileOutputStream     fos = null;
        BufferedOutputStream bos = null;

        try {
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream( fos );

            p.store( bos, "Omitting escape characters for non leading space \" \" in properties");
        }
        catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            if (fos != null)
                fos.close();
        }
    }

    public static void main( String args[] ) throws Exception
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(props.getBytes());
        Properties props0 = new Properties();
        // Load properties with escape character '\' before space characters
        try {
            props0.load(bais);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        Properties props1 = new Properties();
        /*
         * Put the same properties, but without the escape char for space in
         * value part.
         */
        props1.put( "key1", "  Value1, has leading and trailing spaces  " );
        props1.put( "key2",
            "Value2, does not have leading or trailing spaces" );
        props1.put( "key3", "Value3,has,no,spaces" );
        props1.put( "key4", "Value4, does not have leading spaces  " );
        props1.put( "key5",
            "\t  Value5, has leading tab and no trailing spaces" );
        props1.put( "key6", "  Value6,doesnothaveembeddedspaces  " );
        props1.put( " key1 test ", "key1, has leading and trailing spaces  " );
        props1.put( "key2 test",
            "key2, does not have leading or trailing spaces" );
        props1.put( "key3test", "key3,has,no,spaces" );
        props1.put( "key4 test ", "key4, does not have leading spaces  " );
        props1.put( "\t key5 test",
            "key5, has leading tab and no trailing spaces" );
        props1.put( "  key6  ", "  key6,doesnothaveembeddedspaces  " );

        // Check if both the properties match
        if (!props0.equals(props1))
            throw new RuntimeException("Test failed");


        // Also store the new properties to a file
        store(props1, "out1.props");

        Properties props2 = new Properties();
        // Reread the properties from the file
        load(props2, "out1.props");

        // Make sure that the properties match
        if (!props1.equals(props2))
            throw new RuntimeException("Test failed");
    }
}
