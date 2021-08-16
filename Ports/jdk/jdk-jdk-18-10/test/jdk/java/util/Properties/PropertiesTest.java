/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary tests the load and store methods of Properties class
 * @author Xueming Shen
 * @bug 4094886 8224202
 * @modules jdk.charsets
 * @key randomness
 */

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;
import java.util.Random;

public class PropertiesTest {

    private static boolean failure = false;
    private static int failCount = 0;

    /**
     * Main to interpret arguments and run several tests.
     *
     */
    public static void main(String[] args) throws Exception {
        BlankLines();
        EscapeSpace();
        LoadParsing();
        SaveEncoding();
        SaveLoadBasher();
        SaveSeparator();
        SaveClose();
        SaveComments();
        UnicodeEscape();

        if (failure)
            throw new RuntimeException("Failure in Properties testing.");
        else
            System.err.println("OKAY: All tests passed.");
    }

    private static void report(String testName) {
        int spacesToAdd = 30 - testName.length();
        StringBuffer paddedNameBuffer = new StringBuffer(testName);
        for (int i=0; i<spacesToAdd; i++)
            paddedNameBuffer.append(" ");
        String paddedName = paddedNameBuffer.toString();
        System.err.println(paddedName + ": " +
                           (failCount==0 ? "Passed":"Failed("+failCount+")"));
        if (failCount > 0)
            failure = true;
        failCount = 0;
    }

    private static void check(Properties prop1, Properties prop2) {
        if (!prop1.equals(prop2))
            failCount++;
    }

    private static Reader getReader(String src, String csn)
        throws Exception {
        return new InputStreamReader(
                   new ByteArrayInputStream(src.getBytes()),
                   csn);
    }

    private static OutputStream getFOS(String name)
        throws Exception
    {
        return new FileOutputStream(name);
    }

    private static Writer getFOSW(String name, String csn)
        throws Exception
    {
        return new OutputStreamWriter(
                   new FileOutputStream(name),
                   csn);
    }

    private static Reader getReader(byte[] src, String csn)
        throws Exception {
        return new InputStreamReader(new ByteArrayInputStream(src), csn);
    }

    private static InputStream getInputStream(String src) {
        return new ByteArrayInputStream(src.getBytes());
    }

    private static InputStream getInputStream(byte[] src) {
        return new ByteArrayInputStream(src);
    }

    private static void BlankLines() throws Exception {
        // write a single space to the output stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(' ');
        // test empty properties
        Properties prop1 = new Properties();

        // now load the file we just created, into a properties object.
        // the properties object should have no elements, but due to a
        // bug, it has an empty key/value. key = "", value = ""
        Properties prop2 = new Properties();
        prop2.load(getInputStream(baos.toByteArray()));
        check(prop1, prop2);

        // test reader
        prop2 = new Properties();
        prop2.load(getReader(baos.toByteArray(), "UTF-8"));
        check(prop1, prop2);

        report("BlinkLine");
    }

    private static void EscapeSpace() throws Exception {
        String propsCases =
            "key1=\\ \\ Value\\u4e001, has leading and trailing spaces\\  \n" +
            "key2=Value\\u4e002,\\ does not have\\ leading or trailing\\ spaces\n" +
            "key3=Value\\u4e003,has,no,spaces\n" +
            "key4=Value\\u4e004, does not have leading spaces\\  \n" +
            "key5=\\t\\ \\ Value\\u4e005, has leading tab and no trailing spaces\n" +
            "key6=\\ \\ Value\\u4e006,doesnothaveembeddedspaces\\ \\ \n" +
            "\\ key1\\ test\\ =key1, has leading and trailing spaces  \n" +
            "key2\\ test=key2, does not have leading or trailing spaces\n" +
            "key3test=key3,has,no,spaces\n" +
            "key4\\ test\\ =key4, does not have leading spaces  \n" +
            "\\t\\ key5\\ test=key5, has leading tab and no trailing spaces\n" +
            "\\ \\ key6\\ \\ =\\  key6,doesnothaveembeddedspaces  ";

        // Put the same properties, but without the escape char for space in
        // value part.
        Properties props = new Properties();
        props.put("key1", "  Value\u4e001, has leading and trailing spaces  ");
        props.put("key2", "Value\u4e002, does not have leading or trailing spaces");
        props.put("key3", "Value\u4e003,has,no,spaces");
        props.put("key4", "Value\u4e004, does not have leading spaces  ");
        props.put("key5", "\t  Value\u4e005, has leading tab and no trailing spaces");
        props.put("key6", "  Value\u4e006,doesnothaveembeddedspaces  ");
        props.put(" key1 test ", "key1, has leading and trailing spaces  ");
        props.put("key2 test", "key2, does not have leading or trailing spaces");
        props.put("key3test", "key3,has,no,spaces");
        props.put("key4 test ", "key4, does not have leading spaces  ");
        props.put("\t key5 test", "key5, has leading tab and no trailing spaces");
        props.put("  key6  ", "  key6,doesnothaveembeddedspaces  ");

        // Load properties with escape character '\' before space characters
        Properties props1 = new Properties();
        props1.load(getInputStream(propsCases));
        check(props1, props);

        // Test Reader
        props1 = new Properties();
        props1.load(getReader(propsCases, "UTF-8"));
        check(props1, props);

        // Also store the new properties to a storage
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.store(baos, "EscapeSpace Test");

        props1 = new Properties();
        props1.load(getInputStream(baos.toByteArray()));
        check(props1, props);

        // Reader should work as well
        props1 = new Properties();
        props1.load(getReader(baos.toByteArray(), "UTF-8"));
        check(props1, props);

        // Try Writer
        baos = new ByteArrayOutputStream();
        props.store(new OutputStreamWriter(baos, "UTF-8"),
                     "EscapeSpace Test");

        props1 = new Properties();
        props1.load(getReader(baos.toByteArray(), "UTF-8"));
        check(props1, props);

        report("EscapeSpace");
    }

