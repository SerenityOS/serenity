/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4726194 7124209
 * @summary Tests for 4726194
 */

import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import static javax.swing.UIManager.getInstalledLookAndFeels;

public class bug4726194 {

    private static String[] hConstraints = {SpringLayout.WEST, "Width", SpringLayout.EAST, SpringLayout.HORIZONTAL_CENTER};
    private static String[] vConstraints = {SpringLayout.NORTH, "Height", SpringLayout.SOUTH, SpringLayout.VERTICAL_CENTER, SpringLayout.BASELINE};
    private static int[] FAIL = new int[3];
    private static boolean TEST_DUPLICATES = false;

    public static void main(String[] args) throws Exception {
        for (final UIManager.LookAndFeelInfo laf : getInstalledLookAndFeels()) {
            SwingUtilities.invokeAndWait(() -> setLookAndFeel(laf));
            SwingUtilities.invokeAndWait(() -> {
                int minLevel = 2;
                int maxLevel = 2;
                for (int i = minLevel; i <= maxLevel; i++) {
                    test(i, true);
                    test(i, false);
                }
            });
        }
    }

    private static void setLookAndFeel(UIManager.LookAndFeelInfo laf) {
        try {
            System.out.println("LookAndFeel: " + laf.getClassName());
            UIManager.setLookAndFeel(laf.getClassName());
        } catch (UnsupportedLookAndFeelException ignored){
            System.out.println("Unsupported LookAndFeel: " + laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException |
                IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void test(int level, boolean horizontal) {
        List result = new ArrayList();
        String[] constraints = horizontal ? hConstraints : vConstraints;
        test(level, constraints, result, Arrays.asList(new Object[level]));
        JTextField tf = new JTextField("");
        tf.setBorder(BorderFactory.createEmptyBorder());
        tf.setFont(new Font("Dialog", Font.PLAIN, 6));
        System.out.print("\t\t");
        for (int j = 0; j < constraints.length; j++) {
            String constraint = constraints[j];
            System.out.print(constraint + "                ".substring(constraint.length()));
        }
        System.out.println("");
        for (int i = 0; i < result.size(); i++) {
            SpringLayout.Constraints c = new SpringLayout.Constraints(tf);
            List cc = (List) result.get(i);
            for (int j = 0; j < cc.size(); j++) {
                String constraint = (String) cc.get(j);
                c.setConstraint(constraint, Spring.constant((j + 1) * 10));
            }
            System.out.print(" Input:\t\t");
            for (int j = 0; j < constraints.length; j++) {
                String constraint = constraints[j];
                int jj = cc.indexOf(constraint);
                String val = cc.contains(constraint) ? Integer.toString((jj + 1) * 10) : "?";
                System.out.print(val + "\t\t");
            }
            System.out.println("");
            System.out.print("Output:\t\t");
            for (int j = 0; j < constraints.length; j++) {
                String constraint = constraints[j];
                Spring spring = c.getConstraint(constraint);
                String springVal = (spring == null) ? "?" : Integer.toString(spring.getValue());
                System.out.print(springVal);
                System.out.print("\t\t");
            }
            for (int j = 0; j < cc.size(); j++) {
                String constraint = (String) cc.get(j);
                Spring con = c.getConstraint(constraint);
                if (con == null || con.getValue() != (j + 1) * 10) {
                    throw new RuntimeException("Values are wrong!!! ");
                }
            }
            if (horizontal) {
                int[] a1 = getValues(c, new String[]{SpringLayout.WEST, SpringLayout.WIDTH, SpringLayout.EAST});
                if (a1[0] + a1[1] != a1[2]) {
                    throw new RuntimeException("WEST + WIDTH != EAST!!! ");
                }
                int[] a2 = getValues(c, new String[]{SpringLayout.WEST, SpringLayout.WIDTH, SpringLayout.HORIZONTAL_CENTER});
                if (a2[0] + a2[1] / 2 != a2[2]) {
                    throw new RuntimeException("WEST + WIDTH/2 != HORIZONTAL_CENTER!!! ");
                }
            } else {
                int[] a3 = getValues(c, new String[]{SpringLayout.NORTH, SpringLayout.HEIGHT, SpringLayout.SOUTH});
                if (a3[0] + a3[1] != a3[2]) {
                    throw new RuntimeException("NORTH + HEIGHT != SOUTH!!! ");
                }
                int[] a4 = getValues(c, new String[]{SpringLayout.NORTH, SpringLayout.HEIGHT, SpringLayout.VERTICAL_CENTER});
                int vcDiff = Math.abs(a4[0] + a4[1] / 2 - a4[2]);
                if (vcDiff > 1) {
                    throw new RuntimeException("NORTH + HEIGHT/2 != VERTICAL_CENTER!!! ");
                }
                int[] a5 = getValues(c, new String[]{SpringLayout.NORTH, SpringLayout.BASELINE, SpringLayout.SOUTH});
                if (a5[0] > a5[1] != a5[1] > a5[2]) {
                    throw new RuntimeException("BASELINE is not in the range: [NORTH, SOUTH]!!!");
                }
            }
            System.out.println("");
        }
        System.out.println("");
    }

    private static int[] getValues(SpringLayout.Constraints con, String[] cNames) {
        int[] result = new int[cNames.length];
        for (int i = 0; i < cNames.length; i++) {
            String name = cNames[i];
            Spring s = con.getConstraint(name);
            if (s == null) {
                System.out.print("Warning: " + name + " is undefined. ");
                return FAIL;
            }
            result[i] = s.getValue();
        }
        return result;
    }

    public static void test(int level, String[] constraints, List result, List soFar) {
        if (level == 0) {
            result.add(soFar);
            return;
        }
        for (int i = 0; i < constraints.length; i++) {
            if (soFar.contains(constraints[i]) && !TEST_DUPLICATES) {
                continue;
            }
            List child = new ArrayList(soFar);
            child.set(level - 1, constraints[i]);
            test(level - 1, constraints, result, child);
        }
    }
}
