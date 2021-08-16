/*
* Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.AbstractListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/* @test
   @key headful
   @bug 8076249
   @summary  NPE in AccessBridge while editing JList model
   @author Mikhail Cherkasov
   @run main AccessibleJListChildNPETest
*/
public class AccessibleJListChildNPETest {

    private static String[] model = { "1", "2", "3", "4", "5", "6" };
    private static JList<String> list;

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame();
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                final MyModel dataModel = new MyModel(Arrays.asList(model));
                list = new JList<>(dataModel);
                frame.getContentPane().add(list);
                frame.pack();
                frame.setVisible(true);

            }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                AccessibleContext ac = list.getAccessibleContext();
                MyModel model = (MyModel)list.getModel();
                Accessible accessibleChild = ac.getAccessibleChild(model.getSize()-1);
                model.removeFirst();
                accessibleChild.getAccessibleContext().getAccessibleSelection();
                accessibleChild.getAccessibleContext().getAccessibleText();
                accessibleChild.getAccessibleContext().getAccessibleValue();
            }
        });
    }

    protected static class MyModel extends AbstractListModel<String> {
        private List<String> items = new ArrayList<>();

        MyModel(final List<String> newItems) {
            super();
            items.addAll(newItems);
            fireIntervalAdded(this, 0, getSize() - 1);
        }

        void removeFirst() {
            if(getSize() > 0) {
                items.remove(0);
                fireIntervalRemoved(this, 0, 0);
            }
        }

        @Override
        public int getSize() {
            return items.size();
        }

        @Override
        public String getElementAt(int index) {
            return items.get(index);
        }
    }
}
