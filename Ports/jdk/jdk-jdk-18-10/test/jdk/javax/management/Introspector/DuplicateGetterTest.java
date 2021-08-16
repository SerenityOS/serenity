/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6518061
 * @summary Test that an MBean interface can inherit two methods with
 * the same signature from two unrelated parent interfaces
 * @author Eamonn McManus
 */

import java.util.*;
import java.lang.reflect.*;
import javax.management.*;

public class DuplicateGetterTest {
    public static interface FooMBean {
        public MBeanNotificationInfo[] getNotificationInfo();
    }

    public static interface BazMBean {
        public MBeanNotificationInfo[] getNotificationInfo();
    }

    public static interface BarMBean extends FooMBean, BazMBean {
    }

    public static class Bar implements BarMBean {
        public MBeanNotificationInfo[] getNotificationInfo() {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Testing that inheriting the same getter from " +
                           "more than one interface does not cause problems");
        DynamicMBean mbean =
            new StandardMBean(new Bar(), BarMBean.class);
        // Before fix, preceding line threw exception
        MBeanAttributeInfo[] attrs = mbean.getMBeanInfo().getAttributes();
        System.out.println("Attributes: " + Arrays.toString(attrs));
        if (attrs.length != 1)
            throw new Exception("Wrong number of attributes: " + attrs.length);
        if (!attrs[0].getName().equals("NotificationInfo"))
            throw new Exception("Wrong attribute name: " + attrs[0].getName());
        System.out.println("Test passed");
    }
}
