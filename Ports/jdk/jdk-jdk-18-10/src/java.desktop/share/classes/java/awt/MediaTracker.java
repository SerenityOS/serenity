/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.awt.image.ImageObserver;
import java.io.Serial;

import sun.awt.image.MultiResolutionToolkitImage;

/**
 * The {@code MediaTracker} class is a utility class to track
 * the status of a number of media objects. Media objects could
 * include audio clips as well as images, though currently only
 * images are supported.
 * <p>
 * To use a media tracker, create an instance of
 * {@code MediaTracker} and call its {@code addImage}
 * method for each image to be tracked. In addition, each image can
 * be assigned a unique identifier. This identifier controls the
 * priority order in which the images are fetched. It can also be used
 * to identify unique subsets of the images that can be waited on
 * independently. Images with a lower ID are loaded in preference to
 * those with a higher ID number.
 *
 * <p>
 *
 * Tracking an animated image
 * might not always be useful
 * due to the multi-part nature of animated image
 * loading and painting,
 * but it is supported.
 * {@code MediaTracker} treats an animated image
 * as completely loaded
 * when the first frame is completely loaded.
 * At that point, the {@code MediaTracker}
 * signals any waiters
 * that the image is completely loaded.
 * If no {@code ImageObserver}s are observing the image
 * when the first frame has finished loading,
 * the image might flush itself
 * to conserve resources
 * (see {@link Image#flush()}).
 *
 * <p>
 * Here is an example of using {@code MediaTracker}:
 *
 * <hr><blockquote><pre>{@code
 * import java.applet.Applet;
 * import java.awt.Color;
 * import java.awt.Image;
 * import java.awt.Graphics;
 * import java.awt.MediaTracker;
 *
 * public class ImageBlaster extends Applet implements Runnable {
 *      MediaTracker tracker;
 *      Image bg;
 *      Image anim[] = new Image[5];
 *      int index;
 *      Thread animator;
 *
 *      // Get the images for the background (id == 0)
 *      // and the animation frames (id == 1)
 *      // and add them to the MediaTracker
 *      public void init() {
 *          tracker = new MediaTracker(this);
 *          bg = getImage(getDocumentBase(),
 *                  "images/background.gif");
 *          tracker.addImage(bg, 0);
 *          for (int i = 0; i < 5; i++) {
 *              anim[i] = getImage(getDocumentBase(),
 *                      "images/anim"+i+".gif");
 *              tracker.addImage(anim[i], 1);
 *          }
 *      }
 *
 *      // Start the animation thread.
 *      public void start() {
 *          animator = new Thread(this);
 *          animator.start();
 *      }
 *
 *      // Stop the animation thread.
 *      public void stop() {
 *          animator = null;
 *      }
 *
 *      // Run the animation thread.
 *      // First wait for the background image to fully load
 *      // and paint.  Then wait for all of the animation
 *      // frames to finish loading. Finally, loop and
 *      // increment the animation frame index.
 *      public void run() {
 *          try {
 *              tracker.waitForID(0);
 *              tracker.waitForID(1);
 *          } catch (InterruptedException e) {
 *              return;
 *          }
 *          Thread me = Thread.currentThread();
 *          while (animator == me) {
 *              try {
 *                  Thread.sleep(100);
 *              } catch (InterruptedException e) {
 *                  break;
 *              }
 *              synchronized (this) {
 *                  index++;
 *                  if (index >= anim.length) {
 *                      index = 0;
 *                  }
 *              }
 *              repaint();
 *          }
 *      }
 *
 *      // The background image fills the frame so we
 *      // don't need to clear the applet on repaints.
 *      // Just call the paint method.
 *      public void update(Graphics g) {
 *          paint(g);
 *      }
 *
 *      // Paint a large red rectangle if there are any errors
 *      // loading the images.  Otherwise always paint the
 *      // background so that it appears incrementally as it
 *      // is loading.  Finally, only paint the current animation
 *      // frame if all of the frames (id == 1) are done loading,
 *      // so that we don't get partial animations.
 *      public void paint(Graphics g) {
 *          if ((tracker.statusAll(false) & MediaTracker.ERRORED) != 0) {
 *              g.setColor(Color.red);
 *              g.fillRect(0, 0, size().width, size().height);
 *              return;
 *          }
 *          g.drawImage(bg, 0, 0, this);
 *          if (tracker.statusID(1, false) == MediaTracker.COMPLETE) {
 *              g.drawImage(anim[index], 10, 10, this);
 *          }
 *      }
 * }
 * } </pre></blockquote><hr>
 *
 * @author      Jim Graham
 * @since       1.0
 */
