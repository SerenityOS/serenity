/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.ConstructorProperties;
import java.io.InputStream;
import java.io.Serial;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

import sun.awt.AWTAccessor;
import sun.util.logging.PlatformLogger;

/**
 * A class to encapsulate the bitmap representation of the mouse cursor.
 *
 * @see Component#setCursor
 * @author      Amy Fowler
 */
public class Cursor implements java.io.Serializable {

    /**
     * The default cursor type (gets set if no cursor is defined).
     */
    public static final int     DEFAULT_CURSOR                  = 0;

    /**
     * The crosshair cursor type.
     */
    public static final int     CROSSHAIR_CURSOR                = 1;

    /**
     * The text cursor type.
     */
    public static final int     TEXT_CURSOR                     = 2;

    /**
     * The wait cursor type.
     */
    public static final int     WAIT_CURSOR                     = 3;

    /**
     * The south-west-resize cursor type.
     */
    public static final int     SW_RESIZE_CURSOR                = 4;

    /**
     * The south-east-resize cursor type.
     */
    public static final int     SE_RESIZE_CURSOR                = 5;

    /**
     * The north-west-resize cursor type.
     */
    public static final int     NW_RESIZE_CURSOR                = 6;

    /**
     * The north-east-resize cursor type.
     */
    public static final int     NE_RESIZE_CURSOR                = 7;

    /**
     * The north-resize cursor type.
     */
    public static final int     N_RESIZE_CURSOR                 = 8;

    /**
     * The south-resize cursor type.
     */
    public static final int     S_RESIZE_CURSOR                 = 9;

    /**
     * The west-resize cursor type.
     */
    public static final int     W_RESIZE_CURSOR                 = 10;

    /**
     * The east-resize cursor type.
     */
    public static final int     E_RESIZE_CURSOR                 = 11;

    /**
     * The hand cursor type.
     */
    public static final int     HAND_CURSOR                     = 12;

    /**
     * The move cursor type.
     */
    public static final int     MOVE_CURSOR                     = 13;

    /**
      * @deprecated As of JDK version 1.7, the {@link #getPredefinedCursor(int)}
      * method should be used instead.
      */
    @Deprecated
    protected static Cursor[] predefined = new Cursor[14];

    /**
     * This field is a private replacement for 'predefined' array.
     */
    private static final Cursor[] predefinedPrivate = new Cursor[14];

    /* Localization names and default values */
    static final String[][] cursorProperties = {
        { "AWT.DefaultCursor", "Default Cursor" },
        { "AWT.CrosshairCursor", "Crosshair Cursor" },
        { "AWT.TextCursor", "Text Cursor" },
        { "AWT.WaitCursor", "Wait Cursor" },
        { "AWT.SWResizeCursor", "Southwest Resize Cursor" },
        { "AWT.SEResizeCursor", "Southeast Resize Cursor" },
        { "AWT.NWResizeCursor", "Northwest Resize Cursor" },
        { "AWT.NEResizeCursor", "Northeast Resize Cursor" },
        { "AWT.NResizeCursor", "North Resize Cursor" },
        { "AWT.SResizeCursor", "South Resize Cursor" },
        { "AWT.WResizeCursor", "West Resize Cursor" },
        { "AWT.EResizeCursor", "East Resize Cursor" },
        { "AWT.HandCursor", "Hand Cursor" },
        { "AWT.MoveCursor", "Move Cursor" },
    };

    /**
     * The chosen cursor type initially set to
     * the {@code DEFAULT_CURSOR}.
     *
     * @serial
     * @see #getType()
     */
    int type = DEFAULT_CURSOR;

    /**
     * The type associated with all custom cursors.
     */
    public static final int     CUSTOM_CURSOR                   = -1;

    /*
     * hashtable, resource prefix, filename, and properties for custom cursors
     * support
     */
    private static final Hashtable<String,Cursor> systemCustomCursors = new Hashtable<>(1);
    private static final String RESOURCE_PREFIX = "/sun/awt/resources/cursors/";
    private static final String PROPERTIES_FILE = RESOURCE_PREFIX + "cursors.properties";

