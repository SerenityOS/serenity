/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4018937
 * @library /java/text/testlib
 * @build FormatIteratorTest PParser IntlTest
 * @run main FormatIteratorTest
 * @summary Tests the formatToCharacterIterator method of SimpleDateFormat,
 *          MessageFormat and DecimalFormat.
 */

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.text.AttributedCharacterIterator.Attribute;

/**
 * FormatTester creates Formats, and tests the resulting FieldPositions
 * and AttributedCharacterIterator based on a file. The file is a hierarchical
 * set of key/value pairs, where each value can also be an array or map. The
 * top map must contain a tests entry, which will be an array consisting
 * of pairs of maps. The first map specifies the Format that
 * should be created, and consists of:
 * <pre>
 *   class = className
 *   args = (arg1 arg2 ...)
 *   valueClass = className
 *   valueArgs = (arg1 arg2 ...)
 * </pre>
 * The second map dictates what to test, and should consist of the following:
 * <pre>
 *   length = lengthOfFormattedString
 *   text = Result of Formatting
 *   0...lengthOfFormattedString = (arg1 arg2 ...)
 *   limits = ( range1 range2 ...)
 *   fieldPositions = ( fp1 fp2 ...)
 * </pre>
 * <code>lengthOfFormattedString</code> indicate the total length of formatted
 * string. <code>text</code> indicates the resulting string.
 * <code>0...x</code> where x == <code>lengthOfFormattedString - 1</code> is
 * an array of the attributes that should exist at the particular
 * location. <code>limits</code> is an array of maps, where each map
 * can be used to test the bounds of a set of attributes. Each map will
 * consist of:
 * <pre>
 *   attributes = array of attributes
 *   begin = start location
 *   begin2 = second start location
 *   end = limit location
 *   end2 = second limit location
 * </pre>
 * These are tested by iterating from begin to end in the CharacterIterator
 * and doing the following at each index:
 * <pre>
 *   getRunStart() == begin
 *   getRunStart(attributes) == begin2
 *   getRunLimit() == end
 *   getRunLimit(attributes) == end2
 * </pre>
 * <code>fieldPositions</code> is used to test the results of invoking
 * <code>format</code> with a <code>FieldPosition</code>.
 * <code>fieldPositions</code> is an array of maps, where each map contains
 * the following:
 * <pre>
 *   field = Integer field reference (optional)
 *   fieldID = Object reference
 *   begin = begin index of FieldPosition after formatting
 *   end = end index of FieldPosition after formatting
 * </pre>
 * Any lines starting with {@code '#'} are comment lines and ignored.
 */
public class FormatIteratorTest extends IntlTest {
    private Format format;
    private Object value;
    private String text;

    public static final Object ARG0_FIELD_ID = MessageFormat.
                                                     Field.ARGUMENT;
    public static final Object ARG1_FIELD_ID = MessageFormat.
                                                     Field.ARGUMENT;
    public static final Object ARG2_FIELD_ID = MessageFormat.
                                                     Field.ARGUMENT;
    public static final Object ARG3_FIELD_ID = MessageFormat.
                                                     Field.ARGUMENT;

    public static void main(String[] args) throws Exception {
        Locale reservedLocale = Locale.getDefault();
        TimeZone reservedTimeZone = TimeZone.getDefault();
        try {
            // The current tests are only appropriate for US. If tests are
            // added for other locales are added, then a property should be
            // added to each file (test) to be able to specify the locale.
            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            new FormatIteratorTest().run(args);
        } finally {
            // restore the reserved locale and time zone
            Locale.setDefault(reservedLocale);
            TimeZone.setDefault(reservedTimeZone);
        }
    }

    public FormatIteratorTest() {
    }

    public void testDecimalFormat() {
        _test(new File(System.getProperty("test.src", "."),
                       "decimalFormat.props"));
    }

    public void testMessageFormat() {
        _test(new File(System.getProperty("test.src", "."),
                       "messageFormat.props"));
    }

    public void testDateFormat() {
        _test(new File(System.getProperty("test.src", "."),
                       "dateFormat.props"));
    }

    @SuppressWarnings("unchecked")
    private void _test(File file) {
        try {
            logln("testing: " + file);
            PParser parser = new PParser();
            Map<String,Object> contents = parser.parse(new BufferedReader(
                new FileReader(file)));
            List<Object> test = (List)contents.get("tests");

            for (int counter = 0; counter < test.size(); counter++) {
                logln("creating: " + (counter / 2));

                AttributedCharacterIterator iterator =
                    create((Map)test.get(counter));

                logln("verifying: " + (counter / 2));
                verify(iterator, (Map)test.get(++counter));
            }
        } catch (IOException ioe) {
            errln("Error reading: " + ioe);
        }
    }