public class MediaTracker implements java.io.Serializable {

    /**
     * A given {@code Component} that will be
     * tracked by a media tracker where the image will
     * eventually be drawn.
     *
     * @serial
     * @see #MediaTracker(Component)
     */
    Component target;
    /**
     * The head of the list of {@code Images} that is being
     * tracked by the {@code MediaTracker}.
     *
     * @serial
     * @see #addImage(Image, int)
     * @see #removeImage(Image)
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    MediaEntry head;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -483174189758638095L;

    /**
     * Creates a media tracker to track images for a given component.
     * @param     comp the component on which the images
     *                     will eventually be drawn
     */
    public MediaTracker(Component comp) {
        target = comp;
    }

    /**
     * Adds an image to the list of images being tracked by this media
     * tracker. The image will eventually be rendered at its default
     * (unscaled) size.
     * @param     image   the image to be tracked
     * @param     id      an identifier used to track this image
     */
    public void addImage(Image image, int id) {
        addImage(image, id, -1, -1);
    }

    /**
     * Adds a scaled image to the list of images being tracked
     * by this media tracker. The image will eventually be
     * rendered at the indicated width and height.
     *
     * @param     image   the image to be tracked
     * @param     id   an identifier that can be used to track this image
     * @param     w    the width at which the image is rendered
     * @param     h    the height at which the image is rendered
     */
    public synchronized void addImage(Image image, int id, int w, int h) {
        addImageImpl(image, id, w, h);
        Image rvImage = getResolutionVariant(image);
        if (rvImage != null) {
            addImageImpl(rvImage, id,
                    w == -1 ? -1 : 2 * w,
                    h == -1 ? -1 : 2 * h);
        }
    }

    private void addImageImpl(Image image, int id, int w, int h) {
        head = MediaEntry.insert(head,
                                 new ImageMediaEntry(this, image, id, w, h));
    }
    /**
     * Flag indicating that media is currently being loaded.
     * @see         java.awt.MediaTracker#statusAll
     * @see         java.awt.MediaTracker#statusID
     */
    public static final int LOADING = 1;

    /**
     * Flag indicating that the downloading of media was aborted.
     * @see         java.awt.MediaTracker#statusAll
     * @see         java.awt.MediaTracker#statusID
     */
    public static final int ABORTED = 2;

    /**
     * Flag indicating that the downloading of media encountered
     * an error.
     * @see         java.awt.MediaTracker#statusAll
     * @see         java.awt.MediaTracker#statusID
     */
    public static final int ERRORED = 4;

    /**
     * Flag indicating that the downloading of media was completed
     * successfully.
     * @see         java.awt.MediaTracker#statusAll
     * @see         java.awt.MediaTracker#statusID
     */
    public static final int COMPLETE = 8;

    static final int DONE = (ABORTED | ERRORED | COMPLETE);

    /**
     * Checks to see if all images being tracked by this media tracker
     * have finished loading.
     * <p>
     * This method does not start loading the images if they are not
     * already loading.
     * <p>
     * If there is an error while loading or scaling an image, then that
     * image is considered to have finished loading. Use the
     * {@code isErrorAny} or {@code isErrorID} methods to
     * check for errors.
     * @return      {@code true} if all images have finished loading,
     *                       have been aborted, or have encountered
     *                       an error; {@code false} otherwise
     * @see         java.awt.MediaTracker#checkAll(boolean)
     * @see         java.awt.MediaTracker#checkID
     * @see         java.awt.MediaTracker#isErrorAny
     * @see         java.awt.MediaTracker#isErrorID
     */
    public boolean checkAll() {
        return checkAll(false, true);
    }