    private static       Properties systemCustomCursorProperties = null;

    private static final String CURSOR_DOT_PREFIX = "Cursor.";
    private static final String DOT_FILE_SUFFIX = ".File";
    private static final String DOT_HOTSPOT_SUFFIX = ".HotSpot";
    private static final String DOT_NAME_SUFFIX = ".Name";

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 8028237497568985504L;

    private static final PlatformLogger log = PlatformLogger.getLogger("java.awt.Cursor");

    static {
        /* ensure that the necessary native libraries are loaded */
        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }

        AWTAccessor.setCursorAccessor(
            new AWTAccessor.CursorAccessor() {
                public long getPData(Cursor cursor) {
                    return cursor.pData;
                }

                public void setPData(Cursor cursor, long pData) {
                    cursor.pData = pData;
                }

                public int getType(Cursor cursor) {
                    return cursor.type;
                }
            });
    }

    /**
     * Initialize JNI field and method IDs for fields that may be
     * accessed from C.
     */
    private static native void initIDs();

    /**
     * Hook into native data.
     */
    private transient long pData;

    private transient Object anchor = new Object();

    static class CursorDisposer implements sun.java2d.DisposerRecord {
        volatile long pData;
        public CursorDisposer(long pData) {
            this.pData = pData;
        }
        public void dispose() {
            if (pData != 0) {
                finalizeImpl(pData);
            }
        }
    }
    transient CursorDisposer disposer;
    private void setPData(long pData) {
        this.pData = pData;
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        if (disposer == null) {
            disposer = new CursorDisposer(pData);
            // anchor is null after deserialization
            if (anchor == null) {
                anchor = new Object();
            }
            sun.java2d.Disposer.addRecord(anchor, disposer);
        } else {
            disposer.pData = pData;
        }
    }

    /**
     * The user-visible name of the cursor.
     *
     * @serial
     * @see #getName()
     */
    protected String name;

    /**
     * Returns a cursor object with the specified predefined type.
     *
     * @param type the type of predefined cursor
     * @return the specified predefined cursor
     * @throws IllegalArgumentException if the specified cursor type is
     *         invalid
     */
    public static Cursor getPredefinedCursor(int type) {
        if (type < Cursor.DEFAULT_CURSOR || type > Cursor.MOVE_CURSOR) {
            throw new IllegalArgumentException("illegal cursor type");
        }
        Cursor c = predefinedPrivate[type];
        if (c == null) {
            predefinedPrivate[type] = c = new Cursor(type);
        }
        // fill 'predefined' array for backwards compatibility.
        if (predefined[type] == null) {
            predefined[type] = c;
        }
        return c;
    }

    /**
     * Returns a system-specific custom cursor object matching the
     * specified name.  Cursor names are, for example: "Invalid.16x16"
     *
     * @param name a string describing the desired system-specific custom cursor
     * @return the system specific custom cursor named
     * @exception HeadlessException if
     * {@code GraphicsEnvironment.isHeadless} returns true
     * @exception AWTException in case of erroneous retrieving of the cursor
     */
    public static Cursor getSystemCustomCursor(final String name)
        throws AWTException, HeadlessException {
        GraphicsEnvironment.checkHeadless();
        Cursor cursor = systemCustomCursors.get(name);

        if (cursor == null) {
            synchronized(systemCustomCursors) {
                if (systemCustomCursorProperties == null)
                    loadSystemCustomCursorProperties();
            }

            String prefix = CURSOR_DOT_PREFIX + name;
            String key    = prefix + DOT_FILE_SUFFIX;

            if (!systemCustomCursorProperties.containsKey(key)) {
                if (log.isLoggable(PlatformLogger.Level.FINER)) {
                    log.finer("Cursor.getSystemCustomCursor(" + name + ") returned null");
                }
                return null;
            }

            final String fileName =
                systemCustomCursorProperties.getProperty(key);

            final String localized = systemCustomCursorProperties.getProperty(
                    prefix + DOT_NAME_SUFFIX, name);

            String hotspot = systemCustomCursorProperties.getProperty(prefix + DOT_HOTSPOT_SUFFIX);

            if (hotspot == null)
                throw new AWTException("no hotspot property defined for cursor: " + name);

            StringTokenizer st = new StringTokenizer(hotspot, ",");

            if (st.countTokens() != 2)
                throw new AWTException("failed to parse hotspot property for cursor: " + name);

            final Point hotPoint;
            try {
                hotPoint = new Point(Integer.parseInt(st.nextToken()),
                                     Integer.parseInt(st.nextToken()));
            } catch (NumberFormatException nfe) {
                throw new AWTException("failed to parse hotspot property for cursor: " + name);
            }
            final Toolkit toolkit = Toolkit.getDefaultToolkit();
            final String file = RESOURCE_PREFIX + fileName;
            @SuppressWarnings("removal")
            final InputStream in = AccessController.doPrivileged(
                    (PrivilegedAction<InputStream>) () -> {
                        return Cursor.class.getResourceAsStream(file);
                    });
            try (in) {
                Image image = toolkit.createImage(in.readAllBytes());
                cursor = toolkit.createCustomCursor(image, hotPoint, localized);
            } catch (Exception e) {
                throw new AWTException(
                    "Exception: " + e.getClass() + " " + e.getMessage() +
                    " occurred while creating cursor " + name);
            }

            if (cursor == null) {
                if (log.isLoggable(PlatformLogger.Level.FINER)) {
                    log.finer("Cursor.getSystemCustomCursor(" + name + ") returned null");
                }
            } else {
                systemCustomCursors.put(name, cursor);
            }
        }

        return cursor;
    }

    /**
     * Return the system default cursor.
     *
     * @return the default cursor
     */
    public static Cursor getDefaultCursor() {
        return getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    }

    /**
     * Creates a new cursor object with the specified type.
     * @param type the type of cursor
     * @throws IllegalArgumentException if the specified cursor type
     * is invalid
     */
    @ConstructorProperties({"type"})
    public Cursor(int type) {
        if (type < Cursor.DEFAULT_CURSOR || type > Cursor.MOVE_CURSOR) {
            throw new IllegalArgumentException("illegal cursor type");
        }
        this.type = type;

        // Lookup localized name.
        name = Toolkit.getProperty(cursorProperties[type][0],
                                   cursorProperties[type][1]);
    }

    /**
     * Creates a new custom cursor object with the specified name.<p>
     * Note:  this constructor should only be used by AWT implementations
     * as part of their support for custom cursors.  Applications should
     * use Toolkit.createCustomCursor().
     * @param name the user-visible name of the cursor.
     * @see java.awt.Toolkit#createCustomCursor
     */
    protected Cursor(String name) {
        this.type = Cursor.CUSTOM_CURSOR;
        this.name = name;
    }

    /**
     * Returns the type for this cursor.
     *
     * @return the cursor type
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the name of this cursor.
     * @return    a localized description of this cursor.
     * @since     1.2
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of this cursor.
     * @return    a string representation of this cursor.
     * @since     1.2
     */
    public String toString() {
        return getClass().getName() + "[" + getName() + "]";
    }

    /*
     * load the cursor.properties file
     */
    @SuppressWarnings("removal")
    private static void loadSystemCustomCursorProperties() throws AWTException {
        synchronized(systemCustomCursors) {
            systemCustomCursorProperties = new Properties();

            try {
                AccessController.doPrivileged(
                        (PrivilegedExceptionAction<Object>) () -> {
                            try (InputStream is = Cursor.class
                                    .getResourceAsStream(PROPERTIES_FILE)) {
                                systemCustomCursorProperties.load(is);
                            }
                            return null;
                        });
            } catch (Exception e) {
                systemCustomCursorProperties = null;
                 throw new AWTException("Exception: " + e.getClass() + " " +
                   e.getMessage() + " occurred while loading: " +
                   PROPERTIES_FILE);
            }
        }
    }

    private static native void finalizeImpl(long pData);
}
