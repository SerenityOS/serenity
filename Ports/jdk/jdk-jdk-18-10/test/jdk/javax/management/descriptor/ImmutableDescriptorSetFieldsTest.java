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
 * @bug 6305139
 * @summary Check that calling setFields with a field names array with a
 * null name in it and calling setFields with a field names array with an
 * empty name in it throw the expected exceptions.
 * @author Luis-Miguel Alventosa
 *
 * @run clean ImmutableDescriptorSetFieldsTest
 * @run build ImmutableDescriptorSetFieldsTest
 * @run main ImmutableDescriptorSetFieldsTest
 */

import javax.management.ImmutableDescriptor;
import javax.management.RuntimeOperationsException;

public class ImmutableDescriptorSetFieldsTest {
    public static void main(String[] args) throws Exception {
        boolean ok = true;
        ImmutableDescriptor d = new ImmutableDescriptor("k=v");
        try {
            System.out.println(
                "Call ImmutableDescriptor.setFields(fieldNames,fieldValues) " +
                "with empty name in field names array");
            String fieldNames[] = { "a", "", "c" };
            Object fieldValues[] = { 1, 2, 3 };
            d.setFields(fieldNames, fieldValues);
            System.out.println("Didn't get expected exception");
            ok = false;
        } catch (RuntimeOperationsException e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                System.out.println("Got expected exception:");
                ok = true;
            } else {
                System.out.println("Got unexpected exception:");
                ok = false;
            }
            e.printStackTrace(System.out);
        } catch (Exception e) {
            System.out.println("Got unexpected exception:");
            ok = false;
            e.printStackTrace(System.out);
        }
        try {
            System.out.println(
                "Call ImmutableDescriptor.setFields(fieldNames,fieldValues) " +
                "with null name in field names array");
            String fieldNames[] = { "a", null, "c" };
            Object fieldValues[] = { 1, 2, 3 };
            d.setFields(fieldNames, fieldValues);
            System.out.println("Didn't get expected exception");
            ok = false;
        } catch (RuntimeOperationsException e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                System.out.println("Got expected exception:");
                ok = true;
            } else {
                System.out.println("Got unexpected exception:");
                ok = false;
            }
            e.printStackTrace(System.out);
        } catch (Exception e) {
            System.out.println("Got unexpected exception:");
            ok = false;
            e.printStackTrace(System.out);
        }
        if (ok) {
            System.out.println("TEST PASSED");
        } else {
            System.out.println("TEST FAILED");
            throw new Exception("Got unexpected exceptions");
        }
    }
}