    /**
     * Checks to see if all images being tracked by this media tracker
     * have finished loading.
     * <p>
     * If the value of the {@code load} flag is {@code true},
     * then this method starts loading any images that are not yet
     * being loaded.
     * <p>
     * If there is an error while loading or scaling an image, that
     * image is considered to have finished loading. Use the
     * {@code isErrorAny} and {@code isErrorID} methods to
     * check for errors.
     * @param       load   if {@code true}, start loading any
     *                       images that are not yet being loaded
     * @return      {@code true} if all images have finished loading,
     *                       have been aborted, or have encountered
     *                       an error; {@code false} otherwise
     * @see         java.awt.MediaTracker#checkID
     * @see         java.awt.MediaTracker#checkAll()
     * @see         java.awt.MediaTracker#isErrorAny()
     * @see         java.awt.MediaTracker#isErrorID(int)
     */
    public boolean checkAll(boolean load) {
        return checkAll(load, true);
    }

    private synchronized boolean checkAll(boolean load, boolean verify) {
        MediaEntry cur = head;
        boolean done = true;
        while (cur != null) {
            if ((cur.getStatus(load, verify) & DONE) == 0) {
                done = false;
            }
            cur = cur.next;
        }
        return done;
    }

    /**
     * Checks the error status of all of the images.
     * @return   {@code true} if any of the images tracked
     *                  by this media tracker had an error during
     *                  loading; {@code false} otherwise
     * @see      java.awt.MediaTracker#isErrorID
     * @see      java.awt.MediaTracker#getErrorsAny
     */
    public synchronized boolean isErrorAny() {
        MediaEntry cur = head;
        while (cur != null) {
            if ((cur.getStatus(false, true) & ERRORED) != 0) {
                return true;
            }
            cur = cur.next;
        }
        return false;
    }

    /**
     * Returns a list of all media that have encountered an error.
     * @return       an array of media objects tracked by this
     *                        media tracker that have encountered
     *                        an error, or {@code null} if
     *                        there are none with errors
     * @see          java.awt.MediaTracker#isErrorAny
     * @see          java.awt.MediaTracker#getErrorsID
     */
    public synchronized Object[] getErrorsAny() {
        MediaEntry cur = head;
        int numerrors = 0;
        while (cur != null) {
            if ((cur.getStatus(false, true) & ERRORED) != 0) {
                numerrors++;
            }
            cur = cur.next;
        }
        if (numerrors == 0) {
            return null;
        }
        Object[] errors = new Object[numerrors];
        cur = head;
        numerrors = 0;
        while (cur != null) {
            if ((cur.getStatus(false, false) & ERRORED) != 0) {
                errors[numerrors++] = cur.getMedia();
            }
            cur = cur.next;
        }
        return errors;
    }

    /**
     * Starts loading all images tracked by this media tracker. This
     * method waits until all the images being tracked have finished
     * loading.
     * <p>
     * If there is an error while loading or scaling an image, then that
     * image is considered to have finished loading. Use the
     * {@code isErrorAny} or {@code isErrorID} methods to
     * check for errors.
     * @see         java.awt.MediaTracker#waitForID(int)
     * @see         java.awt.MediaTracker#waitForAll(long)
     * @see         java.awt.MediaTracker#isErrorAny
     * @see         java.awt.MediaTracker#isErrorID
     * @exception   InterruptedException  if any thread has
     *                                     interrupted this thread
     */
    public void waitForAll() throws InterruptedException {
        waitForAll(0);
    }

    /**
     * Starts loading all images tracked by this media tracker. This
     * method waits until all the images being tracked have finished
     * loading, or until the length of time specified in milliseconds
     * by the {@code ms} argument has passed.
     * <p>
     * If there is an error while loading or scaling an image, then
     * that image is considered to have finished loading. Use the
     * {@code isErrorAny} or {@code isErrorID} methods to
     * check for errors.
     * @param       ms       the number of milliseconds to wait
     *                       for the loading to complete
     * @return      {@code true} if all images were successfully
     *                       loaded; {@code false} otherwise
     * @see         java.awt.MediaTracker#waitForID(int)
     * @see         java.awt.MediaTracker#waitForAll(long)
     * @see         java.awt.MediaTracker#isErrorAny
     * @see         java.awt.MediaTracker#isErrorID
     * @exception   InterruptedException  if any thread has
     *                                     interrupted this thread.
     */
    public synchronized boolean waitForAll(long ms)
        throws InterruptedException
    {
        long end = System.currentTimeMillis() + ms;
        boolean first = true;
        while (true) {
            int status = statusAll(first, first);
            if ((status & LOADING) == 0) {
                return (status == COMPLETE);
            }
            first = false;
            long timeout;
            if (ms == 0) {
                timeout = 0;
            } else {
                timeout = end - System.currentTimeMillis();
                if (timeout <= 0) {
                    return false;
                }
            }
            wait(timeout);
        }
    }

