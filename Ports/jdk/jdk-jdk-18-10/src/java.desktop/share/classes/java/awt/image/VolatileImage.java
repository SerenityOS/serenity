/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.image;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.Toolkit;
import java.awt.Transparency;

/**
 * VolatileImage is an image which can lose its
 * contents at any time due to circumstances beyond the control of the
 * application (e.g., situations caused by the operating system or by
 * other applications). Because of the potential for hardware acceleration,
 * a VolatileImage object can have significant performance benefits on
 * some platforms.
 * <p>
 * The drawing surface of an image (the memory where the image contents
 * actually reside) can be lost or invalidated, causing the contents of that
 * memory to go away.  The drawing surface thus needs to be restored
 * or recreated and the contents of that surface need to be
 * re-rendered.  VolatileImage provides an interface for
 * allowing the user to detect these problems and fix them
 * when they occur.
 * <p>
 * When a VolatileImage object is created, limited system resources
 * such as video memory (VRAM) may be allocated in order to support
 * the image.
 * When a VolatileImage object is no longer used, it may be
 * garbage-collected and those system resources will be returned,
 * but this process does not happen at guaranteed times.
 * Applications that create many VolatileImage objects (for example,
 * a resizing window may force recreation of its back buffer as the
 * size changes) may run out of optimal system resources for new
 * VolatileImage objects simply because the old objects have not
 * yet been removed from the system.
 * (New VolatileImage objects may still be created, but they
 * may not perform as well as those created in accelerated
 * memory).
 * The flush method may be called at any time to proactively release
 * the resources used by a VolatileImage so that it does not prevent
 * subsequent VolatileImage objects from being accelerated.
 * In this way, applications can have more control over the state
 * of the resources taken up by obsolete VolatileImage objects.
 * <p>
 * This image should not be subclassed directly but should be created
 * by using the {@link java.awt.Component#createVolatileImage(int, int)
 * Component.createVolatileImage} or
 * {@link java.awt.GraphicsConfiguration#createCompatibleVolatileImage(int, int)
 * GraphicsConfiguration.createCompatibleVolatileImage(int, int)} methods.
 * <P>
 * An example of using a VolatileImage object follows:
 * <pre>
 * // image creation
 * VolatileImage vImg = createVolatileImage(w, h);
 *
 *
 * // rendering to the image
 * void renderOffscreen() {
 *      do {
 *          if (vImg.validate(getGraphicsConfiguration()) ==
 *              VolatileImage.IMAGE_INCOMPATIBLE)
 *          {
 *              // old vImg doesn't work with new GraphicsConfig; re-create it
 *              vImg = createVolatileImage(w, h);
 *          }
 *          Graphics2D g = vImg.createGraphics();
 *          //
 *          // miscellaneous rendering commands...
 *          //
 *          g.dispose();
 *      } while (vImg.contentsLost());
 * }
 *
 *
 * // copying from the image (here, gScreen is the Graphics
 * // object for the onscreen window)
 * do {
 *      int returnCode = vImg.validate(getGraphicsConfiguration());
 *      if (returnCode == VolatileImage.IMAGE_RESTORED) {
 *          // Contents need to be restored
 *          renderOffscreen();      // restore contents
 *      } else if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE) {
 *          // old vImg doesn't work with new GraphicsConfig; re-create it
 *          vImg = createVolatileImage(w, h);
 *          renderOffscreen();
 *      }
 *      gScreen.drawImage(vImg, 0, 0, this);
 * } while (vImg.contentsLost());
 * </pre>
 * <P>
 * Note that this class subclasses from the {@link Image} class, which
 * includes methods that take an {@link ImageObserver} parameter for
 * asynchronous notifications as information is received from
 * a potential {@link ImageProducer}.  Since this {@code VolatileImage}
 * is not loaded from an asynchronous source, the various methods that take
 * an {@code ImageObserver} parameter will behave as if the data has
 * already been obtained from the {@code ImageProducer}.
 * Specifically, this means that the return values from such methods
 * will never indicate that the information is not yet available and
 * the {@code ImageObserver} used in such methods will never
 * need to be recorded for an asynchronous callback notification.
 * @since 1.4
 */
