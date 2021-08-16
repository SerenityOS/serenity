/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text.html;

import java.awt.Rectangle;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.Color;
import java.awt.Shape;
import java.awt.Graphics;
import java.awt.Toolkit;

import java.awt.image.ImageObserver;
import java.net.URL;
import java.net.MalformedURLException;

import java.util.Dictionary;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;

import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import javax.swing.text.View;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.ViewFactory;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.Highlighter;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;

import javax.swing.event.DocumentEvent;

/**
 * View of an Image, intended to support the HTML &lt;IMG&gt; tag.
 * Supports scaling via the HEIGHT and WIDTH attributes of the tag.
 * If the image is unable to be loaded any text specified via the
 * <code>ALT</code> attribute will be rendered.
 * <p>
 * While this class has been part of swing for a while now, it is public
 * as of 1.4.
 *
 * @author  Scott Violet
 * @see javax.swing.text.IconView
 * @since 1.4
 */
public class ImageView extends View {
    /**
     * If true, when some of the bits are available a repaint is done.
     * <p>
     * This is set to false as swing does not offer a repaint that takes a
     * delay. If this were true, a bunch of immediate repaints would get
     * generated that end up significantly delaying the loading of the image
     * (or anything else going on for that matter).
     */
    private static boolean sIsInc = false;
    /**
     * Repaint delay when some of the bits are available.
     */
    private static int sIncRate = 100;
    /**
     * Property name for pending image icon
     */
    private static final String PENDING_IMAGE = "html.pendingImage";
    /**
     * Property name for missing image icon
     */
    private static final String MISSING_IMAGE = "html.missingImage";

    /**
     * Document property for image cache.
     */
    private static final String IMAGE_CACHE_PROPERTY = "imageCache";

    // Height/width to use before we know the real size, these should at least
    // the size of <code>sMissingImageIcon</code> and
    // <code>sPendingImageIcon</code>
    private static final int DEFAULT_WIDTH = 38;
    private static final int DEFAULT_HEIGHT= 38;

    /**
     * Default border to use if one is not specified.
     */
    private static final int DEFAULT_BORDER = 2;

    // Bitmask values
    private static final int LOADING_FLAG = 1;
    private static final int LINK_FLAG = 2;
    private static final int WIDTH_FLAG = 4;
    private static final int HEIGHT_FLAG = 8;
    private static final int RELOAD_FLAG = 16;
    private static final int RELOAD_IMAGE_FLAG = 32;
    private static final int SYNC_LOAD_FLAG = 64;

    private AttributeSet attr;
    private Image image;
    private Image disabledImage;
    private int width;
    private int height;
    /** Bitmask containing some of the above bitmask values. Because the
     * image loading notification can happen on another thread access to
     * this is synchronized (at least for modifying it). */
    private int state;
    private Container container;
    private Rectangle fBounds;
    private Color borderColor;
    // Size of the border, the insets contains this valid. For example, if
    // the HSPACE attribute was 4 and BORDER 2, leftInset would be 6.
    private short borderSize;
    // Insets, obtained from the painter.
    private short leftInset;
    private short rightInset;
    private short topInset;
    private short bottomInset;
    /**
     * We don't directly implement ImageObserver, instead we use an instance
     * that calls back to us.
     */
    private ImageObserver imageObserver;
    /**
     * Used for alt text. Will be non-null if the image couldn't be found,
     * and there is valid alt text.
     */
    private View altView;
    /** Alignment along the vertical (Y) axis. */
    private float vAlign;



    /**
     * Creates a new view that represents an IMG element.
     *
     * @param elem the element to create a view for
     */
    public ImageView(Element elem) {
        super(elem);
        fBounds = new Rectangle();
        imageObserver = new ImageHandler();
        state = RELOAD_FLAG | RELOAD_IMAGE_FLAG;
    }

    /**
     * Returns the text to display if the image cannot be loaded. This is
     * obtained from the Elements attribute set with the attribute name
     * <code>HTML.Attribute.ALT</code>.
     *
     * @return the test to display if the image cannot be loaded.
     */
    public String getAltText() {
        return (String)getElement().getAttributes().getAttribute
            (HTML.Attribute.ALT);
    }

