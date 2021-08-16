/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8245785
 * @summary  Verifies if javax.swing.JTabbedPane can be deserialized
 * @run main JTabbedPaneSerialization
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import javax.swing.JTabbedPane;
import javax.swing.JLabel;

public class JTabbedPaneSerialization {

    public static void main(final String[] args) throws Exception {

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Tab1", new JLabel("Label1"));
        tabbedPane.addTab("Tab2", new JLabel("Label2"));
        tabbedPane.addTab("Tab3", new JLabel("Label3"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);

        out.writeObject(tabbedPane);
        out.close();

        byte[] bytes = baos.toByteArray();
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        ObjectInputStream oin = new ObjectInputStream(is);

        final JTabbedPane readPane = (JTabbedPane) oin.readObject();
        System.out.println("readPane: " + readPane.toString());
        oin.close();
        if (tabbedPane.getTabCount() != readPane.getTabCount()) {
            System.out.println("tabbedPane.tabCount " +
                                          tabbedPane.getTabCount());
            System.out.println("readPane.tabCount " +
                                            readPane.getTabCount());
            throw new
               RuntimeException("Number of pages/tab of JTabbedPane is not deserialized");
        }
    }
}
