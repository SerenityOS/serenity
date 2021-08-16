/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.management.MBeanServerConnection;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;

import static javax.management.openmbean.SimpleType.*;

/*
 * @test
 * @bug     8042901
 * @summary Check that MappedMXBeanType.toOpenTypeData supports VMOption
 * @author  Shanliang Jiang
 */
public class VMOptionOpenDataTest {
    private static final String[] names = new String[] {
        "name", "value", "origin", "writeable"
    };
    private static final OpenType[] types = new OpenType[] {
        STRING, STRING, STRING, BOOLEAN
    };

    public static void main(String... args) throws Exception {
        MBeanServerConnection msc = ManagementFactory.getPlatformMBeanServer();
        HotSpotDiagnosticMXBean mxbean =
            ManagementFactory.getPlatformMXBean(msc, HotSpotDiagnosticMXBean.class);


        String[] signatures = new String[] {
            String.class.getName()
        };
        Object obj = msc.invoke(mxbean.getObjectName(), "getVMOption",
            new String[] { "PrintVMOptions"}, signatures);

        CompositeData data = (CompositeData)obj;
        validateType(data);

        VMOption option = mxbean.getVMOption("PrintVMOptions");
        VMOption o = VMOption.from(data);
        assertEquals(option, o);
    }

    private static void validateType(CompositeData data) {
        CompositeType type = data.getCompositeType();
        Set<String> keys = Arrays.stream(names).collect(Collectors.toSet());
        if (!type.keySet().equals(keys)) {
            throw new RuntimeException("key not matched: " + type.keySet().toString());
        }
        for (int i=0; i < names.length; i++) {
            OpenType t = type.getType(names[i]);
            if (t != types[i]) {
                throw new AssertionError(names[i] + ": type not matched: " +
                    t + " expected: " + types[i]);
            }
        }
    }

    private static void assertEquals(VMOption o1, VMOption o2) {
        if (!o1.getName().equals(o2.getName()) ||
            !o1.getOrigin().equals(o2.getOrigin()) ||
            !o1.getValue().equals(o2.getValue()) ||
            o1.isWriteable() != o2.isWriteable()) {
            throw new AssertionError(o1 + " != " + o2);
        }

    }

}