    private static void LoadParsing() throws Exception {
        File f = new File(System.getProperty("test.src", "."), "input.txt");
        FileInputStream myIn = new FileInputStream(f);
        Properties myProps = new Properties();
        int size = 0;
        try {
            myProps.load(myIn);
        } finally {
            myIn.close();
        }

        if (!myProps.getProperty("key1").equals("value1")      || // comment
            !myProps.getProperty("key2").equals("abc\\defg\\") || // slash
            !myProps.getProperty("key3").equals("value3")      || // spaces in key
            !myProps.getProperty("key4").equals(":value4")     || // separator
            // Does not recognize comments with leading spaces
            (myProps.getProperty("#") != null)                 ||
            // Wrong number of keys in Properties
            ((size=myProps.size()) != 4))
            failCount++;
        report("LoadParsing");
    }

    private static void SaveEncoding() throws Exception {
        // Create a properties object to save
        Properties props = new Properties();
        props.put("signal", "val\u0019");
        props.put("ABC 10", "value0");
        props.put("\uff10test", "value\u0020");
        props.put("key with spaces", "value with spaces");
        props.put("key with space and Chinese_\u4e00", "valueWithChinese_\u4e00");
        props.put(" special#=key ", "value3");

        // Save the properties and check output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.store(baos,"A test");

        // Read properties file and verify \u0019
        BufferedReader in = new BufferedReader(
                                new InputStreamReader(
                                    new ByteArrayInputStream(
                                        baos.toByteArray())));
        String firstLine = "foo";
        while (!firstLine.startsWith("signal"))
            firstLine = in.readLine();
        if (firstLine.length() != 16)
            failCount++;

        // Load the properties set
        Properties newProps = new Properties();
        newProps.load(getInputStream(baos.toByteArray()));
        check(newProps, props);

        // Store(Writer)/Load(Reader)
        baos = new ByteArrayOutputStream();
        props.store(new OutputStreamWriter(baos, "EUC_JP"), "A test");
        newProps = new Properties();
        newProps.load(getReader(baos.toByteArray(), "EUC_JP"));
        check(newProps, props);

        report("SaveEncoding");
    }

   /**
    * This class tests to see if a properties object
    * can successfully save and load properties
    * using character encoding
    */
    private static void SaveLoadBasher() throws Exception {
        String keyValueSeparators = "=: \t\r\n\f#!\\";

        Properties originalProps = new Properties();
        Properties loadedProps = new Properties();

        // Generate a unicode key and value
        Random generator = new Random();
        int achar=0;
        StringBuffer aKeyBuffer = new StringBuffer(120);
        StringBuffer aValueBuffer = new StringBuffer(120);
        String aKey;
        String aValue;
        int maxKeyLen = 100;
        int maxValueLen = 100;
        int maxPropsNum = 300;
        for (int x=0; x<maxPropsNum; x++) {
            for(int y=0; y<maxKeyLen; y++) {
                achar = generator.nextInt();
                char test;
                if(achar < 99999) {
                    test = (char)(achar);
                }
                else {
                    int zz = achar % 10;
                    test = keyValueSeparators.charAt(zz);
                }
                if (Character.isHighSurrogate(test)) {
                    aKeyBuffer.append(test);
                    aKeyBuffer.append('\udc00');
                    y++;
                } else if (Character.isLowSurrogate(test)) {
                    aKeyBuffer.append('\ud800');
                    aKeyBuffer.append(test);
                    y++;
                } else
                    aKeyBuffer.append(test);
            }
            aKey = aKeyBuffer.toString();
            for(int y=0; y<maxValueLen; y++) {
                achar = generator.nextInt();
                char test = (char)(achar);
                if (Character.isHighSurrogate(test)) {
                    aKeyBuffer.append(test);
                    aKeyBuffer.append('\udc00');
                    y++;
                } else if (Character.isLowSurrogate(test)) {
                    aKeyBuffer.append('\ud800');
                    aKeyBuffer.append(test);
                    y++;
                } else {
                    aValueBuffer.append(test);
                }
            }
            aValue = aValueBuffer.toString();

            // Attempt to add to original
            try {
                originalProps.put(aKey, aValue);
            }
            catch (IllegalArgumentException e) {
                System.err.println("disallowing...");
            }
            aKeyBuffer.setLength(0);
            aValueBuffer.setLength(0);
        }

        // Store(OutputStream)/Load(InputStream)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        originalProps.store(baos, "test properties");
        loadedProps.load(getInputStream(baos.toByteArray()));
        check(loadedProps, originalProps);

        // Store(Writer)/Load(Reader)
        baos = new ByteArrayOutputStream();
        originalProps.store(new OutputStreamWriter(baos, "UTF-8"),
                            "test properties");
        loadedProps.load(getReader(baos.toByteArray(), "UTF-8"));
        check(loadedProps, originalProps);

        report("SaveLoadBasher");
    }


