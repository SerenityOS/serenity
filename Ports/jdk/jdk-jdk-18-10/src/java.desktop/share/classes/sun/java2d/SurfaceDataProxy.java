/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.AlphaComposite;
import java.awt.GraphicsEnvironment;

import sun.awt.DisplayChangedListener;
import sun.java2d.StateTrackable.State;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.SurfaceType;
import sun.java2d.loops.Blit;
import sun.java2d.loops.BlitBg;
import sun.awt.image.SurfaceManager;
import sun.awt.image.SurfaceManager.FlushableCacheData;

import java.security.AccessController;
import sun.security.action.GetPropertyAction;

/**
 * The proxy class encapsulates the logic for managing alternate
 * SurfaceData representations of a primary SurfaceData.
 * The main class will handle tracking the state changes of the
 * primary SurfaceData and updating the associated SurfaceData
 * proxy variants.
 * <p>
 * Subclasses have 2 main responsibilities:
 * <ul>
 * <li> Override the isSupportedOperation() method to determine if
 *      a given operation can be accelerated with a given source
 *      SurfaceData
 * <li> Override the validateSurfaceData() method to create or update
 *      a given accelerated surface to hold the pixels for the indicated
 *      source SurfaceData
 * </ul>
 * If necessary, a subclass may also override the updateSurfaceData
 * method to transfer the pixels to the accelerated surface.
 * By default the parent class will transfer the pixels using a
 * standard Blit operation between the two SurfaceData objects.
 */
