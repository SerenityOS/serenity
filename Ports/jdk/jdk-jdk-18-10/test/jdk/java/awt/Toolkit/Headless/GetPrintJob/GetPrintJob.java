/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
@test
@bug 7023011
@library ../../../regtesthelpers
@build Sysout
@summary Toolkit.getPrintJob() throws wrong exceptions
@author andrei dmitriev: area=awt.headless
@run main GetPrintJob
 */

import java.awt.*;
import java.util.Properties;
import test.java.awt.regtesthelpers.Sysout;
/*
 * In headfull mode we should always getting NPE on the getPrintJob() call if frame == null.
 */

public class GetPrintJob {

    public static void main(String[] s) {
        boolean stage1Passed = false;
        boolean stage2Passed = false;

        try {
            Toolkit.getDefaultToolkit().getPrintJob(
                    (Frame) null, "title", new Properties());
        } catch (NullPointerException e) {
            stage1Passed = true;
            System.out.println("Stage 1 passed. getPrintJob(null, String, property) has thrown NPE.");
        }
        if (!stage1Passed) {
            throw new RuntimeException("getPrintJob() should have thrown NPE but didn't.");
        }

        try {
            Toolkit.getDefaultToolkit().getPrintJob(
                    (Frame) null, "title", new JobAttributes(), new PageAttributes());
        } catch (NullPointerException e) {
            stage2Passed = true;
            System.out.println("Stage 2 passed. getPrintJob(null, String, jobAttrs, pageAttr) has thrown NPE.");
        }
        if (!stage2Passed) {
            throw new RuntimeException("getPrintJob() should have thrown NPE but didn't.");
        }

        System.out.println("Test PASSED");
    }
}