    /**
     * Return a URL for the image source,
     * or null if it could not be determined.
     *
     * @return the URL for the image source, or null if it could not be determined.
     */
    public URL getImageURL() {
        String src = (String)getElement().getAttributes().
                             getAttribute(HTML.Attribute.SRC);
        if (src == null) {
            return null;
        }

        URL reference = ((HTMLDocument)getDocument()).getBase();
        try {
            URL u = new URL(reference,src);
            return u;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Returns the icon to use if the image could not be found.
     *
     * @return the icon to use if the image could not be found.
     */
    public Icon getNoImageIcon() {
        return (Icon) UIManager.getLookAndFeelDefaults().get(MISSING_IMAGE);
    }

    /**
     * Returns the icon to use while in the process of loading the image.
     *
     * @return the icon to use while in the process of loading the image.
     */
    public Icon getLoadingImageIcon() {
        return (Icon) UIManager.getLookAndFeelDefaults().get(PENDING_IMAGE);
    }

    /**
     * Returns the image to render.
     *
     * @return the image to render.
     */
    public Image getImage() {
        sync();
        return image;
    }

    private Image getImage(boolean enabled) {
        Image img = getImage();
        if (! enabled) {
            if (disabledImage == null) {
                disabledImage = GrayFilter.createDisabledImage(img);
            }
            img = disabledImage;
        }
        return img;
    }

    /**
     * Sets how the image is loaded. If <code>newValue</code> is true,
     * the image will be loaded when first asked for, otherwise it will
     * be loaded asynchronously. The default is to not load synchronously,
     * that is to load the image asynchronously.
     *
     * @param newValue if {@code true} the image will be loaded when first asked for,
     *                 otherwise it will be asynchronously.
     */
    public void setLoadsSynchronously(boolean newValue) {
        synchronized(this) {
            if (newValue) {
                state |= SYNC_LOAD_FLAG;
            }
            else {
                state = (state | SYNC_LOAD_FLAG) ^ SYNC_LOAD_FLAG;
            }
        }
    }

    /**
     * Returns {@code true} if the image should be loaded when first asked for.
     *
     * @return {@code true} if the image should be loaded when first asked for.
     */
    public boolean getLoadsSynchronously() {
        return ((state & SYNC_LOAD_FLAG) != 0);
    }

    /**
     * Convenient method to get the StyleSheet.
     *
     * @return the StyleSheet
     */
    protected StyleSheet getStyleSheet() {
        HTMLDocument doc = (HTMLDocument) getDocument();
        return doc.getStyleSheet();
    }

    /**
     * Fetches the attributes to use when rendering.  This is
     * implemented to multiplex the attributes specified in the
     * model with a StyleSheet.
     */
    public AttributeSet getAttributes() {
        sync();
        return attr;
    }

    /**
     * For images the tooltip text comes from text specified with the
     * <code>ALT</code> attribute. This is overriden to return
     * <code>getAltText</code>.
     *
     * @see JTextComponent#getToolTipText
     */
    public String getToolTipText(float x, float y, Shape allocation) {
        return getAltText();
    }

    /**
     * Update any cached values that come from attributes.
     */
    protected void setPropertiesFromAttributes() {
        StyleSheet sheet = getStyleSheet();
        this.attr = sheet.getViewAttributes(this);

        // Gutters
        borderSize = (short)getIntAttr(HTML.Attribute.BORDER, isLink() ?
                                       DEFAULT_BORDER : 0);

        leftInset = rightInset = (short)(getIntAttr(HTML.Attribute.HSPACE,
                                                    0) + borderSize);
        topInset = bottomInset = (short)(getIntAttr(HTML.Attribute.VSPACE,
                                                    0) + borderSize);

        borderColor = ((StyledDocument)getDocument()).getForeground
                      (getAttributes());

        AttributeSet attr = getElement().getAttributes();

        // Alignment.
        // PENDING: This needs to be changed to support the CSS versions
        // when conversion from ALIGN to VERTICAL_ALIGN is complete.
        Object alignment = attr.getAttribute(HTML.Attribute.ALIGN);

        vAlign = 1.0f;
        if (alignment != null) {
            alignment = alignment.toString();
            if ("top".equals(alignment)) {
                vAlign = 0f;
            }
            else if ("middle".equals(alignment)) {
                vAlign = .5f;
            }
        }

        AttributeSet anchorAttr = (AttributeSet)attr.getAttribute(HTML.Tag.A);
        if (anchorAttr != null && anchorAttr.isDefined
            (HTML.Attribute.HREF)) {
            synchronized(this) {
                state |= LINK_FLAG;
            }
        }
        else {
            synchronized(this) {
                state = (state | LINK_FLAG) ^ LINK_FLAG;
            }
        }
    }

    /**
     * Establishes the parent view for this view.
     * Seize this moment to cache the AWT Container I'm in.
     */
    public void setParent(View parent) {
        View oldParent = getParent();
        super.setParent(parent);
        container = (parent != null) ? getContainer() : null;
        if (oldParent != parent) {
            synchronized(this) {
                state |= RELOAD_FLAG;
            }
        }
    }

    /**
     * Invoked when the Elements attributes have changed. Recreates the image.
     */
    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        super.changedUpdate(e,a,f);

        synchronized(this) {
            state |= RELOAD_FLAG | RELOAD_IMAGE_FLAG;
        }

        // Assume the worst.
        preferenceChanged(null, true, true);
    }

    /**
     * Paints the View.
     *
     * @param g the rendering surface to use
     * @param a the allocated region to render into
     * @see View#paint
     */
    public void paint(Graphics g, Shape a) {
        sync();

        Rectangle rect = (a instanceof Rectangle) ? (Rectangle)a :
                         a.getBounds();
        Rectangle clip = g.getClipBounds();

        fBounds.setBounds(rect);
        paintHighlights(g, a);
        paintBorder(g, rect);
        if (clip != null) {
            g.clipRect(rect.x + leftInset, rect.y + topInset,
                       rect.width - leftInset - rightInset,
                       rect.height - topInset - bottomInset);
        }

        Container host = getContainer();
        Image img = getImage(host == null || host.isEnabled());
        if (img != null) {
            if (! hasPixels(img)) {
                // No pixels yet, use the default
                Icon icon = getLoadingImageIcon();
                if (icon != null) {
                    icon.paintIcon(host, g,
                            rect.x + leftInset, rect.y + topInset);
                }
            }
            else {
                // Draw the image
                g.drawImage(img, rect.x + leftInset, rect.y + topInset,
                            width, height, imageObserver);
            }
        }
        else {
            Icon icon = getNoImageIcon();
            if (icon != null) {
                icon.paintIcon(host, g,
                        rect.x + leftInset, rect.y + topInset);
            }
            View view = getAltView();
            // Paint the view representing the alt text, if its non-null
            if (view != null && ((state & WIDTH_FLAG) == 0 ||
                                 width > DEFAULT_WIDTH)) {
                // Assume layout along the y direction
                Rectangle altRect = new Rectangle
                    (rect.x + leftInset + DEFAULT_WIDTH, rect.y + topInset,
                     rect.width - leftInset - rightInset - DEFAULT_WIDTH,
                     rect.height - topInset - bottomInset);

                view.paint(g, altRect);
            }
        }
        if (clip != null) {
            // Reset clip.
            g.setClip(clip.x, clip.y, clip.width, clip.height);
        }
    }

    private void paintHighlights(Graphics g, Shape shape) {
        if (container instanceof JTextComponent) {
            JTextComponent tc = (JTextComponent)container;
            Highlighter h = tc.getHighlighter();
            if (h instanceof LayeredHighlighter) {
                ((LayeredHighlighter)h).paintLayeredHighlights
                    (g, getStartOffset(), getEndOffset(), shape, tc, this);
            }
        }
    }

    private void paintBorder(Graphics g, Rectangle rect) {
        Color color = borderColor;

        if ((borderSize > 0 || image == null) && color != null) {
            int xOffset = leftInset - borderSize;
            int yOffset = topInset - borderSize;
            g.setColor(color);
            int n = (image == null) ? 1 : borderSize;
            for (int counter = 0; counter < n; counter++) {
                g.drawRect(rect.x + xOffset + counter,
                           rect.y + yOffset + counter,
                           rect.width - counter - counter - xOffset -xOffset-1,
                           rect.height - counter - counter -yOffset-yOffset-1);
            }
        }
    }

    /**
     * Determines the preferred span for this view along an
     * axis.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return   the span the view would like to be rendered into;
     *           typically the view is told to render into the span
     *           that is returned, although there is no guarantee;
     *           the parent may choose to resize or break the view
     */
    public float getPreferredSpan(int axis) {
        sync();

        // If the attributes specified a width/height, always use it!
        if (axis == View.X_AXIS && (state & WIDTH_FLAG) == WIDTH_FLAG) {
            getPreferredSpanFromAltView(axis);
            return width + leftInset + rightInset;
        }
        if (axis == View.Y_AXIS && (state & HEIGHT_FLAG) == HEIGHT_FLAG) {
            getPreferredSpanFromAltView(axis);
            return height + topInset + bottomInset;
        }

        Image image = getImage();

        if (image != null) {
            switch (axis) {
            case View.X_AXIS:
                return width + leftInset + rightInset;
            case View.Y_AXIS:
                return height + topInset + bottomInset;
            default:
                throw new IllegalArgumentException("Invalid axis: " + axis);
            }
        }
        else {
            View view = getAltView();
            float retValue = 0f;

            if (view != null) {
                retValue = view.getPreferredSpan(axis);
            }
            switch (axis) {
            case View.X_AXIS:
                return retValue + (float)(width + leftInset + rightInset);
            case View.Y_AXIS:
                return retValue + (float)(height + topInset + bottomInset);
            default:
                throw new IllegalArgumentException("Invalid axis: " + axis);
            }
        }
    }

    /**
     * Determines the desired alignment for this view along an
     * axis.  This is implemented to give the alignment to the
     * bottom of the icon along the y axis, and the default
     * along the x axis.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return the desired alignment; this should be a value
     *   between 0.0 and 1.0 where 0 indicates alignment at the
     *   origin and 1.0 indicates alignment to the full span
     *   away from the origin; an alignment of 0.5 would be the
     *   center of the view
     */
    public float getAlignment(int axis) {
        switch (axis) {
        case View.Y_AXIS:
            return vAlign;
        default:
            return super.getAlignment(axis);
        }
    }

    /**
     * Provides a mapping from the document model coordinate space
     * to the coordinate space of the view mapped to it.
     *
     * @param pos the position to convert
     * @param a the allocated region to render into
     * @return the bounding box of the given position
     * @exception BadLocationException  if the given position does not represent a
     *   valid location in the associated document
     * @see View#modelToView
     */
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        int p0 = getStartOffset();
        int p1 = getEndOffset();
        if ((pos >= p0) && (pos <= p1)) {
            Rectangle r = a.getBounds();
            if (pos == p1) {
                r.x += r.width;
            }
            r.width = 0;
            return r;
        }
        return null;
    }

