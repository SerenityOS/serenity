/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 6872503
 * @summary Checks that JLayer correctly works with its AWTEventListener
 * @author Alexander Potochkin
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.AWTEventListenerProxy;

public class bug6872503 {

    static JLayer<Component> l1;
    static JLayer<Component> l2;

    private static void createGui() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        int length = toolkit.getAWTEventListeners().length;

        l1 = new JLayer<Component>();
        l1.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK);

        l2 = new JLayer<Component>();
        l2.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

        if (isLayerEventControllerAdded()) {
            throw new RuntimeException("Unexpected AWTEventListener was added");
        }

        JFrame frame = new JFrame();
        frame.setLayout(new FlowLayout());
        frame.add(l1);
        frame.add(l2);

        if (isLayerEventControllerAdded()) {
            throw new RuntimeException("Unexpected AWTEventListener was added");
        }

        frame.pack();

        if (!isLayerEventControllerAdded()) {
            throw new RuntimeException("AWTEventListener was not added");
        }

        if (!layerEventControllerMaskEquals(l1.getLayerEventMask() | l2.getLayerEventMask())) {
             throw new RuntimeException("Wrong mask for AWTEventListener");
        }

        frame.dispose();

        if (isLayerEventControllerAdded()) {
            throw new RuntimeException("Unexpected AWTEventListener was added");
        }
    }

    static boolean isLayerEventControllerAdded() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        AWTEventListener layerEventController = null;
        for (AWTEventListener listener : toolkit.getAWTEventListeners()) {
            if (listener instanceof AWTEventListenerProxy) {
                listener = ((AWTEventListenerProxy)listener).getListener();

            }
            if ("LayerEventController".equals(listener.getClass().getSimpleName())) {
                if (layerEventController != null) {
                    throw new RuntimeException("Duplicated LayerEventController");
                }
                layerEventController = listener;
            }
        }
        boolean ret = layerEventController != null;
        if (ret) {
            System.out.println("LayerEventController found");
        } else {
            System.out.println("No LayerEventController");
        }
        return ret;
    }

    static boolean layerEventControllerMaskEquals(long mask) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        AWTEventListener layerEventController = null;
        for (AWTEventListener listener : toolkit.getAWTEventListeners(mask)) {
            if (listener instanceof AWTEventListenerProxy) {
                listener = ((AWTEventListenerProxy)listener).getListener();

            }
            if ("LayerEventController".equals(listener.getClass().getSimpleName())) {
                if (layerEventController != null) {
                    throw new RuntimeException("Duplicated LayerEventController");
                }
                layerEventController = listener;
            }
        }
        boolean ret = layerEventController != null;
        if (ret) {
            System.out.println("LayerEventController with the correct mask found");
        } else {
            System.out.println("No LayerEventController with the correct mask");
        }
        return ret;
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                bug6872503.createGui();
            }
        });
    }
}
