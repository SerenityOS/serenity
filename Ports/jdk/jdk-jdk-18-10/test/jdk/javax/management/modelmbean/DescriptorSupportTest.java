/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4883712 4869006 4894856 5016685
 * @summary Test that DescriptorSupport correctly validates fields
 * @author Eamonn McManus
 *
 * @run clean DescriptorSupportTest
 * @run build DescriptorSupportTest
 * @run main DescriptorSupportTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.management.Descriptor;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;

public class DescriptorSupportTest {
    private static final Object[] goodFields = {
        "value", "",
        "severity", "0",
        "severity", "6",
    };

    private static final Object[] badFields = {
        "name", null,
        "name", "",
        "descriptorType", null,
        "descriptorType", "",
        "setMethod", null,
        "getMethod", null,
        "role", null,
        "class", null,
        "visibility", null,
        "visibility", new Integer(0),
        "visibility", "0",
        "visibility", new Integer(5),
        "visibility", "5",
        "severity", null,
        "severity", new Integer(-1),
        "severity", "-1",
        "severity", new Integer(7),
        "severity", "7",
        "persistPolicy", null,
        "persistPolicy", "bogusPersistPolicy",
        "persistPeriod", null,
        "persistPeriod", "not a number",
        "currencyTimeLimit", null,
        "currencyTimeLimit", "not a number",
        "lastUpdatedTimeStamp", null,
        "lastUpdatedTimeStamp", "not a number",
        "lastReturnedTimeStamp", null,
        "lastReturnedTimeStamp", "not a number",
        "log", null,
        "log", "not T or F or true or false",
        "log", new Object[0],
    };


    public static void main(String[] args) throws Exception {
        boolean ok = true;

        System.out.println("Checking that name and descriptorType are " +
                           "mandatory");
        // Try omitting name and/or descriptorType
        for (int i = 0; i < 3; i++) {
            final boolean addName = ((i & 1) != 0);
            final boolean addDescriptorType = ((i & 2) != 0);
            final List fields = new ArrayList();
            if (addName)
                fields.add("name=something");
            if (addDescriptorType)
                fields.add("descriptorType=something-else");
            final String[] fs = (String[]) fields.toArray(new String[0]);
            final String what =
                    "DescriptorSupport with " +
                    (addName ? "" : "no ") + "name and " +
                    (addDescriptorType ? "" : "no ") + "descriptorType";
            DescriptorSupport ds = new DescriptorSupport(fs);
            if (ds.isValid()) {
                System.out.println("INCORRECTLY ACCEPTED: " + what);
                ok = false;
            } else
                System.out.println("OK: rejected " + what);
        }

        for (int pass = 0; pass < 2; pass++) {
            boolean shouldAccept = (pass == 0);
            System.out.println("Trying out " +
                               (shouldAccept ? "correct" : "bogus") +
                               " DescriptorSupport fields");
            Object[] fields = shouldAccept ? goodFields : badFields;
            for (int i = 0; i < fields.length; i += 2) {
                String[] names = {"name", "descriptorType"};
                String[] values = {"some-name", "some-type"};
                DescriptorSupport d = new DescriptorSupport(names, values);
                final String name = (String) fields[i];
                final Object value = fields[i + 1];
                final String valueS =
                    (value instanceof String) ? ("\"" + value + "\"") :
                    (value == null) ? "null" : value.toString();
                final String what =
                    "DescriptorSupport with " + name + " = " + valueS;
                try {
                    d.setField(name, value);
                    if (shouldAccept)
                        System.out.println("OK: accepted " + what);
                    else {
                        System.out.println("INCORRECTLY ACCEPTED: " + what);
                        ok = false;
                    }
                } catch (RuntimeOperationsException e) {
                    if (shouldAccept) {
                        System.out.println("INCORRECTLY REJECTED: " + what +
                                           ": " + e);
                        ok = false;
                    } else {
                        System.out.println("OK: rejected " + what);
                        // OK: this is what should happen
                    }
                } catch (Exception e) {
                    System.out.println("WRONG EXCEPTION: " + what + ": " + e);
                    ok = false;
                }
            }
        }

        // 4894856: ModelMBeanInfoSupport.setDescriptor(d, "mbean") fails
        System.out.println("Checking that setDescriptor(d, \"mbean\") works");
        ModelMBeanInfo mmbi =
            new ModelMBeanInfoSupport("x", "descr", null, null, null, null);
        Descriptor d = mmbi.getDescriptor("x", "mbean");
        try {
            mmbi.setDescriptor(d, "mbean");
        } catch (Exception e) {
            System.out.println("Unexpected exception:");
            e.printStackTrace(System.out);
            ok = false;
        }

        // 5016685: DescriptorSupport forces field names to lower case
        System.out.println("Checking that field name case is ignored " +
                           "but preserved");
        ok &= caseTest(new DescriptorSupport(new String[] {"NAME=blah"}),
                       "DescriptorSupport(String[])");
        ok &= caseTest(new DescriptorSupport(new String[] {"NAME"},
                                             new String[] {"blah"}),
                       "DescriptorSupport(String[], Object[])");
        DescriptorSupport d1 = new DescriptorSupport();
        d1.setField("NAME", "blah");
        ok &= caseTest(d1, "DescriptorSupport.setField");
        d1 = new DescriptorSupport(new String[] {"NAME=blah"});
        ok &= caseTest(new DescriptorSupport(d1),
                       "DescriptorSupport(Descriptor)");
        d1 = new DescriptorSupport(new String[] {"NAME=blah"});
        ok &= caseTest(new DescriptorSupport(d1.toXMLString()),
                       "DescriptorSupport(String)");
        d1 = new DescriptorSupport(new String[] {"NAME=blah"});
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(d1);
        oos.close();
        bos.close();
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        d1 = (DescriptorSupport) ois.readObject();
        ok &= caseTest(d1, "serialized DescriptorSupport");

        if (ok)
            System.out.println("Test passed");
        else {
            System.out.println("TEST FAILED");
            System.exit(1);
        }
    }

