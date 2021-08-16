/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6418028
  @author oleg.sukhodolsky: area=awt.focus
  @library ../../regtesthelpers
  @modules java.desktop/java.awt.peer
           java.desktop/sun.awt
           java.desktop/java.awt:open
  @build Util
  @run main RequestOnCompWithNullParent1
*/

import java.awt.*;
import java.awt.event.*;
import java.awt.peer.ButtonPeer;
import java.awt.peer.ComponentPeer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import sun.awt.AWTAccessor;

public class RequestOnCompWithNullParent1 {

    public static void main(final String[] args) throws Exception {
        Frame frame = new Frame("test for 6418028");
        try {
            test(frame);
        } finally {
            frame.dispose();
        }
    }

    private static void test(final Frame frame) throws Exception {
        frame.setLayout(new FlowLayout());
        Button btn1 = new Button("Button1");
        frame.add(btn1);
        TestButton btn2 = new TestButton("Button2");
        frame.add(btn2);
        frame.pack();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                we.getWindow().dispose();
            }
        });
        frame.setVisible(true);

        new Robot().waitForIdle();

        btn2.instrumentPeer();
        btn2.requestFocusInWindow();
        btn2.restorePeer();
    }
}

class TestButton extends Button {
    ButtonPeer origPeer;
    ButtonPeer proxiedPeer;

    /** Creates a new instance of TestButton */
    TestButton(String text) {
        super(text);
    }

    public void instrumentPeer() {
        origPeer = AWTAccessor.getComponentAccessor().getPeer(this);

        InvocationHandler handler = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
                if (method.getName().equals("requestFocus")) {
                    Container parent = getParent();
                    parent.remove(TestButton.this);
                    System.err.println("parent = " + parent);
                    System.err.println("target = " + TestButton.this);
                    System.err.println("new parent = " + TestButton.this.getParent());
                }
                Object ret = null;
                try {
                    ret = method.invoke(origPeer, args);
                } catch (IllegalAccessException iae) {
                    throw new Error("Test error.", iae);
                } catch (InvocationTargetException ita) {
                    throw new Error("Test error.", ita);
                }
                return ret;
            }
        };

        proxiedPeer = (ButtonPeer) Proxy.newProxyInstance(
                                    ButtonPeer.class.getClassLoader(),
                                    new Class[] {ButtonPeer.class}, handler);
        setPeer(proxiedPeer);
    }

    private void setPeer(final ComponentPeer newPeer) {
        try {
            Field peer_field = Component.class.getDeclaredField("peer");
            peer_field.setAccessible(true);
            peer_field.set(this, newPeer);
        } catch (IllegalArgumentException ex) {
            throw new Error("Test error.", ex);
        } catch (SecurityException ex) {
            throw new Error("Test error.", ex);
        } catch (IllegalAccessException ex) {
            throw new Error("Test error.", ex);
        } catch (NoSuchFieldException ex) {
            throw new Error("Test error.", ex);
        }
    }

    public void restorePeer() {
        if (origPeer != null) {
            setPeer(origPeer);
            proxiedPeer = null;
        }
    }
}
