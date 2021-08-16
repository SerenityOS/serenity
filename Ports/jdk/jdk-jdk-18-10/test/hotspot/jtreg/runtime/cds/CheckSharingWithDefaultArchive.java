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
 */

/**
 * @test Default CDS archive
 * @summary Sharing should be enabled by default on supported platform/binaries.
 *          No -Xshare:dump is needed. No -Xshare:auto or -Xshare:on in needed.
 *          Verify a set of well-known shared classes.
 * @requires vm.cds
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *      -XX:+WhiteBoxAPI CheckSharingWithDefaultArchive -showversion
 */
import jdk.test.lib.Platform;
import jtreg.SkippedException;
import sun.hotspot.WhiteBox;

public class CheckSharingWithDefaultArchive {
    public static void main(String[] args) throws Exception {
        if (!Platform.isDefaultCDSArchiveSupported()) {
            throw new SkippedException("Supported platform");
        }

        WhiteBox wb = WhiteBox.getWhiteBox();
        String classes[] = {"java.lang.Object",
                            "java.lang.String",
                            "java.lang.Class"};
        // If maping fails, sharing is disabled
        if (wb.isSharingEnabled()) {
            for (int i = 0; i < classes.length; i++) {
                Class c = Class.forName(classes[i]);
                if (wb.isSharedClass(c)) {
                    System.out.println(classes[i] + " is shared.");
                } else {
                    throw new RuntimeException(classes[i] + " is not shared");
                }
            }
        } else {
           throw new SkippedException("Sharing is not enabled.");
        }
    }
}
