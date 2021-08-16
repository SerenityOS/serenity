/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
   @bug 7149090
   @summary Nimbus:BorderFactory.createTitledBorder() the DEFAULT position of a title is not the same as the TOP
   @modules java.desktop/javax.swing.border:open
   @author Pavel Porvatov
*/

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Test7149090 {
    private static final Object[][] DEFAULT_TITLE_POSITIONS = {
            {"Metal", TitledBorder.TOP},
            {"Motif", TitledBorder.TOP},
            {"Windows", TitledBorder.TOP},
            {"Nimbus", TitledBorder.ABOVE_TOP},
    };

    public static void main(String[] args) throws Exception {
        for (UIManager.LookAndFeelInfo lookAndFeel : UIManager.getInstalledLookAndFeels()) {
            for (Object[] defaultTitlePosition : DEFAULT_TITLE_POSITIONS) {
                if (defaultTitlePosition[0].equals(lookAndFeel.getName())) {
                    UIManager.setLookAndFeel(lookAndFeel.getClassName());

                    final int expectedPosition = (Integer) defaultTitlePosition[1];

                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            List<TitledBorder> borders = new ArrayList<>();

                            borders.add(BorderFactory.createTitledBorder(new EmptyBorder(0, 0, 0, 0), "Title"));

                            try {
                                Method getPositionMethod = TitledBorder.class.getDeclaredMethod("getPosition");

                                getPositionMethod.setAccessible(true);

                                for (TitledBorder border : borders) {
                                    int position = (Integer) getPositionMethod.invoke(border);

                                    if (position != expectedPosition) {
                                        throw new RuntimeException("Invalid title position");
                                    }
                                }
                            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });

                    System.out.println("Test passed for LookAndFeel " + lookAndFeel.getName());
                }
            }
        }
    }
}
