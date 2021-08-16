/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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

package com.sun.java.swing.plaf.motif;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicInternalFrameUI;

/**
 * A Motif {@literal L&F} implementation of InternalFrame.
 *
 * @author Tom Ball
 */
public class MotifInternalFrameUI extends BasicInternalFrameUI {

    Color color;
    Color highlight;
    Color shadow;
    MotifInternalFrameTitlePane titlePane;

    /**
     * As of Java 2 platform v1.3 this previously undocumented field is no
     * longer used.
     * Key bindings are now defined by the LookAndFeel, please refer to
     * the key bindings specification for further details.
     *
     * @deprecated As of Java 2 platform v1.3.
     */
    @Deprecated
    protected KeyStroke closeMenuKey;


/////////////////////////////////////////////////////////////////////////////
// ComponentUI Interface Implementation methods
/////////////////////////////////////////////////////////////////////////////
    public static ComponentUI createUI(JComponent w)    {
        return new MotifInternalFrameUI((JInternalFrame)w);
    }

    public MotifInternalFrameUI(JInternalFrame w)   {
        super(w);
    }

    public void installUI(JComponent c)   {
        super.installUI(c);
        setColors((JInternalFrame)c);
    }

    protected void installDefaults() {
        Border frameBorder = frame.getBorder();
        frame.setLayout(internalFrameLayout = createLayoutManager());
        if (frameBorder == null || frameBorder instanceof UIResource) {
            frame.setBorder(new MotifBorders.InternalFrameBorder(frame));
        }
    }


    protected void installKeyboardActions(){
      super.installKeyboardActions();
      // We replace the
      // we use JPopup in our TitlePane so need escape support
      closeMenuKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    }


    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(frame);
        frame.setLayout(null);
        internalFrameLayout = null;
    }

    private JInternalFrame getFrame(){
      return frame;
    }

    public JComponent createNorthPane(JInternalFrame w) {
        titlePane = new MotifInternalFrameTitlePane(w);
        return titlePane;
    }

    public Dimension getMaximumSize(JComponent x) {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }

    protected void uninstallKeyboardActions(){
      super.uninstallKeyboardActions();
      if (isKeyBindingRegistered()){
        JInternalFrame.JDesktopIcon di = frame.getDesktopIcon();
        SwingUtilities.replaceUIActionMap(di, null);
        SwingUtilities.replaceUIInputMap(di, JComponent.WHEN_IN_FOCUSED_WINDOW,
                                         null);
      }
    }

    @SuppressWarnings("serial") // anonymous class
    protected void setupMenuOpenKey(){
        super.setupMenuOpenKey();
        ActionMap map = SwingUtilities.getUIActionMap(frame);
        if (map != null) {
            // BasicInternalFrameUI creates an action with the same name, we override
            // it as MotifInternalFrameTitlePane has a titlePane ivar that shadows the
            // titlePane ivar in BasicInternalFrameUI, making supers action throw
            // an NPE for us.
            map.put("showSystemMenu", new AbstractAction(){
                public void actionPerformed(ActionEvent e){
                    titlePane.showSystemMenu();
                }
                public boolean isEnabled(){
                    return isKeyBindingActive();
                }
            });
        }
    }

    @SuppressWarnings("serial") // anonymous class
    protected void setupMenuCloseKey(){
        ActionMap map = SwingUtilities.getUIActionMap(frame);
        if (map != null) {
            map.put("hideSystemMenu", new AbstractAction(){
                public void actionPerformed(ActionEvent e){
                    titlePane.hideSystemMenu();
                }
                public boolean isEnabled(){
                    return isKeyBindingActive();
                }
            });
        }

        // Set up the bindings for the DesktopIcon, it is odd that
        // we install them, and not the desktop icon.
        JInternalFrame.JDesktopIcon di = frame.getDesktopIcon();
        InputMap diInputMap = SwingUtilities.getUIInputMap
                          (di, JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (diInputMap == null) {
            Object[] bindings = (Object[])UIManager.get
                                          ("DesktopIcon.windowBindings");
            if (bindings != null) {
                diInputMap = LookAndFeel.makeComponentInputMap(di, bindings);

                SwingUtilities.replaceUIInputMap(di, JComponent.
                                               WHEN_IN_FOCUSED_WINDOW,
                                               diInputMap);
            }
        }
        ActionMap diActionMap = SwingUtilities.getUIActionMap(di);
        if (diActionMap == null) {
            diActionMap = new ActionMapUIResource();
            diActionMap.put("hideSystemMenu", new AbstractAction(){
                public void actionPerformed(ActionEvent e){
                    JInternalFrame.JDesktopIcon icon = getFrame().
                                     getDesktopIcon();
                    MotifDesktopIconUI micon = (MotifDesktopIconUI)icon.
                                               getUI();
                    micon.hideSystemMenu();
                }
                public boolean isEnabled(){
                    return isKeyBindingActive();
                }
            });
            SwingUtilities.replaceUIActionMap(di, diActionMap);
        }
    }

    /** This method is called when the frame becomes selected.
      */
    protected void activateFrame(JInternalFrame f) {
        super.activateFrame(f);
        setColors(f);
    }
    /** This method is called when the frame is no longer selected.
      */
    protected void deactivateFrame(JInternalFrame f) {
        setColors(f);
        super.deactivateFrame(f);
    }

    void setColors(JInternalFrame frame) {
        if (frame.isSelected()) {
            color = UIManager.getColor("InternalFrame.activeTitleBackground");
        } else {
            color = UIManager.getColor("InternalFrame.inactiveTitleBackground");
        }
        highlight = color.brighter();
        shadow = color.darker().darker();
        titlePane.setColors(color, highlight, shadow);
    }
}
