/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing;

import java.awt.Component;
import java.awt.Font;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.KeyEventPostProcessor;
import java.awt.Toolkit;

import java.awt.event.KeyEvent;

import java.security.AccessController;

import javax.swing.plaf.ComponentUI;
import javax.swing.border.Border;

import javax.swing.event.SwingPropertyChangeSupport;
import java.beans.PropertyChangeListener;

import java.io.Serializable;
import java.io.File;
import java.io.FileInputStream;

import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Locale;

import sun.awt.SunToolkit;
import sun.awt.OSInfo;
import sun.security.action.GetPropertyAction;
import sun.swing.SwingUtilities2;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Objects;
import sun.awt.AppContext;
import sun.awt.AWTAccessor;


/**
 * {@code UIManager} manages the current look and feel, the set of
 * available look and feels, {@code PropertyChangeListeners} that
 * are notified when the look and feel changes, look and feel defaults, and
 * convenience methods for obtaining various default values.
 *
 * <h2>Specifying the look and feel</h2>
 *
 * The look and feel can be specified in two distinct ways: by
 * specifying the fully qualified name of the class for the look and
 * feel, or by creating an instance of {@code LookAndFeel} and passing
 * it to {@code setLookAndFeel}. The following example illustrates
 * setting the look and feel to the system look and feel:
 * <pre>
 *   UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
 * </pre>
 * The following example illustrates setting the look and feel based on
 * class name:
 * <pre>
 *   UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
 * </pre>
 * Once the look and feel has been changed it is imperative to invoke
 * {@code updateUI} on all {@code JComponents}. The method {@link
 * SwingUtilities#updateComponentTreeUI} makes it easy to apply {@code
 * updateUI} to a containment hierarchy. Refer to it for
 * details. The exact behavior of not invoking {@code
 * updateUI} after changing the look and feel is
 * unspecified. It is very possible to receive unexpected exceptions,
 * painting problems, or worse.
 *
 * <h2>Default look and feel</h2>
 *
 * The class used for the default look and feel is chosen in the following
 * manner:
 * <ol>
 *   <li>If the system property <code>swing.defaultlaf</code> is
 *       {@code non-null}, use its value as the default look and feel class
 *       name.
 *   <li>If the {@link java.util.Properties} file <code>swing.properties</code>
 *       exists and contains the key <code>swing.defaultlaf</code>,
 *       use its value as the default look and feel class name. The location
 *       that is checked for <code>swing.properties</code> may vary depending
 *       upon the implementation of the Java platform. Typically the
 *       <code>swing.properties</code> file is located in the <code>conf</code>
 *       subdirectory of the Java installation directory.
 *       Refer to the release notes of the implementation being used for
 *       further details.
 *   <li>Otherwise use the cross platform look and feel.
 * </ol>
 *
 * <h2>Defaults</h2>
 *
 * {@code UIManager} manages three sets of {@code UIDefaults}. In order, they
 * are:
 * <ol>
 *   <li>Developer defaults. With few exceptions Swing does not
 *       alter the developer defaults; these are intended to be modified
 *       and used by the developer.
 *   <li>Look and feel defaults. The look and feel defaults are
 *       supplied by the look and feel at the time it is installed as the
 *       current look and feel ({@code setLookAndFeel()} is invoked). The
 *       look and feel defaults can be obtained using the {@code
 *       getLookAndFeelDefaults()} method.
 *   <li>System defaults. The system defaults are provided by Swing.
 * </ol>
 * Invoking any of the various {@code get} methods
 * results in checking each of the defaults, in order, returning
 * the first {@code non-null} value. For example, invoking
 * {@code UIManager.getString("Table.foreground")} results in first
 * checking developer defaults. If the developer defaults contain
 * a value for {@code "Table.foreground"} it is returned, otherwise
 * the look and feel defaults are checked, followed by the system defaults.
 * <p>
 * It's important to note that {@code getDefaults} returns a custom
 * instance of {@code UIDefaults} with this resolution logic built into it.
 * For example, {@code UIManager.getDefaults().getString("Table.foreground")}
 * is equivalent to {@code UIManager.getString("Table.foreground")}. Both
 * resolve using the algorithm just described. In many places the
 * documentation uses the word defaults to refer to the custom instance
 * of {@code UIDefaults} with the resolution logic as previously described.
 * <p>
 * When the look and feel is changed, {@code UIManager} alters only the
 * look and feel defaults; the developer and system defaults are not
 * altered by the {@code UIManager} in any way.
 * <p>
 * The set of defaults a particular look and feel supports is defined
 * and documented by that look and feel. In addition, each look and
 * feel, or {@code ComponentUI} provided by a look and feel, may
 * access the defaults at different times in their life cycle. Some
 * look and feels may aggressively look up defaults, so that changing a
 * default may not have an effect after installing the look and feel.
 * Other look and feels may lazily access defaults so that a change to
 * the defaults may effect an existing look and feel. Finally, other look
 * and feels might not configure themselves from the defaults table in
 * any way. None-the-less it is usually the case that a look and feel
 * expects certain defaults, so that in general
 * a {@code ComponentUI} provided by one look and feel will not
 * work with another look and feel.
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
 * @author Thomas Ball
 * @author Hans Muller
 * @since 1.2
 */
@SuppressWarnings("serial") // Same-version serialization only
public class UIManager implements Serializable
{
    /**
     * This class defines the state managed by the <code>UIManager</code>.  For
     * Swing applications the fields in this class could just as well
     * be static members of <code>UIManager</code> however we give them
     * "AppContext"
     * scope instead so that applets (and potentially multiple lightweight
     * applications running in a single VM) have their own state. For example,
     * an applet can alter its look and feel, see <code>setLookAndFeel</code>.
     * Doing so has no affect on other applets (or the browser).
     */
    private static class LAFState
    {
        Properties swingProps;
        private UIDefaults[] tables = new UIDefaults[2];

        boolean initialized = false;
        boolean focusPolicyInitialized = false;
        MultiUIDefaults multiUIDefaults = new MultiUIDefaults(tables);
        LookAndFeel lookAndFeel;
        LookAndFeel multiLookAndFeel = null;
        Vector<LookAndFeel> auxLookAndFeels = null;
        SwingPropertyChangeSupport changeSupport;

        LookAndFeelInfo[] installedLAFs;

        UIDefaults getLookAndFeelDefaults() { return tables[0]; }
        void setLookAndFeelDefaults(UIDefaults x) { tables[0] = x; }