    /* Note: this regression test only detects incorrect line
     * separator on platform running the test
     */
    private static void SaveSeparator() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Properties props = new Properties();
        props.store(baos, "A test");

        // Examine the result to verify that line.separator was used
        String theSeparator = System.getProperty("line.separator");
        String content = baos.toString();
        if (!content.endsWith(theSeparator))
            failCount++;

        // store(Writer)
        baos = new ByteArrayOutputStream();
        props.store(new OutputStreamWriter(baos, "UTF-8"), "A test");
        content = baos.toString();
        if (!content.endsWith(theSeparator))
            failCount++;

        report("SaveSeparator");
    }

    // Ensure that the save method doesn't close its output stream
    private static void SaveClose() throws Exception {
        Properties p = new Properties();
        p.put("Foo", "Bar");
        class MyOS extends ByteArrayOutputStream {
            boolean closed = false;
            public void close() throws IOException {
                this.closed = true;
            }
        }
        MyOS myos = new MyOS();
        p.store(myos, "Test");
        if (myos.closed)
            failCount++;

        p.store(new OutputStreamWriter(myos, "UTF-8"), "Test");
        if (myos.closed)
            failCount++;

        report ("SaveClose");
    }

    private static void UnicodeEscape() throws Exception {
        checkMalformedUnicodeEscape("b=\\u012\n");
        checkMalformedUnicodeEscape("b=\\u01\n");
        checkMalformedUnicodeEscape("b=\\u0\n");
        checkMalformedUnicodeEscape("b=\\u\n");
        checkMalformedUnicodeEscape("a=\\u0123\nb=\\u012\n");
        checkMalformedUnicodeEscape("a=\\u0123\nb=\\u01\n");
        checkMalformedUnicodeEscape("a=\\u0123\nb=\\u0\n");
        checkMalformedUnicodeEscape("a=\\u0123\nb=\\u\n");
        checkMalformedUnicodeEscape("b=\\u012xyz\n");
        checkMalformedUnicodeEscape("b=x\\u012yz\n");
        checkMalformedUnicodeEscape("b=xyz\\u012\n");
    }

    private static void checkMalformedUnicodeEscape(String propString) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(baos);
        osw.write(propString);
        osw.close();
        Properties props = new Properties();
        boolean failed = true;
        try {
            props.load(getInputStream(baos.toByteArray()));
        } catch (IllegalArgumentException iae) {
            failed = false;
        }
        if (failed)
            failCount++;

        failed = true;
        props = new Properties();
        try {
            props.load(getReader(baos.toByteArray(), "UTF-8"));
        } catch (IllegalArgumentException iae) {
            failed = false;
        }
        if (failed)
            failCount++;
        report("UnicodeEscape");
    }

    private static void SaveComments() throws Exception {
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
        ByteArrayOutputStream baos;
        int i = 0;
        for (i = 0; i < output.length; i++) {
            baos = new ByteArrayOutputStream();
            props.store(baos, input[i]);
            String result = baos.toString("iso8859-1");
            if (result.indexOf(output[i]) == -1) {
                failCount++;
            }
        }
        props.put("key1", "value1");
        props.put("key2", "value2");
        props.put("key3", "value3");
        for (; i < input.length; i++) {
            baos = new ByteArrayOutputStream();
            props.store(baos, input[i]);
            Properties propsNew = new Properties();
            propsNew.load(getInputStream(baos.toByteArray()));
            check(propsNew, props);

            baos = new ByteArrayOutputStream();
            props.store(new OutputStreamWriter(baos, "UTF-8"),
                        input[i]);
            propsNew = new Properties();
            propsNew.load(getReader(baos.toByteArray(), "UTF-8"));
            check(propsNew, props);
        }
        report("SaveComments");
    }
}
