/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Makes sure that JLayer is synchronizable
 * @author Alexander Potochkin
 * @run main SerializationTest
 */

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayOutputStream;

public class SerializationTest {

    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);

        JLayer<JButton> layer = new JLayer<JButton>(new JButton("Hello"));

        layer.setUI(new TestLayerUI<JButton>());

        outputStream.writeObject(layer);
        outputStream.flush();

        ByteArrayInputStream byteArrayInputStream =
                        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        ObjectInputStream inputStream = new ObjectInputStream(byteArrayInputStream);

        JLayer newLayer = (JLayer) inputStream.readObject();

        if (newLayer.getGlassPane() == null) {
            throw new RuntimeException("JLayer's glassPane is null");
        }
        if (newLayer.getUI().getClass() != layer.getUI().getClass()) {
            throw new RuntimeException("Different UIs");
        }
        if (newLayer.getView().getClass() != layer.getView().getClass()) {
            throw new RuntimeException("Different Views");
        }
    }

    static class TestLayerUI<V extends JComponent> extends LayerUI<V> {
        public String toString() {
            return "TestLayerUI";
        }
    }
}