    /**
     * Calculates and returns the bitwise inclusive <b>OR</b> of the
     * status of all media that are tracked by this media tracker.
     * <p>
     * Possible flags defined by the
     * {@code MediaTracker} class are {@code LOADING},
     * {@code ABORTED}, {@code ERRORED}, and
     * {@code COMPLETE}. An image that hasn't started
     * loading has zero as its status.
     * <p>
     * If the value of {@code load} is {@code true}, then
     * this method starts loading any images that are not yet being loaded.
     *
     * @param        load   if {@code true}, start loading
     *                            any images that are not yet being loaded
     * @return       the bitwise inclusive <b>OR</b> of the status of
     *                            all of the media being tracked
     * @see          java.awt.MediaTracker#statusID(int, boolean)
     * @see          java.awt.MediaTracker#LOADING
     * @see          java.awt.MediaTracker#ABORTED
     * @see          java.awt.MediaTracker#ERRORED
     * @see          java.awt.MediaTracker#COMPLETE
     */
    public int statusAll(boolean load) {
        return statusAll(load, true);
    }

    private synchronized int statusAll(boolean load, boolean verify) {
        MediaEntry cur = head;
        int status = 0;
        while (cur != null) {
            status = status | cur.getStatus(load, verify);
            cur = cur.next;
        }
        return status;
    }

    /**
     * Checks to see if all images tracked by this media tracker that
     * are tagged with the specified identifier have finished loading.
     * <p>
     * This method does not start loading the images if they are not
     * already loading.
     * <p>
     * If there is an error while loading or scaling an image, then that
     * image is considered to have finished loading. Use the
     * {@code isErrorAny} or {@code isErrorID} methods to
     * check for errors.
     * @param       id   the identifier of the images to check
     * @return      {@code true} if all images have finished loading,
     *                       have been aborted, or have encountered
     *                       an error; {@code false} otherwise
     * @see         java.awt.MediaTracker#checkID(int, boolean)
     * @see         java.awt.MediaTracker#checkAll()
     * @see         java.awt.MediaTracker#isErrorAny()
     * @see         java.awt.MediaTracker#isErrorID(int)
     */
    public boolean checkID(int id) {
        return checkID(id, false, true);
    }

    /**
     * Checks to see if all images tracked by this media tracker that
     * are tagged with the specified identifier have finished loading.
     * <p>
     * If the value of the {@code load} flag is {@code true},
     * then this method starts loading any images that are not yet
     * being loaded.
     * <p>
     * If there is an error while loading or scaling an image, then that
     * image is considered to have finished loading. Use the
     * {@code isErrorAny} or {@code isErrorID} methods to
     * check for errors.
     * @param       id       the identifier of the images to check
     * @param       load     if {@code true}, start loading any
     *                       images that are not yet being loaded
     * @return      {@code true} if all images have finished loading,
     *                       have been aborted, or have encountered
     *                       an error; {@code false} otherwise
     * @see         java.awt.MediaTracker#checkID(int, boolean)
     * @see         java.awt.MediaTracker#checkAll()
     * @see         java.awt.MediaTracker#isErrorAny()
     * @see         java.awt.MediaTracker#isErrorID(int)
     */
    public boolean checkID(int id, boolean load) {
        return checkID(id, load, true);
    }

    private synchronized boolean checkID(int id, boolean load, boolean verify)
    {
        MediaEntry cur = head;
        boolean done = true;
        while (cur != null) {
            if (cur.getID() == id
                && (cur.getStatus(load, verify) & DONE) == 0)
            {
                done = false;
            }
            cur = cur.next;
        }
        return done;
    }

