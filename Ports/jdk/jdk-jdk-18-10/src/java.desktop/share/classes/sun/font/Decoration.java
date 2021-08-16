/*
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
 *
 */

/*
 * (C) Copyright IBM Corp. 1999-2003, All Rights Reserved
 *
 */

package sun.font;

import java.util.Map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;

import java.awt.font.TextAttribute;

import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.text.AttributedCharacterIterator.Attribute;

import static sun.font.AttributeValues.*;
import static sun.font.EAttribute.*;

/**
 * This class handles underlining, strikethrough, and foreground and
 * background styles on text.  Clients simply acquire instances
 * of this class and hand them off to ExtendedTextLabels or GraphicComponents.
 */
public class Decoration {

    /**
     * This interface is implemented by clients that use Decoration.
     * Unfortunately, interface methods have to public;  ideally these
     * would be package-private.
     */
    public interface Label {
        CoreMetrics getCoreMetrics();
        Rectangle2D getLogicalBounds();

        void handleDraw(Graphics2D g2d, float x, float y);
        Rectangle2D handleGetCharVisualBounds(int index);
        Rectangle2D handleGetVisualBounds();
        Shape handleGetOutline(float x, float y);
    }

    private Decoration() {
    }

    /**
     * Return a Decoration which does nothing.
     */
    public static Decoration getPlainDecoration() {

        return PLAIN;
    }

    private static final int VALUES_MASK =
        AttributeValues.getMask(EFOREGROUND, EBACKGROUND, ESWAP_COLORS,
                                ESTRIKETHROUGH, EUNDERLINE, EINPUT_METHOD_HIGHLIGHT,
                                EINPUT_METHOD_UNDERLINE);

    public static Decoration getDecoration(AttributeValues values) {
        if (values == null || !values.anyDefined(VALUES_MASK)) {
            return PLAIN;
        }

        values = values.applyIMHighlight();

        return new DecorationImpl(values.getForeground(),
                                  values.getBackground(),
                                  values.getSwapColors(),
                                  values.getStrikethrough(),
                                  Underline.getUnderline(values.getUnderline()),
                                  Underline.getUnderline(values.getInputMethodUnderline()));
    }

    /**
     * Return a Decoration appropriate for the given Map.
     * @param attributes the Map used to determine the Decoration
     */
    public static Decoration getDecoration(Map<? extends Attribute, ?> attributes) {
        if (attributes == null) {
            return PLAIN;
        }
        return getDecoration(AttributeValues.fromMap(attributes));
    }

    public void drawTextAndDecorations(Label label,
                                Graphics2D g2d,
                                float x,
                                float y) {

        label.handleDraw(g2d, x, y);
    }

    public Rectangle2D getVisualBounds(Label label) {

        return label.handleGetVisualBounds();
    }

    public Rectangle2D getCharVisualBounds(Label label, int index) {

        return label.handleGetCharVisualBounds(index);
    }

    Shape getOutline(Label label,
                     float x,
                     float y) {

        return label.handleGetOutline(x, y);
    }

    private static final Decoration PLAIN = new Decoration();

    private static final class DecorationImpl extends Decoration {

        private Paint fgPaint = null;
        private Paint bgPaint = null;
        private boolean swapColors = false;
        private boolean strikethrough = false;
        private Underline stdUnderline = null; // underline from TextAttribute.UNDERLINE_ON
        private Underline imUnderline = null; // input method underline

        DecorationImpl(Paint foreground,
                       Paint background,
                       boolean swapColors,
                       boolean strikethrough,
                       Underline stdUnderline,
                       Underline imUnderline) {

            fgPaint = foreground;
            bgPaint = background;

            this.swapColors = swapColors;
            this.strikethrough = strikethrough;

            this.stdUnderline = stdUnderline;
            this.imUnderline = imUnderline;
        }

        private static boolean areEqual(Object lhs, Object rhs) {

            if (lhs == null) {
                return rhs == null;
            }
            else {
                return lhs.equals(rhs);
            }
        }

        public boolean equals(Object rhs) {

            if (rhs == this) {
                return true;
            }
            if (rhs == null) {
                return false;
            }

            DecorationImpl other = null;
            try {
                other = (DecorationImpl) rhs;
            }
            catch(ClassCastException e) {
                return false;
            }

            if (!(swapColors == other.swapColors &&
                        strikethrough == other.strikethrough)) {
                return false;
            }

            if (!areEqual(stdUnderline, other.stdUnderline)) {
                return false;
            }
            if (!areEqual(fgPaint, other.fgPaint)) {
                return false;
            }
            if (!areEqual(bgPaint, other.bgPaint)) {
                return false;
            }
            return areEqual(imUnderline, other.imUnderline);
        }

        public int hashCode() {

            int hc = 1;
            if (strikethrough) {
                hc |= 2;
            }
            if (swapColors) {
                hc |= 4;
            }
            if (stdUnderline != null) {
                hc += stdUnderline.hashCode();
            }
            return hc;
        }

        /**
        * Return the bottom of the Rectangle which encloses pixels
        * drawn by underlines.
        */
        private float getUnderlineMaxY(CoreMetrics cm) {

            float maxY = 0;
            if (stdUnderline != null) {

                float ulBottom = cm.underlineOffset;
                ulBottom += stdUnderline.getLowerDrawLimit(cm.underlineThickness);
                maxY = Math.max(maxY, ulBottom);
            }

            if (imUnderline != null) {

                float ulBottom = cm.underlineOffset;
                ulBottom += imUnderline.getLowerDrawLimit(cm.underlineThickness);
                maxY = Math.max(maxY, ulBottom);
            }

            return maxY;
        }

