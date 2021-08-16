/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.*;

/*
 * @test
 * @summary Check for AWTEventMulticaster working in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessAWTEventMulticaster
 */

public class HeadlessAWTEventMulticaster {
    class ComponentListenerImpl implements ComponentListener {
        public boolean hidden = false;
        public boolean moved = false;
        public boolean resized = false;
        public boolean shown = false;

        public void componentHidden(ComponentEvent e) {
            hidden = true;
        }

        public void componentMoved(ComponentEvent e) {
            moved = true;
        }

        public void componentResized(ComponentEvent e) {
            resized = true;
        }

        public void componentShown(ComponentEvent e) {
            shown = true;
        }
    }

    class ContainerListenerImpl implements ContainerListener {
        public boolean removed = false;
        public boolean added = false;

        public void componentAdded(ContainerEvent e) {
            added = true;
        }

        public void componentRemoved(ContainerEvent e) {
            removed = true;
        }
    }

    class FocusListenerImpl implements FocusListener {
        public boolean gained = false;
        public boolean lost = false;

        public void focusGained(FocusEvent e) {
            gained = true;
        }

        public void focusLost(FocusEvent e) {
            lost = true;
        }
    }

    class KeyListenerImpl implements KeyListener {
        public boolean pressed = false;
        public boolean released = false;
        public boolean typed = false;

        public void keyPressed(KeyEvent e) {
            pressed = true;
        }

        public void keyReleased(KeyEvent e) {
            released = true;
        }

        public void keyTyped(KeyEvent e) {
            typed = true;
        }
    }

    public static void main(String args[]) {
        new HeadlessAWTEventMulticaster().doTest();
    }

    void doTest() {
        ComponentListener compList;
        ComponentListenerImpl compListImpl;

        ContainerListener contList;
        ContainerListenerImpl contListImpl;

        FocusListener focList;
        FocusListenerImpl focListImpl;

        KeyListener keyList;
        KeyListenerImpl keyListImpl;

        Component component = new Component(){};

        // Component resized
        compListImpl = new ComponentListenerImpl();
        compList = AWTEventMulticaster.add(compListImpl, null);
        compList.componentResized(new ComponentEvent(component,
                ComponentEvent.COMPONENT_RESIZED));
        if (compListImpl.hidden || compListImpl.moved || compListImpl.shown) {
            throw new RuntimeException("Wrong id delivered: hidden || moved || shown");
        }
        if (!compListImpl.resized) {
            throw new RuntimeException("Expected id, resized, not delivered");
        }

        // Component moved
        compListImpl = new ComponentListenerImpl();
        compList = AWTEventMulticaster.add(compListImpl, null);
        compList.componentMoved(new ComponentEvent(component,
                ComponentEvent.COMPONENT_MOVED));
        if (compListImpl.hidden || compListImpl.resized || compListImpl.shown) {
            throw new RuntimeException("Wrong id delivered: hidden || resized || shown");
        }
        if (!compListImpl.moved) {
            throw new RuntimeException("Expected id, moved, not delivered");
        }

        // Component shown
        compListImpl = new ComponentListenerImpl();
        compList = AWTEventMulticaster.add(compListImpl, null);
        compList.componentShown(new ComponentEvent(component,
                ComponentEvent.COMPONENT_SHOWN));
        if (compListImpl.hidden || compListImpl.resized || compListImpl.moved) {
            throw new RuntimeException("Wrong id delivered: hidden || resized || moved");
        }
        if (!compListImpl.shown) {
            throw new RuntimeException("Expected id, shown, not delivered");
        }

        // Component hidden
        compListImpl = new ComponentListenerImpl();
        compList = AWTEventMulticaster.add(compListImpl, null);
        compList.componentHidden(new ComponentEvent(component,
                ComponentEvent.COMPONENT_HIDDEN));
        if (compListImpl.shown || compListImpl.resized || compListImpl.moved) {
            throw new RuntimeException("Wrong id delivered: shown || resized || moved");
        }
        if (!compListImpl.hidden) {
            throw new RuntimeException("Expected id, hidden, not delivered");
        }

        // Component added
        contListImpl = new ContainerListenerImpl();
        contList = AWTEventMulticaster.add(contListImpl, null);
        contList.componentAdded(new ContainerEvent(component,
                ContainerEvent.COMPONENT_ADDED, component));
        if (contListImpl.removed) {
            throw new RuntimeException("Wrong id delivered: removed");
        }
        if (!contListImpl.added) {
            throw new RuntimeException("Expected id, added, not delivered");
        }

        // Component removed
        contListImpl = new ContainerListenerImpl();
        contList = AWTEventMulticaster.add(contListImpl, null);
        contList.componentRemoved(new ContainerEvent(component,
                ContainerEvent.COMPONENT_REMOVED, component));
        if (contListImpl.added) {
            throw new RuntimeException("Wrong id delivered: added");
        }
        if (!contListImpl.removed) {
            throw new RuntimeException("Expected id, removed, not delivered");
        }

        // Focus gained
        focListImpl = new FocusListenerImpl();
        focList = AWTEventMulticaster.add(focListImpl, null);
        focList.focusGained(new FocusEvent(component, FocusEvent.FOCUS_GAINED));
        if (focListImpl.lost) {
            throw new RuntimeException("Wrong id delivered: lost");
        }
        if (!focListImpl.gained) {
            throw new RuntimeException("Expected id, gained, not delivered");
        }

        // Focus lost
        focListImpl = new FocusListenerImpl();
        focList = AWTEventMulticaster.add(focListImpl, null);
        focList.focusLost(new FocusEvent(component, FocusEvent.FOCUS_LOST));
        if (focListImpl.gained) {
            throw new RuntimeException("Wrong id delivered: gained");
        }
        if (!focListImpl.lost) {
            throw new RuntimeException("Expected id, lost, not delivered");
        }

        // Key typed
        keyListImpl = new KeyListenerImpl();
        keyList = AWTEventMulticaster.add(keyListImpl, null);
        keyList.keyTyped(new KeyEvent(component,
                KeyEvent.KEY_TYPED, 0L, 0, 0));
        if (keyListImpl.pressed || keyListImpl.released)
            throw new RuntimeException("Wrong id delivered: pressed || released");

        if (!keyListImpl.typed)
            throw new RuntimeException("Expected id, typed, not delivered");

        // Key pressed
        keyListImpl = new KeyListenerImpl();
        keyList = AWTEventMulticaster.add(keyListImpl, null);
        keyList.keyPressed(new KeyEvent(component,
                KeyEvent.KEY_PRESSED, 0L, 0, 0));
        if (keyListImpl.typed || keyListImpl.released)
            throw new RuntimeException("Wrong id delivered: typed || released");

        if (!keyListImpl.pressed)
            throw new RuntimeException("Expected id, pressed, not delivered");

        // Key released
        keyListImpl = new KeyListenerImpl();
        keyList = AWTEventMulticaster.add(keyListImpl, null);
        keyList.keyReleased(new KeyEvent(component,
                KeyEvent.KEY_RELEASED, 0L, 0, 0));
        if (keyListImpl.pressed || keyListImpl.typed)
            throw new RuntimeException("Wrong id delivered: pressed || typed");

        if (!keyListImpl.released)
            throw new RuntimeException("Expected id, released, not delivered");
    }
}