    /**
     * Checks the error status of all of the images tracked by this
     * media tracker with the specified identifier.
     * @param        id   the identifier of the images to check
     * @return       {@code true} if any of the images with the
     *                          specified identifier had an error during
     *                          loading; {@code false} otherwise
     * @see          java.awt.MediaTracker#isErrorAny
     * @see          java.awt.MediaTracker#getErrorsID
     */
    public synchronized boolean isErrorID(int id) {
        MediaEntry cur = head;
        while (cur != null) {
            if (cur.getID() == id
                && (cur.getStatus(false, true) & ERRORED) != 0)
            {
                return true;
            }
            cur = cur.next;
        }
        return false;
    }

    /**
     * Returns a list of media with the specified ID that
     * have encountered an error.
     * @param       id   the identifier of the images to check
     * @return      an array of media objects tracked by this media
     *                       tracker with the specified identifier
     *                       that have encountered an error, or
     *                       {@code null} if there are none with errors
     * @see         java.awt.MediaTracker#isErrorID
     * @see         java.awt.MediaTracker#isErrorAny
     * @see         java.awt.MediaTracker#getErrorsAny
     */
    public synchronized Object[] getErrorsID(int id) {
        MediaEntry cur = head;
        int numerrors = 0;
        while (cur != null) {
            if (cur.getID() == id
                && (cur.getStatus(false, true) & ERRORED) != 0)
            {
                numerrors++;
            }
            cur = cur.next;
        }
        if (numerrors == 0) {
            return null;
        }
        Object[] errors = new Object[numerrors];
        cur = head;
        numerrors = 0;
        while (cur != null) {
            if (cur.getID() == id
                && (cur.getStatus(false, false) & ERRORED) != 0)
            {
                errors[numerrors++] = cur.getMedia();
            }
            cur = cur.next;
        }
        return errors;
    }

    /**
     * Starts loading all images tracked by this media tracker with the
     * specified identifier. This method waits until all the images with
     * the specified identifier have finished loading.
     * <p>
     * If there is an error while loading or scaling an image, then that
     * image is considered to have finished loading. Use the
     * {@code isErrorAny} and {@code isErrorID} methods to
     * check for errors.
     * @param         id   the identifier of the images to check
     * @see           java.awt.MediaTracker#waitForAll
     * @see           java.awt.MediaTracker#isErrorAny()
     * @see           java.awt.MediaTracker#isErrorID(int)
     * @exception     InterruptedException  if any thread has
     *                          interrupted this thread.
     */
    public void waitForID(int id) throws InterruptedException {
        waitForID(id, 0);
    }

    /**
     * Starts loading all images tracked by this media tracker with the
     * specified identifier. This method waits until all the images with
     * the specified identifier have finished loading, or until the
     * length of time specified in milliseconds by the {@code ms}
     * argument has passed.
     * <p>
     * If there is an error while loading or scaling an image, then that
     * image is considered to have finished loading. Use the
     * {@code statusID}, {@code isErrorID}, and
     * {@code isErrorAny} methods to check for errors.
     * @param  id the identifier of the images to check
     * @param  ms the length of time, in milliseconds, to wait
     *         for the loading to complete
     * @return {@code true} if the loading completed in time;
     *         otherwise {@code false}
     * @see           java.awt.MediaTracker#waitForAll
     * @see           java.awt.MediaTracker#waitForID(int)
     * @see           java.awt.MediaTracker#statusID
     * @see           java.awt.MediaTracker#isErrorAny()
     * @see           java.awt.MediaTracker#isErrorID(int)
     * @exception     InterruptedException  if any thread has
     *                          interrupted this thread.
     */
    public synchronized boolean waitForID(int id, long ms)
        throws InterruptedException
    {
        long end = System.currentTimeMillis() + ms;
        boolean first = true;
        while (true) {
            int status = statusID(id, first, first);
            if ((status & LOADING) == 0) {
                return (status == COMPLETE);
            }
            first = false;
            long timeout;
            if (ms == 0) {
                timeout = 0;
            } else {
                timeout = end - System.currentTimeMillis();
                if (timeout <= 0) {
                    return false;
                }
            }
            wait(timeout);
        }
    }

