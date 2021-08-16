/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @key headful
  @bug 6520635
  @summary Test that modality and modal exclusion types are handled
correctly according Toolkit.isModalityTypeSupported() and
Toolkit.isModalExclusionTypeSupported() methods
  @author artem.ananiev: area=awt.modal
  @run main SupportedTest
*/

import java.awt.*;

public class SupportedTest
{
    public static void main(String[] args)
    {
        boolean passed = true;
        Toolkit tk = Toolkit.getDefaultToolkit();

        // check for modality types

        Frame f = new Frame("F");
        for (Dialog.ModalityType mt : Dialog.ModalityType.values())
        {
            if (!tk.isModalityTypeSupported(mt))
            {
                Dialog d = new Dialog(f, "D", mt);
                if (!d.getModalityType().equals(Dialog.ModalityType.MODELESS))
                {
                    System.err.println("Error: modality type " + mt + " is not supported\n" +
                                       "but a dialog with this modality type can be created");
                    passed = false;
                }
            }
        }

        // check for modal exclusion types
        for (Dialog.ModalExclusionType et : Dialog.ModalExclusionType.values())
        {
            if (!tk.isModalExclusionTypeSupported(et))
            {
                Frame g = new Frame("G");
                g.setModalExclusionType(et);
                if (!g.getModalExclusionType().equals(Dialog.ModalExclusionType.NO_EXCLUDE))
                {
                    System.err.println("Error: modal exclusion type " + et + "is not supported\n" +
                                       "but a window with this modal exclusion type can be created");
                    passed = false;
                }
            }
        }

        if (!passed)
        {
            throw new RuntimeException("Test FAILED: some of modality types and/or modal exclusion types are handled incorrectly");
        }
    }
}