    @SuppressWarnings("unchecked")
    public void verify(AttributedCharacterIterator iterator,Map<String,Object> table) {
        int length = Integer.parseInt((String)table.get("length"));

        // Verify the text
        if (!getText(iterator).equals(
                escapeIfNecessary((String)table.get("text")))) {
            String text = getText(iterator);

            errln("text doesn't match, got: " + getText(iterator));
        }
        if (iterator.getBeginIndex() != 0) {
            errln("Bogus start: " + iterator.getBeginIndex());
        }
        if (iterator.getEndIndex() != length) {
            errln("Bogus end: " + iterator.getEndIndex());
        }
        for (int counter = 0; counter < length; counter++) {
            iterator.setIndex(counter);
            if (!verifyAttributes(iterator.getAttributes().keySet(),
                    makeAttributes((List)table.get(Integer.
                                                      toString(counter))))) {
                errln("Attributes don't match at " + counter + " expecting " +
                      makeAttributes((List)table.get(Integer.toString
                                                       (counter))) + " got " +
                      iterator.getAttributes().keySet());
            }
        }
        for (int counter = length - 1; counter >= 0; counter--) {
            iterator.setIndex(counter);
            if (!verifyAttributes(iterator.getAttributes().keySet(),
                    makeAttributes((List)table.get(Integer.
                                                      toString(counter))))) {
                errln("Attributes don't match at " + counter + " expecting " +
                      makeAttributes((List)table.get(Integer.toString
                                                       (counter))) + " got " +
                      iterator.getAttributes().keySet());
            }
        }
        verifyLimits(iterator, table);

        text = escapeIfNecessary((String)table.get("text"));
        List<Object> fps = (List)table.get("fieldPositions");

        if (fps != null) {
            for (int counter = 0; counter < fps.size(); counter++) {
                verifyFieldPosition(counter,(Map)fps.get(counter));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void verifyLimits(AttributedCharacterIterator iterator,
                              Map<String,Object> table) {
        List<Object> limits = (List)table.get("limits");

        if (limits != null) {
            for (int counter = 0; counter < limits.size(); counter++) {
                verifyLimit(iterator, (Map)limits.get(counter));
            }
        }
    }

    private void verifyLimit(AttributedCharacterIterator iterator,
                             Map<String,Object> table) {
        int begin = Integer.parseInt((String)table.get("begin"));
        int end = Integer.parseInt((String)table.get("end"));
        @SuppressWarnings("unchecked")
        Set<Attribute> attrs = makeAttributes((List)table.get("attributes"));
        String begin2S = (String)table.get("begin2");
        int begin2 = (begin2S != null) ? Integer.parseInt(begin2S) : begin;
        String end2S = (String)table.get("end2");
        int end2 = (end2S != null) ? Integer.parseInt(end2S) : end;

        for (int counter = begin; counter < end; counter++) {
            iterator.setIndex(counter);
            if (iterator.getRunStart() != begin) {
                errln("Begin doesn't match want " + begin + " got " +
                      iterator.getRunStart() + " at " + counter + " attrs " +
                      attrs);
            }
            if (iterator.getRunStart(attrs) != begin2) {
                errln("Begin2 doesn't match want " + begin2 + " got " +
                      iterator.getRunStart(attrs) + " at " + counter +
                      " attrs " + attrs);
            }
            if (iterator.getRunLimit() != end) {
                errln("End doesn't match want " + end + " got " +
                      iterator.getRunLimit() + " at " + counter + " attrs " +
                      attrs);
            }
            if (iterator.getRunLimit(attrs) != end2) {
                errln("End2 doesn't match want " + end2 + " got " +
                      iterator.getRunLimit(attrs) + " at " + counter +
                      " attrs " + attrs);
            }
        }
    }

    private boolean verifyAttributes(Set<Attribute> a, Set<Attribute> b) {
        boolean aEmpty = a.isEmpty();
        boolean bEmpty = b.isEmpty();

        if (aEmpty && bEmpty) {
            return true;
        }
        else if (aEmpty || bEmpty) {
            return false;
        }
        return a.equals(b);
    }

    private String getText(AttributedCharacterIterator iterator) {
        StringBuffer buffer = new StringBuffer();

        for (int counter = 0; counter < iterator.getEndIndex(); counter++) {
            buffer.append(iterator.setIndex(counter));
        }
        return buffer.toString();
    }

    private void verifyFieldPosition(int index, Map<String,Object> table) {
        Object o = table.get("field");
        int begin = Integer.parseInt((String)table.get("begin"));
        int end = Integer.parseInt((String)table.get("end"));

        if (o != null) {
            FieldPosition fp = new FieldPosition(((Integer)
                                          lookupField((String)o)));

            verifyFieldPosition(fp, begin, end, index);
        }
        o = table.get("fieldID");
        if (o != null) {
            FieldPosition fp = new FieldPosition((Format.Field)
                                                 lookupField((String)o));
            verifyFieldPosition(fp, begin, end, index);
        }
    }

    private void verifyFieldPosition(FieldPosition fp, int begin, int end,
                                     int index) {
        StringBuffer buffer = new StringBuffer();

        format.format(value, buffer, fp);
        if (fp.getBeginIndex() != begin) {
            errln("bogus begin want " + begin + " got " + fp.getBeginIndex() +
                  " for " + fp + " at " + index);
        }
        if (fp.getEndIndex() != end) {
            errln("bogus end want " + end + " got " + fp.getEndIndex() +
                  " for " + fp + " at " + index);
        }
        if (!buffer.toString().equals(text)) {
            errln("Text does not match, want !" + buffer.toString() +
                  "! got !" + text + "!");
        }
    }

    public AttributedCharacterIterator create(Map<String,Object> table) {
        format = (Format)createInstance((String)table.get("class"),
                                        ((List)table.get("args")).toArray());
        value = createInstance((String)table.get("valueClass"),
                               ((List)table.get("valueArgs")).toArray());

        logln("Created format: " + format + " value " + value);
        AttributedCharacterIterator aci = format.
                           formatToCharacterIterator(value);

        logln("Obtained Iterator: " + aci);
        return aci;
    }

    public Format.Field makeAttribute(String name) {
        return (Format.Field)lookupField(name);
    }

    private Object createInstance(String className, Object[] args) {
        if (className.equals("java.lang.reflect.Array")) {
            for (int counter = 0; counter < args.length; counter++) {
                if (args[counter] instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> v = (List<Object>)args[counter];

                    args[counter] = createInstance((String)v.get(0),
                                               ((List)v.get(1)).toArray());
                }
            }
            return args;
        }
        for (int counter = 0; counter < args.length; counter++) {
            args[counter] = escapeIfNecessary((String)args[counter]);
        }
        try {
            if (className.equals("java.util.concurrent.atomic.AtomicInteger")) {
                return new AtomicInteger(Integer.valueOf((String)args[0]));
            } else if (className.equals("java.util.concurrent.atomic.AtomicLong")) {
                return new AtomicLong(Long.valueOf((String)args[0]));
            } else {
                Class<?> klass = lookupClass(className);
                Constructor<?> cons = klass.getConstructor(
                    new Class<?>[] { String.class });
                Object value = cons.newInstance(args);

                return value;
            }
        } catch (Throwable th) {
            errln("Error creating instance " + th);
            return null;
        }
    }

    private  Class<?> lookupClass(String name) throws ClassNotFoundException {
        try {
            Class<?> klass = Class.forName(name);

            return klass;
        } catch (ClassNotFoundException e1) {}

        try {
            Class<?> klass = Class.forName("java.lang." + name);

            return klass;
        } catch (ClassNotFoundException e1) {}

        Class<?> klass = Class.forName("java.text." + name);

        return klass;
    }

    private Object lookupField(String name) {
        Throwable error = null;

        try {
            int dotIndex = name.indexOf('.');
            Class<?> klass = lookupClass(name.substring(0, dotIndex));
            String fieldName = name.substring(dotIndex + 1);
            Field[] fields = klass.getFields();

            for (int counter = fields.length - 1; counter >= 0; counter--) {
                if (fields[counter].getName().equals(fieldName)) {
                    return fields[counter].get(null);
                }
            }
        } catch (Throwable th) {
            error = th;
        }
        errln("Could not lookup field " + name + " " + error);
        return null;
    }

    protected String escapeIfNecessary(String string) {
        if (string != null) {
            int index;

            if ((index = string.indexOf("\\u")) != -1) {
                StringBuffer sb = new StringBuffer(string.substring(0, index));

                sb.append((char)Integer.parseInt(
                    string.substring(index + 2, index + 6), 16));
                sb.append(string.substring(index + 6));
                string = sb.toString();
            }
        }
        return string;
    }

    public Set<Attribute> makeAttributes(List<Object> names) {
        Set<Attribute> set = new HashSet<>(Math.max(1, names.size()));

        for (int counter = 0; counter < names.size(); counter++) {
            set.add(makeAttribute((String)names.get(counter)));
        }
        return set;
    }
}
