/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package org.netbeans.jemmy.explorer;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.TestOut;

/**
 * Auxiliary class to find an event sequence which should be posted to reproduce
 * user actions.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class TrialListenerManager implements Outputable {

    Component comp;
    TrialMouseListener mListener;
    TrialMouseMotionListener mmListener;
    TrialKeyListener kListener;
    TestOut output;

    /**
     * Contructor.
     *
     * @param comp Component to display event sequence for.
     */
    public TrialListenerManager(Component comp) {
        this.comp = comp;
        mListener = new TrialMouseListener();
        mmListener = new TrialMouseMotionListener();
        kListener = new TrialKeyListener();
        output = JemmyProperties.getCurrentOutput();
    }

    @Override
    public void setOutput(TestOut output) {
        this.output = output;
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    /**
     * Removes mouse listener.
     *
     * @see #addMouseListener
     */
    public void removeMouseListener() {
        comp.removeMouseListener(mListener);
    }

    /**
     * Adds mouse listener.
     *
     * @see #removeMouseListener
     */
    public void addMouseListener() {
        removeMouseListener();
        comp.addMouseListener(mListener);
    }

    /**
     * Removes mouse motion listener.
     *
     * @see #addMouseMotionListener
     */
    public void removeMouseMotionListener() {
        comp.removeMouseMotionListener(mmListener);
    }

    /**
     * Adds mouse motion listener.
     *
     * @see #removeMouseMotionListener
     */
    public void addMouseMotionListener() {
        removeMouseMotionListener();
        comp.addMouseMotionListener(mmListener);
    }

    /**
     * Removes key listener.
     *
     * @see #addKeyListener
     */
    public void removeKeyListener() {
        comp.removeKeyListener(kListener);
    }

    /**
     * Adds key listener.
     *
     * @see #removeKeyListener
     */
    public void addKeyListener() {
        removeKeyListener();
        comp.addKeyListener(kListener);
    }

    void printEvent(final AWTEvent event) {
        // if event != null run toString in dispatch thread
        String eventToString = new QueueTool().invokeSmoothly(
                new QueueTool.QueueAction<String>("event.toString()") {
            @Override
            public String launch() {
                return event.toString();
            }
        }
        );
        output.printLine(eventToString);
    }

    private class TrialMouseListener implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            printEvent(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            printEvent(e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            printEvent(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            printEvent(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            printEvent(e);
        }
    }

    private class TrialMouseMotionListener implements MouseMotionListener {

        @Override
        public void mouseDragged(MouseEvent e) {
            printEvent(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            printEvent(e);
        }
    }

    private class TrialKeyListener implements KeyListener {

        @Override
        public void keyPressed(KeyEvent e) {
            printEvent(e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            printEvent(e);
        }

        @Override
        public void keyTyped(KeyEvent e) {
            printEvent(e);
        }
    }
}
