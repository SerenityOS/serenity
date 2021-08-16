/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
package sun.awt.X11;

import java.awt.*;
import java.awt.peer.*;
import java.awt.event.MouseEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import javax.swing.plaf.basic.*;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
public class XButtonPeer extends XComponentPeer implements ButtonPeer {
    private boolean pressed;
    private boolean armed;
    private Insets focusInsets;
    private Insets borderInsets;
    private Insets contentAreaInsets;

    private static final String propertyPrefix = "Button" + ".";
    protected Color focusColor =  SystemColor.windowText;

    private boolean disposed = false;

    String label;

    protected String getPropertyPrefix() {
        return propertyPrefix;
    }

    void preInit(XCreateWindowParams params) {
        super.preInit(params);
        borderInsets = new Insets(2,2,2,2);
        focusInsets = new Insets(0,0,0,0);
        contentAreaInsets = new Insets(3,3,3,3);
    }


    public  XButtonPeer(Button target) {
        super(target);
        pressed = false;
        armed = false;
        label = target.getLabel();
        updateMotifColors(getPeerBackground());
    }

    public  void dispose() {
        synchronized (target)
        {
            disposed = true;
        }
        super.dispose();
    }

    public boolean isFocusable() {
        return true;
    }

    @Override
    public void setLabel(String label) {
        if (label == null) {
            label = "";
        }
        if (!label.equals(this.label)) {
            this.label = label;
            repaint();
        }
    }

    public void setBackground(Color c) {
        updateMotifColors(c);
        super.setBackground(c);
    }

    void handleJavaMouseEvent(MouseEvent e) {
        super.handleJavaMouseEvent(e);
        int id = e.getID();
        switch (id) {
          case MouseEvent.MOUSE_PRESSED:
              if (XToolkit.isLeftMouseButton(e) ) {
                  Button b = (Button) e.getSource();

                  if(b.contains(e.getX(), e.getY())) {
                      if (!isEnabled()) {
                          // Disabled buttons ignore all input...
                          return;
                      }
                      pressed = true;
                      armed = true;
                      repaint();
                  }
              }

              break;

          case MouseEvent.MOUSE_RELEASED:
              if (XToolkit.isLeftMouseButton(e)) {
                  if (armed)
                  {
                      @SuppressWarnings("deprecation")
                      final int modifiers = e.getModifiers();
                      action(e.getWhen(), modifiers);
                  }
                  pressed = false;
                  armed = false;
                  repaint();
              }

              break;

          case  MouseEvent.MOUSE_ENTERED:
              if (pressed)
                  armed = true;
              break;
          case MouseEvent.MOUSE_EXITED:
              armed = false;
              break;
        }
    }


    // NOTE: This method is called by privileged threads.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    public void action(final long when, final int modifiers) {
        postEvent(new ActionEvent(target, ActionEvent.ACTION_PERFORMED,
                                  ((Button)target).getActionCommand(),
                                  when, modifiers));
    }


    public void focusGained(FocusEvent e) {
        super.focusGained(e);
        repaint();
    }

    public void focusLost(FocusEvent e) {
        super.focusLost(e);
        repaint();
    }

    void handleJavaKeyEvent(KeyEvent e) {
        int id = e.getID();
        switch (id) {
          case KeyEvent.KEY_PRESSED:
              if (e.getKeyCode() == KeyEvent.VK_SPACE)
              {
                  pressed=true;
                  armed=true;
                  repaint();
                  @SuppressWarnings("deprecation")
                  final int modifiers = e.getModifiers();
                  action(e.getWhen(), modifiers);
              }

              break;

          case KeyEvent.KEY_RELEASED:
              if (e.getKeyCode() == KeyEvent.VK_SPACE)
              {
                  pressed = false;
                  armed = false;
                  repaint();
              }

              break;


        }
    }

    public Dimension getMinimumSize() {
        FontMetrics fm = getFontMetrics(getPeerFont());
        if ( label == null ) {
            label = "";
        }
        return new Dimension(fm.stringWidth(label) + 14,
                             fm.getHeight() + 8);
    }

    /**
     * This method is called from Toolkit Thread and so it should not call any
     * client code.
     */
    @Override
    void paintPeer(final Graphics g) {
        if (!disposed) {
            Dimension size = getPeerSize();
            g.setColor( getPeerBackground() );   /* erase the existing button remains */
            g.fillRect(0,0, size.width , size.height);
            paintBorder(g,borderInsets.left,
                        borderInsets.top,
                        size.width-(borderInsets.left+borderInsets.right),
                        size.height-(borderInsets.top+borderInsets.bottom));

            FontMetrics fm = g.getFontMetrics();

            Rectangle textRect,iconRect,viewRect;

            textRect = new Rectangle();
            viewRect = new Rectangle();
            iconRect = new Rectangle();


            viewRect.width = size.width - (contentAreaInsets.left+contentAreaInsets.right);
            viewRect.height = size.height - (contentAreaInsets.top+contentAreaInsets.bottom);
            viewRect.x = contentAreaInsets.left;
            viewRect.y = contentAreaInsets.top;
            String llabel = (label != null) ? label : "";
            // layout the text and icon
            String text = SwingUtilities.layoutCompoundLabel(
                                                             fm, llabel, null,
                                                             SwingConstants.CENTER, SwingConstants.CENTER,
                                                             SwingConstants.CENTER, SwingConstants.CENTER,
                                                             viewRect, iconRect, textRect, 0);

            Font f = getPeerFont();

            g.setFont(f);

            // perform UI specific press action, e.g. Windows L&F shifts text
            if (pressed && armed) {
                paintButtonPressed(g,target);
            }

            paintText(g, target, textRect, text);

            if (hasFocus()) {
                // paint UI specific focus
                paintFocus(g,focusInsets.left,
                           focusInsets.top,
                           size.width-(focusInsets.left+focusInsets.right)-1,
                           size.height-(focusInsets.top+focusInsets.bottom)-1);
            }
        }
        flush();
    }

    public void paintBorder(Graphics g, int x, int y, int w, int h) {
        drawMotif3DRect(g, x, y, w-1, h-1, pressed);
    }

    protected void paintFocus(Graphics g, int x, int y, int w, int h){
        g.setColor(focusColor);
        g.drawRect(x,y,w,h);
    }

    protected void paintButtonPressed(Graphics g, Component b) {
        Dimension size = getPeerSize();
        g.setColor(selectColor);
        g.fillRect(contentAreaInsets.left,
                   contentAreaInsets.top,
                   size.width-(contentAreaInsets.left+contentAreaInsets.right),
                   size.height-(contentAreaInsets.top+contentAreaInsets.bottom));

    }
    protected void paintText(Graphics g, Component c, Rectangle textRect, String text) {
        FontMetrics fm = g.getFontMetrics();

        int mnemonicIndex = -1;

        /* Draw the Text */
        if(isEnabled()) {
            /*** paint the text normally */
            g.setColor(getPeerForeground());
            BasicGraphicsUtils.drawStringUnderlineCharAt(g,text,mnemonicIndex , textRect.x , textRect.y + fm.getAscent() );
        }
        else {
            /*** paint the text disabled ***/
            g.setColor(getPeerBackground().brighter());
            BasicGraphicsUtils.drawStringUnderlineCharAt(g,text, mnemonicIndex,
                                                         textRect.x, textRect.y + fm.getAscent());
            g.setColor(getPeerBackground().darker());
            BasicGraphicsUtils.drawStringUnderlineCharAt(g,text, mnemonicIndex,
                                                         textRect.x - 1, textRect.y + fm.getAscent() - 1);
        }
    }
}