        private void drawTextAndEmbellishments(Label label,
                                               Graphics2D g2d,
                                               float x,
                                               float y) {

            label.handleDraw(g2d, x, y);

            if (!strikethrough && stdUnderline == null && imUnderline == null) {
                return;
            }

            float x1 = x;
            float x2 = x1 + (float)label.getLogicalBounds().getWidth();

            CoreMetrics cm = label.getCoreMetrics();
            if (strikethrough) {
                Stroke savedStroke = g2d.getStroke();
                g2d.setStroke(new BasicStroke(cm.strikethroughThickness,
                                              BasicStroke.CAP_BUTT,
                                              BasicStroke.JOIN_MITER));
                float strikeY = y + cm.strikethroughOffset;
                g2d.draw(new Line2D.Float(x1, strikeY, x2, strikeY));
                g2d.setStroke(savedStroke);
            }

            float ulOffset = cm.underlineOffset;
            float ulThickness = cm.underlineThickness;

            if (stdUnderline != null) {
                stdUnderline.drawUnderline(g2d, ulThickness, x1, x2, y + ulOffset);
            }

            if (imUnderline != null) {
                imUnderline.drawUnderline(g2d, ulThickness, x1, x2, y + ulOffset);
            }
        }

        public void drawTextAndDecorations(Label label,
                                    Graphics2D g2d,
                                    float x,
                                    float y) {

            if (fgPaint == null && bgPaint == null && swapColors == false) {
                drawTextAndEmbellishments(label, g2d, x, y);
            }
            else {
                Paint savedPaint = g2d.getPaint();
                Paint foreground, background;

                if (swapColors) {
                    background = fgPaint==null? savedPaint : fgPaint;
                    if (bgPaint == null) {
                        if (background instanceof Color) {
                            Color bg = (Color)background;
                            // 30/59/11 is standard weights, tweaked a bit
                            int brightness = 33 * bg.getRed()
                                + 53 * bg.getGreen()
                                + 14 * bg.getBlue();
                            foreground = brightness > 18500 ? Color.BLACK : Color.WHITE;
                        } else {
                            foreground = Color.WHITE;
                        }
                    } else {
                        foreground = bgPaint;
                    }
                }
                else {
                    foreground = fgPaint==null? savedPaint : fgPaint;
                    background = bgPaint;
                }

                if (background != null) {

                    Rectangle2D bgArea = label.getLogicalBounds();
                    bgArea = new Rectangle2D.Float(x + (float)bgArea.getX(),
                                                y + (float)bgArea.getY(),
                                                (float)bgArea.getWidth(),
                                                (float)bgArea.getHeight());

                    g2d.setPaint(background);
                    g2d.fill(bgArea);
                }

                g2d.setPaint(foreground);
                drawTextAndEmbellishments(label, g2d, x, y);
                g2d.setPaint(savedPaint);
            }
        }

        public Rectangle2D getVisualBounds(Label label) {

            Rectangle2D visBounds = label.handleGetVisualBounds();

            if (swapColors || bgPaint != null || strikethrough
                        || stdUnderline != null || imUnderline != null) {

                float minX = 0;
                Rectangle2D lb = label.getLogicalBounds();

                float minY = 0, maxY = 0;

                if (swapColors || bgPaint != null) {

                    minY = (float)lb.getY();
                    maxY = minY + (float)lb.getHeight();
                }

                maxY = Math.max(maxY, getUnderlineMaxY(label.getCoreMetrics()));

                Rectangle2D ab = new Rectangle2D.Float(minX, minY, (float)lb.getWidth(), maxY-minY);
                visBounds.add(ab);
            }

            return visBounds;
        }

        Shape getOutline(Label label,
                         float x,
                         float y) {

            if (!strikethrough && stdUnderline == null && imUnderline == null) {
                return label.handleGetOutline(x, y);
            }

            CoreMetrics cm = label.getCoreMetrics();

            // NOTE:  The performace of the following code may
            // be very poor.
            float ulThickness = cm.underlineThickness;
            float ulOffset = cm.underlineOffset;

            Rectangle2D lb = label.getLogicalBounds();
            float x1 = x;
            float x2 = x1 + (float)lb.getWidth();

            Area area = null;

            if (stdUnderline != null) {
                Shape ul = stdUnderline.getUnderlineShape(ulThickness,
                                                          x1, x2, y+ulOffset);
                area = new Area(ul);
            }

            if (strikethrough) {
                Stroke stStroke = new BasicStroke(cm.strikethroughThickness,
                                                  BasicStroke.CAP_BUTT,
                                                  BasicStroke.JOIN_MITER);
                float shiftY = y + cm.strikethroughOffset;
                Line2D line = new Line2D.Float(x1, shiftY, x2, shiftY);
                Area slArea = new Area(stStroke.createStrokedShape(line));
                if(area == null) {
                    area = slArea;
                } else {
                    area.add(slArea);
                }
            }

            if (imUnderline != null) {
                Shape ul = imUnderline.getUnderlineShape(ulThickness,
                                                         x1, x2, y+ulOffset);
                Area ulArea = new Area(ul);
                if (area == null) {
                    area = ulArea;
                }
                else {
                    area.add(ulArea);
                }
            }

            // area won't be null here, since at least one underline exists.
            area.add(new Area(label.handleGetOutline(x, y)));

            return new GeneralPath(area);
        }


        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.toString());
            sb.append("[");
            if (fgPaint != null) sb.append("fgPaint: " + fgPaint);
            if (bgPaint != null) sb.append(" bgPaint: " + bgPaint);
            if (swapColors) sb.append(" swapColors: true");
            if (strikethrough) sb.append(" strikethrough: true");
            if (stdUnderline != null) sb.append(" stdUnderline: " + stdUnderline);
            if (imUnderline != null) sb.append(" imUnderline: " + imUnderline);
            sb.append("]");
            return sb.toString();
        }
    }
}