public abstract class VolatileImage extends Image implements Transparency
{

    /**
     * Constructor for subclasses to call.
     */
    protected VolatileImage() {}

    // Return codes for validate() method

    /**
     * Validated image is ready to use as-is.
     */
    public static final int IMAGE_OK = 0;

    /**
     * Validated image has been restored and is now ready to use.
     * Note that restoration causes contents of the image to be lost.
     */
    public static final int IMAGE_RESTORED = 1;

    /**
     * Validated image is incompatible with supplied
     * {@code GraphicsConfiguration} object and should be
     * re-created as appropriate.  Usage of the image as-is
     * after receiving this return code from {@code validate}
     * is undefined.
     */
    public static final int IMAGE_INCOMPATIBLE = 2;

    /**
     * Returns a static snapshot image of this object.  The
     * {@code BufferedImage} returned is only current with
     * the {@code VolatileImage} at the time of the request
     * and will not be updated with any future changes to the
     * {@code VolatileImage}.
     * @return a {@link BufferedImage} representation of this
     *          {@code VolatileImage}
     * @see BufferedImage
     */
    public abstract BufferedImage getSnapshot();

    /**
     * Returns the width of the {@code VolatileImage}.
     * @return the width of this {@code VolatileImage}.
     */
    public abstract int getWidth();

    /**
     * Returns the height of the {@code VolatileImage}.
     * @return the height of this {@code VolatileImage}.
     */
    public abstract int getHeight();

    // Image overrides

    /**
     * This returns an ImageProducer for this VolatileImage.
     * Note that the VolatileImage object is optimized for
     * rendering operations and blitting to the screen or other
     * VolatileImage objects, as opposed to reading back the
     * pixels of the image.  Therefore, operations such as
     * {@code getSource} may not perform as fast as
     * operations that do not rely on reading the pixels.
     * Note also that the pixel values read from the image are current
     * with those in the image only at the time that they are
     * retrieved. This method takes a snapshot
     * of the image at the time the request is made and the
     * ImageProducer object returned works with
     * that static snapshot image, not the original VolatileImage.
     * Calling getSource()
     * is equivalent to calling getSnapshot().getSource().
     * @return an {@link ImageProducer} that can be used to produce the
     * pixels for a {@code BufferedImage} representation of
     * this Image.
     * @see ImageProducer
     * @see #getSnapshot()
     */
    public ImageProducer getSource() {
        // REMIND: Make sure this functionality is in line with the
        // spec.  In particular, we are returning the Source for a
        // static image (the snapshot), not a changing image (the
        // VolatileImage).  So if the user expects the Source to be
        // up-to-date with the current contents of the VolatileImage,
        // they will be disappointed...
        // REMIND: This assumes that getSnapshot() returns something
        // valid and not the default null object returned by this class
        // (so it assumes that the actual VolatileImage object is
        // subclassed off something that does the right thing
        // (e.g., SunVolatileImage).
        return getSnapshot().getSource();
    }

    // REMIND: if we want any decent performance for getScaledInstance(),
    // we should override the Image implementation of it...

    /**
     * This method returns a {@link Graphics2D}, but is here
     * for backwards compatibility.  {@link #createGraphics() createGraphics} is more
     * convenient, since it is declared to return a
     * {@code Graphics2D}.
     * @return a {@code Graphics2D}, which can be used to draw into
     *          this image.
     */
    public Graphics getGraphics() {
        return createGraphics();
    }

    /**
     * Creates a {@code Graphics2D}, which can be used to draw into
     * this {@code VolatileImage}.
     * @return a {@code Graphics2D}, used for drawing into this
     *          image.
     */
    public abstract Graphics2D createGraphics();


