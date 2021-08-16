/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4842658
 * @summary Tests the addAll methods of DefaultListModel.
 * @run main DefaultListModelAddAllTest
 */

import javax.swing.DefaultListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Vector;
import java.util.stream.IntStream;

public class DefaultListModelAddAllTest {
    private static final int START = 0;
    private static final int END = 50;
    private static final Vector<Integer> vector =
            IntStream.range(START, END).collect(Vector::new,
                                                Vector::add,
                                                Vector::addAll);

    private static final TreeSet<Integer> set =
            IntStream.range(START, END).collect(TreeSet::new,
                                                TreeSet::add,
                                                TreeSet::addAll);

    private static final ArrayList<Integer> arrayList =
            IntStream.range(START, END).collect(ArrayList::new,
                                                ArrayList::add,
                                                ArrayList::addAll);

    public static void main(String[] args) {
        checkAddAll();
        checkAddAllWithIndex();
        System.out.println("Test case passed.");
    }

    private static class MyListDataListener implements ListDataListener {
        @Override public void intervalAdded(ListDataEvent e) {
            if (e.getIndex1() - e.getIndex0() != END - START - 1) {
                throw new RuntimeException("Test case failed. Expected " + (END - START) +
                        " elements to be added, but only got " + (e.getIndex1() - e.getIndex0()));
            }
        }

        @Override public void intervalRemoved(ListDataEvent e) {}
        @Override public void contentsChanged(ListDataEvent e) {}
    }

    private static void checkAddAll() {
        DefaultListModel<Integer> lm = new DefaultListModel<>();
        lm.addListDataListener(new MyListDataListener());

        try {
            lm.addAll(arrayList);
            System.out.println("Successfully added " + (END - START) + "elements.");
        } catch (Exception e) {
            throw new RuntimeException("Test case failed. " + e.getMessage());
        }
    }

    private static void checkAddAllWithIndex() {
        DefaultListModel<Integer> lm = new DefaultListModel<>();

        lm.addListDataListener(new MyListDataListener());
        lm.addAll(set);

        try {
            lm.addAll(START - 1, vector);
            throw new RuntimeException("Test case failed. Expected failure not reported.");
        } catch (ArrayIndexOutOfBoundsException e){
            System.out.println("Encountered exception as expected, when trying to add elements" +
                    "before the start of the list.");
        }

        try {
            lm.addAll(15, vector);
            System.out.println("Successfully added elements at a particular index");
        } catch (Exception e) {
            throw new RuntimeException("Unexpected failure: " + e.getMessage());
        }

        try {
            lm.addAll(lm.getSize() + 1, vector);
            throw new RuntimeException("Test case failed. Expected failure not reported.");
        } catch (ArrayIndexOutOfBoundsException e){
            System.out.println("Encountered exception as expected, when trying to add elements" +
                "after the end of the list.");
        }
    }
}