        UIDefaults getSystemDefaults() { return tables[1]; }
        void setSystemDefaults(UIDefaults x) { tables[1] = x; }

        /**
         * Returns the SwingPropertyChangeSupport for the current
         * AppContext.  If <code>create</code> is a true, a non-null
         * <code>SwingPropertyChangeSupport</code> will be returned, if
         * <code>create</code> is false and this has not been invoked
         * with true, null will be returned.
         */
        public synchronized SwingPropertyChangeSupport
                                 getPropertyChangeSupport(boolean create) {
            if (create && changeSupport == null) {
                changeSupport = new SwingPropertyChangeSupport(
                                         UIManager.class);
            }
            return changeSupport;
        }
    }




    /* Lock object used in place of class object for synchronization. (4187686)
     */
    private static final Object classLock = new Object();

    /**
     * Constructs a {@code UIManager}.
     */
    public UIManager() {}

    /**
     * Return the <code>LAFState</code> object, lazily create one if necessary.
     * All access to the <code>LAFState</code> fields is done via this method,
     * for example:
     * <pre>
     *     getLAFState().initialized = true;
     * </pre>
     */
    private static LAFState getLAFState() {
        LAFState rv = (LAFState)SwingUtilities.appContextGet(
                SwingUtilities2.LAF_STATE_KEY);
        if (rv == null) {
            synchronized (classLock) {
                rv = (LAFState)SwingUtilities.appContextGet(
                        SwingUtilities2.LAF_STATE_KEY);
                if (rv == null) {
                    SwingUtilities.appContextPut(
                            SwingUtilities2.LAF_STATE_KEY,
                            (rv = new LAFState()));
                }
            }
        }
        return rv;
    }


    /* Keys used in the <code>swing.properties</code> properties file.
     * See loadUserProperties(), initialize().
     */

    private static final String defaultLAFKey = "swing.defaultlaf";
    private static final String auxiliaryLAFsKey = "swing.auxiliarylaf";
    private static final String multiplexingLAFKey = "swing.plaf.multiplexinglaf";
    private static final String installedLAFsKey = "swing.installedlafs";
    private static final String disableMnemonicKey = "swing.disablenavaids";

    /**
     * Return a <code>swing.properties</code> file key for the attribute of specified
     * look and feel.  The attr is either "name" or "class", a typical
     * key would be: "swing.installedlaf.windows.name"
     */
    private static String makeInstalledLAFKey(String laf, String attr) {
        return "swing.installedlaf." + laf + "." + attr;
    }

