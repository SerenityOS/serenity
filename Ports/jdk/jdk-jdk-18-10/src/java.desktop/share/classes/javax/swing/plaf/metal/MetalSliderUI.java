/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.plaf.basic.BasicSliderUI;

import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Color;
import java.beans.*;

import javax.swing.*;
import javax.swing.plaf.*;

/**
 * A Java L&amp;F implementation of SliderUI.
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
 */
@SuppressWarnings("serial") // Same-version serialization only
public class MetalSliderUI extends BasicSliderUI {

    /**
     * The buffer of a tick.
     */
    protected final int TICK_BUFFER = 4;

    /**
     * The value of the property {@code JSlider.isFilled}.
     * By default, {@code false} if the property is not set,
     * {@code true} for Ocean theme.
     */
    protected boolean filledSlider = false;

    // NOTE: these next five variables are currently unused.
    /**
     * The color of a thumb
     */
    protected static Color thumbColor;

    /**
     * The color of highlighting.
     */
    protected static Color highlightColor;

    /**
     * The color of dark shadow.
     */
    protected static Color darkShadowColor;

    /**
     * The width of a track.
     */
    protected static int trackWidth;

    /**
     * The length of a tick.
     */
    protected static int tickLength;
    private int safeLength;

   /**
    * A default horizontal thumb <code>Icon</code>. This field might not be
    * used. To change the <code>Icon</code> used by this delegate directly set it
    * using the <code>Slider.horizontalThumbIcon</code> UIManager property.
    */
    protected static Icon horizThumbIcon;

   /**
    * A default vertical thumb <code>Icon</code>. This field might not be
    * used. To change the <code>Icon</code> used by this delegate directly set it
    * using the <code>Slider.verticalThumbIcon</code> UIManager property.
    */
    protected static Icon vertThumbIcon;

    private static Icon SAFE_HORIZ_THUMB_ICON;
    private static Icon SAFE_VERT_THUMB_ICON;

    /**
     * Property for {@code JSlider.isFilled}.
     */
    protected final String SLIDER_FILL = "JSlider.isFilled";

    /**
     * Constructs a {@code MetalSliderUI} instance.
     *
     * @param c a component
     * @return a {@code MetalSliderUI} instance
     */
    public static ComponentUI createUI(JComponent c)    {
        return new MetalSliderUI();
    }

    /**
     * Constructs a {@code MetalSliderUI} instance.
     */
    public MetalSliderUI() {
        super( null );
    }

    @SuppressWarnings("removal")
    private static Icon getHorizThumbIcon() {
        if (System.getSecurityManager() != null) {
            return SAFE_HORIZ_THUMB_ICON;
        } else {
            return horizThumbIcon;
        }
    }

    @SuppressWarnings("removal")
    private static Icon getVertThumbIcon() {
        if (System.getSecurityManager() != null) {
            return SAFE_VERT_THUMB_ICON;
        } else {
            return vertThumbIcon;
        }
    }

    public void installUI( JComponent c ) {
        trackWidth = ((Integer)UIManager.get( "Slider.trackWidth" )).intValue();
        tickLength = safeLength = ((Integer)UIManager.get( "Slider.majorTickLength" )).intValue();
        horizThumbIcon = SAFE_HORIZ_THUMB_ICON =
                UIManager.getIcon( "Slider.horizontalThumbIcon" );
        vertThumbIcon = SAFE_VERT_THUMB_ICON =
                UIManager.getIcon( "Slider.verticalThumbIcon" );

        super.installUI( c );

        thumbColor = UIManager.getColor("Slider.thumb");
        highlightColor = UIManager.getColor("Slider.highlight");
        darkShadowColor = UIManager.getColor("Slider.darkShadow");

        scrollListener.setScrollByBlock( false );

        prepareFilledSliderField();
    }

    /**
     * Constructs {@code MetalPropertyListener}.
     *
     * @param slider a {@code JSlider}
     * @return the {@code MetalPropertyListener}
     */
    protected PropertyChangeListener createPropertyChangeListener( JSlider slider ) {
        return new MetalPropertyListener();
    }

