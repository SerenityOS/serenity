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
 * @bug 4780400
 * @summary Test that MBeanServerDelegate.DELEGATE_NAME and ObjectName.WILDCARD
 *          public constants have been initialized properly.
 * @author Luis-Miguel Alventosa
 *
 * @run clean DelegateNameWildcardNameTest
 * @run build DelegateNameWildcardNameTest
 * @run main DelegateNameWildcardNameTest
 */

import javax.management.MBeanServerDelegate;
import javax.management.ObjectName;

public class DelegateNameWildcardNameTest {

    public static void main(String[] args) throws Exception {

        System.out.println(
            "Test that <MBeanServerDelegate.DELEGATE_NAME> equals " +
            "<new ObjectName(\"JMImplementation:type=MBeanServerDelegate\")>");
        final ObjectName delegateName =
                new ObjectName("JMImplementation:type=MBeanServerDelegate");
        if (!delegateName.equals(MBeanServerDelegate.DELEGATE_NAME))
            throw new AssertionError("Unexpected value: " +
                    "MBeanServerDelegate.DELEGATE_NAME = " +
                    MBeanServerDelegate.DELEGATE_NAME);
        System.out.println("MBeanServerDelegate.DELEGATE_NAME = " +
                "new ObjectName(\"" + delegateName + "\")");

        System.out.println("Test that <ObjectName.WILDCARD> " +
                           "equals <new ObjectName(\"*:*\")>");
        final ObjectName wildcardName = new ObjectName("*:*");
        if (!wildcardName.equals(ObjectName.WILDCARD))
            throw new AssertionError("Unexpected value: " +
                    "ObjectName.WILDCARD = " +
                    ObjectName.WILDCARD);
        System.out.println("ObjectName.WILDCARD = " +
                "new ObjectName(\"" + wildcardName + "\")");

        System.out.println("Test passes: constants were initialized properly");
    }
}
