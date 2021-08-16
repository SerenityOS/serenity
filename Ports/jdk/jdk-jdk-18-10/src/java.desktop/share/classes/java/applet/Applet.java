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

package java.applet;

import java.awt.AWTPermission;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Panel;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

import com.sun.media.sound.JavaSoundAudioClip;

/**
 * An applet is a small program that is intended not to be run on its own, but
 * rather to be embedded inside another application.
 * <p>
 * The {@code Applet} class must be the superclass of any applet that is to be
 * embedded in a Web page or viewed by the Java Applet Viewer. The
 * {@code Applet} class provides a standard interface between applets and their
 * environment.
 *
 * @author Arthur van Hoff
 * @author Chris Warth
 * @since 1.0
 * @deprecated The Applet API is deprecated, no replacement.
 */
@Deprecated(since = "9", forRemoval = true)
@SuppressWarnings("removal")
public class Applet extends Panel {

    /**
     * Constructs a new Applet.
     * <p>
     * Note: Many methods in {@code java.applet.Applet} may be invoked by the
     * applet only after the applet is fully constructed; applet should avoid
     * calling methods in {@code java.applet.Applet} in the constructor.
     *
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *         returns {@code true}
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @since 1.4
     */
    public Applet() throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
    }

    /**
     * Applets can be serialized but the following conventions MUST be followed:
     * <p>
     * Before Serialization: An applet must be in STOPPED state.
     * <p>
     * After Deserialization: The applet will be restored in STOPPED state (and
     * most clients will likely move it into RUNNING state). The stub field will
     * be restored by the reader.
     */
    private transient AppletStub stub;

    /**
     * Use serialVersionUID from JDK 1.0 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -5836846270535785031L;

    /**
     * Read an applet from an object input stream.
     *
     * @param  s the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *         returns {@code true}
     * @serial
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @since 1.4
     */
    @Serial
    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException, HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        s.defaultReadObject();
    }

    /**
     * Sets this applet's stub. This is done automatically by the system.
     * <p>
     * If there is a security manager, its {@code checkPermission} method is
     * called with the {@code AWTPermission("setAppletStub")} permission if a
     * stub has already been set.
     *
     * @param  stub the new stub
     * @throws SecurityException if the caller cannot set the stub
     */
    public final void setStub(AppletStub stub) {
        if (this.stub != null) {
            SecurityManager s = System.getSecurityManager();
            if (s != null) {
                s.checkPermission(new AWTPermission("setAppletStub"));
            }
        }
        this.stub = stub;
    }

    /**
     * Determines if this applet is active. An applet is marked active just
     * before its {@code start} method is called. It becomes inactive just
     * before its {@code stop} method is called.
     *
     * @return {@code true} if the applet is active; {@code false} otherwise
     * @see java.applet.Applet#start()
     * @see java.applet.Applet#stop()
     */
    public boolean isActive() {
        if (stub != null) {
            return stub.isActive();
        } else {        // If stub field not filled in, applet never active
            return false;
        }
    }

    /**
     * Gets the {@code URL} of the document in which this applet is embedded.
     * For example, suppose an applet is contained within the document:
     * <blockquote><pre>
     *    http://www.oracle.com/technetwork/java/index.html
     * </pre></blockquote>
     * The document base is:
     * <blockquote><pre>
     *    http://www.oracle.com/technetwork/java/index.html
     * </pre></blockquote>
     *
     * @return the {@link java.net.URL} of the document that contains this
     *         applet
     * @see java.applet.Applet#getCodeBase()
     */
    public URL getDocumentBase() {
        return stub.getDocumentBase();
    }

    /**
     * Gets the base {@code URL}. This is the {@code URL} of the directory which
     * contains this applet.
     *
     * @return the base {@link java.net.URL} of the directory which contains
     *         this applet
     * @see java.applet.Applet#getDocumentBase()
     */
    public URL getCodeBase() {
        return stub.getCodeBase();
    }

    /**
     * Returns the value of the named parameter in the HTML tag. For example, if
     * this applet is specified as
     * <blockquote><pre>
     * &lt;applet code="Clock" width=50 height=50&gt;
     * &lt;param name=Color value="blue"&gt;
     * &lt;/applet&gt;
     * </pre></blockquote>
     * <p>
     * then a call to {@code getParameter("Color")} returns the value
     * {@code "blue"}.
     * <p>
     * The {@code name} argument is case insensitive.
     *
     * @param  name a parameter name
     * @return the value of the named parameter, or {@code null} if not set
     */
    public String getParameter(String name) {
        return stub.getParameter(name);
    }

    /**
     * Determines this applet's context, which allows the applet to query and
     * affect the environment in which it runs.
     * <p>
     * This environment of an applet represents the document that contains the
     * applet.
     *
     * @return the applet's context
     */
    public AppletContext getAppletContext() {
        return stub.getAppletContext();
    }

    /**
     * Requests that this applet be resized.
     *
     * @param  width the new requested width for the applet
     * @param  height the new requested height for the applet
     */
    @SuppressWarnings("deprecation")
    public void resize(int width, int height) {
        Dimension d = size();
        if ((d.width != width) || (d.height != height)) {
            super.resize(width, height);
            if (stub != null) {
                stub.appletResize(width, height);
            }
        }
    }

    /**
     * Requests that this applet be resized.
     *
     * @param  d an object giving the new width and height
     */
    @SuppressWarnings("deprecation")
    public void resize(Dimension d) {
        resize(d.width, d.height);
    }

    /**
     * Indicates if this container is a validate root.
     * <p>
     * {@code Applet} objects are the validate roots, and, therefore, they
     * override this method to return {@code true}.
     *
     * @return {@code true}
     * @see java.awt.Container#isValidateRoot
     * @since 1.7
     */
    @Override
    public boolean isValidateRoot() {
        return true;
    }

    /**
     * Requests that the argument string be displayed in the "status window".
     * Many browsers and applet viewers provide such a window, where the
     * application can inform users of its current state.
     *
     * @param  msg a string to display in the status window
     */
    public void showStatus(String msg) {
        getAppletContext().showStatus(msg);
    }

    /**
     * Returns an {@code Image} object that can then be painted on the screen.
     * The {@code url} that is passed as an argument must specify an absolute
     * {@code URL}.
     * <p>
     * This method always returns immediately, whether or not the image exists.
     * When this applet attempts to draw the image on the screen, the data will
     * be loaded. The graphics primitives that draw the image will incrementally
     * paint on the screen.
     *
     * @param  url an absolute {@code URL} giving the location of the image
     * @return the image at the specified {@code URL}
     * @see java.awt.Image
     */
    public Image getImage(URL url) {
        return getAppletContext().getImage(url);
    }

    /**
     * Returns an {@code Image} object that can then be painted on the screen.
     * The {@code url} argument must specify an absolute {@code URL}. The
     * {@code name} argument is a specifier that is relative to the {@code url}
     * argument.
     * <p>
     * This method always returns immediately, whether or not the image exists.
     * When this applet attempts to draw the image on the screen, the data will
     * be loaded. The graphics primitives that draw the image will incrementally
     * paint on the screen.
     *
     * @param  url an absolute URL giving the base location of the image
     * @param  name the location of the image, relative to the {@code url}
     *         argument
     * @return the image at the specified {@code URL}
     * @see java.awt.Image
     */
    public Image getImage(URL url, String name) {
        try {
            return getImage(new URL(url, name));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Get an audio clip from the given {@code URL}.
     *
     * @param  url points to the audio clip
     * @return the audio clip at the specified {@code URL}
     * @since 1.2
     */
    public static final AudioClip newAudioClip(URL url) {
        return JavaSoundAudioClip.create(url);
    }

    /**
     * Returns the {@code AudioClip} object specified by the {@code URL}
     * argument.
     * <p>
     * This method always returns immediately, whether or not the audio clip
     * exists. When this applet attempts to play the audio clip, the data will
     * be loaded.
     *
     * @param  url an absolute {@code URL} giving the location of the audio clip
     * @return the audio clip at the specified {@code URL}
     * @see java.applet.AudioClip
     */
    public AudioClip getAudioClip(URL url) {
        return getAppletContext().getAudioClip(url);
    }

    /**
     * Returns the {@code AudioClip} object specified by the {@code URL} and
     * {@code name} arguments.
     * <p>
     * This method always returns immediately, whether or not the audio clip
     * exists. When this applet attempts to play the audio clip, the data will
     * be loaded.
     *
     * @param  url an absolute {@code URL} giving the base location of the audio
     *         clip
     * @param  name the location of the audio clip, relative to the {@code url}
     *         argument
     * @return the audio clip at the specified {@code URL}
     * @see java.applet.AudioClip
     */
    public AudioClip getAudioClip(URL url, String name) {
        try {
            return getAudioClip(new URL(url, name));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Returns information about this applet. An applet should override this
     * method to return a {@code String} containing information about the
     * author, version, and copyright of the applet.
     * <p>
     * The implementation of this method provided by the {@code Applet} class
     * returns {@code null}.
     *
     * @return a string containing information about the author, version, and
     *         copyright of the applet
     */
    public String getAppletInfo() {
        return null;
    }

    /**
     * Gets the locale of the applet. It allows the applet to maintain its own
     * locale separated from the locale of the browser or appletviewer.
     *
     * @return the locale of the applet; if no locale has been set, the default
     *         locale is returned
     * @since 1.1
     */
    public Locale getLocale() {
        Locale locale = super.getLocale();
        if (locale == null) {
            return Locale.getDefault();
        }
        return locale;
    }

    /**
     * Returns information about the parameters that are understood by this
     * applet. An applet should override this method to return an array of
     * strings describing these parameters.
     * <p>
     * Each element of the array should be a set of three strings containing the
     * name, the type, and a description. For example:
     * <blockquote><pre>
     * String pinfo[][] = {
     *   {"fps",    "1-10",    "frames per second"},
     *   {"repeat", "boolean", "repeat image loop"},
     *   {"imgs",   "url",     "images directory"}
     * };
     * </pre></blockquote>
     * <p>
     * The implementation of this method provided by the {@code Applet} class
     * returns {@code null}.
     *
     * @return an array describing the parameters this applet looks for
     */
    public String[][] getParameterInfo() {
        return null;
    }

    /**
     * Plays the audio clip at the specified absolute {@code URL}. Nothing
     * happens if the audio clip cannot be found.
     *
     * @param  url an absolute {@code URL} giving the location of the audio clip
     */
    public void play(URL url) {
        AudioClip clip = getAudioClip(url);
        if (clip != null) {
            clip.play();
        }
    }

    /**
     * Plays the audio clip given the {@code URL} and a specifier that is
     * relative to it. Nothing happens if the audio clip cannot be found.
     *
     * @param  url an absolute {@code URL} giving the base location of the audio
     *         clip
     * @param  name the location of the audio clip, relative to the {@code url}
     *         argument
     */
    public void play(URL url, String name) {
        AudioClip clip = getAudioClip(url, name);
        if (clip != null) {
            clip.play();
        }
    }

    /**
     * Called by the browser or applet viewer to inform this applet that it has
     * been loaded into the system. It is always called before the first time
     * that the {@code start} method is called.
     * <p>
     * A subclass of {@code Applet} should override this method if it has
     * initialization to perform. For example, an applet with threads would use
     * the {@code init} method to create the threads and the {@code destroy}
     * method to kill them.
     * <p>
     * The implementation of this method provided by the {@code Applet} class
     * does nothing.
     *
     * @see java.applet.Applet#destroy()
     * @see java.applet.Applet#start()
     * @see java.applet.Applet#stop()
     */
    public void init() {
    }

    /**
     * Called by the browser or applet viewer to inform this applet that it
     * should start its execution. It is called after the {@code init} method
     * and each time the applet is revisited in a Web page.
     * <p>
     * A subclass of {@code Applet} should override this method if it has any
     * operation that it wants to perform each time the Web page containing it
     * is visited. For example, an applet with animation might want to use the
     * {@code start} method to resume animation, and the {@code stop} method to
     * suspend the animation.
     * <p>
     * Note: some methods, such as {@code getLocationOnScreen}, can only provide
     * meaningful results if the applet is showing. Because {@code isShowing}
     * returns {@code false} when the applet's {@code start} is first called,
     * methods requiring {@code isShowing} to return {@code true} should be
     * called from a {@code ComponentListener}.
     * <p>
     * The implementation of this method provided by the {@code Applet} class
     * does nothing.
     *
     * @see java.applet.Applet#destroy()
     * @see java.applet.Applet#init()
     * @see java.applet.Applet#stop()
     * @see java.awt.Component#isShowing()
     * @see java.awt.event.ComponentListener#componentShown(ComponentEvent)
     */
    public void start() {
    }

    /**
     * Called by the browser or applet viewer to inform this applet that it
     * should stop its execution. It is called when the Web page that contains
     * this applet has been replaced by another page, and also just before the
     * applet is to be destroyed.
     * <p>
     * A subclass of {@code Applet} should override this method if it has any
     * operation that it wants to perform each time the Web page containing it
     * is no longer visible. For example, an applet with animation might want to
     * use the {@code start} method to resume animation, and the {@code stop}
     * method to suspend the animation.
     * <p>
     * The implementation of this method provided by the {@code Applet} class
     * does nothing.
     *
     * @see java.applet.Applet#destroy()
     * @see java.applet.Applet#init()
     */
    public void stop() {
    }

    /**
     * Called by the browser or applet viewer to inform this applet that it is
     * being reclaimed and that it should destroy any resources that it has
     * allocated. The {@code stop} method will always be called before
     * {@code destroy}.
     * <p>
     * A subclass of {@code Applet} should override this method if it has any
     * operation that it wants to perform before it is destroyed. For example,
     * an applet with threads would use the {@code init} method to create the
     * threads and the {@code destroy} method to kill them.
     * <p>
     * The implementation of this method provided by the {@code Applet} class
     * does nothing.
     *
     * @see java.applet.Applet#init()
     * @see java.applet.Applet#start()
     * @see java.applet.Applet#stop()
     */
    public void destroy() {
    }

    //
    // Accessibility support
    //

    /**
     * The accessible context associated with this {@code Applet}.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    AccessibleContext accessibleContext = null;

    /**
     * Gets the {@code AccessibleContext} associated with this {@code Applet}.
     * For applets, the {@code AccessibleContext} takes the form of an
     * {@code AccessibleApplet}. A new {@code AccessibleApplet} instance is
     * created if necessary.
     *
     * @return an {@code AccessibleApplet} that serves as the
     *         {@code AccessibleContext} of this {@code Applet}
     * @since 1.3
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleApplet();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the {@code Applet} class.
     * It provides an implementation of the Java Accessibility API appropriate
     * to applet user-interface elements.
     *
     * @since 1.3
     */
    protected class AccessibleApplet extends AccessibleAWTPanel {

        /**
         * Use serialVersionUID from JDK 1.3 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 8127374778187708896L;

        /**
         * Constructs an {@code AccessibleApplet}.
         */
        protected AccessibleApplet() {}

        /**
         * Get the role of this object.
         *
         * @return an instance of {@code AccessibleRole} describing the role of
         *         the object
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.FRAME;
        }

        /**
         * Get the state of this object.
         *
         * @return an instance of {@code AccessibleStateSet} containing the
         *         current state set of the object
         * @see AccessibleState
         */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = super.getAccessibleStateSet();
            states.add(AccessibleState.ACTIVE);
            return states;
        }
    }
}