    // Volatile management methods

    /**
     * Attempts to restore the drawing surface of the image if the surface
     * had been lost since the last {@code validate} call.  Also
     * validates this image against the given GraphicsConfiguration
     * parameter to see whether operations from this image to the
     * GraphicsConfiguration are compatible.  An example of an
     * incompatible combination might be a situation where a VolatileImage
     * object was created on one graphics device and then was used
     * to render to a different graphics device.  Since VolatileImage
     * objects tend to be very device-specific, this operation might
     * not work as intended, so the return code from this validate
     * call would note that incompatibility.  A null or incorrect
     * value for gc may cause incorrect values to be returned from
     * {@code validate} and may cause later problems with rendering.
     *
     * @param   gc   a {@code GraphicsConfiguration} object for this
     *          image to be validated against.  A null gc implies that the
     *          validate method should skip the compatibility test.
     * @return  {@code IMAGE_OK} if the image did not need validation<BR>
     *          {@code IMAGE_RESTORED} if the image needed restoration.
     *          Restoration implies that the contents of the image may have
     *          been affected and the image may need to be re-rendered.<BR>
     *          {@code IMAGE_INCOMPATIBLE} if the image is incompatible
     *          with the {@code GraphicsConfiguration} object passed
     *          into the {@code validate} method.  Incompatibility
     *          implies that the image may need to be recreated with a
     *          new {@code Component} or
     *          {@code GraphicsConfiguration} in order to get an image
     *          that can be used successfully with this
     *          {@code GraphicsConfiguration}.
     *          An incompatible image is not checked for whether restoration
     *          was necessary, so the state of the image is unchanged
     *          after a return value of {@code IMAGE_INCOMPATIBLE}
     *          and this return value implies nothing about whether the
     *          image needs to be restored.
     * @see java.awt.GraphicsConfiguration
     * @see java.awt.Component
     * @see #IMAGE_OK
     * @see #IMAGE_RESTORED
     * @see #IMAGE_INCOMPATIBLE
     */
    public abstract int validate(GraphicsConfiguration gc);

    /**
     * Returns {@code true} if rendering data was lost since last
     * {@code validate} call.  This method should be called by the
     * application at the end of any series of rendering operations to
     * or from the image to see whether
     * the image needs to be validated and the rendering redone.
     * @return {@code true} if the drawing surface needs to be restored;
     * {@code false} otherwise.
     */
    public abstract boolean contentsLost();

    /**
     * Returns an ImageCapabilities object which can be
     * inquired as to the specific capabilities of this
     * VolatileImage.  This would allow programmers to find
     * out more runtime information on the specific VolatileImage
     * object that they have created.  For example, the user
     * might create a VolatileImage but the system may have
     * no video memory left for creating an image of that
     * size, so although the object is a VolatileImage, it is
     * not as accelerated as other VolatileImage objects on
     * this platform might be.  The user might want that
     * information to find other solutions to their problem.
     * @return an {@code ImageCapabilities} object that contains
     *         the capabilities of this {@code VolatileImage}.
     * @since 1.4
     */
    public abstract ImageCapabilities getCapabilities();

    /**
     * The transparency value with which this image was created.
     * @see java.awt.GraphicsConfiguration#createCompatibleVolatileImage(int,
     *      int,int)
     * @see java.awt.GraphicsConfiguration#createCompatibleVolatileImage(int,
     *      int,ImageCapabilities,int)
     * @see Transparency
     * @since 1.5
     */
    protected int transparency = TRANSLUCENT;

    /**
     * Returns the transparency.  Returns either OPAQUE, BITMASK,
     * or TRANSLUCENT.
     * @return the transparency of this {@code VolatileImage}.
     * @see Transparency#OPAQUE
     * @see Transparency#BITMASK
     * @see Transparency#TRANSLUCENT
     * @since 1.5
     */
    public int getTransparency() {
        return transparency;
    }
}