    private static boolean caseTest(Descriptor d, String what) {
        boolean ok = true;

        System.out.println("..." + what);

        String[] names = d.getFieldNames();
        if (names.length != 1 || !names[0].equals("NAME")) {
            ok = false;
            System.out.println("...getFieldNames() fails: " +
                               Arrays.asList(names));
        }

        String[] fields = d.getFields();
        if (fields.length != 1 || !fields[0].equals("NAME=blah")) {
            ok = false;
            System.out.println("...getFields() fails: " +
                               Arrays.asList(fields));
        }

        Object value = d.getFieldValue("namE");
        if (!"blah".equals(value)) {
            ok = false;
            System.out.println("...getFieldValue(\"namE\") fails: " + value);
        }

        Object[] values = d.getFieldValues(new String[] {"namE"});
        if (values.length != 1 || !"blah".equals(values[0])) {
            ok = false;
            System.out.println("...getFieldValues({\"namE\"}) fails: " +
                               Arrays.asList(values));
        }

        d.setField("namE", "newblah");
        Object newblah = d.getFieldValue("Name");
        if (!"newblah".equals(newblah)) {
            ok = false;
            System.out.println("...setField value not returned: " + newblah);
        }

        d.setFields(new String[] {"NaMe"}, new Object[] {"newerblah"});
        Object newerblah = d.getFieldValue("naMe");
        if (!"newerblah".equals(newerblah)) {
            ok = false;
            System.out.println("...setFields value not returned: " +
                               newerblah);
        }

        Descriptor d1 = (Descriptor) d.clone();
        newerblah = d1.getFieldValue("NAMe");
        if (!"newerblah".equals(newerblah)) {
            ok = false;
            System.out.println("...clone incorrect: " + newerblah);
        }

        d.removeField("NAme");
        names = d.getFieldNames();
        if (names.length != 0) {
            ok = false;
            System.out.println("...removeField failed: " +
                               Arrays.asList(names));
        }

        if (ok)
            System.out.println("...succeeded");

        return ok;
    }
}