    /**
     * {@code PropertyListener} for {@code JSlider.isFilled}.
     */
    protected class MetalPropertyListener extends BasicSliderUI.PropertyChangeHandler {
        /**
         * Constructs a {@code MetalPropertyListener}.
         */
        protected MetalPropertyListener() {}

        public void propertyChange( PropertyChangeEvent e ) {  // listen for slider fill
            super.propertyChange( e );

            if (e.getPropertyName().equals(SLIDER_FILL)) {
                prepareFilledSliderField();
            }
        }
    }

    private void prepareFilledSliderField() {
        // Use true for Ocean theme
        filledSlider = MetalLookAndFeel.usingOcean();

        Object sliderFillProp = slider.getClientProperty(SLIDER_FILL);

        if (sliderFillProp != null) {
            filledSlider = ((Boolean) sliderFillProp).booleanValue();
        }
    }

    public void paintThumb(Graphics g)  {
        Rectangle knobBounds = thumbRect;

        g.translate( knobBounds.x, knobBounds.y );

        if ( slider.getOrientation() == JSlider.HORIZONTAL ) {
            getHorizThumbIcon().paintIcon( slider, g, 0, 0 );
        }
        else {
            getVertThumbIcon().paintIcon( slider, g, 0, 0 );
        }

        g.translate( -knobBounds.x, -knobBounds.y );
    }