    /**
     * Calculates and returns the bitwise inclusive <b>OR</b> of the
     * status of all media with the specified identifier that are
     * tracked by this media tracker.
     * <p>
     * Possible flags defined by the
     * {@code MediaTracker} class are {@code LOADING},
     * {@code ABORTED}, {@code ERRORED}, and
     * {@code COMPLETE}. An image that hasn't started
     * loading has zero as its status.
     * <p>
     * If the value of {@code load} is {@code true}, then
     * this method starts loading any images that are not yet being loaded.
     * @param        id   the identifier of the images to check
     * @param        load   if {@code true}, start loading
     *                            any images that are not yet being loaded
     * @return       the bitwise inclusive <b>OR</b> of the status of
     *                            all of the media with the specified
     *                            identifier that are being tracked
     * @see          java.awt.MediaTracker#statusAll(boolean)
     * @see          java.awt.MediaTracker#LOADING
     * @see          java.awt.MediaTracker#ABORTED
     * @see          java.awt.MediaTracker#ERRORED
     * @see          java.awt.MediaTracker#COMPLETE
     */
    public int statusID(int id, boolean load) {
        return statusID(id, load, true);
    }

    private synchronized int statusID(int id, boolean load, boolean verify) {
        MediaEntry cur = head;
        int status = 0;
        while (cur != null) {
            if (cur.getID() == id) {
                status = status | cur.getStatus(load, verify);
            }
            cur = cur.next;
        }
        return status;
    }

    /**
     * Removes the specified image from this media tracker.
     * All instances of the specified image are removed,
     * regardless of scale or ID.
     * @param   image     the image to be removed
     * @see     java.awt.MediaTracker#removeImage(java.awt.Image, int)
     * @see     java.awt.MediaTracker#removeImage(java.awt.Image, int, int, int)
     * @since   1.1
     */
    public synchronized void removeImage(Image image) {
        removeImageImpl(image);
        Image rvImage = getResolutionVariant(image);
        if (rvImage != null) {
            removeImageImpl(rvImage);
        }
        notifyAll();    // Notify in case remaining images are "done".
    }

    private void removeImageImpl(Image image) {
        MediaEntry cur = head;
        MediaEntry prev = null;
        while (cur != null) {
            MediaEntry next = cur.next;
            if (cur.getMedia() == image) {
                if (prev == null) {
                    head = next;
                } else {
                    prev.next = next;
                }
                cur.cancel();
            } else {
                prev = cur;
            }
            cur = next;
        }
    }

    /**
     * Removes the specified image from the specified tracking
     * ID of this media tracker.
     * All instances of {@code Image} being tracked
     * under the specified ID are removed regardless of scale.
     * @param      image the image to be removed
     * @param      id the tracking ID from which to remove the image
     * @see        java.awt.MediaTracker#removeImage(java.awt.Image)
     * @see        java.awt.MediaTracker#removeImage(java.awt.Image, int, int, int)
     * @since      1.1
     */
    public synchronized void removeImage(Image image, int id) {
        removeImageImpl(image, id);
        Image rvImage = getResolutionVariant(image);
        if (rvImage != null) {
            removeImageImpl(rvImage, id);
        }
        notifyAll();    // Notify in case remaining images are "done".
    }

    private void removeImageImpl(Image image, int id) {
        MediaEntry cur = head;
        MediaEntry prev = null;
        while (cur != null) {
            MediaEntry next = cur.next;
            if (cur.getID() == id && cur.getMedia() == image) {
                if (prev == null) {
                    head = next;
                } else {
                    prev.next = next;
                }
                cur.cancel();
            } else {
                prev = cur;
            }
            cur = next;
        }
    }

    /**
     * Removes the specified image with the specified
     * width, height, and ID from this media tracker.
     * Only the specified instance (with any duplicates) is removed.
     * @param   image the image to be removed
     * @param   id the tracking ID from which to remove the image
     * @param   width the width to remove (-1 for unscaled)
     * @param   height the height to remove (-1 for unscaled)
     * @see     java.awt.MediaTracker#removeImage(java.awt.Image)
     * @see     java.awt.MediaTracker#removeImage(java.awt.Image, int)
     * @since   1.1
     */
    public synchronized void removeImage(Image image, int id,
                                         int width, int height) {
        removeImageImpl(image, id, width, height);
        Image rvImage = getResolutionVariant(image);
        if (rvImage != null) {
            removeImageImpl(rvImage, id,
                    width == -1 ? -1 : 2 * width,
                    height == -1 ? -1 : 2 * height);
        }
        notifyAll();    // Notify in case remaining images are "done".
    }

