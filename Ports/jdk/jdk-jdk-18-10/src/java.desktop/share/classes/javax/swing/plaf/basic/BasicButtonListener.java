/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.basic;

import sun.swing.DefaultLookup;
import sun.swing.SwingUtilities2;
import sun.swing.UIAction;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.ComponentInputMapUIResource;

/**
 * Button Listener
 *
 * @author Jeff Dinkins
 * @author Arnaud Weber (keyboard UI support)
 */

public class BasicButtonListener implements MouseListener, MouseMotionListener,
                                   FocusListener, ChangeListener, PropertyChangeListener
{
    private long lastPressedTimestamp = -1;
    private boolean shouldDiscardRelease = false;

    /**
     * Populates Buttons actions.
     */
    static void loadActionMap(LazyActionMap map) {
        map.put(new Actions(Actions.PRESS));
        map.put(new Actions(Actions.RELEASE));
    }


    /**
     * Constructs a new instance of {@code BasicButtonListener}.
     *
     * @param b an abstract button
     */
    public BasicButtonListener(AbstractButton b) {
    }

    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();
        if(prop == AbstractButton.MNEMONIC_CHANGED_PROPERTY) {
            updateMnemonicBinding((AbstractButton)e.getSource());
        }
        else if(prop == AbstractButton.CONTENT_AREA_FILLED_CHANGED_PROPERTY) {
            checkOpacity((AbstractButton) e.getSource() );
        }
        else if(prop == AbstractButton.TEXT_CHANGED_PROPERTY || "font" == prop
                || "foreground" == prop || SwingUtilities2.isScaleChanged(e)) {
            AbstractButton b = (AbstractButton) e.getSource();
            BasicHTML.updateRenderer(b, b.getText());
        }
    }

    /**
     * Checks the opacity of the {@code AbstractButton}.
     *
     * @param b an abstract button
     */
    protected void checkOpacity(AbstractButton b) {
        b.setOpaque( b.isContentAreaFilled() );
    }

    /**
     * Register default key actions: pressing space to "click" a
     * button and registering the keyboard mnemonic (if any).
     *
     * @param c a component
     */
    public void installKeyboardActions(JComponent c) {
        AbstractButton b = (AbstractButton)c;
        // Update the mnemonic binding.
        updateMnemonicBinding(b);

        LazyActionMap.installLazyActionMap(c, BasicButtonListener.class,
                                           "Button.actionMap");

        InputMap km = getInputMap(JComponent.WHEN_FOCUSED, c);

        SwingUtilities.replaceUIInputMap(c, JComponent.WHEN_FOCUSED, km);
    }

    /**
     * Unregister default key actions.
     *
     * @param c a component
     */
    public void uninstallKeyboardActions(JComponent c) {
        SwingUtilities.replaceUIInputMap(c, JComponent.
                                         WHEN_IN_FOCUSED_WINDOW, null);
        SwingUtilities.replaceUIInputMap(c, JComponent.WHEN_FOCUSED, null);
        SwingUtilities.replaceUIActionMap(c, null);
    }

    /**
     * Returns the InputMap for condition <code>condition</code>. Called as
     * part of <code>installKeyboardActions</code>.
     */
    InputMap getInputMap(int condition, JComponent c) {
        if (condition == JComponent.WHEN_FOCUSED) {
            BasicButtonUI ui = (BasicButtonUI)BasicLookAndFeel.getUIOfType(
                         ((AbstractButton)c).getUI(), BasicButtonUI.class);
            if (ui != null) {
                return (InputMap)DefaultLookup.get(
                             c, ui, ui.getPropertyPrefix() + "focusInputMap");
            }
        }
        return null;
    }

    /**
     * Resets the binding for the mnemonic in the WHEN_IN_FOCUSED_WINDOW
     * UI InputMap.
     */
    void updateMnemonicBinding(AbstractButton b) {
        int m = b.getMnemonic();
        if(m != 0) {
            InputMap map = SwingUtilities.getUIInputMap(
                                b, JComponent.WHEN_IN_FOCUSED_WINDOW);

            if (map == null) {
                map = new ComponentInputMapUIResource(b);
                SwingUtilities.replaceUIInputMap(b,
                               JComponent.WHEN_IN_FOCUSED_WINDOW, map);
            }
            map.clear();
            map.put(KeyStroke.getKeyStroke(m, BasicLookAndFeel.getFocusAcceleratorKeyMask(), false),
                    "pressed");
            map.put(KeyStroke.getKeyStroke(m, SwingUtilities2.setAltGraphMask
                            (BasicLookAndFeel.getFocusAcceleratorKeyMask()),
                    false),
                    "pressed");

            map.put(KeyStroke.getKeyStroke(m, BasicLookAndFeel.getFocusAcceleratorKeyMask(), true),
                    "released");
            map.put(KeyStroke.getKeyStroke(m,
                    SwingUtilities2.setAltGraphMask
                            (BasicLookAndFeel.getFocusAcceleratorKeyMask()), true),
                    "released");
            map.put(KeyStroke.getKeyStroke(m, 0, true), "released");
        }
        else {
            InputMap map = SwingUtilities.getUIInputMap(b, JComponent.
                                             WHEN_IN_FOCUSED_WINDOW);
            if (map != null) {
                map.clear();
            }
        }
    }

    public void stateChanged(ChangeEvent e) {
        AbstractButton b = (AbstractButton) e.getSource();
        b.repaint();
    }

    public void focusGained(FocusEvent e) {
        AbstractButton b = (AbstractButton) e.getSource();
        if (b instanceof JButton && ((JButton)b).isDefaultCapable()) {
            JRootPane root = b.getRootPane();
            if (root != null) {
               BasicButtonUI ui = (BasicButtonUI)BasicLookAndFeel.getUIOfType(
                         b.getUI(), BasicButtonUI.class);
               if (ui != null && DefaultLookup.getBoolean(b, ui,
                                   ui.getPropertyPrefix() +
                                   "defaultButtonFollowsFocus", true)) {
                   root.putClientProperty("temporaryDefaultButton", b);
                   root.setDefaultButton((JButton)b);
                   root.putClientProperty("temporaryDefaultButton", null);
               }
            }
        }
        b.repaint();
    }

    public void focusLost(FocusEvent e) {
        AbstractButton b = (AbstractButton) e.getSource();
        JRootPane root = b.getRootPane();
        if (root != null) {
           JButton initialDefault = (JButton)root.getClientProperty("initialDefaultButton");
           if (b != initialDefault) {
               BasicButtonUI ui = (BasicButtonUI)BasicLookAndFeel.getUIOfType(
                         b.getUI(), BasicButtonUI.class);
               if (ui != null && DefaultLookup.getBoolean(b, ui,
                                   ui.getPropertyPrefix() +
                                   "defaultButtonFollowsFocus", true)) {
                   root.setDefaultButton(initialDefault);
               }
           }
        }

        ButtonModel model = b.getModel();
        model.setPressed(false);
        model.setArmed(false);
        b.repaint();
    }

    public void mouseMoved(MouseEvent e) {
    }


    public void mouseDragged(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
       if (SwingUtilities.isLeftMouseButton(e) ) {
          AbstractButton b = (AbstractButton) e.getSource();

          if(b.contains(e.getX(), e.getY())) {
              long lastTime = lastPressedTimestamp;
              lastPressedTimestamp = e.getWhen();
              long timeSinceLastClick = lastPressedTimestamp - lastTime;
              if (lastTime != -1 &&
                  timeSinceLastClick > 0 &&
                  timeSinceLastClick < b.getMultiClickThreshhold()) {

                  shouldDiscardRelease = true;
                  return;
              }

             ButtonModel model = b.getModel();
             if (!model.isEnabled()) {
                // Disabled buttons ignore all input...
                return;
             }
             if (!model.isArmed()) {
                // button not armed, should be
                model.setArmed(true);
             }
             model.setPressed(true);
             if(!b.hasFocus() && b.isRequestFocusEnabled()) {
                b.requestFocus();
             }
          }
       }
    }

    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            // Support for multiClickThreshhold
            if (shouldDiscardRelease) {
                shouldDiscardRelease = false;
                return;
            }
            AbstractButton b = (AbstractButton) e.getSource();
            ButtonModel model = b.getModel();
            model.setPressed(false);
            model.setArmed(false);
        }
    }

    public void mouseEntered(MouseEvent e) {
        AbstractButton b = (AbstractButton) e.getSource();
        ButtonModel model = b.getModel();
        if (b.isRolloverEnabled() && !SwingUtilities.isLeftMouseButton(e)) {
            model.setRollover(true);
        }
        if (model.isPressed())
                model.setArmed(true);
    }

    public void mouseExited(MouseEvent e) {
        AbstractButton b = (AbstractButton) e.getSource();
        ButtonModel model = b.getModel();
        if(b.isRolloverEnabled()) {
            model.setRollover(false);
        }
        model.setArmed(false);
    }


    /**
     * Actions for Buttons. Two types of action are supported:
     * pressed: Moves the button to a pressed state
     * released: Disarms the button.
     */
    private static class Actions extends UIAction {
        private static final String PRESS = "pressed";
        private static final String RELEASE = "released";

        Actions(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            AbstractButton b = (AbstractButton)e.getSource();
            String key = getName();
            if (key == PRESS) {
                ButtonModel model = b.getModel();
                model.setArmed(true);
                model.setPressed(true);
                if(!b.hasFocus()) {
                    b.requestFocus();
                }
            }
            else if (key == RELEASE) {
                ButtonModel model = b.getModel();
                model.setPressed(false);
                model.setArmed(false);
            }
        }

        @Override
        public boolean accept(Object sender) {
            return !((sender instanceof AbstractButton) &&
                    !((AbstractButton)sender).getModel().isEnabled());
        }
    }
}
