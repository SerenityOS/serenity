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
 * @bug 4911846
 * @summary Test that MBeanRegistration can change caller ObjectName
 * @author Eamonn McManus
 *
 * @run clean PreRegisterTest
 * @run build PreRegisterTest
 * @run main PreRegisterTest
 */

/* Check that an ObjectName returned by MBeanRegistration.preRegister is
   the one used, even if createMBean had a different non-null ObjectName.  */

import java.util.Set;
import javax.management.*;

public class PreRegisterTest {
    static final ObjectName oldName, newName;

    static {
        try {
            oldName = new ObjectName("a:type=old");
            newName = new ObjectName("a:type=new");
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
            throw new Error();
        }
    }

    public static class X implements XMBean, MBeanRegistration {
        public ObjectName preRegister(MBeanServer mbs, ObjectName name) {
            return newName;
        }
        public void postRegister(Boolean done) {}
        public void preDeregister() {}
        public void postDeregister() {}
    }

    public static interface XMBean {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Testing preRegister ObjectName substitution");
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        mbs.createMBean(X.class.getName(), oldName);
        Set names = mbs.queryNames(null, null);
        System.out.println("MBean names after createMBean: " + names);
        boolean ok = true;
        if (names.contains(oldName)) {
            ok = false;
            System.out.println("TEST FAILS: previous name was used");
        }
        if (!names.contains(newName)) {
            ok = false;
            System.out.println("TEST FAILS: substitute name was not used");
        }

        if (ok) {
            System.out.println("Test passes: ObjectName correctly " +
                               "substituted");
        } else {
            System.out.println("TEST FAILS: ObjectName not correctly " +
                               "substituted");
            System.exit(1);
        }
    }
}