    /**
     * Provides a mapping from the view coordinate space to the logical
     * coordinate space of the model.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param a the allocated region to render into
     * @return the location within the model that best represents the
     *  given point of view
     * @see View#viewToModel
     */
    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
        Rectangle alloc = (Rectangle) a;
        if (x < alloc.x + alloc.width) {
            bias[0] = Position.Bias.Forward;
            return getStartOffset();
        }
        bias[0] = Position.Bias.Backward;
        return getEndOffset();
    }

    /**
     * Sets the size of the view.  This should cause
     * layout of the view if it has any layout duties.
     *
     * @param width the width &gt;= 0
     * @param height the height &gt;= 0
     */
    public void setSize(float width, float height) {
        sync();

        if (getImage() == null) {
            View view = getAltView();

            if (view != null) {
                view.setSize(Math.max(0f, width - (float)(DEFAULT_WIDTH + leftInset + rightInset)),
                             Math.max(0f, height - (float)(topInset + bottomInset)));
            }
        }
    }

    /**
     * Returns true if this image within a link?
     */
    private boolean isLink() {
        return ((state & LINK_FLAG) == LINK_FLAG);
    }

    /**
     * Returns true if the passed in image has a non-zero width and height.
     */
    private boolean hasPixels(Image image) {
        return image != null &&
            (image.getHeight(imageObserver) > 0) &&
            (image.getWidth(imageObserver) > 0);
    }

    /**
     * Returns the preferred span of the View used to display the alt text,
     * or 0 if the view does not exist.
     */
    private float getPreferredSpanFromAltView(int axis) {
        if (getImage() == null) {
            View view = getAltView();

            if (view != null) {
                return view.getPreferredSpan(axis);
            }
        }
        return 0f;
    }

    /**
     * Request that this view be repainted.
     * Assumes the view is still at its last-drawn location.
     */
    private void repaint(long delay) {
        if (container != null && fBounds != null) {
            container.repaint(delay, fBounds.x, fBounds.y, fBounds.width,
                               fBounds.height);
        }
    }

    /**
     * Convenient method for getting an integer attribute from the elements
     * AttributeSet.
     */
    private int getIntAttr(HTML.Attribute name, int deflt) {
        AttributeSet attr = getElement().getAttributes();
        if (attr.isDefined(name)) {             // does not check parents!
            int i;
            String val = (String)attr.getAttribute(name);
            if (val == null) {
                i = deflt;
            }
            else {
                try{
                    i = Math.max(0, Integer.parseInt(val));
                }catch( NumberFormatException x ) {
                    i = deflt;
                }
            }
            return i;
        } else
            return deflt;
    }

    /**
     * Makes sure the necessary properties and image is loaded.
     */
    private void sync() {
        int s = state;
        if ((s & RELOAD_IMAGE_FLAG) != 0) {
            refreshImage();
        }
        s = state;
        if ((s & RELOAD_FLAG) != 0) {
            synchronized(this) {
                state = (state | RELOAD_FLAG) ^ RELOAD_FLAG;
            }
            setPropertiesFromAttributes();
        }
    }

    /**
     * Loads the image and updates the size accordingly. This should be
     * invoked instead of invoking <code>loadImage</code> or
     * <code>updateImageSize</code> directly.
     */
    private void refreshImage() {
        synchronized(this) {
            // clear out width/height/realoadimage flag and set loading flag
            state = (state | LOADING_FLAG | RELOAD_IMAGE_FLAG | WIDTH_FLAG |
                     HEIGHT_FLAG) ^ (WIDTH_FLAG | HEIGHT_FLAG |
                                     RELOAD_IMAGE_FLAG);
            image = null;
            width = height = 0;
        }

        try {
            // Load the image
            loadImage();

            // And update the size params
            updateImageSize();
        }
        finally {
            synchronized(this) {
                // Clear out state in case someone threw an exception.
                state = (state | LOADING_FLAG) ^ LOADING_FLAG;
            }
        }
    }

    /**
     * Loads the image from the URL <code>getImageURL</code>. This should
     * only be invoked from <code>refreshImage</code>.
     */
    private void loadImage() {
        URL src = getImageURL();
        Image newImage = null;
        if (src != null) {
            @SuppressWarnings("unchecked")
            Dictionary<URL, Image> cache = (Dictionary)getDocument().
                getProperty(IMAGE_CACHE_PROPERTY);
            if (cache != null) {
                newImage = cache.get(src);
            }
            else {
                newImage = Toolkit.getDefaultToolkit().createImage(src);
                if (newImage != null && getLoadsSynchronously()) {
                    // Force the image to be loaded by using an ImageIcon.
                    ImageIcon ii = new ImageIcon();
                    ii.setImage(newImage);
                }
            }
        }
        image = newImage;
    }

    /**
     * Recreates and reloads the image.  This should
     * only be invoked from <code>refreshImage</code>.
     */
    private void updateImageSize() {
        int newWidth = 0;
        int newHeight = 0;
        int newState = 0;
        Image newImage = getImage();

        if (newImage != null) {
            Element elem = getElement();
            AttributeSet attr = elem.getAttributes();

            // Get the width/height and set the state ivar before calling
            // anything that might cause the image to be loaded, and thus the
            // ImageHandler to be called.
            newWidth = getIntAttr(HTML.Attribute.WIDTH, -1);
            newHeight = getIntAttr(HTML.Attribute.HEIGHT, -1);

            if (newWidth > 0) {
                newState |= WIDTH_FLAG;
            }

            if (newHeight > 0) {
                newState |= HEIGHT_FLAG;
            }

            Image img;
            synchronized(this) {
                img = image;
            }
            if (newWidth <= 0) {
                newWidth = img.getWidth(imageObserver);
                if (newWidth <= 0) {
                    newWidth = DEFAULT_WIDTH;
                }
            }
            if (newHeight <= 0) {
                newHeight = img.getHeight(imageObserver);
                if (newHeight <= 0) {
                    newHeight = DEFAULT_HEIGHT;
                }
            }
            /*
            If synchronous loading flag is set, then make sure that the image is
            scaled appropriately.
            Otherwise, the ImageHandler::imageUpdate takes care of scaling the image
            appropriately.
            */
            if (getLoadsSynchronously()) {
                Dimension d = adjustWidthHeight(newWidth, newHeight);
                newWidth = d.width;
                newHeight = d.height;
                newState |= (WIDTH_FLAG | HEIGHT_FLAG);
            }

            // Make sure the image starts loading:
            if ((newState & (WIDTH_FLAG | HEIGHT_FLAG)) != 0) {
                Toolkit.getDefaultToolkit().prepareImage(newImage, newWidth,
                                                         newHeight,
                                                         imageObserver);
            }
            else {
                Toolkit.getDefaultToolkit().prepareImage(newImage, -1, -1,
                                                         imageObserver);
            }

            boolean createText = false;
            synchronized(this) {
                // If imageloading failed, other thread may have called
                // ImageLoader which will null out image, hence we check
                // for it.
                if (image != null) {
                    if ((newState & WIDTH_FLAG) == WIDTH_FLAG || width == 0) {
                        width = newWidth;
                    }
                    if ((newState & HEIGHT_FLAG) == HEIGHT_FLAG ||
                        height == 0) {
                        height = newHeight;
                    }
                }
                else {
                    createText = true;
                    if ((newState & WIDTH_FLAG) == WIDTH_FLAG) {
                        width = newWidth;
                    }
                    if ((newState & HEIGHT_FLAG) == HEIGHT_FLAG) {
                        height = newHeight;
                    }
                }
                state = state | newState;
                state = (state | LOADING_FLAG) ^ LOADING_FLAG;
            }
            if (createText) {
                // Only reset if this thread determined image is null
                updateAltTextView();
            }
        }
        else {
            width = height = DEFAULT_HEIGHT;
            updateAltTextView();
        }
    }

    /**
     * Updates the view representing the alt text.
     */
    private void updateAltTextView() {
        String text = getAltText();

        if (text != null) {
            ImageLabelView newView;

            newView = new ImageLabelView(getElement(), text);
            synchronized(this) {
                altView = newView;
            }
        }
    }

    /**
     * Returns the view to use for alternate text. This may be null.
     */
    private View getAltView() {
        View view;

        synchronized(this) {
            view = altView;
        }
        if (view != null && view.getParent() == null) {
            view.setParent(getParent());
        }
        return view;
    }

    /**
     * Invokes <code>preferenceChanged</code> on the event displatching
     * thread.
     */
    private void safePreferenceChanged() {
        if (SwingUtilities.isEventDispatchThread()) {
            Document doc = getDocument();
            if (doc instanceof AbstractDocument) {
                ((AbstractDocument)doc).readLock();
            }
            preferenceChanged(null, true, true);
            if (doc instanceof AbstractDocument) {
                ((AbstractDocument)doc).readUnlock();
            }
        }
        else {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        safePreferenceChanged();
                    }
                });
        }
    }

    private Dimension adjustWidthHeight(int newWidth, int newHeight) {
        Dimension d = new Dimension();
        double proportion = 0.0;
        final int specifiedWidth = getIntAttr(HTML.Attribute.WIDTH, -1);
        final int specifiedHeight = getIntAttr(HTML.Attribute.HEIGHT, -1);
        /**
         * If either of the attributes are not specified, then calculate the
         * proportion for the specified dimension wrt actual value, and then
         * apply the same proportion to the unspecified dimension as well,
         * so that the aspect ratio of the image is maintained.
         */
        if (specifiedWidth != -1 && specifiedHeight != -1) {
            newWidth = specifiedWidth;
            newHeight = specifiedHeight;
        } else if (specifiedWidth != -1 ^ specifiedHeight != -1) {
            if (specifiedWidth <= 0) {
                proportion = specifiedHeight / ((double)newHeight);
                newWidth = (int)(proportion * newWidth);
                newHeight = specifiedHeight;
            }

            if (specifiedHeight <= 0) {
                proportion = specifiedWidth / ((double)newWidth);
                newHeight = (int)(proportion * newHeight);
                newWidth = specifiedWidth;
            }
        }

        d.width = newWidth;
        d.height = newHeight;

        return d;
    }

    /**
     * ImageHandler implements the ImageObserver to correctly update the
     * display as new parts of the image become available.
     */
    private class ImageHandler implements ImageObserver {
        // This can come on any thread. If we are in the process of reloading
        // the image and determining our state (loading == true) we don't fire
        // preference changed, or repaint, we just reset the fWidth/fHeight as
        // necessary and return. This is ok as we know when loading finishes
        // it will pick up the new height/width, if necessary.
        public boolean imageUpdate(Image img, int flags, int x, int y,
                                   int newWidth, int newHeight ) {
            if (img != image && img != disabledImage ||
                image == null || getParent() == null) {

                return false;
            }

            // Bail out if there was an error:
            if ((flags & (ABORT|ERROR)) != 0) {
                repaint(0);
                synchronized(ImageView.this) {
                    if (image == img) {
                        // Be sure image hasn't changed since we don't
                        // initialy synchronize
                        image = null;
                        if ((state & WIDTH_FLAG) != WIDTH_FLAG) {
                            width = DEFAULT_WIDTH;
                        }
                        if ((state & HEIGHT_FLAG) != HEIGHT_FLAG) {
                            height = DEFAULT_HEIGHT;
                        }
                    } else {
                        disabledImage = null;
                    }
                    if ((state & LOADING_FLAG) == LOADING_FLAG) {
                        // No need to resize or repaint, still in the process
                        // of loading.
                        return false;
                    }
                }
                updateAltTextView();
                safePreferenceChanged();
                return false;
            }

            if (image == img) {
                // Resize image if necessary:
                short changed = 0;
                if ((flags & ImageObserver.HEIGHT) != 0 && !getElement().
                      getAttributes().isDefined(HTML.Attribute.HEIGHT)) {
                    changed |= 1;
                }
                if ((flags & ImageObserver.WIDTH) != 0 && !getElement().
                      getAttributes().isDefined(HTML.Attribute.WIDTH)) {
                    changed |= 2;
                }

                /**
                 * If the image properties (height and width) have been loaded,
                 * then figure out if scaling is necessary based on the
                 * specified HTML attributes.
                 */
                if (((flags & ImageObserver.HEIGHT) != 0) &&
                    ((flags & ImageObserver.WIDTH) != 0)) {
                        Dimension d = adjustWidthHeight(newWidth, newHeight);
                        newWidth = d.width;
                        newHeight = d.height;
                        changed |= 3;
                }
                synchronized(ImageView.this) {
                    if ((changed & 1) == 1 && (state & HEIGHT_FLAG) == 0) {
                        height = newHeight;
                    }
                    if ((changed & 2) == 2 && (state & WIDTH_FLAG) == 0) {
                        width = newWidth;
                    }
                    if ((state & LOADING_FLAG) == LOADING_FLAG) {
                        // No need to resize or repaint, still in the process of
                        // loading.
                        return true;
                    }
                }
                if (changed != 0) {
                    // May need to resize myself, asynchronously:
                    safePreferenceChanged();
                    return true;
                }
            }

            // Repaint when done or when new pixels arrive:
            if ((flags & (FRAMEBITS|ALLBITS)) != 0) {
                repaint(0);
            }
            else if ((flags & SOMEBITS) != 0 && sIsInc) {
                repaint(sIncRate);
            }
            return ((flags & ALLBITS) == 0);
        }
    }


    /**
     * ImageLabelView is used if the image can't be loaded, and
     * the attribute specified an alt attribute. It overriden a handle of
     * methods as the text is hardcoded and does not come from the document.
     */
    private class ImageLabelView extends InlineView {
        private Segment segment;
        private Color fg;

        ImageLabelView(Element e, String text) {
            super(e);
            reset(text);
        }

        public void reset(String text) {
            segment = new Segment(text.toCharArray(), 0, text.length());
        }

        public void paint(Graphics g, Shape a) {
            // Don't use supers paint, otherwise selection will be wrong
            // as our start/end offsets are fake.
            GlyphPainter painter = getGlyphPainter();

            if (painter != null) {
                g.setColor(getForeground());
                painter.paint(this, g, a, getStartOffset(), getEndOffset());
            }
        }

        public Segment getText(int p0, int p1) {
            if (p0 < 0 || p1 > segment.array.length) {
                throw new RuntimeException("ImageLabelView: Stale view");
            }
            segment.offset = p0;
            segment.count = p1 - p0;
            return segment;
        }

        public int getStartOffset() {
            return 0;
        }

        public int getEndOffset() {
            return segment.array.length;
        }

        public View breakView(int axis, int p0, float pos, float len) {
            // Don't allow a break
            return this;
        }

        public Color getForeground() {
            View parent;
            if (fg == null && (parent = getParent()) != null) {
                Document doc = getDocument();
                AttributeSet attr = parent.getAttributes();

                if (attr != null && (doc instanceof StyledDocument)) {
                    fg = ((StyledDocument)doc).getForeground(attr);
                }
            }
            return fg;
        }
    }
}
