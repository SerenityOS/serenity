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

package com.sun.java.swing.plaf.windows;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;

import static com.sun.java.swing.plaf.windows.TMSchema.Part;
import static com.sun.java.swing.plaf.windows.TMSchema.Prop;
import static com.sun.java.swing.plaf.windows.XPStyle.Skin;

/**
 * Windows rendition of the component.
 *
 * @author Michael C. Albers
 */
public class WindowsProgressBarUI extends BasicProgressBarUI
{

    private Rectangle previousFullBox;
    private Insets indeterminateInsets;

    public static ComponentUI createUI(JComponent x) {
        return new WindowsProgressBarUI();
    }


    protected void installDefaults() {
        super.installDefaults();

        if (XPStyle.getXP() != null) {
            LookAndFeel.installProperty(progressBar, "opaque", Boolean.FALSE);
            progressBar.setBorder(null);
            indeterminateInsets = UIManager.getInsets("ProgressBar.indeterminateInsets");
        }
    }

    /**
     * Returns the baseline.
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @see javax.swing.JComponent#getBaseline(int, int)
     * @since 1.6
     */
    public int getBaseline(JComponent c, int width, int height) {
        int baseline = super.getBaseline(c, width, height);
        if (XPStyle.getXP() != null && progressBar.isStringPainted() &&
                progressBar.getOrientation() == JProgressBar.HORIZONTAL) {
            FontMetrics metrics = progressBar.
                    getFontMetrics(progressBar.getFont());
            int y = progressBar.getInsets().top;
            if (progressBar.isIndeterminate()) {
                y = -1;
                height--;
            }
            else {
                y = 0;
                height -= 3;
            }
            baseline = y + (height + metrics.getAscent() -
                        metrics.getLeading() -
                        metrics.getDescent()) / 2;
        }
        return baseline;
    }

