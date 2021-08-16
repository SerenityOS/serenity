/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6204469
 * @summary Test that Open MBean attributes and parameters check constraints
 * @author Eamonn McManus
 *
 * @run clean ConstraintTest
 * @run build ConstraintTest
 * @run main ConstraintTest
 */

import java.util.*;
import javax.management.*;
import javax.management.openmbean.*;

public class ConstraintTest {
    private static String failure;

    public static void main(String[] args) throws Exception {
        for (Object[][] test : tests) {
            if (test.length != 4) {
                throw new Exception("Test element has wrong length: " +
                                    Arrays.deepToString(test));
            }

            if (test[0].length != 4) {
                throw new Exception("Test constraints should have size 4: " +
                                    Arrays.deepToString(test[0]));
            }
            Object defaultValue = test[0][0];
            Comparable<?> minValue = (Comparable<?>) test[0][1];
            Comparable<?> maxValue = (Comparable<?>) test[0][2];
            Object[] legalValues = (Object[]) test[0][3];
            System.out.println("test: defaultValue=" + defaultValue +
                               "; minValue=" + minValue +
                               "; maxValue=" + maxValue +
                               "; legalValues=" +
                               Arrays.deepToString(legalValues));

            if (test[1].length != 1) {
                throw new Exception("OpenType list should have size 1: " +
                                    Arrays.deepToString(test[1]));
            }
            OpenType<?> openType = (OpenType<?>) test[1][0];

            Object[] valid = test[2];
            Object[] invalid = test[3];

            System.out.println("...valid=" + Arrays.deepToString(valid));
            System.out.println("...invalid=" + Arrays.deepToString(invalid));

            test(openType, defaultValue, minValue, maxValue, legalValues,
                 valid, invalid);
        }

        if (failure == null)
            System.out.println("Test passed");
        else
            throw new Exception("TEST FAILED: " + failure);
    }

    private static <T> void test(OpenType<T> openType, Object defaultValue,
                                 Comparable<?> minValue,
                                 Comparable<?> maxValue, Object[] legalValues,
                                 Object[] valid, Object[] invalid)
            throws Exception {
        /* This hack is needed to avoid grief from the parameter checking
           in the OpenMBean*InfoSupport constructors.  Since they are defined
           to check that the defaultValue etc are of the same type as the
           OpenType<T>, there is no way to pass a defaultValue etc when
           the type is OpenType<?>.  So either you have to write plain
           OpenType, and get unchecked warnings for every constructor
           invocation, or you do this, and get the unchecked warnings just
           here.  */
        test1(openType, (T) defaultValue, (Comparable<T>) minValue,
              (Comparable<T>) maxValue, (T[]) legalValues, valid, invalid);
    }

    private static <T> void test1(OpenType<T> openType, T defaultValue,
                                  Comparable<T> minValue,
                                  Comparable<T> maxValue, T[] legalValues,
                                  Object[] valid, Object[] invalid)
            throws Exception {

        if (legalValues != null && (minValue != null || maxValue != null))
            throw new Exception("Test case has both legals and min/max");

        if (defaultValue == null && minValue == null && maxValue == null &&
            legalValues == null) {
            test(new OpenMBeanAttributeInfoSupport("name", "descr", openType,
                                                   true, true, false),
                 valid, invalid);
            test(new OpenMBeanAttributeInfoSupport("name", "descr", openType,
                                                   true, true, false, nullD),
                 valid, invalid);
            test(new OpenMBeanAttributeInfoSupport("name", "descr", openType,
                                                   true, true, false, emptyD),
                 valid, invalid);
            test(new OpenMBeanParameterInfoSupport("name", "descr", openType),
                 valid, invalid);
            test(new OpenMBeanParameterInfoSupport("name", "descr", openType,
                                                   nullD),
                 valid, invalid);
            test(new OpenMBeanParameterInfoSupport("name", "descr", openType,
                                                   emptyD),
                 valid, invalid);
        }

        if (minValue == null && maxValue == null && legalValues == null) {
            Descriptor d = descriptor("defaultValue", defaultValue);
            test(new OpenMBeanAttributeInfoSupport("blah", "descr", openType,
                                                   true, true, false, d),
                 valid, invalid);
            test(new OpenMBeanAttributeInfoSupport("blah", "descr",
                                                   openType, true, true,
                                                   false, defaultValue),
                 valid, invalid);
            test(new OpenMBeanParameterInfoSupport("blah", "descr", openType,
                                                   d),
                 valid, invalid);
            test(new OpenMBeanParameterInfoSupport("blah", "descr", openType,
                                                   defaultValue),
                 valid, invalid);
        }

        if (legalValues == null) {
            Descriptor d = descriptor("defaultValue", defaultValue,
                                      "minValue", minValue,
                                      "maxValue", maxValue);
            test(new OpenMBeanAttributeInfoSupport("name", "descr", openType,
                                                   true, true, false, d),
                 valid, invalid);
            test(new OpenMBeanAttributeInfoSupport("name", "descr", openType,
                                                   true, true, false,
                                                   defaultValue,
                                                   minValue, maxValue),
                 valid, invalid);
            test(new OpenMBeanParameterInfoSupport("name", "descr", openType,
                                                   d),
                 valid, invalid);
            test(new OpenMBeanParameterInfoSupport("name", "descr", openType,
                                                   defaultValue,
                                                   minValue, maxValue),
                 valid, invalid);
        }

        if (minValue == null && maxValue == null) {
            // Legal values in descriptor can be either an array or a set
            Descriptor d1 = descriptor("defaultValue", defaultValue,
                                       "legalValues", legalValues);
            Descriptor d2;
            if (legalValues == null)
                d2 = d1;
            else {
                d2 = descriptor("defaultValue", defaultValue,
                                "legalValues", arraySet(legalValues));
            }
            test(new OpenMBeanAttributeInfoSupport("name", "descr", openType,
                                                   true, true, false, d1),
                 valid, invalid);
            test(new OpenMBeanAttributeInfoSupport("name", "descr", openType,
                                                   true, true, false, d2),
                 valid, invalid);
            test(new OpenMBeanAttributeInfoSupport("name", "descr", openType,
                                                   true, true, false,
                                                   defaultValue, legalValues),
                 valid, invalid);
            test(new OpenMBeanParameterInfoSupport("name", "descr", openType,
                                                   d1),
                 valid, invalid);
            test(new OpenMBeanParameterInfoSupport("name", "descr", openType,
                                                   d2),
                 valid, invalid);
            test(new OpenMBeanParameterInfoSupport("name", "descr", openType,
                                                   defaultValue, legalValues),
                 valid, invalid);
        }
    }

