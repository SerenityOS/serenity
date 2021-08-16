/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.image;

import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.ImageCapabilities;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import sun.awt.DisplayChangedListener;
import sun.java2d.SunGraphicsEnvironment;
import sun.java2d.SurfaceData;
import static sun.java2d.pipe.hw.AccelSurface.*;

/**
 * This SurfaceManager variant manages an accelerated volatile surface, if it
 * is possible to create that surface.  If there is limited accelerated
 * memory, or if the volatile surface disappears due to an operating system
 * event, the VolatileSurfaceManager will attempt to restore the
 * accelerated surface.  If that fails, a system memory surface will be
 * created in its place.
 */
public abstract class VolatileSurfaceManager
    extends SurfaceManager
    implements DisplayChangedListener
{
    /**
     * A reference to the VolatileImage whose contents are being managed.
     */
    protected SunVolatileImage vImg;

    /**
     * A reference to the AffineTransform corresponding to the graphics
     * configuration of the volatile image. Affine Transformation is usually
     * derived from the screen device. During the displayChanged() callback,
     * the existing transform is compared with the updated screen transform to
     * determine whether the software backed surface needs to be re-created
     */
    protected AffineTransform atCurrent;

    /**
     * The accelerated SurfaceData object.
     */
    protected SurfaceData sdAccel;

    /**
     * The software-based SurfaceData object.  Only create when first asked
     * to (otherwise it is a waste of memory as it will only be used in
     * situations of surface loss).
     */
    protected SurfaceData sdBackup;

    /**
     * The current SurfaceData object.
     */
    protected SurfaceData sdCurrent;

    /**
     * A record-keeping object.  This keeps track of which SurfaceData was
     * in use during the last call to validate().  This lets us see whether
     * the SurfaceData object has changed since then and allows us to return
     * the correct returnCode to the user in the validate() call.
     */
    protected SurfaceData sdPrevious;

    /**
     * Tracks loss of surface contents; queriable by user to see whether
     * contents need to be restored.
     */
    protected boolean lostSurface;

    /**
     * Context for extra initialization parameters.
     */
    protected Object context;

    protected VolatileSurfaceManager(SunVolatileImage vImg, Object context) {
        this.vImg = vImg;
        this.context = context;
        this.atCurrent = vImg.getGraphicsConfig().getDefaultTransform();

        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        // We could have a HeadlessGE at this point, so double-check before
        // assuming anything.
        if (ge instanceof SunGraphicsEnvironment) {
            ((SunGraphicsEnvironment)ge).addDisplayChangedListener(this);
        }
    }

    /**
     * This init function is separate from the constructor because the
     * things we are doing here necessitate the object's existence.
     * Otherwise, we end up calling into a subclass' overridden method
     * during construction, before that subclass is completely constructed.
     */
    public void initialize() {
        if (isAccelerationEnabled()) {
            sdAccel = initAcceleratedSurface();
            if (sdAccel != null) {
                sdCurrent = sdAccel;
            }
        }
        // only initialize the backup surface for images with unforced
        // acceleration type
        if (sdCurrent == null &&
            vImg.getForcedAccelSurfaceType() == UNDEFINED)
        {
            sdCurrent = getBackupSurface();
        }
    }

    public SurfaceData getPrimarySurfaceData() {
        return sdCurrent;
    }

    /**
     * Returns true if acceleration is enabled.  If not, we simply use the
     * backup SurfaceData object and return quickly from most methods
     * in this class.
     */
    protected abstract boolean isAccelerationEnabled();

    /**
     * Get the image ready for rendering.  This method is called to make
     * sure that the accelerated SurfaceData exists and is
     * ready to be used.  Users call this method prior to any set of
     * rendering to or from the image, to make sure the image is ready
     * and compatible with the given GraphicsConfiguration.
     *
     * The image may not be "ready" if either we had problems creating
     * it in the first place (e.g., there was no space in vram) or if
     * the surface became lost (e.g., some other app or the OS caused
     * vram surfaces to be removed).
     *
     * Note that we want to return RESTORED in any situation where the
     * SurfaceData is different than it was last time.  So whether it's
     * software or hardware, if we have a different SurfaceData object,
     * then the contents have been altered and we must reflect that
     * change to the user.
     */
    public int validate(GraphicsConfiguration gc) {
        int returnCode = VolatileImage.IMAGE_OK;
        boolean lostSurfaceTmp = lostSurface;
        lostSurface = false;

        if (isAccelerationEnabled()) {
            if (!isConfigValid(gc)) {
                // If we're asked to render to a different device than the
                // one we were created under, return INCOMPATIBLE error code.
                // Note that a null gc simply ignores the incompatibility
                // issue
                returnCode = VolatileImage.IMAGE_INCOMPATIBLE;
            } else if (sdAccel == null) {
                // We either had problems creating the surface or the display
                // mode changed and we nullified the old one.  Try it again.
                sdAccel = initAcceleratedSurface();
                if (sdAccel != null) {
                    // set the current SurfaceData to accelerated version
                    sdCurrent = sdAccel;
                    // we don't need the system memory surface anymore, so
                    // let's release it now (it can always be restored later)
                    sdBackup = null;
                    returnCode = VolatileImage.IMAGE_RESTORED;
                } else {
                    sdCurrent = getBackupSurface();
                }
            } else if (sdAccel.isSurfaceLost()) {
                try {
                    restoreAcceleratedSurface();
                    // set the current SurfaceData to accelerated version
                    sdCurrent = sdAccel;
                    // restoration successful: accel surface no longer lost
                    sdAccel.setSurfaceLost(false);
                    // we don't need the system memory surface anymore, so
                    // let's release it now (it can always be restored later)
                    sdBackup = null;
                    returnCode = VolatileImage.IMAGE_RESTORED;
                } catch (sun.java2d.InvalidPipeException e) {
                    // Set the current SurfaceData to software version so that
                    // drawing can continue.  Note that we still have
                    // the lostAccelSurface flag set so that we will continue
                    // to attempt to restore the accelerated surface.
                    sdCurrent = getBackupSurface();
                }
            } else if (lostSurfaceTmp) {
                // Something else triggered this loss/restoration.  Could
                // be a palette change that didn't require a SurfaceData
                // recreation but merely a re-rendering of the pixels.
                returnCode = VolatileImage.IMAGE_RESTORED;
            }
        } else if (sdAccel != null) {
            // if the "acceleration enabled" state changed to disabled,
            // switch to software surface
            sdCurrent = getBackupSurface();
            sdAccel = null;
            returnCode = VolatileImage.IMAGE_RESTORED;
        } else if (lostSurfaceTmp) {
            // A software surface has been restored. This could be due to
            // display mode change on a non-accelerated volatile image.
            returnCode = VolatileImage.IMAGE_RESTORED;
        }

        if ((returnCode != VolatileImage.IMAGE_INCOMPATIBLE) &&
            (sdCurrent != sdPrevious))
        {
            // contents have changed - return RESTORED to user
            sdPrevious = sdCurrent;
            returnCode = VolatileImage.IMAGE_RESTORED;
        }

        if (returnCode == VolatileImage.IMAGE_RESTORED) {
            // clear the current surface with the background color,
            // only if the surface has been restored
            initContents();
        }

        return returnCode;
    }

    /**
     * Returns true if rendering data was lost since the last validate call.
     *
     * @see java.awt.image.VolatileImage#contentsLost
     */
    public boolean contentsLost() {
        return lostSurface;
    }

    /**
     * Creates a new accelerated surface that is compatible with the
     * current GraphicsConfiguration.  Returns the new accelerated
     * SurfaceData object, or null if the surface creation was not successful.
     *
     * Platform-specific subclasses should initialize an accelerated
     * surface (e.g. a DirectDraw surface on Windows, an OpenGL FBO,
     * or an X11 pixmap).
     */
    protected abstract SurfaceData initAcceleratedSurface();

    /**
     * Creates a software-based surface (of type BufImgSurfaceData).
     * The software representation is only created when needed, which
     * is only during some situation in which the hardware surface
     * cannot be allocated.  This allows apps to at least run,
     * albeit more slowly than they would otherwise.
     */
    protected SurfaceData getBackupSurface() {
        if (sdBackup == null) {
            GraphicsConfiguration gc = vImg.getGraphicsConfig();
            AffineTransform tx = gc.getDefaultTransform();
            double scaleX = tx.getScaleX();
            double scaleY = tx.getScaleY();
            BufferedImage bImg = vImg.getBackupImage(scaleX, scaleY);
            // Sabotage the acceleration capabilities of the BufImg surface
            SunWritableRaster.stealTrackable(bImg
                                             .getRaster()
                                             .getDataBuffer()).setUntrackable();
            sdBackup = BufImgSurfaceData.createData(bImg, scaleX, scaleY);
        }
        return sdBackup;
    }

    /**
     * Set contents of the current SurfaceData to default state (i.e. clear
     * the background).
     */
    public void initContents() {
        // images with forced acceleration type may have a null sdCurrent
        // because we do not create a backup surface for them
        if (sdCurrent != null) {
            Graphics g = vImg.createGraphics();
            g.clearRect(0, 0, vImg.getWidth(), vImg.getHeight());
            g.dispose();
        }
    }

    /**
     * Called from a SurfaceData object, indicating that our
     * accelerated surface has been lost and should be restored (perhaps
     * using a backup system memory surface).  Returns the newly restored
     * primary SurfaceData object.
     */
    public SurfaceData restoreContents() {
        return getBackupSurface();
    }

    /**
     * If the accelerated surface is the current SurfaceData for this manager,
     * sets the variable lostSurface to true, which indicates that something
     * happened to the image under management.  This variable is used in the
     * validate method to tell the caller that the surface contents need to
     * be restored.
     */
    public void acceleratedSurfaceLost() {
        if (isAccelerationEnabled() && (sdCurrent == sdAccel)) {
            lostSurface = true;
        }
    }

    /**
     * Restore sdAccel in case it was lost.  Do nothing in this
     * default case; platform-specific implementations may do more in
     * this situation as appropriate.
     */
    protected void restoreAcceleratedSurface() {
    }

    /**
     * Called from SunGraphicsEnv when there has been a display mode change.
     * Note that we simply invalidate hardware surfaces here; we do not
     * attempt to recreate or re-render them.  This is to avoid threading
     * conflicts with the native toolkit and associated threads.  Instead,
     * we just nullify the old surface data object and wait for a future
     * method in the rendering process to recreate the surface.
     */
    public void displayChanged() {
        lostSurface = true;
        if (sdAccel != null) {
            // First, nullify the software surface.  This guards against
            // using a SurfaceData that was created in a different
            // display mode.
            sdBackup = null;
            // Now, invalidate the old hardware-based SurfaceData
            // Note that getBackupSurface may set sdAccel to null so we have to invalidate it before
            SurfaceData oldData = sdAccel;
            sdAccel = null;
            oldData.invalidate();
            sdCurrent = getBackupSurface();
        }
        // Update graphicsConfig for the vImg in case it changed due to
        // this display change event
        vImg.updateGraphicsConfig();

        // Compare the Graphics configuration transforms to determine
        // whether the software backed surface needs to be invalidated.
        AffineTransform atUpdated = vImg.getGraphicsConfig()
                                        .getDefaultTransform();
        if (!isAccelerationEnabled()) {
            if (!atUpdated.equals(atCurrent)) {
                // Ideally there is no need to re-create a software surface.
                // But some OSs allow changes to display state at runtime. Such
                // a provision would cause mismatch in graphics configuration of
                // the display and the surface. Hence we re-create the software
                // surface as well.
                sdBackup = null;
                sdCurrent = getBackupSurface();
            } else {
                // Software backed surface was not invalidated.
                lostSurface = false;
            }
        }

        // Update the AffineTransformation backing the volatile image
        atCurrent = atUpdated;
    }

    /**
     * When device palette changes, need to force a new copy
     * of the image into our hardware cache to update the
     * color indices of the pixels (indexed mode only).
     */
    public void paletteChanged() {
        lostSurface = true;
    }

    /**
     * Called by validate() to see whether the GC passed in is ok for
     * rendering to.  This generic implementation checks to see
     * whether the GC is either null or is from the same
     * device as the one that this image was created on.  Platform-
     * specific implementations may perform other checks as
     * appropriate.
     */
    protected boolean isConfigValid(GraphicsConfiguration gc) {
        return ((gc == null) ||
                (gc.getDevice() == vImg.getGraphicsConfig().getDevice()));
    }

    @Override
    public ImageCapabilities getCapabilities(GraphicsConfiguration gc) {
        if (isConfigValid(gc)) {
            return isAccelerationEnabled() ?
                new AcceleratedImageCapabilities() :
                new ImageCapabilities(false);
        }
        return super.getCapabilities(gc);
    }

    private class AcceleratedImageCapabilities
        extends ImageCapabilities
    {
        AcceleratedImageCapabilities() {
            super(false);
        }
        @Override
        public boolean isAccelerated() {
            return (sdCurrent == sdAccel);
        }
        @Override
        public boolean isTrueVolatile() {
            return isAccelerated();
        }
    }

    /**
     * Releases any associated hardware memory for this image by
     * calling flush on sdAccel.  This method forces a lostSurface
     * situation so any future operations on the image will need to
     * revalidate the image first.
     */
    public void flush() {
        lostSurface = true;
        SurfaceData oldSD = sdAccel;
        sdAccel = null;
        if (oldSD != null) {
            oldSD.flush();
        }
    }
}
