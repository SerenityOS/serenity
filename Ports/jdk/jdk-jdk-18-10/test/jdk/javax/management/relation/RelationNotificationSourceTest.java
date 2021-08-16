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
 * @bug 4892674
 * @summary Test that RelationNotification can be constructed with ObjectName.
 * @author Eamonn McManus
 *
 * @run clean RelationNotificationSourceTest
 * @run build RelationNotificationSourceTest
 * @run main RelationNotificationSourceTest
 */

import java.util.Collections;
import javax.management.*;
import javax.management.relation.*;
import static javax.management.relation.RelationNotification.*;

public class RelationNotificationSourceTest {
    public static void main(String[] args) throws Exception {
        ObjectName name1 = new ObjectName("a:n=1");
        ObjectName name2 = new ObjectName("a:n=2");
        ObjectName name = new ObjectName("a:b=c");
        Notification n1 =
            new RelationNotification(RELATION_BASIC_REMOVAL,
                                     name,
                                     1234L,
                                     System.currentTimeMillis(),
                                     "message",
                                     "id",
                                     "typeName",
                                     name1,
                                     Collections.singletonList(name2));
        if (!name.equals(n1.getSource()))
            throw new Exception("FAILED: source is " + n1.getSource());
        Notification n2 =
            new RelationNotification(RELATION_BASIC_UPDATE,
                                     name,
                                     1234L,
                                     System.currentTimeMillis(),
                                     "message",
                                     "id",
                                     "typeName",
                                     name1,
                                     "role",
                                     Collections.singletonList(name2),
                                     Collections.singletonList(name2));
        if (!name.equals(n2.getSource()))
            throw new Exception("FAILED: source is " + n2.getSource());
        System.out.println("TEST PASSED");
    }
}