    /**
     * The location of the <code>swing.properties</code> property file is
     * implementation-specific.
     * It is typically located in the <code>conf</code> subdirectory of the Java
     * installation directory. This method returns a bogus filename
     * if <code>java.home</code> isn't defined.
     */
    private static String makeSwingPropertiesFilename() {
        String sep = File.separator;
        // No need to wrap this in a doPrivileged as it's called from
        // a doPrivileged.
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            javaHome = "<java.home undefined>";
        }
        return javaHome + sep + "conf" + sep + "swing.properties";
    }


    /**
     * Provides a little information about an installed
     * <code>LookAndFeel</code> for the sake of configuring a menu or
     * for initial application set up.
     *
     * @see UIManager#getInstalledLookAndFeels
     * @see LookAndFeel
     */
    public static class LookAndFeelInfo {
        private String name;
        private String className;

        /**
         * Constructs a <code>UIManager</code>s
         * <code>LookAndFeelInfo</code> object.
         *
         * @param name      a <code>String</code> specifying the name of
         *                      the look and feel
         * @param className a <code>String</code> specifying the name of
         *                      the class that implements the look and feel
         */
        public LookAndFeelInfo(String name, String className) {
            this.name = name;
            this.className = className;
        }

        /**
         * Returns the name of the look and feel in a form suitable
         * for a menu or other presentation
         * @return a <code>String</code> containing the name
         * @see LookAndFeel#getName
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the name of the class that implements this look and feel.
         * @return the name of the class that implements this
         *              <code>LookAndFeel</code>
         * @see LookAndFeel
         */
        public String getClassName() {
            return className;
        }

        /**
         * Returns a string that displays and identifies this
         * object's properties.
         *
         * @return a <code>String</code> representation of this object
         */
        public String toString() {
            return getClass().getName() + "[" + getName() + " " + getClassName() + "]";
        }
    }


    /**
     * The default value of <code>installedLAFS</code> is used when no
     * <code>swing.properties</code>
     * file is available or if the file doesn't contain a "swing.installedlafs"
     * property.
     *
     * @see #initializeInstalledLAFs
     */
    private static LookAndFeelInfo[] installedLAFs;

    static {
        ArrayList<LookAndFeelInfo> iLAFs = new ArrayList<LookAndFeelInfo>(4);
        iLAFs.add(new LookAndFeelInfo(
                      "Metal", "javax.swing.plaf.metal.MetalLookAndFeel"));
        iLAFs.add(new LookAndFeelInfo(
                      "Nimbus", "javax.swing.plaf.nimbus.NimbusLookAndFeel"));
        iLAFs.add(new LookAndFeelInfo("CDE/Motif",
                  "com.sun.java.swing.plaf.motif.MotifLookAndFeel"));

        // Only include windows on Windows boxs.
        @SuppressWarnings("removal")
        OSInfo.OSType osType = AccessController.doPrivileged(OSInfo.getOSTypeAction());
        if (osType == OSInfo.OSType.WINDOWS) {
            iLAFs.add(new LookAndFeelInfo("Windows",
                        "com.sun.java.swing.plaf.windows.WindowsLookAndFeel"));
            if (Toolkit.getDefaultToolkit().getDesktopProperty(
                    "win.xpstyle.themeActive") != null) {
                iLAFs.add(new LookAndFeelInfo("Windows Classic",
                 "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel"));
            }
        }
        else if (osType == OSInfo.OSType.MACOSX) {
            iLAFs.add(new LookAndFeelInfo("Mac OS X", "com.apple.laf.AquaLookAndFeel"));
        }
        else {
            // GTK is not shipped on Windows.
            iLAFs.add(new LookAndFeelInfo("GTK+",
                  "com.sun.java.swing.plaf.gtk.GTKLookAndFeel"));
        }
        installedLAFs = iLAFs.toArray(new LookAndFeelInfo[iLAFs.size()]);
    }


    /**
     * Returns an array of {@code LookAndFeelInfo}s representing the
     * {@code LookAndFeel} implementations currently available. The
     * <code>LookAndFeelInfo</code> objects can be used by an
     * application to construct a menu of look and feel options for
     * the user, or to determine which look and feel to set at startup
     * time. To avoid the penalty of creating numerous {@code
     * LookAndFeel} objects, {@code LookAndFeelInfo} maintains the
     * class name of the {@code LookAndFeel} class, not the actual
     * {@code LookAndFeel} instance.
     * <p>
     * The following example illustrates setting the current look and feel
     * from an instance of {@code LookAndFeelInfo}:
     * <pre>
     *   UIManager.setLookAndFeel(info.getClassName());
     * </pre>
     *
     * @return an array of <code>LookAndFeelInfo</code> objects
     * @see #setLookAndFeel
     */
    public static LookAndFeelInfo[] getInstalledLookAndFeels() {
        maybeInitialize();
        LookAndFeelInfo[] ilafs = getLAFState().installedLAFs;
        if (ilafs == null) {
            ilafs = installedLAFs;
        }
        LookAndFeelInfo[] rv = new LookAndFeelInfo[ilafs.length];
        System.arraycopy(ilafs, 0, rv, 0, ilafs.length);
        return rv;
    }


    /**
     * Sets the set of available look and feels. While this method does
     * not check to ensure all of the {@code LookAndFeelInfos} are
     * {@code non-null}, it is strongly recommended that only {@code non-null}
     * values are supplied in the {@code infos} array.
     *
     * @param infos set of <code>LookAndFeelInfo</code> objects specifying
     *        the available look and feels
     *
     * @see #getInstalledLookAndFeels
     * @throws NullPointerException if {@code infos} is {@code null}
     */
    public static void setInstalledLookAndFeels(LookAndFeelInfo[] infos)
        throws SecurityException
    {
        maybeInitialize();
        LookAndFeelInfo[] newInfos = new LookAndFeelInfo[infos.length];
        System.arraycopy(infos, 0, newInfos, 0, infos.length);
        getLAFState().installedLAFs = newInfos;
    }


    /**
     * Adds the specified look and feel to the set of available look
     * and feels. While this method allows a {@code null} {@code info},
     * it is strongly recommended that a {@code non-null} value be used.
     *
     * @param info a <code>LookAndFeelInfo</code> object that names the
     *          look and feel and identifies the class that implements it
     * @see #setInstalledLookAndFeels
     */
    public static void installLookAndFeel(LookAndFeelInfo info) {
        LookAndFeelInfo[] infos = getInstalledLookAndFeels();
        LookAndFeelInfo[] newInfos = new LookAndFeelInfo[infos.length + 1];
        System.arraycopy(infos, 0, newInfos, 0, infos.length);
        newInfos[infos.length] = info;
        setInstalledLookAndFeels(newInfos);
    }


    /**
     * Adds the specified look and feel to the set of available look
     * and feels. While this method does not check the
     * arguments in any way, it is strongly recommended that {@code
     * non-null} values be supplied.
     *
     * @param name descriptive name of the look and feel
     * @param className name of the class that implements the look and feel
     * @see #setInstalledLookAndFeels
     */
    public static void installLookAndFeel(String name, String className) {
        installLookAndFeel(new LookAndFeelInfo(name, className));
    }


    /**
     * Returns the current look and feel or <code>null</code>.
     *
     * @return current look and feel, or <code>null</code>
     * @see #setLookAndFeel
     */
    public static LookAndFeel getLookAndFeel() {
        maybeInitialize();
        return getLAFState().lookAndFeel;
    }

    /**
     * Creates a supported built-in Java {@code LookAndFeel} specified
     * by the given {@code L&F name} name.
     *
     * @param name a {@code String} specifying the name of the built-in
     *             look and feel
     * @return the built-in {@code LookAndFeel} object
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws UnsupportedLookAndFeelException if the built-in Java {@code L&F}
     *         is not found for the given name or it is not supported by the
     *         underlying platform
     *
     * @see LookAndFeel#getName
     * @see LookAndFeel#isSupportedLookAndFeel
     *
     * @since 9
     */
    @SuppressWarnings("deprecation")
    public static LookAndFeel createLookAndFeel(String name)
            throws UnsupportedLookAndFeelException {
        Objects.requireNonNull(name);

        if ("GTK look and feel".equals(name)) {
            name = "GTK+";
        }

        try {
            for (LookAndFeelInfo info : installedLAFs) {
                if (info.getName().equals(name)) {
                    Class<?> cls = Class.forName(UIManager.class.getModule(),
                                                 info.getClassName());
                    LookAndFeel laf =
                        (LookAndFeel) cls.newInstance();
                    if (!laf.isSupportedLookAndFeel()) {
                        break;
                    }
                    return laf;
                }
            }
        } catch (ReflectiveOperationException |
                 IllegalArgumentException ignore) {
        }

        throw new UnsupportedLookAndFeelException(name);
    }

    /**
     * Sets the current look and feel to {@code newLookAndFeel}.
     * If the current look and feel is {@code non-null} {@code
     * uninitialize} is invoked on it. If {@code newLookAndFeel} is
     * {@code non-null}, {@code initialize} is invoked on it followed
     * by {@code getDefaults}. The defaults returned from {@code
     * newLookAndFeel.getDefaults()} replace those of the defaults
     * from the previous look and feel. If the {@code newLookAndFeel} is
     * {@code null}, the look and feel defaults are set to {@code null}.
     * <p>
     * A value of {@code null} can be used to set the look and feel
     * to {@code null}. As the {@code LookAndFeel} is required for
     * most of Swing to function, setting the {@code LookAndFeel} to
     * {@code null} is strongly discouraged.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param newLookAndFeel {@code LookAndFeel} to install
     * @throws UnsupportedLookAndFeelException if
     *          {@code newLookAndFeel} is {@code non-null} and
     *          {@code newLookAndFeel.isSupportedLookAndFeel()} returns
     *          {@code false}
     * @see #getLookAndFeel
     */
    public static void setLookAndFeel(LookAndFeel newLookAndFeel)
        throws UnsupportedLookAndFeelException
    {
        if ((newLookAndFeel != null) && !newLookAndFeel.isSupportedLookAndFeel()) {
            String s = newLookAndFeel.toString() + " not supported on this platform";
            throw new UnsupportedLookAndFeelException(s);
        }

        LAFState lafState = getLAFState();
        LookAndFeel oldLookAndFeel = lafState.lookAndFeel;
        if (oldLookAndFeel != null) {
            oldLookAndFeel.uninitialize();
        }

        lafState.lookAndFeel = newLookAndFeel;
        if (newLookAndFeel != null) {
            sun.swing.DefaultLookup.setDefaultLookup(null);
            newLookAndFeel.initialize();
            lafState.setLookAndFeelDefaults(newLookAndFeel.getDefaults());
        }
        else {
            lafState.setLookAndFeelDefaults(null);
        }

        SwingPropertyChangeSupport changeSupport = lafState.
                                         getPropertyChangeSupport(false);
        if (changeSupport != null) {
            changeSupport.firePropertyChange("lookAndFeel", oldLookAndFeel,
                                             newLookAndFeel);
        }
    }


    /**
     * Loads the {@code LookAndFeel} specified by the given class
     * name, using the current thread's context class loader, and
     * passes it to {@code setLookAndFeel(LookAndFeel)}.
     *
     * @param className  a string specifying the name of the class that implements
     *        the look and feel
     * @throws ClassNotFoundException if the <code>LookAndFeel</code>
     *           class could not be found
     * @throws InstantiationException if a new instance of the class
     *          couldn't be created
     * @throws IllegalAccessException if the class or initializer isn't accessible
     * @throws UnsupportedLookAndFeelException if
     *          <code>lnf.isSupportedLookAndFeel()</code> is false
     * @throws ClassCastException if {@code className} does not identify
     *         a class that extends {@code LookAndFeel}
     * @throws NullPointerException if {@code className} is {@code null}
     */
    @SuppressWarnings("deprecation")
    public static void setLookAndFeel(String className)
        throws ClassNotFoundException,
               InstantiationException,
               IllegalAccessException,
               UnsupportedLookAndFeelException
    {
        if ("javax.swing.plaf.metal.MetalLookAndFeel".equals(className)) {
            // Avoid reflection for the common case of metal.
            setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
        }
        else {
            Class<?> lnfClass = SwingUtilities.loadSystemClass(className);
            setLookAndFeel((LookAndFeel)(lnfClass.newInstance()));
        }
    }

    /**
     * Returns the name of the <code>LookAndFeel</code> class that implements
     * the native system look and feel if there is one, otherwise
     * the name of the default cross platform <code>LookAndFeel</code>
     * class. This value can be overriden by setting the
     * <code>swing.systemlaf</code> system property.
     *
     * @return the <code>String</code> of the <code>LookAndFeel</code>
     *          class
     *
     * @see #setLookAndFeel
     * @see #getCrossPlatformLookAndFeelClassName
     */
    public static String getSystemLookAndFeelClassName() {
        @SuppressWarnings("removal")
        String systemLAF = AccessController.doPrivileged(
                             new GetPropertyAction("swing.systemlaf"));
        if (systemLAF != null) {
            return systemLAF;
        }
        @SuppressWarnings("removal")
        OSInfo.OSType osType = AccessController.doPrivileged(OSInfo.getOSTypeAction());
        if (osType == OSInfo.OSType.WINDOWS) {
            return "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        } else {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            if (toolkit instanceof SunToolkit) {
                SunToolkit suntk = (SunToolkit)toolkit;
                String desktop = suntk.getDesktop();
                boolean gtkAvailable = suntk.isNativeGTKAvailable();
                if ("gnome".equals(desktop) && gtkAvailable) {
                    return "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
                }
            }
            if (osType == OSInfo.OSType.MACOSX) {
                if (toolkit.getClass() .getName()
                                       .equals("sun.lwawt.macosx.LWCToolkit")) {
                    return "com.apple.laf.AquaLookAndFeel";
                }
            }
        }
        return getCrossPlatformLookAndFeelClassName();
    }


    /**
     * Returns the name of the <code>LookAndFeel</code> class that implements
     * the default cross platform look and feel -- the Java
     * Look and Feel (JLF).  This value can be overriden by setting the
     * <code>swing.crossplatformlaf</code> system property.
     *
     * @return  a string with the JLF implementation-class
     * @see #setLookAndFeel
     * @see #getSystemLookAndFeelClassName
     */
    public static String getCrossPlatformLookAndFeelClassName() {
        @SuppressWarnings("removal")
        String laf = AccessController.doPrivileged(
                             new GetPropertyAction("swing.crossplatformlaf"));
        if (laf != null) {
            return laf;
        }
        return "javax.swing.plaf.metal.MetalLookAndFeel";
    }


    /**
     * Returns the defaults. The returned defaults resolve using the
     * logic specified in the class documentation.
     *
     * @return a <code>UIDefaults</code> object containing the default values
     */
    public static UIDefaults getDefaults() {
        maybeInitialize();
        return getLAFState().multiUIDefaults;
    }

    /**
     * Returns a font from the defaults. If the value for {@code key} is
     * not a {@code Font}, {@code null} is returned.
     *
     * @param key  an <code>Object</code> specifying the font
     * @return the <code>Font</code> object
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public static Font getFont(Object key) {
        return getDefaults().getFont(key);
    }

    /**
     * Returns a font from the defaults that is appropriate
     * for the given locale. If the value for {@code key} is
     * not a {@code Font}, {@code null} is returned.
     *
     * @param key  an <code>Object</code> specifying the font
     * @param l the <code>Locale</code> for which the font is desired; refer
     *        to {@code UIDefaults} for details on how a {@code null}
     *        {@code Locale} is handled
     * @return the <code>Font</code> object
     * @throws NullPointerException if {@code key} is {@code null}
     * @since 1.4
     */
    public static Font getFont(Object key, Locale l) {
        return getDefaults().getFont(key,l);
    }

    /**
     * Returns a color from the defaults. If the value for {@code key} is
     * not a {@code Color}, {@code null} is returned.
     *
     * @param key  an <code>Object</code> specifying the color
     * @return the <code>Color</code> object
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public static Color getColor(Object key) {
        return getDefaults().getColor(key);
    }

    /**
     * Returns a color from the defaults that is appropriate
     * for the given locale. If the value for {@code key} is
     * not a {@code Color}, {@code null} is returned.
     *
     * @param key  an <code>Object</code> specifying the color
     * @param l the <code>Locale</code> for which the color is desired; refer
     *        to {@code UIDefaults} for details on how a {@code null}
     *        {@code Locale} is handled
     * @return the <code>Color</code> object
     * @throws NullPointerException if {@code key} is {@code null}
     * @since 1.4
     */
    public static Color getColor(Object key, Locale l) {
        return getDefaults().getColor(key,l);
    }

    /**
     * Returns an <code>Icon</code> from the defaults. If the value for
     * {@code key} is not an {@code Icon}, {@code null} is returned.
     *
     * @param key  an <code>Object</code> specifying the icon
     * @return the <code>Icon</code> object
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public static Icon getIcon(Object key) {
        return getDefaults().getIcon(key);
    }

    /**
     * Returns an <code>Icon</code> from the defaults that is appropriate
     * for the given locale. If the value for
     * {@code key} is not an {@code Icon}, {@code null} is returned.
     *
     * @param key  an <code>Object</code> specifying the icon
     * @param l the <code>Locale</code> for which the icon is desired; refer
     *        to {@code UIDefaults} for details on how a {@code null}
     *        {@code Locale} is handled
     * @return the <code>Icon</code> object
     * @throws NullPointerException if {@code key} is {@code null}
     * @since 1.4
     */
    public static Icon getIcon(Object key, Locale l) {
        return getDefaults().getIcon(key,l);
    }

    /**
     * Returns a border from the defaults. If the value for
     * {@code key} is not a {@code Border}, {@code null} is returned.
     *
     * @param key  an <code>Object</code> specifying the border
     * @return the <code>Border</code> object
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public static Border getBorder(Object key) {
        return getDefaults().getBorder(key);
    }

    /**
     * Returns a border from the defaults that is appropriate
     * for the given locale.  If the value for
     * {@code key} is not a {@code Border}, {@code null} is returned.
     *
     * @param key  an <code>Object</code> specifying the border
     * @param l the <code>Locale</code> for which the border is desired; refer
     *        to {@code UIDefaults} for details on how a {@code null}
     *        {@code Locale} is handled
     * @return the <code>Border</code> object
     * @throws NullPointerException if {@code key} is {@code null}
     * @since 1.4
     */
    public static Border getBorder(Object key, Locale l) {
        return getDefaults().getBorder(key,l);
    }

    /**
     * Returns a string from the defaults. If the value for
     * {@code key} is not a {@code String}, {@code null} is returned.
     *
     * @param key  an <code>Object</code> specifying the string
     * @return the <code>String</code>
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public static String getString(Object key) {
        return getDefaults().getString(key);
    }

    /**
     * Returns a string from the defaults that is appropriate for the
     * given locale.  If the value for
     * {@code key} is not a {@code String}, {@code null} is returned.
     *
     * @param key  an <code>Object</code> specifying the string
     * @param l the <code>Locale</code> for which the string is desired; refer
     *        to {@code UIDefaults} for details on how a {@code null}
     *        {@code Locale} is handled
     * @return the <code>String</code>
     * @since 1.4
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public static String getString(Object key, Locale l) {
        return getDefaults().getString(key,l);
    }

    /**
     * Returns a string from the defaults that is appropriate for the
     * given locale.  If the value for
     * {@code key} is not a {@code String}, {@code null} is returned.
     *
     * @param key  an <code>Object</code> specifying the string
     * @param c {@code Component} used to determine the locale;
     *          {@code null} implies the default locale as
     *          returned by {@code Locale.getDefault()}
     * @return the <code>String</code>
     * @throws NullPointerException if {@code key} is {@code null}
     */
    static String getString(Object key, Component c) {
        Locale l = (c == null) ? Locale.getDefault() : c.getLocale();
        return getString(key, l);
    }

    /**
     * Returns an integer from the defaults. If the value for
     * {@code key} is not an {@code Integer}, or does not exist,
     * {@code 0} is returned.
     *
     * @param key  an <code>Object</code> specifying the int
     * @return the int
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public static int getInt(Object key) {
        return getDefaults().getInt(key);
    }

    /**
     * Returns an integer from the defaults that is appropriate
     * for the given locale. If the value for
     * {@code key} is not an {@code Integer}, or does not exist,
     * {@code 0} is returned.
     *
     * @param key  an <code>Object</code> specifying the int
     * @param l the <code>Locale</code> for which the int is desired; refer
     *        to {@code UIDefaults} for details on how a {@code null}
     *        {@code Locale} is handled
     * @return the int
     * @throws NullPointerException if {@code key} is {@code null}
     * @since 1.4
     */
    public static int getInt(Object key, Locale l) {
        return getDefaults().getInt(key,l);
    }

    /**
     * Returns a boolean from the defaults which is associated with
     * the key value. If the key is not found or the key doesn't represent
     * a boolean value then {@code false} is returned.
     *
     * @param key  an <code>Object</code> specifying the key for the desired boolean value
     * @return the boolean value corresponding to the key
     * @throws NullPointerException if {@code key} is {@code null}
     * @since 1.4
     */
    public static boolean getBoolean(Object key) {
        return getDefaults().getBoolean(key);
    }

    /**
     * Returns a boolean from the defaults which is associated with
     * the key value and the given <code>Locale</code>. If the key is not
     * found or the key doesn't represent
     * a boolean value then {@code false} will be returned.
     *
     * @param key  an <code>Object</code> specifying the key for the desired
     *             boolean value
     * @param l the <code>Locale</code> for which the boolean is desired; refer
     *        to {@code UIDefaults} for details on how a {@code null}
     *        {@code Locale} is handled
     * @return the boolean value corresponding to the key
     * @throws NullPointerException if {@code key} is {@code null}
     * @since 1.4
     */
    public static boolean getBoolean(Object key, Locale l) {
        return getDefaults().getBoolean(key,l);
    }

    /**
     * Returns an <code>Insets</code> object from the defaults. If the value
     * for {@code key} is not an {@code Insets}, {@code null} is returned.
     *
     * @param key  an <code>Object</code> specifying the <code>Insets</code> object
     * @return the <code>Insets</code> object
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public static Insets getInsets(Object key) {
        return getDefaults().getInsets(key);
    }

    /**
     * Returns an <code>Insets</code> object from the defaults that is
     * appropriate for the given locale. If the value
     * for {@code key} is not an {@code Insets}, {@code null} is returned.
     *
     * @param key  an <code>Object</code> specifying the <code>Insets</code> object
     * @param l the <code>Locale</code> for which the object is desired; refer
     *        to {@code UIDefaults} for details on how a {@code null}
     *        {@code Locale} is handled
     * @return the <code>Insets</code> object
     * @throws NullPointerException if {@code key} is {@code null}
     * @since 1.4
     */
    public static Insets getInsets(Object key, Locale l) {
        return getDefaults().getInsets(key,l);
    }

    /**
     * Returns a dimension from the defaults. If the value
     * for {@code key} is not a {@code Dimension}, {@code null} is returned.
     *
     * @param key  an <code>Object</code> specifying the dimension object
     * @return the <code>Dimension</code> object
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public static Dimension getDimension(Object key) {
        return getDefaults().getDimension(key);
    }

    /**
     * Returns a dimension from the defaults that is appropriate
     * for the given locale. If the value
     * for {@code key} is not a {@code Dimension}, {@code null} is returned.
     *
     * @param key  an <code>Object</code> specifying the dimension object
     * @param l the <code>Locale</code> for which the object is desired; refer
     *        to {@code UIDefaults} for details on how a {@code null}
     *        {@code Locale} is handled
     * @return the <code>Dimension</code> object
     * @throws NullPointerException if {@code key} is {@code null}
     * @since 1.4
     */
    public static Dimension getDimension(Object key, Locale l) {
        return getDefaults().getDimension(key,l);
    }

    /**
     * Returns an object from the defaults.
     *
     * @param key  an <code>Object</code> specifying the desired object
     * @return the <code>Object</code>
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public static Object get(Object key) {
        return getDefaults().get(key);
    }

    /**
     * Returns an object from the defaults that is appropriate for
     * the given locale.
     *
     * @param key  an <code>Object</code> specifying the desired object
     * @param l the <code>Locale</code> for which the object is desired; refer
     *        to {@code UIDefaults} for details on how a {@code null}
     *        {@code Locale} is handled
     * @return the <code>Object</code>
     * @throws NullPointerException if {@code key} is {@code null}
     * @since 1.4
     */
    public static Object get(Object key, Locale l) {
        return getDefaults().get(key,l);
    }

    /**
     * Stores an object in the developer defaults. This is a cover method
     * for {@code getDefaults().put(key, value)}. This only effects the
     * developer defaults, not the system or look and feel defaults.
     *
     * @param key    an <code>Object</code> specifying the retrieval key
     * @param value  the <code>Object</code> to store; refer to
     *               {@code UIDefaults} for details on how {@code null} is
     *               handled
     * @return the <code>Object</code> returned by {@link UIDefaults#put}
     * @throws NullPointerException if {@code key} is {@code null}
     * @see UIDefaults#put
     */
    public static Object put(Object key, Object value) {
        return getDefaults().put(key, value);
    }

    /**
     * Returns the appropriate {@code ComponentUI} implementation for
     * {@code target}. Typically, this is a cover for
     * {@code getDefaults().getUI(target)}. However, if an auxiliary
     * look and feel has been installed, this first invokes
     * {@code getUI(target)} on the multiplexing look and feel's
     * defaults, and returns that value if it is {@code non-null}.
     *
     * @param target the <code>JComponent</code> to return the
     *        {@code ComponentUI} for
     * @return the <code>ComponentUI</code> object for {@code target}
     * @throws NullPointerException if {@code target} is {@code null}
     * @see UIDefaults#getUI
     */
    public static ComponentUI getUI(JComponent target) {
        maybeInitialize();
        maybeInitializeFocusPolicy(target);
        ComponentUI ui = null;
        LookAndFeel multiLAF = getLAFState().multiLookAndFeel;
        if (multiLAF != null) {
            // This can return null if the multiplexing look and feel
            // doesn't support a particular UI.
            ui = multiLAF.getDefaults().getUI(target);
        }
        if (ui == null) {
            ui = getDefaults().getUI(target);
        }
        return ui;
    }


    /**
     * Returns the {@code UIDefaults} from the current look and feel,
     * that were obtained at the time the look and feel was installed.
     * <p>
     * In general, developers should use the {@code UIDefaults} returned from
     * {@code getDefaults()}. As the current look and feel may expect
     * certain values to exist, altering the {@code UIDefaults} returned
     * from this method could have unexpected results.
     *
     * @return <code>UIDefaults</code> from the current look and feel
     * @see #getDefaults
     * @see #setLookAndFeel(LookAndFeel)
     * @see LookAndFeel#getDefaults
     */
    public static UIDefaults getLookAndFeelDefaults() {
        maybeInitialize();
        return getLAFState().getLookAndFeelDefaults();
    }

    /**
     * Finds the Multiplexing <code>LookAndFeel</code>.
     */
    @SuppressWarnings("deprecation")
    private static LookAndFeel getMultiLookAndFeel() {
        LookAndFeel multiLookAndFeel = getLAFState().multiLookAndFeel;
        if (multiLookAndFeel == null) {
            String defaultName = "javax.swing.plaf.multi.MultiLookAndFeel";
            String className = getLAFState().swingProps.getProperty(multiplexingLAFKey, defaultName);
            try {
                Class<?> lnfClass = SwingUtilities.loadSystemClass(className);
                multiLookAndFeel =
                        (LookAndFeel)lnfClass.newInstance();
            } catch (Exception exc) {
                System.err.println("UIManager: failed loading " + className);
            }
        }
        return multiLookAndFeel;
    }

    /**
     * Adds a <code>LookAndFeel</code> to the list of auxiliary look and feels.
     * The auxiliary look and feels tell the multiplexing look and feel what
     * other <code>LookAndFeel</code> classes for a component instance are to be used
     * in addition to the default <code>LookAndFeel</code> class when creating a
     * multiplexing UI.  The change will only take effect when a new
     * UI class is created or when the default look and feel is changed
     * on a component instance.
     * <p>Note these are not the same as the installed look and feels.
     *
     * @param laf the <code>LookAndFeel</code> object
     * @see #removeAuxiliaryLookAndFeel
     * @see #setLookAndFeel
     * @see #getAuxiliaryLookAndFeels
     * @see #getInstalledLookAndFeels
     */
    public static void addAuxiliaryLookAndFeel(LookAndFeel laf) {
        maybeInitialize();

        if (!laf.isSupportedLookAndFeel()) {
            // Ideally we would throw an exception here, but it's too late
            // for that.
            return;
        }
        Vector<LookAndFeel> v = getLAFState().auxLookAndFeels;
        if (v == null) {
            v = new Vector<LookAndFeel>();
        }

        if (!v.contains(laf)) {
            v.addElement(laf);
            laf.initialize();
            getLAFState().auxLookAndFeels = v;

            if (getLAFState().multiLookAndFeel == null) {
                getLAFState().multiLookAndFeel = getMultiLookAndFeel();
            }
        }
    }

    /**
     * Removes a <code>LookAndFeel</code> from the list of auxiliary look and feels.
     * The auxiliary look and feels tell the multiplexing look and feel what
     * other <code>LookAndFeel</code> classes for a component instance are to be used
     * in addition to the default <code>LookAndFeel</code> class when creating a
     * multiplexing UI.  The change will only take effect when a new
     * UI class is created or when the default look and feel is changed
     * on a component instance.
     * <p>Note these are not the same as the installed look and feels.
     *
     * @param laf the {@code LookAndFeel} to be removed
     * @return true if the <code>LookAndFeel</code> was removed from the list
     * @see #removeAuxiliaryLookAndFeel
     * @see #getAuxiliaryLookAndFeels
     * @see #setLookAndFeel
     * @see #getInstalledLookAndFeels
     */
    public static boolean removeAuxiliaryLookAndFeel(LookAndFeel laf) {
        maybeInitialize();

        boolean result;

        Vector<LookAndFeel> v = getLAFState().auxLookAndFeels;
        if ((v == null) || (v.size() == 0)) {
            return false;
        }

        result = v.removeElement(laf);
        if (result) {
            if (v.size() == 0) {
                getLAFState().auxLookAndFeels = null;
                getLAFState().multiLookAndFeel = null;
            } else {
                getLAFState().auxLookAndFeels = v;
            }
        }
        laf.uninitialize();

        return result;
    }

    /**
     * Returns the list of auxiliary look and feels (can be <code>null</code>).
     * The auxiliary look and feels tell the multiplexing look and feel what
     * other <code>LookAndFeel</code> classes for a component instance are
     * to be used in addition to the default LookAndFeel class when creating a
     * multiplexing UI.
     * <p>Note these are not the same as the installed look and feels.
     *
     * @return list of auxiliary <code>LookAndFeel</code>s or <code>null</code>
     * @see #addAuxiliaryLookAndFeel
     * @see #removeAuxiliaryLookAndFeel
     * @see #setLookAndFeel
     * @see #getInstalledLookAndFeels
     */
    public static LookAndFeel[] getAuxiliaryLookAndFeels() {
        maybeInitialize();

        Vector<LookAndFeel> v = getLAFState().auxLookAndFeels;
        if ((v == null) || (v.size() == 0)) {
            return null;
        }
        else {
            LookAndFeel[] rv = new LookAndFeel[v.size()];
            for (int i = 0; i < rv.length; i++) {
                rv[i] = v.elementAt(i);
            }
            return rv;
        }
    }


    /**
     * Adds a <code>PropertyChangeListener</code> to the listener list.
     * The listener is registered for all properties.
     *
     * @param listener  the <code>PropertyChangeListener</code> to be added
     * @see java.beans.PropertyChangeSupport
     */
    public static void addPropertyChangeListener(PropertyChangeListener listener)
    {
        synchronized (classLock) {
            getLAFState().getPropertyChangeSupport(true).
                             addPropertyChangeListener(listener);
        }
    }


    /**
     * Removes a <code>PropertyChangeListener</code> from the listener list.
     * This removes a <code>PropertyChangeListener</code> that was registered
     * for all properties.
     *
     * @param listener  the <code>PropertyChangeListener</code> to be removed
     * @see java.beans.PropertyChangeSupport
     */
    public static void removePropertyChangeListener(PropertyChangeListener listener)
    {
        synchronized (classLock) {
            getLAFState().getPropertyChangeSupport(true).
                          removePropertyChangeListener(listener);
        }
    }


    /**
     * Returns an array of all the <code>PropertyChangeListener</code>s added
     * to this UIManager with addPropertyChangeListener().
     *
     * @return all of the <code>PropertyChangeListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    public static PropertyChangeListener[] getPropertyChangeListeners() {
        synchronized(classLock) {
            return getLAFState().getPropertyChangeSupport(true).
                      getPropertyChangeListeners();
        }
    }

    @SuppressWarnings("removal")
    private static Properties loadSwingProperties()
    {
        /* Don't bother checking for Swing properties if untrusted, as
         * there's no way to look them up without triggering SecurityExceptions.
         */
        if (UIManager.class.getClassLoader() != null) {
            return new Properties();
        }
        else {
            final Properties props = new Properties();

            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Object>() {
                public Object run() {
                    OSInfo.OSType osType = AccessController.doPrivileged(OSInfo.getOSTypeAction());
                    if (osType == OSInfo.OSType.MACOSX) {
                        props.put(defaultLAFKey, getSystemLookAndFeelClassName());
                    }

                    try {
                        File file = new File(makeSwingPropertiesFilename());

                        if (file.exists()) {
                            // InputStream has been buffered in Properties
                            // class
                            FileInputStream ins = new FileInputStream(file);
                            props.load(ins);
                            ins.close();
                        }
                    }
                    catch (Exception e) {
                        // No such file, or file is otherwise non-readable.
                    }

                    // Check whether any properties were overridden at the
                    // command line.
                    checkProperty(props, defaultLAFKey);
                    checkProperty(props, auxiliaryLAFsKey);
                    checkProperty(props, multiplexingLAFKey);
                    checkProperty(props, installedLAFsKey);
                    checkProperty(props, disableMnemonicKey);
                    // Don't care about return value.
                    return null;
                }
            });
            return props;
        }
    }

    private static void checkProperty(Properties props, String key) {
        // No need to do catch the SecurityException here, this runs
        // in a doPrivileged.
        String value = System.getProperty(key);
        if (value != null) {
            props.put(key, value);
        }
    }


    /**
     * If a <code>swing.properties</code> file exist and it has a
     * <code>swing.installedlafs</code> property
     * then initialize the <code>installedLAFs</code> field.
     *
     * @see #getInstalledLookAndFeels
     */
    private static void initializeInstalledLAFs(Properties swingProps)
    {
        String ilafsString = swingProps.getProperty(installedLAFsKey);
        if (ilafsString == null) {
            return;
        }

        /* Create a vector that contains the value of the swing.installedlafs
         * property.  For example given "swing.installedlafs=motif,windows"
         * lafs = {"motif", "windows"}.
         */
        Vector<String> lafs = new Vector<String>();
        StringTokenizer st = new StringTokenizer(ilafsString, ",", false);
        while (st.hasMoreTokens()) {
            lafs.addElement(st.nextToken());
        }

        /* Look up the name and class for each name in the "swing.installedlafs"
         * list.  If they both exist then add a LookAndFeelInfo to
         * the installedLafs array.
         */
        Vector<LookAndFeelInfo> ilafs = new Vector<LookAndFeelInfo>(lafs.size());
        for (String laf : lafs) {
            String name = swingProps.getProperty(makeInstalledLAFKey(laf, "name"), laf);
            String cls = swingProps.getProperty(makeInstalledLAFKey(laf, "class"));
            if (cls != null) {
                ilafs.addElement(new LookAndFeelInfo(name, cls));
            }
        }

        LookAndFeelInfo[] installedLAFs = new LookAndFeelInfo[ilafs.size()];
        for(int i = 0; i < ilafs.size(); i++) {
            installedLAFs[i] = ilafs.elementAt(i);
        }
        getLAFState().installedLAFs = installedLAFs;
    }


    /**
     * If the user has specified a default look and feel, use that.
     * Otherwise use the look and feel that's native to this platform.
     * If this code is called after the application has explicitly
     * set it's look and feel, do nothing.
     *
     * @see #maybeInitialize
     */
    private static void initializeDefaultLAF(Properties swingProps)
    {
        if (getLAFState().lookAndFeel != null) {
            return;
        }

        // Try to get default LAF from system property, then from AppContext
        // (6653395), then use cross-platform one by default.
        String lafName = null;
        @SuppressWarnings("unchecked")
        HashMap<Object, String> lafData =
                (HashMap) AppContext.getAppContext().remove("swing.lafdata");
        if (lafData != null) {
            lafName = lafData.remove("defaultlaf");
        }
        if (lafName == null) {
            lafName = getCrossPlatformLookAndFeelClassName();
        }
        lafName = swingProps.getProperty(defaultLAFKey, lafName);

        try {
            setLookAndFeel(lafName);
        } catch (Exception e) {
            throw new Error("Cannot load " + lafName);
        }

        // Set any properties passed through AppContext (6653395).
        if (lafData != null) {
            for (Object key: lafData.keySet()) {
                UIManager.put(key, lafData.get(key));
            }
        }
    }


    @SuppressWarnings("deprecation")
    private static void initializeAuxiliaryLAFs(Properties swingProps)
    {
        String auxLookAndFeelNames = swingProps.getProperty(auxiliaryLAFsKey);
        if (auxLookAndFeelNames == null) {
            return;
        }

        Vector<LookAndFeel> auxLookAndFeels = new Vector<LookAndFeel>();

        StringTokenizer p = new StringTokenizer(auxLookAndFeelNames,",");
        String factoryName;

        /* Try to load each LookAndFeel subclass in the list.
         */

        while (p.hasMoreTokens()) {
            String className = p.nextToken();
            try {
                Class<?> lnfClass = SwingUtilities.loadSystemClass(className);
                LookAndFeel newLAF =
                        (LookAndFeel)lnfClass.newInstance();
                newLAF.initialize();
                auxLookAndFeels.addElement(newLAF);
            }
            catch (Exception e) {
                System.err.println("UIManager: failed loading auxiliary look and feel " + className);
            }
        }

        /* If there were problems and no auxiliary look and feels were
         * loaded, make sure we reset auxLookAndFeels to null.
         * Otherwise, we are going to use the MultiLookAndFeel to get
         * all component UI's, so we need to load it now.
         */
        if (auxLookAndFeels.size() == 0) {
            auxLookAndFeels = null;
        }
        else {
            getLAFState().multiLookAndFeel = getMultiLookAndFeel();
            if (getLAFState().multiLookAndFeel == null) {
                auxLookAndFeels = null;
            }
        }

        getLAFState().auxLookAndFeels = auxLookAndFeels;
    }


    private static void initializeSystemDefaults(Properties swingProps) {
        getLAFState().swingProps = swingProps;
    }


    /*
     * This method is called before any code that depends on the
     * <code>AppContext</code> specific LAFState object runs.  When the AppContext
     * corresponds to a set of applets it's possible for this method
     * to be re-entered, which is why we grab a lock before calling
     * initialize().
     */
    private static void maybeInitialize() {
        synchronized (classLock) {
            if (!getLAFState().initialized) {
                getLAFState().initialized = true;
                initialize();
            }
        }
    }

    /*
     * Sets default swing focus traversal policy.
     */
    @SuppressWarnings("deprecation")
    private static void maybeInitializeFocusPolicy(JComponent comp) {
        // Check for JRootPane which indicates that a swing toplevel
        // is coming, in which case a swing default focus policy
        // should be instatiated. See 7125044.
        if (comp instanceof JRootPane) {
            synchronized (classLock) {
                if (!getLAFState().focusPolicyInitialized) {
                    getLAFState().focusPolicyInitialized = true;

                    if (FocusManager.isFocusManagerEnabled()) {
                        KeyboardFocusManager.getCurrentKeyboardFocusManager().
                            setDefaultFocusTraversalPolicy(
                                new LayoutFocusTraversalPolicy());
                    }
                }
            }
        }
    }

    /*
     * Only called by maybeInitialize().
     */
    private static void initialize() {
        Properties swingProps = loadSwingProperties();
        initializeSystemDefaults(swingProps);
        initializeDefaultLAF(swingProps);
        initializeAuxiliaryLAFs(swingProps);
        initializeInstalledLAFs(swingProps);

        // Install Swing's PaintEventDispatcher
        if (RepaintManager.HANDLE_TOP_LEVEL_PAINT) {
            sun.awt.PaintEventDispatcher.setPaintEventDispatcher(
                                        new SwingPaintEventDispatcher());
        }
        // Install a hook that will be invoked if no one consumes the
        // KeyEvent.  If the source isn't a JComponent this will process
        // key bindings, if the source is a JComponent it implies that
        // processKeyEvent was already invoked and thus no need to process
        // the bindings again, unless the Component is disabled, in which
        // case KeyEvents will no longer be dispatched to it so that we
        // handle it here.
        KeyboardFocusManager.getCurrentKeyboardFocusManager().
                addKeyEventPostProcessor(new KeyEventPostProcessor() {
                    public boolean postProcessKeyEvent(KeyEvent e) {
                        Component c = e.getComponent();

                        if ((!(c instanceof JComponent) ||
                             (c != null && !c.isEnabled())) &&
                                JComponent.KeyboardState.shouldProcess(e) &&
                                SwingUtilities.processKeyBindings(e)) {
                            e.consume();
                            return true;
                        }
                        return false;
                    }
                });
        AWTAccessor.getComponentAccessor().
            setRequestFocusController(JComponent.focusController);
    }
}
