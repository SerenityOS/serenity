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
 * @bug 6175387
 * @summary Check that OnUnregister is an allowed value for persistPolicy
 * in ModelMBeanAttributeInfo
 * @author Eamonn McManus
 *
 * @run clean OnUnregisterTest
 * @run build OnUnregisterTest
 * @run main OnUnregisterTest
 */

// Since our RequiredModelMBean implementation doesn't support
// persistence, it doesn't have any behaviour for OnUnregister, so we
// can't test that.  We can only test that the value is allowed.

// In versions of the API prior to the addition of OnUnregister,  the
// attempt to construct a DescriptorSupport with persistPolicy=OnUnregister
// will throw an exception.

// The OnUnregister value is not case-sensitive, and we test that.

import javax.management.*;
import javax.management.modelmbean.*;

public class OnUnregisterTest {
    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName on = new ObjectName("a:b=c");

        DescriptorSupport desc;
        ModelMBeanAttributeInfo mmbai;
        ModelMBeanInfo mmbi;
        ModelMBean mmb;

        desc = new DescriptorSupport("name=foo",
                                     "descriptorType=attribute",
                                     "persistPolicy=OnUnregister");
        mmbai = new ModelMBeanAttributeInfo("foo", "int", "a foo",
                                            true, true, false, desc);
        mmbi = new ModelMBeanInfoSupport("a.b.c", "description",
                                         new ModelMBeanAttributeInfo[] {mmbai},
                                         null, null, null);
        mmb = new RequiredModelMBean(mmbi);

        mbs.registerMBean(mmb, on);
        mbs.unregisterMBean(on);

        desc = new DescriptorSupport("name=foo", "descriptorType=attribute");
        mmbai = new ModelMBeanAttributeInfo("foo", "int", "a foo",
                                            true, true, false, desc);
        desc = new DescriptorSupport("name=bar",
                                     "descriptorType=mbean",
                                     "persistPolicy=onUnregister");
        mmbi = new ModelMBeanInfoSupport("a.b.c", "description",
                                         new ModelMBeanAttributeInfo[] {mmbai},
                                         null, null, null, desc);
        mmb = new RequiredModelMBean(mmbi);
        mbs.registerMBean(mmb, on);
        mbs.unregisterMBean(on);
    }
}