public abstract class SurfaceDataProxy
    implements DisplayChangedListener, SurfaceManager.FlushableCacheData
{
    private static boolean cachingAllowed;
    private static int defaultThreshold;

    static {
        cachingAllowed = true;
        @SuppressWarnings("removal")
        String manimg = AccessController.doPrivileged(
            new GetPropertyAction("sun.java2d.managedimages"));
        if (manimg != null && manimg.equals("false")) {
            cachingAllowed = false;
            System.out.println("Disabling managed images");
        }

        defaultThreshold = 1;
        @SuppressWarnings("removal")
        String num = AccessController.doPrivileged(
            new GetPropertyAction("sun.java2d.accthreshold"));
        if (num != null) {
            try {
                int parsed = Integer.parseInt(num);
                if (parsed >= 0) {
                    defaultThreshold = parsed;
                    System.out.println("New Default Acceleration Threshold: " +
                                       defaultThreshold);
                }
            } catch (NumberFormatException e) {
                System.err.println("Error setting new threshold:" + e);
            }
        }
    }

    public static boolean isCachingAllowed() {
        return cachingAllowed;
    }

    /**
     * Determine if an alternate form for the srcData is needed
     * and appropriate from the given operational parameters.
     */
    public abstract boolean isSupportedOperation(SurfaceData srcData,
                                                 int txtype,
                                                 CompositeType comp,
                                                 Color bgColor);

    /**
     * Construct an alternate form of the given SurfaceData.
     * The contents of the returned SurfaceData may be undefined
     * since the calling code will take care of updating the
     * contents with a subsequent call to updateSurfaceData.
     * <p>
     * If the method returns null then there was a problem with
     * allocating the accelerated surface.  The getRetryTracker()
     * method will be called to track when to attempt another
     * revalidation.
     */
    public abstract SurfaceData validateSurfaceData(SurfaceData srcData,
                                                    SurfaceData cachedData,
                                                    int w, int h);

    /**
     * If the subclass is unable to validate or create a cached
     * SurfaceData then this method will be used to get a
     * StateTracker object that will indicate when to attempt
     * to validate the surface again.  Subclasses may return
     * trackers which count down an ever increasing threshold
     * to provide hysteresis on creating surfaces during low
     * memory conditions.  The default implementation just waits
     * another "threshold" number of accesses before trying again.
     */
    public StateTracker getRetryTracker(SurfaceData srcData) {
        return new CountdownTracker(threshold);
    }

    public static class CountdownTracker implements StateTracker {
        private int countdown;

        public CountdownTracker(int threshold) {
            this.countdown = threshold;
        }

        public synchronized boolean isCurrent() {
            return (--countdown >= 0);
        }
    }

    /**
     * This instance is for cases where a caching implementation
     * determines that a particular source image will never need
     * to be cached - either the source SurfaceData was of an
     * incompatible type, or it was in an UNTRACKABLE state or
     * some other factor is discovered that permanently prevents
     * acceleration or caching.
     * This class optimally implements NOP variants of all necessary
     * methods to avoid caching with a minimum of fuss.
     */
    public static SurfaceDataProxy UNCACHED = new SurfaceDataProxy(0) {
        @Override
        public boolean isAccelerated() {
            return false;
        }

        @Override
        public boolean isSupportedOperation(SurfaceData srcData,
                                            int txtype,
                                            CompositeType comp,
                                            Color bgColor)
        {
            return false;
        }

        @Override
        public SurfaceData validateSurfaceData(SurfaceData srcData,
                                               SurfaceData cachedData,
                                               int w, int h)
        {
            throw new InternalError("UNCACHED should never validate SDs");
        }

        @Override
        public SurfaceData replaceData(SurfaceData srcData,
                                       int txtype,
                                       CompositeType comp,
                                       Color bgColor)
        {
            // Not necessary to override this, but doing so is faster
            return srcData;
        }
    };

    // The number of attempts to copy from a STABLE source before
    // a cached copy is created or updated.
    private int threshold;

    /*
     * Source tracking data
     *
     * Every time that srcTracker is out of date we will reset numtries
     * to threshold and set the cacheTracker to one that is non-current.
     * numtries will then count down to 0 at which point the cacheTracker
     * will remind us that we need to update the cachedSD before we can
     * use it.
     *
     * Note that since these fields interrelate we should synchronize
     * whenever we update them, but it should be OK to read them
     * without synchronization.
     */
    private StateTracker srcTracker;
    private int numtries;

    /*
     * Cached data
     *
     * We cache a SurfaceData created by the subclass in cachedSD and
     * track its state (isValid and !surfaceLost) in cacheTracker.
     *
     * Also, when we want to note that cachedSD needs to be updated
     * we replace the cacheTracker with a NEVER_CURRENT tracker which
     * will cause us to try to revalidate and update the surface on
     * next use.
     */
    private SurfaceData cachedSD;
    private StateTracker cacheTracker;

    /*
     * Are we still the best object to control caching of data
     * for the source image?
     */
    private boolean valid;

    /**
     * Create a SurfaceData proxy manager that attempts to create
     * and cache a variant copy of the source SurfaceData after
     * the default threshold number of attempts to copy from the
     * STABLE source.
     */
    public SurfaceDataProxy() {
        this(defaultThreshold);
    }

    /**
     * Create a SurfaceData proxy manager that attempts to create
     * and cache a variant copy of the source SurfaceData after
     * the specified threshold number of attempts to copy from
     * the STABLE source.
     */
    public SurfaceDataProxy(int threshold) {
        this.threshold = threshold;

        this.srcTracker = StateTracker.NEVER_CURRENT;
        // numtries will be reset on first use
        this.cacheTracker = StateTracker.NEVER_CURRENT;

        this.valid = true;
    }

    /**
     * Returns true iff this SurfaceData proxy is still the best
     * way to control caching of the given source on the given
     * destination.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Sets the valid state to false so that the next time this
     * proxy is fetched to generate a replacement SurfaceData,
     * the code in SurfaceData knows to replace the proxy first.
     */
    public void invalidate() {
        this.valid = false;
    }

    /**
     * Flush all cached resources as per the FlushableCacheData interface.
     * The deaccelerated parameter indicates if the flush is
     * happening because the associated surface is no longer
     * being accelerated (for instance the acceleration priority
     * is set below the threshold needed for acceleration).
     * Returns a boolean that indicates if the cached object is
     * no longer needed and should be removed from the cache.
     */
    public boolean flush(boolean deaccelerated) {
        if (deaccelerated) {
            invalidate();
        }
        flush();
        return !isValid();
    }

    /**
     * Actively flushes (drops and invalidates) the cached surface
     * so that it can be reclaimed quickly.
     */
    public synchronized void flush() {
        SurfaceData csd = this.cachedSD;
        this.cachedSD = null;
        this.cacheTracker = StateTracker.NEVER_CURRENT;
        if (csd != null) {
            csd.flush();
        }
    }

    /**
     * Returns true iff this SurfaceData proxy is still valid
     * and if it has a currently cached replacement that is also
     * valid and current.
     */
    public boolean isAccelerated() {
        return (isValid() &&
                srcTracker.isCurrent() &&
                cacheTracker.isCurrent());
    }

    /**
     * This method should be called from subclasses which create
     * cached SurfaceData objects that depend on the current
     * properties of the display.
     */
    protected void activateDisplayListener() {
        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        // We could have a HeadlessGE at this point, so double-check before
        // assuming anything.
        // Also, no point in listening to display change events if
        // the image is never going to be accelerated.
        if (ge instanceof SunGraphicsEnvironment) {
            ((SunGraphicsEnvironment)ge).addDisplayChangedListener(this);
        }
    }

    /**
     * Invoked when the display mode has changed.
     * This method will invalidate and drop the internal cachedSD object.
     */
    public void displayChanged() {
        flush();
    }

    /**
     * Invoked when the palette has changed.
     */
    public void paletteChanged() {
        // We could potentially get away with just resetting cacheTracker
        // here but there is a small window of vulnerability in the
        // replaceData method where we could be just finished with
        // updating the cachedSD when this method is called and even
        // though we set a non-current cacheTracker here it will then
        // immediately get set to a current one by the thread that is
        // updating the cachedSD.  It is safer to just replace the
        // srcTracker with a non-current version that will trigger a
        // full update cycle the next time this proxy is used.
        // The downside is having to go through a full threshold count
        // before we can update and use our cache again, but palette
        // changes should be relatively rare...
        this.srcTracker = StateTracker.NEVER_CURRENT;
    }

    /**
     * This method attempts to replace the srcData with a cached version.
     * It relies on the subclass to determine if the cached version will
     * be useful given the operational parameters.
     * This method checks any preexisting cached copy for being "up to date"
     * and tries to update it if it is stale or non-existant and the
     * appropriate number of accesses have occurred since it last was stale.
     * <p>
     * An outline of the process is as follows:
     * <ol>
     * <li> Check the operational parameters (txtype, comp, bgColor)
     *      to make sure that the operation is supported.  Return the
     *      original SurfaceData if the operation cannot be accelerated.
     * <li> Check the tracker for the source surface to see if it has
     *      remained stable since it was last cached.  Update the state
     *      variables to cause both a threshold countdown and an update
     *      of the cached copy if it is not.  (Setting cacheTracker to
     *      NEVER_CURRENT effectively marks it as "needing to be updated".)
     * <li> Check the tracker for the cached copy to see if is still
     *      valid and up to date.  Note that the cacheTracker may be
     *      non-current if either something happened to the cached copy
     *      (eg. surfaceLost) or if the source was out of date and the
     *      cacheTracker was set to NEVER_CURRENT to force an update.
     *      Decrement the countdown and copy the source to the cache
     *      as necessary and then update the variables to show that
     *      the cached copy is stable.
     * </ol>
     */
    public SurfaceData replaceData(SurfaceData srcData,
                                   int txtype,
                                   CompositeType comp,
                                   Color bgColor)
    {
        if (isSupportedOperation(srcData, txtype, comp, bgColor)) {
            // First deal with tracking the source.
            if (!srcTracker.isCurrent()) {
                synchronized (this) {
                    this.numtries = threshold;
                    this.srcTracker = srcData.getStateTracker();
                    this.cacheTracker = StateTracker.NEVER_CURRENT;
                }

                if (!srcTracker.isCurrent()) {
                    // Dynamic or Untrackable (or a very recent modification)
                    if (srcData.getState() == State.UNTRACKABLE) {
                        // UNTRACKABLE means we can never cache again.

                        // Invalidate so we get replaced next time we are used
                        // (presumably with an UNCACHED proxy).
                        invalidate();

                        // Aggressively drop our reference to the cachedSD
                        // in case this proxy is not consulted again (and
                        // thus replaced) for a long time.
                        flush();
                    }
                    return srcData;
                }
            }

            // Then deal with checking the validity of the cached SurfaceData
            SurfaceData csd = this.cachedSD;
            if (!cacheTracker.isCurrent()) {
                // Next make sure the dust has settled
                synchronized (this) {
                    if (numtries > 0) {
                        --numtries;
                        return srcData;
                    }
                }

                Rectangle r = srcData.getBounds();
                int w = r.width;
                int h = r.height;

                // Snapshot the tracker in case it changes while
                // we are updating the cached SD...
                StateTracker curTracker = srcTracker;

                csd = validateSurfaceData(srcData, csd, w, h);
                if (csd == null) {
                    synchronized (this) {
                        if (curTracker == srcTracker) {
                            this.cacheTracker = getRetryTracker(srcData);
                            this.cachedSD = null;
                        }
                    }
                    return srcData;
                }

                updateSurfaceData(srcData, csd, w, h);
                if (!csd.isValid()) {
                    return srcData;
                }

                synchronized (this) {
                    // We only reset these variables if the tracker from
                    // before the surface update is still in use and current
                    // Note that we must use a srcTracker that was fetched
                    // from before the update process to make sure that we
                    // do not lose some pixel changes in the shuffle.
                    if (curTracker == srcTracker && curTracker.isCurrent()) {
                        this.cacheTracker = csd.getStateTracker();
                        this.cachedSD = csd;
                    }
                }
            }

            if (csd != null) {
                return csd;
            }
        }

        return srcData;
    }

    /**
     * This is the default implementation for updating the cached
     * SurfaceData from the source (primary) SurfaceData.
     * A simple Blit is used to copy the pixels from the source to
     * the destination SurfaceData.
     * A subclass can override this implementation if a more complex
     * operation is required to update its cached copies.
     */
    public void updateSurfaceData(SurfaceData srcData,
                                  SurfaceData dstData,
                                  int w, int h)
    {
        SurfaceType srcType = srcData.getSurfaceType();
        SurfaceType dstType = dstData.getSurfaceType();
        Blit blit = Blit.getFromCache(srcType,
                                      CompositeType.SrcNoEa,
                                      dstType);
        blit.Blit(srcData, dstData,
                  AlphaComposite.Src, null,
                  0, 0, 0, 0, w, h);
        dstData.markDirty();
    }

    /**
     * This is an alternate implementation for updating the cached
     * SurfaceData from the source (primary) SurfaceData using a
     * background color for transparent pixels.
     * A simple BlitBg is used to copy the pixels from the source to
     * the destination SurfaceData with the specified bgColor.
     * A subclass can override the normal updateSurfaceData method
     * and call this implementation instead if it wants to use color
     * keying for bitmask images.
     */
    public void updateSurfaceDataBg(SurfaceData srcData,
                                    SurfaceData dstData,
                                    int w, int h, Color bgColor)
    {
        SurfaceType srcType = srcData.getSurfaceType();
        SurfaceType dstType = dstData.getSurfaceType();
        BlitBg blitbg = BlitBg.getFromCache(srcType,
                                            CompositeType.SrcNoEa,
                                            dstType);
        blitbg.BlitBg(srcData, dstData,
                      AlphaComposite.Src, null, bgColor.getRGB(),
                      0, 0, 0, 0, w, h);
        dstData.markDirty();
    }
}
