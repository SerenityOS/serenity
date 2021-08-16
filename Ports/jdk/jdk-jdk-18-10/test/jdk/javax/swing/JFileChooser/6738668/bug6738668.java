/*
 * Copyright (c) 2009,2017, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 6738668 6962725
   @summary JFileChooser cannot be created under SecurityManager
   @author Pavel Porvatov
   @run main/othervm/policy=security.policy bug6738668
*/

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.UIManager;

public class bug6738668 {
    public static void main(String[] args) throws Exception {
        for (UIManager.LookAndFeelInfo lookAndFeelInfo : UIManager.getInstalledLookAndFeels()) {
            UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());

            String tmpdir = System.getProperty("java.io.tmpdir");
            System.out.println("tmp dir " + tmpdir);
            new JFileChooser(new File(tmpdir+"/temp"));


            System.out.println("Test passed for LookAndFeel " + lookAndFeelInfo.getClassName());
        }
    }
}