    /**
     * Returns a rectangle enclosing the track that will be painted.
     */
    private Rectangle getPaintTrackRect() {
        int trackLeft = 0, trackRight, trackTop = 0, trackBottom;
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            trackBottom = (trackRect.height - 1) - getThumbOverhang();
            trackTop = trackBottom - (getTrackWidth() - 1);
            trackRight = trackRect.width - 1;
        }
        else {
            if (MetalUtils.isLeftToRight(slider)) {
                trackLeft = (trackRect.width - getThumbOverhang()) -
                                                         getTrackWidth();
                trackRight = (trackRect.width - getThumbOverhang()) - 1;
            }
            else {
                trackLeft = getThumbOverhang();
                trackRight = getThumbOverhang() + getTrackWidth() - 1;
            }
            trackBottom = trackRect.height - 1;
        }
        return new Rectangle(trackRect.x + trackLeft, trackRect.y + trackTop,
                             trackRight - trackLeft, trackBottom - trackTop);
    }

    public void paintTrack(Graphics g)  {
        if (MetalLookAndFeel.usingOcean()) {
            oceanPaintTrack(g);
            return;
        }
        Color trackColor = !slider.isEnabled() ? MetalLookAndFeel.getControlShadow() :
                           slider.getForeground();

        boolean leftToRight = MetalUtils.isLeftToRight(slider);

        g.translate( trackRect.x, trackRect.y );

        int trackLeft = 0;
        int trackTop = 0;
        int trackRight;
        int trackBottom;

        // Draw the track
        if ( slider.getOrientation() == JSlider.HORIZONTAL ) {
            trackBottom = (trackRect.height - 1) - getThumbOverhang();
            trackTop = trackBottom - (getTrackWidth() - 1);
            trackRight = trackRect.width - 1;
        }
        else {
            if (leftToRight) {
                trackLeft = (trackRect.width - getThumbOverhang()) -
                                                         getTrackWidth();
                trackRight = (trackRect.width - getThumbOverhang()) - 1;
            }
            else {
                trackLeft = getThumbOverhang();
                trackRight = getThumbOverhang() + getTrackWidth() - 1;
            }
            trackBottom = trackRect.height - 1;
        }

        if ( slider.isEnabled() ) {
            g.setColor( MetalLookAndFeel.getControlDarkShadow() );
            g.drawRect( trackLeft, trackTop,
                        (trackRight - trackLeft) - 1, (trackBottom - trackTop) - 1 );

            g.setColor( MetalLookAndFeel.getControlHighlight() );
            g.drawLine( trackLeft + 1, trackBottom, trackRight, trackBottom );
            g.drawLine( trackRight, trackTop + 1, trackRight, trackBottom );

            g.setColor( MetalLookAndFeel.getControlShadow() );
            g.drawLine( trackLeft + 1, trackTop + 1, trackRight - 2, trackTop + 1 );
            g.drawLine( trackLeft + 1, trackTop + 1, trackLeft + 1, trackBottom - 2 );
        }
        else {
            g.setColor( MetalLookAndFeel.getControlShadow() );
            g.drawRect( trackLeft, trackTop,
                        (trackRight - trackLeft) - 1, (trackBottom - trackTop) - 1 );
        }

        // Draw the fill
        if ( filledSlider ) {
            int middleOfThumb;
            int fillTop;
            int fillLeft;
            int fillBottom;
            int fillRight;

            if ( slider.getOrientation() == JSlider.HORIZONTAL ) {
                middleOfThumb = thumbRect.x + (thumbRect.width / 2);
                middleOfThumb -= trackRect.x; // To compensate for the g.translate()
                fillTop = !slider.isEnabled() ? trackTop : trackTop + 1;
                fillBottom = !slider.isEnabled() ? trackBottom - 1 : trackBottom - 2;

                if ( !drawInverted() ) {
                    fillLeft = !slider.isEnabled() ? trackLeft : trackLeft + 1;
                    fillRight = middleOfThumb;
                }
                else {
                    fillLeft = middleOfThumb;
                    fillRight = !slider.isEnabled() ? trackRight - 1 : trackRight - 2;
                }
            }
            else {
                middleOfThumb = thumbRect.y + (thumbRect.height / 2);
                middleOfThumb -= trackRect.y; // To compensate for the g.translate()
                fillLeft = !slider.isEnabled() ? trackLeft : trackLeft + 1;
                fillRight = !slider.isEnabled() ? trackRight - 1 : trackRight - 2;

                if ( !drawInverted() ) {
                    fillTop = middleOfThumb;
                    fillBottom = !slider.isEnabled() ? trackBottom - 1 : trackBottom - 2;
                }
                else {
                    fillTop = !slider.isEnabled() ? trackTop : trackTop + 1;
                    fillBottom = middleOfThumb;
                }
            }

            if ( slider.isEnabled() ) {
                g.setColor( slider.getBackground() );
                g.drawLine( fillLeft, fillTop, fillRight, fillTop );
                g.drawLine( fillLeft, fillTop, fillLeft, fillBottom );

                g.setColor( MetalLookAndFeel.getControlShadow() );
                g.fillRect( fillLeft + 1, fillTop + 1,
                            fillRight - fillLeft, fillBottom - fillTop );
            }
            else {
                g.setColor( MetalLookAndFeel.getControlShadow() );
                g.fillRect(fillLeft, fillTop, fillRight - fillLeft, fillBottom - fillTop);
            }
        }

        g.translate( -trackRect.x, -trackRect.y );
    }

    private void oceanPaintTrack(Graphics g)  {
        boolean leftToRight = MetalUtils.isLeftToRight(slider);
        boolean drawInverted = drawInverted();
        Color sliderAltTrackColor = (Color)UIManager.get(
                                    "Slider.altTrackColor");

        // Translate to the origin of the painting rectangle
        Rectangle paintRect = getPaintTrackRect();
        g.translate(paintRect.x, paintRect.y);

        // Width and height of the painting rectangle.
        int w = paintRect.width;
        int h = paintRect.height;

        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            int middleOfThumb = thumbRect.x + thumbRect.width / 2 - paintRect.x;

            if (slider.isEnabled()) {
                int fillMinX;
                int fillMaxX;

                if (middleOfThumb > 0) {
                    g.setColor(drawInverted ? MetalLookAndFeel.getControlDarkShadow() :
                            MetalLookAndFeel.getPrimaryControlDarkShadow());

                    g.drawRect(0, 0, middleOfThumb - 1, h - 1);
                }

                if (middleOfThumb < w) {
                    g.setColor(drawInverted ? MetalLookAndFeel.getPrimaryControlDarkShadow() :
                            MetalLookAndFeel.getControlDarkShadow());

                    g.drawRect(middleOfThumb, 0, w - middleOfThumb - 1, h - 1);
                }

                if (filledSlider) {
                    g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
                    if (drawInverted) {
                        fillMinX = middleOfThumb;
                        fillMaxX = w - 2;
                        g.drawLine(1, 1, middleOfThumb, 1);
                    } else {
                        fillMinX = 1;
                        fillMaxX = middleOfThumb;
                        g.drawLine(middleOfThumb, 1, w - 1, 1);
                    }
                    if (h == 6) {
                        g.setColor(MetalLookAndFeel.getWhite());
                        g.drawLine(fillMinX, 1, fillMaxX, 1);
                        g.setColor(sliderAltTrackColor);
                        g.drawLine(fillMinX, 2, fillMaxX, 2);
                        g.setColor(MetalLookAndFeel.getControlShadow());
                        g.drawLine(fillMinX, 3, fillMaxX, 3);
                        g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
                        g.drawLine(fillMinX, 4, fillMaxX, 4);
                    }
                }
            } else {
                g.setColor(MetalLookAndFeel.getControlShadow());

                if (middleOfThumb > 0) {
                    if (!drawInverted && filledSlider) {
                        g.fillRect(0, 0, middleOfThumb - 1, h - 1);
                    } else {
                        g.drawRect(0, 0, middleOfThumb - 1, h - 1);
                    }
                }

                if (middleOfThumb < w) {
                    if (drawInverted && filledSlider) {
                        g.fillRect(middleOfThumb, 0, w - middleOfThumb - 1, h - 1);
                    } else {
                        g.drawRect(middleOfThumb, 0, w - middleOfThumb - 1, h - 1);
                    }
                }
            }
        } else {
            int middleOfThumb = thumbRect.y + (thumbRect.height / 2) - paintRect.y;

            if (slider.isEnabled()) {
                int fillMinY;
                int fillMaxY;

                if (middleOfThumb > 0) {
                    g.setColor(drawInverted ? MetalLookAndFeel.getPrimaryControlDarkShadow() :
                            MetalLookAndFeel.getControlDarkShadow());

                    g.drawRect(0, 0, w - 1, middleOfThumb - 1);
                }

                if (middleOfThumb < h) {
                    g.setColor(drawInverted ? MetalLookAndFeel.getControlDarkShadow() :
                            MetalLookAndFeel.getPrimaryControlDarkShadow());

                    g.drawRect(0, middleOfThumb, w - 1, h - middleOfThumb - 1);
                }

                if (filledSlider) {
                    g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
                    if (drawInverted()) {
                        fillMinY = 1;
                        fillMaxY = middleOfThumb;
                        if (leftToRight) {
                            g.drawLine(1, middleOfThumb, 1, h - 1);
                        } else {
                            g.drawLine(w - 2, middleOfThumb, w - 2, h - 1);
                        }
                    } else {
                        fillMinY = middleOfThumb;
                        fillMaxY = h - 2;
                        if (leftToRight) {
                            g.drawLine(1, 1, 1, middleOfThumb);
                        } else {
                            g.drawLine(w - 2, 1, w - 2, middleOfThumb);
                        }
                    }
                    if (w == 6) {
                        g.setColor(leftToRight ? MetalLookAndFeel.getWhite() : MetalLookAndFeel.getPrimaryControlShadow());
                        g.drawLine(1, fillMinY, 1, fillMaxY);
                        g.setColor(leftToRight ? sliderAltTrackColor : MetalLookAndFeel.getControlShadow());
                        g.drawLine(2, fillMinY, 2, fillMaxY);
                        g.setColor(leftToRight ? MetalLookAndFeel.getControlShadow() : sliderAltTrackColor);
                        g.drawLine(3, fillMinY, 3, fillMaxY);
                        g.setColor(leftToRight ? MetalLookAndFeel.getPrimaryControlShadow() : MetalLookAndFeel.getWhite());
                        g.drawLine(4, fillMinY, 4, fillMaxY);
                    }
                }
            } else {
                g.setColor(MetalLookAndFeel.getControlShadow());

                if (middleOfThumb > 0) {
                    if (drawInverted && filledSlider) {
                        g.fillRect(0, 0, w - 1, middleOfThumb - 1);
                    } else {
                        g.drawRect(0, 0, w - 1, middleOfThumb - 1);
                    }
                }

                if (middleOfThumb < h) {
                    if (!drawInverted && filledSlider) {
                        g.fillRect(0, middleOfThumb, w - 1, h - middleOfThumb - 1);
                    } else {
                        g.drawRect(0, middleOfThumb, w - 1, h - middleOfThumb - 1);
                    }
                }
            }
        }

        g.translate(-paintRect.x, -paintRect.y);
    }

    public void paintFocus(Graphics g)  {
    }

    protected Dimension getThumbSize() {
        Dimension size = new Dimension();

        if ( slider.getOrientation() == JSlider.VERTICAL ) {
            size.width = getVertThumbIcon().getIconWidth();
            size.height = getVertThumbIcon().getIconHeight();
        }
        else {
            size.width = getHorizThumbIcon().getIconWidth();
            size.height = getHorizThumbIcon().getIconHeight();
        }

        return size;
    }

    /**
     * Gets the height of the tick area for horizontal sliders and the width of the
     * tick area for vertical sliders.  BasicSliderUI uses the returned value to
     * determine the tick area rectangle.
     */
    public int getTickLength() {
        return slider.getOrientation() == JSlider.HORIZONTAL ? safeLength + TICK_BUFFER + 1 :
        safeLength + TICK_BUFFER + 3;
    }

    /**
     * Returns the shorter dimension of the track.
     *
     * @return the shorter dimension of the track
     */
    protected int getTrackWidth() {
        // This strange calculation is here to keep the
        // track in proportion to the thumb.
        final double kIdealTrackWidth = 7.0;
        final double kIdealThumbHeight = 16.0;
        final double kWidthScalar = kIdealTrackWidth / kIdealThumbHeight;

        if ( slider.getOrientation() == JSlider.HORIZONTAL ) {
            return (int)(kWidthScalar * thumbRect.height);
        }
        else {
            return (int)(kWidthScalar * thumbRect.width);
        }
    }

    /**
     * Returns the longer dimension of the slide bar.  (The slide bar is only the
     * part that runs directly under the thumb)
     *
     * @return the longer dimension of the slide bar
     */
    protected int getTrackLength() {
        if ( slider.getOrientation() == JSlider.HORIZONTAL ) {
            return trackRect.width;
        }
        return trackRect.height;
    }

    /**
     * Returns the amount that the thumb goes past the slide bar.
     *
     * @return the amount that the thumb goes past the slide bar
     */
    protected int getThumbOverhang() {
        return (int)(getThumbSize().getHeight()-getTrackWidth())/2;
    }

    protected void scrollDueToClickInTrack( int dir ) {
        scrollByUnit( dir );
    }

    protected void paintMinorTickForHorizSlider( Graphics g, Rectangle tickBounds, int x ) {
        g.setColor( slider.isEnabled() ? slider.getForeground() : MetalLookAndFeel.getControlShadow() );
        g.drawLine( x, TICK_BUFFER, x, TICK_BUFFER + (safeLength / 2) );
    }

    protected void paintMajorTickForHorizSlider( Graphics g, Rectangle tickBounds, int x ) {
        g.setColor( slider.isEnabled() ? slider.getForeground() : MetalLookAndFeel.getControlShadow() );
        g.drawLine( x, TICK_BUFFER , x, TICK_BUFFER + (safeLength - 1) );
    }

    protected void paintMinorTickForVertSlider( Graphics g, Rectangle tickBounds, int y ) {
        g.setColor( slider.isEnabled() ? slider.getForeground() : MetalLookAndFeel.getControlShadow() );

        if (MetalUtils.isLeftToRight(slider)) {
            g.drawLine( TICK_BUFFER, y, TICK_BUFFER + (safeLength / 2), y );
        }
        else {
            g.drawLine( 0, y, safeLength/2, y );
        }
    }

    protected void paintMajorTickForVertSlider( Graphics g, Rectangle tickBounds, int y ) {
        g.setColor( slider.isEnabled() ? slider.getForeground() : MetalLookAndFeel.getControlShadow() );

        if (MetalUtils.isLeftToRight(slider)) {
            g.drawLine( TICK_BUFFER, y, TICK_BUFFER + safeLength, y );
        }
        else {
            g.drawLine( 0, y, safeLength, y );
        }
    }
}
