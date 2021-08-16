/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.locks.*;
import sun.awt.AppContext;

/**
 * ImageCache - A fixed pixel count sized cache of Images keyed by arbitrary
 * set of arguments. All images are held with SoftReferences so they will be
 * dropped by the GC if heap memory gets tight. When our size hits max pixel
 * count least recently requested images are removed first.
 *
 * The ImageCache must be used from the thread with an AppContext only.
 *
 */
public final class ImageCache {

    // Ordered Map keyed by args hash, ordered by most recent accessed entry.
    private final LinkedHashMap<PixelsKey, ImageSoftReference> map
            = new LinkedHashMap<>(16, 0.75f, true);

    // Maximum number of pixels to cache, this is used if maxCount
    private final int maxPixelCount;
    // The current number of pixels stored in the cache
    private int currentPixelCount = 0;

    // Lock for concurrent access to map
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    // Reference queue for tracking lost softreferences to images in the cache
    private final ReferenceQueue<Image> referenceQueue = new ReferenceQueue<>();

    public static ImageCache getInstance() {
        return AppContext.getSoftReferenceValue(ImageCache.class,
                () -> new ImageCache());
    }

    ImageCache(final int maxPixelCount) {
        this.maxPixelCount = maxPixelCount;
    }

    ImageCache() {
        this((8 * 1024 * 1024) / 4); // 8Mb of pixels
    }

    public void flush() {
        lock.writeLock().lock();
        try {
            map.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Image getImage(final PixelsKey key){
        final ImageSoftReference ref;
        lock.readLock().lock();
        try {
            ref = map.get(key);
        } finally {
            lock.readLock().unlock();
        }
        return ref == null ? null : ref.get();
    }

    /**
     * Sets the cached image for the specified constraints.
     *
     * @param key The key with which the specified image is to be associated
     * @param image  The image to store in cache
     */
    public void setImage(final PixelsKey key, final Image image) {

        lock.writeLock().lock();
        try {
            ImageSoftReference ref = map.get(key);

            // check if currently in map
            if (ref != null) {
                if (ref.get() != null) {
                    return;
                }
                // soft image has been removed
                currentPixelCount -= key.getPixelCount();
                map.remove(key);
            };


            // add new image to pixel count
            final int newPixelCount = key.getPixelCount();
            currentPixelCount += newPixelCount;
            // clean out lost references if not enough space
            if (currentPixelCount > maxPixelCount) {
                while ((ref = (ImageSoftReference)referenceQueue.poll()) != null) {
                    //reference lost
                    map.remove(ref.key);
                    currentPixelCount -= ref.key.getPixelCount();
                }
            }

            // remove old items till there is enough free space
            if (currentPixelCount > maxPixelCount) {
                final Iterator<Map.Entry<PixelsKey, ImageSoftReference>>
                        mapIter = map.entrySet().iterator();
                while ((currentPixelCount > maxPixelCount) && mapIter.hasNext()) {
                    final Map.Entry<PixelsKey, ImageSoftReference> entry =
                            mapIter.next();
                    mapIter.remove();
                    final Image img = entry.getValue().get();
                    if (img != null) img.flush();
                    currentPixelCount -= entry.getValue().key.getPixelCount();
                }
            }

            // finally put new in map
            map.put(key, new ImageSoftReference(key, image, referenceQueue));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public interface PixelsKey {

        int getPixelCount();
    }

    private static class ImageSoftReference extends SoftReference<Image> {

        final PixelsKey key;

        ImageSoftReference(final PixelsKey key, final Image referent,
                final ReferenceQueue<? super Image> q) {
            super(referent, q);
            this.key = key;
        }
    }
}
