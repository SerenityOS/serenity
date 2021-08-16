/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.metal;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Polygon;

import javax.swing.*;

import javax.swing.plaf.basic.BasicArrowButton;


/**
 * JButton object for Metal scrollbar arrows.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Tom Santos
 * @author Steve Wilson
 */
@SuppressWarnings("serial") // Same-version serialization only
public class MetalScrollButton extends BasicArrowButton
{
  private static Color shadowColor;
  private static Color highlightColor;
  private boolean isFreeStanding = false;

  private int buttonWidth;

        /**
         * Constructs an instance of {@code MetalScrollButton}.
         *
         * @param direction the direction
         * @param width the width
         * @param freeStanding the free standing value
         */
        public MetalScrollButton( int direction, int width, boolean freeStanding )
        {
            super( direction );

            shadowColor = UIManager.getColor("ScrollBar.darkShadow");
            highlightColor = UIManager.getColor("ScrollBar.highlight");

            buttonWidth = width;
            isFreeStanding = freeStanding;
        }

        /**
         * Sets the free standing value.
         *
         * @param freeStanding the free standing value
         */
        public void setFreeStanding( boolean freeStanding )
        {
            isFreeStanding = freeStanding;
        }

        public void paint( Graphics g )
        {
            boolean leftToRight = MetalUtils.isLeftToRight(this);
            boolean isEnabled = getParent().isEnabled();

            Color arrowColor = isEnabled ? MetalLookAndFeel.getControlInfo() : MetalLookAndFeel.getControlDisabled();
            boolean isPressed = getModel().isPressed();
            int width = getWidth();
            int height = getHeight();
            int w = width;
            int h = height;
            int arrowHeight = (height+1) / 4;
            int arrowWidth = (height+1) / 2;

            if ( isPressed )
            {
                g.setColor( MetalLookAndFeel.getControlShadow() );
            }
            else
            {
                g.setColor( getBackground() );
            }

            g.fillRect( 0, 0, width, height );

            if ( getDirection() == NORTH )
            {
                if ( !isFreeStanding ) {
                    height +=1;
                    g.translate( 0, -1 );
                    width += 2;
                    if ( !leftToRight ) {
                        g.translate( -1, 0 );
                    }
                }

                // Draw the arrow
                g.setColor( arrowColor );
                int startY = ((h+1) - arrowHeight) / 2;
                int startX = (w / 2);

                g.translate(startX, startY);
                g.fillPolygon(new int[]{0, 1, arrowHeight + 1, -arrowHeight},
                              new int[]{0, 0, arrowHeight, arrowHeight}, 4);
                g.translate(-startX, -startY);

                if (isEnabled) {
                    g.setColor( highlightColor );

                    if ( !isPressed )
                    {
                        g.drawLine( 1, 1, width - 3, 1 );
                        g.drawLine( 1, 1, 1, height - 1 );
                    }

                    g.drawLine( width - 1, 1, width - 1, height - 1 );

                    g.setColor( shadowColor );
                    g.drawLine( 0, 0, width - 2, 0 );
                    g.drawLine( 0, 0, 0, height - 1 );
                    g.drawLine( width - 2, 2, width - 2, height - 1 );
                } else {
                    MetalUtils.drawDisabledBorder(g, 0, 0, width, height+1);
                }
                if ( !isFreeStanding ) {
                    height -= 1;
                    g.translate( 0, 1 );
                    width -= 2;
                    if ( !leftToRight ) {
                        g.translate( 1, 0 );
                    }
                }
            }
            else if ( getDirection() == SOUTH )
            {
                if ( !isFreeStanding ) {
                    height += 1;
                    width += 2;
                    if ( !leftToRight ) {
                        g.translate( -1, 0 );
                    }
                }

                // Draw the arrow
                g.setColor( arrowColor );

                int startY = (((h+1) - arrowHeight) / 2)+ arrowHeight-1;
                int startX = (w / 2);
                g.translate(startX, startY);
                g.fillPolygon(new int[]{0, 1, arrowHeight + 1, -arrowHeight},
                              new int[]{0, 0, -arrowHeight, -arrowHeight}, 4);
                g.translate(-startX, -startY);

                if (isEnabled) {
                    g.setColor( highlightColor );

                    if ( !isPressed )
                    {
                        g.drawLine( 1, 0, width - 3, 0 );
                        g.drawLine( 1, 0, 1, height - 3 );
                    }

                    g.drawLine( 1, height - 1, width - 1, height - 1 );
                    g.drawLine( width - 1, 0, width - 1, height - 1 );

                    g.setColor( shadowColor );
                    g.drawLine( 0, 0, 0, height - 2 );
                    g.drawLine( width - 2, 0, width - 2, height - 2 );
                    g.drawLine( 2, height - 2, width - 2, height - 2 );
                } else {
                    MetalUtils.drawDisabledBorder(g, 0,-1, width, height+1);
                }

                if ( !isFreeStanding ) {
                    height -= 1;
                    width -= 2;
                    if ( !leftToRight ) {
                        g.translate( 1, 0 );
                    }
                }
            }
            else if ( getDirection() == EAST )
            {
                if ( !isFreeStanding ) {
                    height += 2;
                    width += 1;
                }

                // Draw the arrow
                g.setColor( arrowColor );

                int startX = (((w+1) - arrowHeight) / 2) + arrowHeight-1;
                int startY = (h / 2);

                g.translate(startX, startY);
                g.fillPolygon(new int[]{0, 0, -arrowHeight, -arrowHeight},
                              new int[]{0, 1, arrowHeight + 1, -arrowHeight}, 4);
                g.translate(-startX, -startY);

                if (isEnabled) {
                    g.setColor( highlightColor );

                    if ( !isPressed )
                    {
                        g.drawLine( 0, 1, width - 3, 1 );
                        g.drawLine( 0, 1, 0, height - 3 );
                    }

                    g.drawLine( width - 1, 1, width - 1, height - 1 );
                    g.drawLine( 0, height - 1, width - 1, height - 1 );

                    g.setColor( shadowColor );
                    g.drawLine( 0, 0,width - 2, 0 );
                    g.drawLine( width - 2, 2, width - 2, height - 2 );
                    g.drawLine( 0, height - 2, width - 2, height - 2 );
                } else {
                    MetalUtils.drawDisabledBorder(g,-1,0, width+1, height);
                }
                if ( !isFreeStanding ) {
                    height -= 2;
                    width -= 1;
                }
            }
            else if ( getDirection() == WEST )
            {
                if ( !isFreeStanding ) {
                    height += 2;
                    width += 1;
                    g.translate( -1, 0 );
                }

                // Draw the arrow
                g.setColor( arrowColor );

                int startX = (((w+1) - arrowHeight) / 2);
                int startY = (h / 2);

                g.translate(startX, startY);
                g.fillPolygon(new int[]{0, 0, arrowHeight, arrowHeight},
                              new int[]{0, 1, arrowHeight + 1, -arrowHeight}, 4);
                g.translate(-startX, -startY);

                if (isEnabled) {
                    g.setColor( highlightColor );


                    if ( !isPressed )
                    {
                        g.drawLine( 1, 1, width - 1, 1 );
                        g.drawLine( 1, 1, 1, height - 3 );
                    }

                    g.drawLine( 1, height - 1, width - 1, height - 1 );

                    g.setColor( shadowColor );
                    g.drawLine( 0, 0, width - 1, 0 );
                    g.drawLine( 0, 0, 0, height - 2 );
                    g.drawLine( 2, height - 2, width - 1, height - 2 );
                } else {
                    MetalUtils.drawDisabledBorder(g,0,0, width+1, height);
                }

                if ( !isFreeStanding ) {
                    height -= 2;
                    width -= 1;
                    g.translate( 1, 0 );
                }
            }
        }

        public Dimension getPreferredSize()
        {
            if ( getDirection() == NORTH )
            {
                return new Dimension( buttonWidth, buttonWidth - 2 );
            }
            else if ( getDirection() == SOUTH )
            {
                return new Dimension( buttonWidth, buttonWidth - (isFreeStanding ? 1 : 2) );
            }
            else if ( getDirection() == EAST )
            {
                return new Dimension( buttonWidth - (isFreeStanding ? 1 : 2), buttonWidth );
            }
            else if ( getDirection() == WEST )
            {
                return new Dimension( buttonWidth - 2, buttonWidth );
            }
            else
            {
                return new Dimension( 0, 0 );
            }
        }

        public Dimension getMinimumSize()
        {
            return getPreferredSize();
        }

        public Dimension getMaximumSize()
        {
            return new Dimension( Integer.MAX_VALUE, Integer.MAX_VALUE );
        }

        /**
         * Returns the width of the button.
         *
         * @return the width of the button
         */
        public int getButtonWidth() {
            return buttonWidth;
        }
}