    private void removeImageImpl(Image image, int id, int width, int height) {
        MediaEntry cur = head;
        MediaEntry prev = null;
        while (cur != null) {
            MediaEntry next = cur.next;
            if (cur.getID() == id && cur instanceof ImageMediaEntry
                && ((ImageMediaEntry) cur).matches(image, width, height))
            {
                if (prev == null) {
                    head = next;
                } else {
                    prev.next = next;
                }
                cur.cancel();
            } else {
                prev = cur;
            }
            cur = next;
        }
    }

    synchronized void setDone() {
        notifyAll();
    }

    private static Image getResolutionVariant(Image image) {
        if (image instanceof MultiResolutionToolkitImage) {
            return ((MultiResolutionToolkitImage) image).getResolutionVariant();
        }
        return null;
    }
}

abstract class MediaEntry {
    MediaTracker tracker;
    int ID;
    MediaEntry next;

    int status;
    boolean cancelled;

    MediaEntry(MediaTracker mt, int id) {
        tracker = mt;
        ID = id;
    }

    abstract Object getMedia();

    static MediaEntry insert(MediaEntry head, MediaEntry me) {
        MediaEntry cur = head;
        MediaEntry prev = null;
        while (cur != null) {
            if (cur.ID > me.ID) {
                break;
            }
            prev = cur;
            cur = cur.next;
        }
        me.next = cur;
        if (prev == null) {
            head = me;
        } else {
            prev.next = me;
        }
        return head;
    }

    int getID() {
        return ID;
    }

    abstract void startLoad();

    void cancel() {
        cancelled = true;
    }

    static final int LOADING = MediaTracker.LOADING;
    static final int ABORTED = MediaTracker.ABORTED;
    static final int ERRORED = MediaTracker.ERRORED;
    static final int COMPLETE = MediaTracker.COMPLETE;

    static final int LOADSTARTED = (LOADING | ERRORED | COMPLETE);
    static final int DONE = (ABORTED | ERRORED | COMPLETE);

    synchronized int getStatus(boolean doLoad, boolean doVerify) {
        if (doLoad && ((status & LOADSTARTED) == 0)) {
            status = (status & ~ABORTED) | LOADING;
            startLoad();
        }
        return status;
    }

    void setStatus(int flag) {
        synchronized (this) {
            status = flag;
        }
        tracker.setDone();
    }
}

/**
 * The entry of the list of {@code Images} that is being tracked by the
 * {@code MediaTracker}.
 */
@SuppressWarnings("serial") // MediaEntry does not have a no-arg ctor
class ImageMediaEntry extends MediaEntry implements ImageObserver,
java.io.Serializable {
    @SuppressWarnings("serial") // Not statically typed as Serializable
    Image image;
    int width;
    int height;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 4739377000350280650L;

    ImageMediaEntry(MediaTracker mt, Image img, int c, int w, int h) {
        super(mt, c);
        image = img;
        width = w;
        height = h;
    }

    boolean matches(Image img, int w, int h) {
        return (image == img && width == w && height == h);
    }

    Object getMedia() {
        return image;
    }

    synchronized int getStatus(boolean doLoad, boolean doVerify) {
        if (doVerify) {
            int flags = tracker.target.checkImage(image, width, height, null);
            int s = parseflags(flags);
            if (s == 0) {
                if ((status & (ERRORED | COMPLETE)) != 0) {
                    setStatus(ABORTED);
                }
            } else if (s != status) {
                setStatus(s);
            }
        }
        return super.getStatus(doLoad, doVerify);
    }

    void startLoad() {
        if (tracker.target.prepareImage(image, width, height, this)) {
            setStatus(COMPLETE);
        }
    }

    int parseflags(int infoflags) {
        if ((infoflags & ERROR) != 0) {
            return ERRORED;
        } else if ((infoflags & ABORT) != 0) {
            return ABORTED;
        } else if ((infoflags & (ALLBITS | FRAMEBITS)) != 0) {
            return COMPLETE;
        }
        return 0;
    }

    public boolean imageUpdate(Image img, int infoflags,
                               int x, int y, int w, int h) {
        if (cancelled) {
            return false;
        }
        int s = parseflags(infoflags);
        if (s != 0 && s != status) {
            setStatus(s);
        }
        return ((status & LOADING) != 0);
    }
}