    /* Test one of the objects.  Note that OpenMBeanAttributeInfo
       extends OpenMBeanParameterInfo, so OpenMBeanAttributeInfoSupport
       is-an OpenMBeanParameterInfo.  */
    private static void test(OpenMBeanParameterInfo info,
                             Object[] valid, Object[] invalid) {
        test1(info, valid, invalid);

        // Check that the constraints can be specified as strings
        // rather than objects
        if (info.getOpenType() instanceof SimpleType<?>) {
            Descriptor d = ((DescriptorRead) info).getDescriptor();
            String[] names = d.getFieldNames();
            Object[] values = d.getFieldValues(names);
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null)
                    continue;
                if (names[i].equals("legalValues")) {
                    Collection<?> legals;
                    if (values[i] instanceof Collection<?>)
                        legals = (Collection<?>) values[i];
                    else
                        legals = Arrays.asList((Object[]) values[i]);
                    List<String> strings = new ArrayList<String>();
                    for (Object legal : legals)
                        strings.add(legal.toString());
                    values[i] = strings.toArray(new String[0]);
                } else if (!(values[i] instanceof OpenType<?>))
                    values[i] = values[i].toString();
            }
            d = new ImmutableDescriptor(names, values);
            OpenType<?> ot = info.getOpenType();
            if (info instanceof OpenMBeanAttributeInfo) {
                OpenMBeanAttributeInfo ai = (OpenMBeanAttributeInfo) info;
                info = new OpenMBeanAttributeInfoSupport(info.getName(),
                                                         info.getDescription(),
                                                         info.getOpenType(),
                                                         ai.isReadable(),
                                                         ai.isWritable(),
                                                         ai.isIs(),
                                                         d);
            } else {
                info = new OpenMBeanParameterInfoSupport(info.getName(),
                                                         info.getDescription(),
                                                         info.getOpenType(),
                                                         d);
            }
            test1(info, valid, invalid);
        }
    }

    private static void test1(OpenMBeanParameterInfo info,
                              Object[] valid, Object[] invalid) {

        for (Object x : valid) {
            if (!info.isValue(x)) {
                fail("Object should be valid but is not: " + x + " for: " +
                     info);
            }
        }

        for (Object x : invalid) {
            if (info.isValue(x)) {
                fail("Object should not be valid but is: " + x + " for: " +
                     info);
            }
        }

        /* If you specify e.g. minValue in a descriptor, then we arrange
           for getMinValue() to return the same value, and if you specify
           a minValue as a constructor parameter then we arrange for the
           descriptor to have a minValue entry.  Check that these values
           do in fact match.  */

        Descriptor d = ((DescriptorRead) info).getDescriptor();

        checkSameValue("defaultValue", info.getDefaultValue(),
                       d.getFieldValue("defaultValue"));
        checkSameValue("minValue", info.getMinValue(),
                       d.getFieldValue("minValue"));
        checkSameValue("maxValue", info.getMaxValue(),
                       d.getFieldValue("maxValue"));
        checkSameValues("legalValues", info.getLegalValues(),
                        d.getFieldValue("legalValues"));
    }

    private static void checkSameValue(String what, Object getterValue,
                                       Object descriptorValue) {
        if (getterValue == null) {
            if (descriptorValue != null) {
                fail("Getter returned null but descriptor has entry for " +
                     what + ": " + descriptorValue);
            }
        } else if (descriptorValue == null) {
            fail("Getter returned value but descriptor has no entry for " +
                 what + ": " + getterValue);
        } else if (!getterValue.equals(descriptorValue) &&
                   !getterValue.toString().equals(descriptorValue)) {
            fail("For " + what + " getter returned " + getterValue +
                 " but descriptor entry is " + descriptorValue);
        }
    }

    private static void checkSameValues(String what, Set<?> getterValues,
                                        Object descriptorValues) {
        if (getterValues == null) {
            if (descriptorValues != null) {
                fail("Getter returned null but descriptor has entry for " +
                     what + ": " + descriptorValues);
            }
        } else if (descriptorValues == null) {
            fail("Getter returned value but descriptor has no entry for " +
                 what + ": " + getterValues);
        } else {
            Set<?> descriptorValueSet;
            if (descriptorValues instanceof Set<?>)
                descriptorValueSet = (Set<?>) descriptorValues;
            else
                descriptorValueSet = arraySet((Object[]) descriptorValues);
            boolean same = true;
            if (getterValues.size() != descriptorValueSet.size())
                same = false;
            else {
                for (Object x : getterValues) {
                    if (!descriptorValueSet.contains(x)
                        && !descriptorValueSet.contains(x.toString())) {
                        same = false;
                        break;
                    }
                }
            }
            if (!same) {
                fail("For " + what + " getter returned " + getterValues +
                     " but descriptor entry is " + descriptorValueSet);
            }
        }
    }

    private static void fail(String why) {
        System.out.println("FAILED: " + why);
        failure = why;
    }

    private static Descriptor descriptor(Object... entries) {
        if (entries.length % 2 != 0)
            throw new RuntimeException("Odd length descriptor entries");
        String[] names = new String[entries.length / 2];
        Object[] values = new Object[entries.length / 2];
        for (int i = 0; i < entries.length; i += 2) {
            names[i / 2] = (String) entries[i];
            values[i / 2] = entries[i + 1];
        }
        return new ImmutableDescriptor(names, values);
    }

    private static <T> Set<T> arraySet(T[] array) {
        return new HashSet<T>(Arrays.asList(array));
    }

    private static final OpenType<?>
        ostring = SimpleType.STRING,
        oint = SimpleType.INTEGER,
        obool = SimpleType.BOOLEAN,
        olong = SimpleType.LONG,
        obyte = SimpleType.BYTE,
        ofloat = SimpleType.FLOAT,
        odouble = SimpleType.DOUBLE,
        ostringarray, ostringarray2;
    private static final CompositeType ocomposite;
    private static final CompositeData compositeData, compositeData2;
    static {
        try {
            ostringarray = new ArrayType<String[]>(1, ostring);
            ostringarray2 = new ArrayType<String[][]>(2, ostring);
            ocomposite =
                new CompositeType("name", "descr",
                                  new String[] {"s", "i"},
                                  new String[] {"sdesc", "idesc"},
                                  new OpenType[] {ostring, oint});
            compositeData =
                new CompositeDataSupport(ocomposite,
                                         new String[] {"s", "i"},
                                         new Object[] {"foo", 23});
            compositeData2 =
                new CompositeDataSupport(ocomposite,
                                         new String[] {"s", "i"},
                                         new Object[] {"bar", -23});
        } catch (OpenDataException e) { // damn checked exceptions...
            throw new IllegalArgumentException(e.toString(), e);
        }
    }

    private static final Descriptor
        nullD = null,
        emptyD = ImmutableDescriptor.EMPTY_DESCRIPTOR;

    /* The elements of this array are grouped as follows.  Each
       element contains four Object[]s.  The first one is a set of
       four values: default value, min value, max value, legal values
       (an Object[]), some of which can be null.  These will be used
       to derive the OpenMBean*Info values to be tested.  The second
       is an array with one element that is the OpenType that will be
       given to the constructors of the OpenMBean*Infos.  The third
       element is a set of values that should be valid according to
       the constraints in the OpenMBean*Info.  The fourth is a set of
       values that should be invalid according to those
       constraints.  */
    private static final Object[][][] tests = {

        // Test cases when there are no constraints
        // Validity checking is limited to type of object

        {{null, null, null, null},
         {oint},
         {-1, 0, 1, Integer.MAX_VALUE, Integer.MIN_VALUE},
         {null, "noddy", 1.3, false, 3L, Long.MAX_VALUE, emptyD,
          new int[2], new Integer[2], new Integer[] {3}, new Integer[0]}},

        {{null, null, null, null},
         {obool},
         {true, false},
         {null, "noddy", 1.3, 3, 3L, Long.MAX_VALUE, emptyD,
          new int[2], new Integer[2], new Integer[] {3}, new Integer[0]}},

        {{null, null, null, null},
         {ostring},
         {"", "yes!"},
         {null, 1.3, 3, false, 3L, Long.MAX_VALUE, emptyD,
          new int[2], new Integer[2], new Integer[] {3}, new Integer[0]}},

        {{null, null, null, null},
         {obyte},
         {Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) 0},
         {null, "noddy", 1.3, 3, false, 3L, Long.MAX_VALUE, emptyD,
          new int[2], new Integer[2], new Integer[] {3}, new Integer[0]}},

        {{null, null, null, null},
         {ostringarray},
         {new String[0], new String[] {"hello", "world"}},
         {null, "noddy", 1.3, 3, false, 3L, Long.MAX_VALUE, emptyD,
          new int[2], new Integer[2], new Integer[] {3}, new Integer[0]}},

        {{null, null, null, null},
         {ostringarray2},
         {new String[0][0], new String[][] {{"hello", "world"},
                                            {"goodbye", "cruel", "world"}}},
         {null, "noddy", 1.3, 3, false, 3L, Long.MAX_VALUE, emptyD,
          new int[2], new Integer[2], new Integer[] {3}, new Integer[0]}},

        {{null, null, null, null},
         {ocomposite},
         {compositeData, compositeData2},
         {null, "noddy", 1.3, 3, false, 3L, Long.MAX_VALUE, emptyD,
          new int[2], new Integer[2], new Integer[] {3}, new Integer[0]}},

        // Test cases where there is a default value, so null is allowed

        {{23, null, null, null},
         {oint},
         {null, -1, 0, 1, Integer.MAX_VALUE, Integer.MIN_VALUE},
         {"noddy", 1.3, false, 3L, Long.MAX_VALUE, emptyD,
          new int[2], new Integer[2], new Integer[] {3}, new Integer[0]}},

        {{true, null, null, null},
         {obool},
         {null, true, false},
         {"noddy", 1.3, 3, 3L, Long.MAX_VALUE, emptyD,
          new int[2], new Integer[2], new Integer[] {3}, new Integer[0]}},

        {{"foo", null, null, null},
         {ostring},
         {null, "", "yes!"},
         {1.3, 3, false, 3L, Long.MAX_VALUE, emptyD,
          new int[2], new Integer[2], new Integer[] {3}, new Integer[0]}},

        {{(byte) 23, null, null, null},
         {obyte},
         {null, Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) 0},
         {"noddy", 1.3, 3, false, 3L, Long.MAX_VALUE, emptyD,
          new int[2], new Integer[2], new Integer[] {3}, new Integer[0]}},

        {{compositeData, null, null, null},
         {ocomposite},
         {null, compositeData, compositeData2},
         {"noddy", 1.3, 3, false, 3L, Long.MAX_VALUE, emptyD,
          new int[2], new Integer[2], new Integer[] {3}, new Integer[0]}},

        // Test cases where there is a min and/or max, with or without default

        {{23, 0, 50, null},
         {oint},
         {null, 0, 25, 50},
         {"noddy", -1, 51, Integer.MIN_VALUE, Integer.MAX_VALUE, 25L}},

        {{null, 0, 50, null},
         {oint},
         {0, 25, 50},
         {null, "noddy", -1, 51, Integer.MIN_VALUE, Integer.MAX_VALUE, 25L}},

        {{null, 0, null, null},
         {oint},
         {0, 25, 50, Integer.MAX_VALUE},
         {null, "noddy", -1, Integer.MIN_VALUE, 25L}},

        {{null, null, 50, null},
         {oint},
         {Integer.MIN_VALUE, -1, 0, 25, 50},
         {null, "noddy", 51, Integer.MAX_VALUE, 25L}},

        {{"go", "a", "z~", null},
         {ostring},
         {null, "a", "z~", "zzzz", "z!"},
         {"A", "~", "", -1}},

        // Test cases where there is a set of legal values

        {{23, null, null, new Integer[] {2, 3, 5, 7, 11, 13, 17, 23}},
         {oint},
         {null, 2, 11, 23},
         {"noddy", -1, 1, 51, Integer.MIN_VALUE, Integer.MAX_VALUE, 25L}},

        {{null, null, null, new CompositeData[] {compositeData}},
         {ocomposite},
         {compositeData},
         {null, compositeData2, "noddy"}},

        {{null, null, null, new Long[0]},
         {olong},
         {}, // constraint is impossible to satisfy!
         {null, 23L, "x", 23}},
    };
}
