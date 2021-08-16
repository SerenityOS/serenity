/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.io.File;
import java.net.URL;
import sun.hotspot.WhiteBox;

//
// - Test static final String field with initial value in cached mirror should be also archived.
// - GC should not crash when reference fields in cached mirror are updated at runtime
//     - Reference fields are updated to point to runtime created objects
//     - Reference fields are nullified
//
public class MirrorWithReferenceFieldsApp {

    // Static String field with initial value
    static final String archived_field = "abc";

    // Static object field
    static Object non_archived_field_1;

    // Instance field
    Integer non_archived_field_2;

    public MirrorWithReferenceFieldsApp() {
        non_archived_field_1 = new Object();
        non_archived_field_2 = Integer.valueOf(1);
    }

    public static void main(String args[]) throws Exception {
        WhiteBox wb = WhiteBox.getWhiteBox();

        if (!wb.areOpenArchiveHeapObjectsMapped()) {
            System.out.println("Archived open_archive_heap objects are not mapped.");
            System.out.println("This may happen during normal operation. Test Skipped.");
            return;
        }

        MirrorWithReferenceFieldsApp m = new MirrorWithReferenceFieldsApp();
        m.test(wb);
    }

    public void test(WhiteBox wb) {
        Class c = MirrorWithReferenceFieldsApp.class;
        if (wb.isSharedClass(c)) {
            // Check if the Class object is cached
            if (wb.isShared(c)) {
                System.out.println(c + " mirror is cached. Expected.");
            } else {
                throw new RuntimeException(
                    "FAILED. " + c + " mirror should be cached.");
            }

            // Check fields

            if (wb.isShared(archived_field)) {
                System.out.println("archived_field is archived as excepted");
            } else {
                throw new RuntimeException(
                    "FAILED. archived_field is not archived.");
            }

            // GC should not crash
            System.gc();
            System.gc();
            System.gc();

            non_archived_field_1 = null;
            non_archived_field_2 = null;

            System.gc();
            System.gc();
            System.gc();

            System.out.println("Done.");
        }
    }
}