    protected Dimension getPreferredInnerHorizontal() {
        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
             Skin skin = xp.getSkin(progressBar, Part.PP_BAR);
             return new Dimension(
                     (int)super.getPreferredInnerHorizontal().getWidth(),
                     skin.getHeight());
         }
         return super.getPreferredInnerHorizontal();
    }

    protected Dimension getPreferredInnerVertical() {
         XPStyle xp = XPStyle.getXP();
         if (xp != null) {
             Skin skin = xp.getSkin(progressBar, Part.PP_BARVERT);
             return new Dimension(
                     skin.getWidth(),
                     (int)super.getPreferredInnerVertical().getHeight());
         }
         return super.getPreferredInnerVertical();
    }

    protected void paintDeterminate(Graphics g, JComponent c) {
        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            boolean vertical = (progressBar.getOrientation() == JProgressBar.VERTICAL);
            boolean isLeftToRight = WindowsGraphicsUtils.isLeftToRight(c);
            int barRectWidth = progressBar.getWidth();
            int barRectHeight = progressBar.getHeight()-1;
            // amount of progress to draw
            int amountFull = getAmountFull(null, barRectWidth, barRectHeight);

            paintXPBackground(g, vertical, barRectWidth, barRectHeight);
            // Paint progress
            if (progressBar.isStringPainted()) {
                // Do not paint the standard stripes from the skin, because they obscure
                // the text
                g.setColor(progressBar.getForeground());
                barRectHeight -= 2;
                barRectWidth -= 2;

                if (barRectWidth <= 0 || barRectHeight <= 0) {
                    return;
                }

                Graphics2D g2 = (Graphics2D)g;
                g2.setStroke(new BasicStroke((float)(vertical ? barRectWidth : barRectHeight),
                                             BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
                if (!vertical) {
                    if (isLeftToRight) {
                        g2.drawLine(2,              barRectHeight / 2 + 1,
                                    amountFull - 2, barRectHeight / 2 + 1);
                    } else {
                        g2.drawLine(2 + barRectWidth,
                                    barRectHeight / 2 + 1,
                                    2 + barRectWidth - (amountFull - 2),
                                    barRectHeight / 2 + 1);
                    }
                    paintString(g, 0, 0, barRectWidth, barRectHeight, amountFull, null);
                } else {
                    g2.drawLine(barRectWidth/2 + 1, barRectHeight + 1,
                                barRectWidth/2 + 1, barRectHeight + 1 - amountFull + 2);
                    paintString(g, 2, 2, barRectWidth, barRectHeight, amountFull, null);
                }

            } else {
                Skin skin = xp.getSkin(progressBar, vertical ? Part.PP_CHUNKVERT : Part.PP_CHUNK);
                int thickness;
                if (vertical) {
                    thickness = barRectWidth - 5;
                } else {
                    thickness = barRectHeight - 5;
                }

                int chunkSize = xp.getInt(progressBar, Part.PP_PROGRESS, null, Prop.PROGRESSCHUNKSIZE, 2);
                int spaceSize = xp.getInt(progressBar, Part.PP_PROGRESS, null, Prop.PROGRESSSPACESIZE, 0);
                int nChunks = (amountFull-4) / (chunkSize + spaceSize);

                // See if we can squeeze in an extra chunk without spacing after
                if (spaceSize > 0 && (nChunks * (chunkSize + spaceSize) + chunkSize) < (amountFull-4)) {
                    nChunks++;
                }

                for (int i = 0; i < nChunks; i++) {
                    if (vertical) {
                        skin.paintSkin(g,
                                       3, barRectHeight - i * (chunkSize + spaceSize) - chunkSize - 2,
                                       thickness, chunkSize, null);
                    } else {
                        if (isLeftToRight) {
                            skin.paintSkin(g,
                                           4 + i * (chunkSize + spaceSize), 2,
                                           chunkSize, thickness, null);
                        } else {
                            skin.paintSkin(g,
                                           barRectWidth - (2 + (i+1) * (chunkSize + spaceSize)), 2,
                                           chunkSize, thickness, null);
                        }
                    }
                }
            }
        } else {
            super.paintDeterminate(g, c);
        }
    }


    /**
     * {@inheritDoc}
     * @since 1.6
     */
    protected void setAnimationIndex(int newValue) {
        super.setAnimationIndex(newValue);
        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            if (boxRect != null) {
                // get the full repaint area and add it the
                // previous one so we can erase it
                Rectangle chunk = getFullChunkBounds(boxRect);
                if (previousFullBox != null) {
                    chunk.add(previousFullBox);
                }
                progressBar.repaint(chunk);
            } else {
                progressBar.repaint();
            }
        }
    }


    /**
     * {@inheritDoc}
     * @since 1.6
     */
    protected int getBoxLength(int availableLength, int otherDimension) {
        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            return 6; // an apparently hard coded value in Windows
        }
        return super.getBoxLength(availableLength, otherDimension);
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    protected Rectangle getBox(Rectangle r) {
        Rectangle rect = super.getBox(r);

        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            boolean vertical = (progressBar.getOrientation()
                                 == JProgressBar.VERTICAL);
            Part part = vertical ? Part.PP_BARVERT : Part.PP_BAR;
            Insets ins = indeterminateInsets;

            int currentFrame = getAnimationIndex();
            int framecount = getFrameCount()/2;

            int gap = xp.getInt(progressBar, Part.PP_PROGRESS, null,
                    Prop.PROGRESSSPACESIZE, 0);
            currentFrame = currentFrame % framecount;

            // this code adjusts the chunk size to properly account for the
            // size and gap specified in the XP style. It also does it's own
            // box placement for the chunk animation. This is required because
            // the inherited algorithm from BasicProgressBarUI goes back and
            // forth whereas XP only goes in one direction. XP also has ghosted
            // trailing chunks to create the illusion of speed. This code
            // adjusts the pixel length of the animation to account for the
            // trails.
            if (!vertical) {
                rect.y = rect.y + ins.top;
                rect.height = progressBar.getHeight() - ins.top - ins.bottom;
                int len = progressBar.getWidth() - ins.left - ins.right;
                len += (rect.width+gap)*2; // add 2x for the trails
                double delta = (double)(len) / (double)framecount;
                rect.x = (int)(delta * currentFrame) + ins.left;
            } else {
                rect.x = rect.x + ins.left;
                rect.width = progressBar.getWidth() - ins.left - ins.right;
                int len = progressBar.getHeight() - ins.top - ins.bottom;
                len += (rect.height+gap)*2; // add 2x for the trails
                double delta = (double)(len) / (double)framecount;
                rect.y = (int)(delta * currentFrame) + ins.top;
            }
        }
        return rect;
    }


    protected void paintIndeterminate(Graphics g, JComponent c) {
        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            boolean vertical = (progressBar.getOrientation()
                                 == JProgressBar.VERTICAL);
            int barRectWidth = progressBar.getWidth();
            int barRectHeight = progressBar.getHeight();
            paintXPBackground(g, vertical, barRectWidth, barRectHeight);

            // Paint the bouncing box.
            boxRect = getBox(boxRect);
            if (boxRect != null) {
                g.setColor(progressBar.getForeground());
                if (!(g instanceof Graphics2D)) {
                    return;
                }
                paintIndeterminateFrame(boxRect, (Graphics2D)g, vertical,
                                        barRectWidth, barRectHeight);
                if (progressBar.isStringPainted()) {
                    if (!vertical) {
                        paintString(g, -1, -1, barRectWidth, barRectHeight, 0, null);
                    } else {
                        paintString(g, 1, 1, barRectWidth, barRectHeight, 0, null);
                    }
                }
            }
        } else {
            super.paintIndeterminate(g, c);
        }
    }

    private Rectangle getFullChunkBounds(Rectangle box) {
        boolean vertical = (progressBar.getOrientation() == JProgressBar.VERTICAL);
        XPStyle xp = XPStyle.getXP();
        int gap = (xp != null) ? xp.getInt(progressBar, Part.PP_PROGRESS,
                                           null, Prop.PROGRESSSPACESIZE, 0)
                               : 0;

        if (!vertical) {
            int chunksize = box.width+gap;
            return new Rectangle(box.x-chunksize*2, box.y, chunksize*3, box.height);
        } else {
            int chunksize = box.height+gap;
            return new Rectangle(box.x, box.y-chunksize*2, box.width, chunksize*3);
        }
    }

    private void paintIndeterminateFrame(Rectangle box, Graphics2D g,
                                          boolean vertical,
                                          int bgwidth, int bgheight) {
        XPStyle xp = XPStyle.getXP();
        if (xp == null) {
            return;
        }

        // create a new graphics to keep drawing surface state
        Graphics2D gfx = (Graphics2D)g.create();

        Part part = vertical ? Part.PP_BARVERT : Part.PP_BAR;
        Part chunk = vertical ? Part.PP_CHUNKVERT : Part.PP_CHUNK;

        // calculate the chunk offsets
        int gap = xp.getInt(progressBar, Part.PP_PROGRESS, null,
                            Prop.PROGRESSSPACESIZE, 0);
        int deltax = 0;
        int deltay = 0;
        if (!vertical) {
            deltax = -box.width - gap;
            deltay = 0;
        } else {
            deltax = 0;
            deltay = -box.height - gap;
        }

        // Calculate the area of the chunks combined
        Rectangle fullBox = getFullChunkBounds(box);

        // save this box for the next time
        previousFullBox = fullBox;

        // this is the entire progress bar minus the track and borders
        Insets ins = indeterminateInsets;
        Rectangle progbarExtents = new Rectangle(ins.left, ins.top,
                                                 bgwidth  - ins.left - ins.right,
                                                 bgheight - ins.top  - ins.bottom);

        // only paint where the chunks overlap with the progress bar drawing area
        Rectangle repaintArea = progbarExtents.intersection(fullBox);

        // adjust the cliprect to chop the chunks when they go off the end
        gfx.clip(repaintArea);

        // get the skin
        XPStyle.Skin skin = xp.getSkin(progressBar, chunk);

        // do the drawing
        gfx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        skin.paintSkin(gfx, box.x, box.y, box.width, box.height, null);
        box.translate(deltax, deltay);
        gfx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        skin.paintSkin(gfx, box.x, box.y, box.width, box.height, null);
        box.translate(deltax, deltay);
        gfx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
        skin.paintSkin(gfx, box.x, box.y, box.width, box.height, null);

        // get rid of our clip and composite changes
        gfx.dispose();
    }

    private void paintXPBackground(Graphics g, boolean vertical,
                                   int barRectWidth, int barRectHeight) {
        XPStyle xp = XPStyle.getXP();
        if (xp == null) {
            return;
        }
        Part part = vertical ? Part.PP_BARVERT : Part.PP_BAR;
        Skin skin = xp.getSkin(progressBar, part);

        // Paint background
        skin.paintSkin(g, 0, 0, barRectWidth, barRectHeight, null);
    }
}
