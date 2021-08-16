/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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


/**
 * Ported from awt_wm.c, SCCS v1.11, author Valeriy Ushakov
 * Author: Denis Mikhalkin
 */
package sun.awt.X11;

import sun.awt.IconInfo;
import jdk.internal.misc.Unsafe;
import java.awt.Insets;
import java.awt.Frame;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.util.logging.PlatformLogger;


/**
 * Class incapsulating knowledge about window managers in general
 * Descendants should provide some information about specific window manager.
 */
final class XWM
{

    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XWM");
    private static final PlatformLogger insLog = PlatformLogger.getLogger("sun.awt.X11.insets.XWM");
    private static final PlatformLogger stateLog = PlatformLogger.getLogger("sun.awt.X11.states.XWM");

    static final XAtom XA_MWM_HINTS = new XAtom();

    private static Unsafe unsafe = XlibWrapper.unsafe;


/* Good old ICCCM */
    static XAtom XA_WM_STATE = new XAtom();


    XAtom XA_UTF8_STRING = XAtom.get("UTF8_STRING");    /* like STRING but encoding is UTF-8 */

/* Currently we only care about max_v and max_h in _NET_WM_STATE */
    static final int AWT_NET_N_KNOWN_STATES=2;

/* Enlightenment */
    static final XAtom XA_E_FRAME_SIZE = new XAtom();

/* KWin (KDE2) */
    static final XAtom XA_KDE_NET_WM_FRAME_STRUT = new XAtom();

/* KWM (KDE 1.x) OBSOLETE??? */
    static final XAtom XA_KWM_WIN_ICONIFIED = new XAtom();
    static final XAtom XA_KWM_WIN_MAXIMIZED = new XAtom();

/* OpenLook */
    static final XAtom XA_OL_DECOR_DEL = new XAtom();
    static final XAtom XA_OL_DECOR_HEADER = new XAtom();
    static final XAtom XA_OL_DECOR_RESIZE = new XAtom();
    static final XAtom XA_OL_DECOR_PIN = new XAtom();
    static final XAtom XA_OL_DECOR_CLOSE = new XAtom();

/* EWMH */
    static final XAtom XA_NET_FRAME_EXTENTS = new XAtom();
    static final XAtom XA_NET_REQUEST_FRAME_EXTENTS = new XAtom();

    static final int
        UNDETERMINED_WM = 1,
        NO_WM = 2,
        OTHER_WM = 3,
        OPENLOOK_WM = 4,
        MOTIF_WM = 5,
        CDE_WM = 6,
        ENLIGHTEN_WM = 7,
        KDE2_WM = 8,
        SAWFISH_WM = 9,
        ICE_WM = 10,
        METACITY_WM = 11,
        COMPIZ_WM = 12,
        LG3D_WM = 13,
        CWM_WM = 14,
        MUTTER_WM = 15,
        UNITY_COMPIZ_WM = 16;
    public String toString() {
        switch  (WMID) {
          case NO_WM:
              return "NO WM";
          case OTHER_WM:
              return "Other WM";
          case OPENLOOK_WM:
              return "OPENLOOK";
          case MOTIF_WM:
              return "MWM";
          case CDE_WM:
              return "DTWM";
          case ENLIGHTEN_WM:
              return "Enlightenment";
          case KDE2_WM:
              return "KWM2";
          case SAWFISH_WM:
              return "Sawfish";
          case ICE_WM:
              return "IceWM";
          case METACITY_WM:
              return "Metacity";
          case COMPIZ_WM:
              return "Compiz";
            case UNITY_COMPIZ_WM:
              return "Unity Compiz";
          case LG3D_WM:
              return "LookingGlass";
          case CWM_WM:
              return "CWM";
          case MUTTER_WM:
              return "Mutter";
          case UNDETERMINED_WM:
          default:
              return "Undetermined WM";
        }
    }


    int WMID;
    static final Insets zeroInsets = new Insets(0, 0, 0, 0);
    static final Insets defaultInsets = new Insets(25, 5, 5, 5);

    XWM(int WMID) {
        this.WMID = WMID;
        initializeProtocols();
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Window manager: " + toString());
        }
    }
    int getID() {
        return WMID;
    }


    static Insets normalize(Insets insets) {
        if (insets.top > 64 || insets.top < 0) {
            insets.top = 28;
        }
        if (insets.left > 32 || insets.left < 0) {
            insets.left = 6;
        }
        if (insets.right > 32 || insets.right < 0) {
            insets.right = 6;
        }
        if (insets.bottom > 32 || insets.bottom < 0) {
            insets.bottom = 6;
        }
        return insets;
    }

    static XNETProtocol g_net_protocol = null;
    static XWINProtocol g_win_protocol = null;
    static boolean isNetWMName(String name) {
        if (g_net_protocol != null) {
            return g_net_protocol.isWMName(name);
        } else {
            return false;
        }
    }

    static void initAtoms() {
        final Object[][] atomInitList ={
            { XA_WM_STATE,                      "WM_STATE"                  },

            { XA_KDE_NET_WM_FRAME_STRUT,    "_KDE_NET_WM_FRAME_STRUT"       },

            { XA_E_FRAME_SIZE,              "_E_FRAME_SIZE"                 },

            { XA_KWM_WIN_ICONIFIED,          "KWM_WIN_ICONIFIED"             },
            { XA_KWM_WIN_MAXIMIZED,          "KWM_WIN_MAXIMIZED"             },

            { XA_OL_DECOR_DEL,               "_OL_DECOR_DEL"                 },
            { XA_OL_DECOR_HEADER,            "_OL_DECOR_HEADER"              },
            { XA_OL_DECOR_RESIZE,            "_OL_DECOR_RESIZE"              },
            { XA_OL_DECOR_PIN,               "_OL_DECOR_PIN"                 },
            { XA_OL_DECOR_CLOSE,             "_OL_DECOR_CLOSE"               },
            { XA_MWM_HINTS,                  "_MOTIF_WM_HINTS"               },
            { XA_NET_FRAME_EXTENTS,          "_NET_FRAME_EXTENTS"            },
            { XA_NET_REQUEST_FRAME_EXTENTS,  "_NET_REQUEST_FRAME_EXTENTS"    },
        };

        String[] names = new String[atomInitList.length];
        for (int index = 0; index < names.length; index++) {
            names[index] = (String)atomInitList[index][1];
        }

        int atomSize = XAtom.getAtomSize();
        long atoms = unsafe.allocateMemory(names.length*atomSize);
        XToolkit.awtLock();
        try {
            int status = XlibWrapper.XInternAtoms(XToolkit.getDisplay(), names, false, atoms);
            if (status == 0) {
                return;
            }
            for (int atom = 0, atomPtr = 0; atom < names.length; atom++, atomPtr += atomSize) {
                ((XAtom)(atomInitList[atom][0])).setValues(XToolkit.getDisplay(), names[atom], XAtom.getAtom(atoms + atomPtr));
            }
        } finally {
            XToolkit.awtUnlock();
            unsafe.freeMemory(atoms);
        }
    }

    /*
     * MUST BE CALLED UNDER AWTLOCK.
     *
     * If *any* window manager is running?
     *
     * According to ICCCM 2.0 section 4.3.
     * WM will acquire ownership of a selection named WM_Sn, where n is
     * the screen number.
     *
     * No selection owner, but, perhaps it is not ICCCM compliant WM
     * (e.g. CDE/Sawfish).
     * Try selecting for SubstructureRedirect, that only one client
     * can select for, and if the request fails, than some other WM is
     * already running.
     *
     * We also treat eXcursion as NO_WM.
     */
    private static boolean isNoWM() {
        /*
         * Quick checks for specific servers.
         */
        String vendor_string = XlibWrapper.ServerVendor(XToolkit.getDisplay());
        if (vendor_string.indexOf("eXcursion") != -1) {
            /*
             * Use NO_WM since in all other aspects eXcursion is like not
             * having a window manager running. I.e. it does not reparent
             * top level shells.
             */
            if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
                insLog.finer("eXcursion means NO_WM");
            }
            return true;
        }

        XSetWindowAttributes substruct = new XSetWindowAttributes();
        try {
            /*
             * Let's check an owner of WM_Sn selection for the default screen.
             */
            final long default_screen_number =
                XlibWrapper.DefaultScreen(XToolkit.getDisplay());
            final String selection_name = "WM_S" + default_screen_number;

            long selection_owner =
                XlibWrapper.XGetSelectionOwner(XToolkit.getDisplay(),
                                               XAtom.get(selection_name).getAtom());
            if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
                insLog.finer("selection owner of " + selection_name
                             + " is " + selection_owner);
            }

            if (selection_owner != XConstants.None) {
                return false;
            }

            winmgr_running = false;
            substruct.set_event_mask(XConstants.SubstructureRedirectMask);

            XErrorHandlerUtil.WITH_XERROR_HANDLER(detectWMHandler);
            XlibWrapper.XChangeWindowAttributes(XToolkit.getDisplay(),
                                                XToolkit.getDefaultRootWindow(),
                                                XConstants.CWEventMask,
                                                substruct.pData);
            XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

            /*
             * If no WM is running then our selection for SubstructureRedirect
             * succeeded and needs to be undone (hey we are *not* a WM ;-).
             */
            if (!winmgr_running) {
                substruct.set_event_mask(0);
                XlibWrapper.XChangeWindowAttributes(XToolkit.getDisplay(),
                                                    XToolkit.getDefaultRootWindow(),
                                                    XConstants.CWEventMask,
                                                    substruct.pData);
                if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
                    insLog.finer("It looks like there is no WM thus NO_WM");
                }
            }

            return !winmgr_running;
        } finally {
            substruct.dispose();
        }
    }

    static XAtom XA_ENLIGHTENMENT_COMMS = new XAtom("ENLIGHTENMENT_COMMS", false);
    /*
     * Helper function for isEnlightenment().
     * Enlightenment uses STRING property for its comms window id.  Gaaa!
     * The property is ENLIGHTENMENT_COMMS, STRING/8 and the string format
     * is "WINID %8x".  Gee, I haven't been using scanf for *ages*... :-)
     */
    static long getECommsWindowIDProperty(long window) {

        if (!XA_ENLIGHTENMENT_COMMS.isInterned()) {
            return 0;
        }

        WindowPropertyGetter getter =
            new WindowPropertyGetter(window, XA_ENLIGHTENMENT_COMMS, 0, 14, false,
                                     XAtom.XA_STRING);
        try {
            int status = getter.execute(XErrorHandler.IgnoreBadWindowHandler.getInstance());
            if (status != XConstants.Success || getter.getData() == 0) {
                return 0;
            }

            if (getter.getActualType() != XAtom.XA_STRING
                || getter.getActualFormat() != 8
                || getter.getNumberOfItems() != 14 || getter.getBytesAfter() != 0)
            {
                return 0;
            }

            // Convert data to String, ASCII
            byte[] bytes = XlibWrapper.getStringBytes(getter.getData());
            String id = new String(bytes);

            if (log.isLoggable(PlatformLogger.Level.FINER)) {
                log.finer("ENLIGHTENMENT_COMMS is " + id);
            }

            // Parse WINID
            Pattern winIdPat = Pattern.compile("WINID\\s+(\\p{XDigit}{0,8})");
            try {
                Matcher match = winIdPat.matcher(id);
                if (match.matches()) {
                    if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                        log.finest("Match group count: " + match.groupCount());
                    }
                    String longId = match.group(1);
                    if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                        log.finest("Match group 1 " + longId);
                    }
                    long winid = Long.parseLong(longId, 16);
                    if (log.isLoggable(PlatformLogger.Level.FINER)) {
                        log.finer("Enlightenment communication window " + winid);
                    }
                    return winid;
                } else {
                    log.finer("ENLIGHTENMENT_COMMS has wrong format");
                    return 0;
                }
            } catch (Exception e) {
                if (log.isLoggable(PlatformLogger.Level.FINER)) {
                    e.printStackTrace();
                }
                return 0;
            }
        } finally {
            getter.dispose();
        }
    }

    /*
     * Is Enlightenment WM running?  Congruent to awt_wm_checkAnchor, but
     * uses STRING property peculiar to Enlightenment.
     */
    static boolean isEnlightenment() {

        long root_xref = getECommsWindowIDProperty(XToolkit.getDefaultRootWindow());
        if (root_xref == 0) {
            return false;
        }

        long self_xref = getECommsWindowIDProperty(root_xref);
        if (self_xref != root_xref) {
            return false;
        }

        return true;
    }

    /*
     * Is CDE running?
     *
     * XXX: This is hairy...  CDE is MWM as well.  It seems we simply test
     * for default setup and will be bitten if user changes things...
     *
     * Check for _DT_SM_WINDOW_INFO(_DT_SM_WINDOW_INFO) on root.  Take the
     * second element of the property and check for presence of
     * _DT_SM_STATE_INFO(_DT_SM_STATE_INFO) on that window.
     *
     * XXX: Any header that defines this structures???
     */
    static final XAtom XA_DT_SM_WINDOW_INFO = new XAtom("_DT_SM_WINDOW_INFO", false);
    static final XAtom XA_DT_SM_STATE_INFO = new XAtom("_DT_SM_STATE_INFO", false);
    static boolean isCDE() {

        if (!XA_DT_SM_WINDOW_INFO.isInterned()) {
            if (log.isLoggable(PlatformLogger.Level.FINER)) {
                log.finer("{0} is not interned", XA_DT_SM_WINDOW_INFO);
            }
            return false;
        }

        WindowPropertyGetter getter =
            new WindowPropertyGetter(XToolkit.getDefaultRootWindow(),
                                     XA_DT_SM_WINDOW_INFO, 0, 2,
                                     false, XA_DT_SM_WINDOW_INFO);
        try {
            int status = getter.execute();
            if (status != XConstants.Success || getter.getData() == 0) {
                log.finer("Getting of _DT_SM_WINDOW_INFO is not successfull");
                return false;
            }
            if (getter.getActualType() != XA_DT_SM_WINDOW_INFO.getAtom()
                || getter.getActualFormat() != 32
                || getter.getNumberOfItems() != 2 || getter.getBytesAfter() != 0)
            {
                log.finer("Wrong format of _DT_SM_WINDOW_INFO");
                return false;
            }

            long wmwin = Native.getWindow(getter.getData(), 1); //unsafe.getInt(getter.getData()+4);

            if (wmwin == 0) {
                log.fine("WARNING: DT_SM_WINDOW_INFO exists but returns zero windows");
                return false;
            }

            /* Now check that this window has _DT_SM_STATE_INFO (ignore contents) */
            if (!XA_DT_SM_STATE_INFO.isInterned()) {
                if (log.isLoggable(PlatformLogger.Level.FINER)) {
                    log.finer("{0} is not interned", XA_DT_SM_STATE_INFO);
                }
                return false;
            }
            WindowPropertyGetter getter2 =
                new WindowPropertyGetter(wmwin, XA_DT_SM_STATE_INFO, 0, 1,
                                         false, XA_DT_SM_STATE_INFO);
            try {
                status = getter2.execute(XErrorHandler.IgnoreBadWindowHandler.getInstance());


                if (status != XConstants.Success || getter2.getData() == 0) {
                    log.finer("Getting of _DT_SM_STATE_INFO is not successfull");
                    return false;
                }
                if (getter2.getActualType() != XA_DT_SM_STATE_INFO.getAtom()
                    || getter2.getActualFormat() != 32)
                {
                    log.finer("Wrong format of _DT_SM_STATE_INFO");
                    return false;
                }

                return true;
            } finally {
                getter2.dispose();
            }
        } finally {
            getter.dispose();
        }
    }

    /*
     * Is MWM running?  (Note that CDE will test positive as well).
     *
     * Check for _MOTIF_WM_INFO(_MOTIF_WM_INFO) on root.  Take the
     * second element of the property and check for presence of
     * _DT_SM_STATE_INFO(_DT_SM_STATE_INFO) on that window.
     */
    static final XAtom XA_MOTIF_WM_INFO = new XAtom("_MOTIF_WM_INFO", false);
    static final XAtom XA_DT_WORKSPACE_CURRENT = new XAtom("_DT_WORKSPACE_CURRENT", false);
    static boolean isMotif() {

        if (!(XA_MOTIF_WM_INFO.isInterned()/* && XA_DT_WORKSPACE_CURRENT.isInterned()*/) ) {
            return false;
        }

        WindowPropertyGetter getter =
            new WindowPropertyGetter(XToolkit.getDefaultRootWindow(),
                                     XA_MOTIF_WM_INFO, 0,
                                     MWMConstants.PROP_MOTIF_WM_INFO_ELEMENTS,
                                     false, XA_MOTIF_WM_INFO);
        try {
            int status = getter.execute();

            if (status != XConstants.Success || getter.getData() == 0) {
                return false;
            }

            if (getter.getActualType() != XA_MOTIF_WM_INFO.getAtom()
                || getter.getActualFormat() != 32
                || getter.getNumberOfItems() != MWMConstants.PROP_MOTIF_WM_INFO_ELEMENTS
                || getter.getBytesAfter() != 0)
            {
                return false;
            }

            long wmwin = Native.getLong(getter.getData(), 1);
            if (wmwin != 0) {
                if (XA_DT_WORKSPACE_CURRENT.isInterned()) {
                    /* Now check that this window has _DT_WORKSPACE_CURRENT */
                    XAtom[] curws = XA_DT_WORKSPACE_CURRENT.getAtomListProperty(wmwin);
                    if (curws.length == 0) {
                        return false;
                    }
                    return true;
                } else {
                    // No DT_WORKSPACE, however in our tests MWM sometimes can be without desktop -
                    // and that is still MWM.  So simply check for the validity of this window
                    // (through WM_STATE property).
                    WindowPropertyGetter state_getter =
                        new WindowPropertyGetter(wmwin,
                                                 XA_WM_STATE,
                                                 0, 1, false,
                                                 XA_WM_STATE);
                    try {
                        if (state_getter.execute() == XConstants.Success &&
                            state_getter.getData() != 0 &&
                            state_getter.getActualType() == XA_WM_STATE.getAtom())
                        {
                            return true;
                        }
                    } finally {
                        state_getter.dispose();
                    }
                }
            }
        } finally {
            getter.dispose();
        }
        return false;
    }

    /*
     * Is Sawfish running?
     */
    static boolean isSawfish() {
        return isNetWMName("Sawfish");
    }

    /*
     * Is KDE2 (KWin) running?
     */
    static boolean isKDE2() {
        return isNetWMName("KWin");
    }

    static boolean isCompiz() {
        return isNetWMName("compiz");
    }

    static boolean isUnityCompiz() {
        return isNetWMName("Compiz");
    }

    static boolean isLookingGlass() {
        return isNetWMName("LG3D");
    }

    static boolean isCWM() {
        return isNetWMName("CWM");
    }

    /*
     * Is Metacity running?
     */
    static boolean isMetacity() {
        return isNetWMName("Metacity");
//         || (
//             XA_NET_SUPPORTING_WM_CHECK.
//             getIntProperty(XToolkit.getDefaultRootWindow(), XA_NET_SUPPORTING_WM_CHECK.
//                            getIntProperty(XToolkit.getDefaultRootWindow(), XAtom.XA_CARDINAL)) == 0);
    }

    static boolean isMutter() {
        return isNetWMName("Mutter") || isNetWMName("GNOME Shell");
    }

    static int awtWMNonReparenting = -1;
    static boolean isNonReparentingWM() {
        if (awtWMNonReparenting == -1) {
            awtWMNonReparenting = (XToolkit.getEnv("_JAVA_AWT_WM_NONREPARENTING") != null) ? 1 : 0;
        }
        return (awtWMNonReparenting == 1 || XWM.getWMID() == XWM.COMPIZ_WM
                || XWM.getWMID() == XWM.LG3D_WM || XWM.getWMID() == XWM.CWM_WM);
    }

    /*
     * Prepare IceWM check.
     *
     * The only way to detect IceWM, seems to be by setting
     * _ICEWM_WINOPTHINT(_ICEWM_WINOPTHINT/8) on root and checking if it
     * was immediately deleted by IceWM.
     *
     * But messing with PropertyNotify here is way too much trouble, so
     * approximate the check by setting the property in this function and
     * checking if it still exists later on.
     *
     * Gaa, dirty dances...
     */
    static final XAtom XA_ICEWM_WINOPTHINT = new XAtom("_ICEWM_WINOPTHINT", false);
    static final char[] opt = {
        'A','W','T','_','I','C','E','W','M','_','T','E','S','T','\0',
        'a','l','l','W','o','r','k','s','p','a','c','e','s','\0',
        '0','\0'
    };
    static boolean prepareIsIceWM() {
        /*
         * Choose something innocuous: "AWT_ICEWM_TEST allWorkspaces 0".
         * IceWM expects "class\0option\0arg\0" with zero bytes as delimiters.
         */

        if (!XA_ICEWM_WINOPTHINT.isInterned()) {
            if (log.isLoggable(PlatformLogger.Level.FINER)) {
                log.finer("{0} is not interned", XA_ICEWM_WINOPTHINT);
            }
            return false;
        }

        XToolkit.awtLock();
        try {
            XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.VerifyChangePropertyHandler.getInstance());
            XlibWrapper.XChangePropertyS(XToolkit.getDisplay(), XToolkit.getDefaultRootWindow(),
                                         XA_ICEWM_WINOPTHINT.getAtom(),
                                         XA_ICEWM_WINOPTHINT.getAtom(),
                                         8, XConstants.PropModeReplace,
                                         new String(opt));
            XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

            if ((XErrorHandlerUtil.saved_error != null) &&
                (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success)) {
                log.finer("Erorr getting XA_ICEWM_WINOPTHINT property");
                return false;
            }
            log.finer("Prepared for IceWM detection");
            return true;
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /*
     * Is IceWM running?
     *
     * Note well: Only call this if awt_wm_prepareIsIceWM succeeded, or a
     * false positive will be reported.
     */
    static boolean isIceWM() {
        if (!XA_ICEWM_WINOPTHINT.isInterned()) {
            if (log.isLoggable(PlatformLogger.Level.FINER)) {
                log.finer("{0} is not interned", XA_ICEWM_WINOPTHINT);
            }
            return false;
        }

        WindowPropertyGetter getter =
            new WindowPropertyGetter(XToolkit.getDefaultRootWindow(),
                                     XA_ICEWM_WINOPTHINT, 0, 0xFFFF,
                                     true, XA_ICEWM_WINOPTHINT);
        try {
            int status = getter.execute();
            boolean res = (status == XConstants.Success && getter.getActualType() != 0);
            if (log.isLoggable(PlatformLogger.Level.FINER)) {
                log.finer("Status getting XA_ICEWM_WINOPTHINT: " + !res);
            }
            return !res || isNetWMName("IceWM");
        } finally {
            getter.dispose();
        }
    }

    /*
     * Is OpenLook WM running?
     *
     * This one is pretty lame, but the only property peculiar to OLWM is
     * _SUN_WM_PROTOCOLS(ATOM[]).  Fortunately, olwm deletes it on exit.
     */
    static final XAtom XA_SUN_WM_PROTOCOLS = new XAtom("_SUN_WM_PROTOCOLS", false);
    static boolean isOpenLook() {
        if (!XA_SUN_WM_PROTOCOLS.isInterned()) {
            return false;
        }

        XAtom[] list = XA_SUN_WM_PROTOCOLS.getAtomListProperty(XToolkit.getDefaultRootWindow());
        return (list.length != 0);
    }

    /*
     * Temporary error handler that checks if selecting for
     * SubstructureRedirect failed.
     */
    private static boolean winmgr_running = false;
    private static XErrorHandler detectWMHandler = new XErrorHandler.XBaseErrorHandler() {
        @Override
        public int handleError(long display, XErrorEvent err) {
            if ((err.get_request_code() == XProtocolConstants.X_ChangeWindowAttributes) &&
                (err.get_error_code() == XConstants.BadAccess))
            {
                winmgr_running = true;
                return 0;
            }
            return super.handleError(display, err);
        }
    };

    /*
     * Make an educated guess about running window manager.
     * XXX: ideally, we should detect wm restart.
     */
    static int awt_wmgr = XWM.UNDETERMINED_WM;
    static XWM wm;
    static XWM getWM() {
        if (wm == null) {
            wm = new XWM(awt_wmgr = getWMID()/*XWM.OTHER_WM*/);
        }
        return wm;
    }
    static int getWMID() {
        if (insLog.isLoggable(PlatformLogger.Level.FINEST)) {
            insLog.finest("awt_wmgr = " + awt_wmgr);
        }
        /*
         * Ideally, we should support cases when a different WM is started
         * during a Java app lifetime.
         */

        if (awt_wmgr != XWM.UNDETERMINED_WM) {
            return awt_wmgr;
        }

        XSetWindowAttributes substruct = new XSetWindowAttributes();
        XToolkit.awtLock();
        try {
            if (isNoWM()) {
                awt_wmgr = XWM.NO_WM;
                return awt_wmgr;
            }

            // Initialize _NET protocol - used to detect Window Manager.
            // Later, WM will initialize its own version of protocol
            XNETProtocol l_net_protocol = g_net_protocol = new XNETProtocol();
            l_net_protocol.detect();
            if (log.isLoggable(PlatformLogger.Level.FINE) && l_net_protocol.active()) {
                log.fine("_NET_WM_NAME is " + l_net_protocol.getWMName());
            }
            XWINProtocol win = g_win_protocol = new XWINProtocol();
            win.detect();

            /* actual check for IceWM to follow below */
            boolean doIsIceWM = prepareIsIceWM(); /* and let IceWM to act */

            /*
             * Ok, some WM is out there.  Check which one by testing for
             * "distinguishing" atoms.
             */
            if (isEnlightenment()) {
                awt_wmgr = XWM.ENLIGHTEN_WM;
            } else if (isMetacity()) {
                awt_wmgr = XWM.METACITY_WM;
            } else if (isMutter()) {
                awt_wmgr = XWM.MUTTER_WM;
            } else if (isSawfish()) {
                awt_wmgr = XWM.SAWFISH_WM;
            } else if (isKDE2()) {
                awt_wmgr =XWM.KDE2_WM;
            } else if (isCompiz()) {
                awt_wmgr = XWM.COMPIZ_WM;
            } else if (isLookingGlass()) {
                awt_wmgr = LG3D_WM;
            } else if (isCWM()) {
                awt_wmgr = CWM_WM;
            } else if (doIsIceWM && isIceWM()) {
                awt_wmgr = XWM.ICE_WM;
            } else if (isUnityCompiz()) {
                awt_wmgr = XWM.UNITY_COMPIZ_WM;
            }
            /*
             * We don't check for legacy WM when we already know that WM
             * supports WIN or _NET wm spec.
             */
            else if (l_net_protocol.active()) {
                awt_wmgr = XWM.OTHER_WM;
            } else if (win.active()) {
                awt_wmgr = XWM.OTHER_WM;
            }
            /*
             * Check for legacy WMs.
             */
            else if (isCDE()) { /* XXX: must come before isMotif */
                awt_wmgr = XWM.CDE_WM;
            } else if (isMotif()) {
                awt_wmgr = XWM.MOTIF_WM;
            } else if (isOpenLook()) {
                awt_wmgr = XWM.OPENLOOK_WM;
            } else {
                awt_wmgr = XWM.OTHER_WM;
            }

            return awt_wmgr;
        } finally {
            XToolkit.awtUnlock();
            substruct.dispose();
        }
    }


/*****************************************************************************\
 *
 * Size and decoration hints ...
 *
\*****************************************************************************/


    /*
     * Remove size hints specified by the mask.
     * XXX: Why do we need this in the first place???
     */
    static void removeSizeHints(XDecoratedPeer window, long mask) {
        mask &= XUtilConstants.PMaxSize | XUtilConstants.PMinSize;

        XToolkit.awtLock();
        try {
            XSizeHints hints = window.getHints();
            if ((hints.get_flags() & mask) == 0) {
                return;
            }

            hints.set_flags(hints.get_flags() & ~mask);
            if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
                insLog.finer("Setting hints, flags " + XlibWrapper.hintsToString(hints.get_flags()));
            }
            XlibWrapper.XSetWMNormalHints(XToolkit.getDisplay(),
                                          window.getWindow(),
                                          hints.pData);
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /*
     * If MWM_DECOR_ALL bit is set, then the rest of the bit-mask is taken
     * to be subtracted from the decorations.  Normalize decoration spec
     * so that we can map motif decor to something else bit-by-bit in the
     * rest of the code.
     */
    static int normalizeMotifDecor(int decorations) {
        if ((decorations & MWMConstants.MWM_DECOR_ALL) == 0) {
            return decorations;
        }
        int d = MWMConstants.MWM_DECOR_BORDER | MWMConstants.MWM_DECOR_RESIZEH
            | MWMConstants.MWM_DECOR_TITLE
            | MWMConstants.MWM_DECOR_MENU | MWMConstants.MWM_DECOR_MINIMIZE
            | MWMConstants.MWM_DECOR_MAXIMIZE;
        d &= ~decorations;
        return d;
    }

    /*
     * If MWM_FUNC_ALL bit is set, then the rest of the bit-mask is taken
     * to be subtracted from the functions.  Normalize function spec
     * so that we can map motif func to something else bit-by-bit in the
     * rest of the code.
     */
    static int normalizeMotifFunc(int functions) {
        if ((functions & MWMConstants.MWM_FUNC_ALL) == 0) {
            return functions;
        }
        int f = MWMConstants.MWM_FUNC_RESIZE |
                MWMConstants.MWM_FUNC_MOVE |
                MWMConstants.MWM_FUNC_MAXIMIZE |
                MWMConstants.MWM_FUNC_MINIMIZE |
                MWMConstants.MWM_FUNC_CLOSE;
        f &= ~functions;
        return f;
    }

    /*
     * Infer OL properties from MWM decorations.
     * Use _OL_DECOR_DEL(ATOM[]) to remove unwanted ones.
     */
    static void setOLDecor(XWindow window, boolean resizable, int decorations) {
        if (window == null) {
            return;
        }

        XAtomList decorDel = new XAtomList();
        decorations = normalizeMotifDecor(decorations);
        if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
            insLog.finer("Setting OL_DECOR to " + Integer.toBinaryString(decorations));
        }
        if ((decorations & MWMConstants.MWM_DECOR_TITLE) == 0) {
            decorDel.add(XA_OL_DECOR_HEADER);
        }
        if ((decorations & (MWMConstants.MWM_DECOR_RESIZEH | MWMConstants.MWM_DECOR_MAXIMIZE)) == 0) {
            decorDel.add(XA_OL_DECOR_RESIZE);
        }
        if ((decorations & (MWMConstants.MWM_DECOR_MENU |
                            MWMConstants.MWM_DECOR_MAXIMIZE |
                            MWMConstants.MWM_DECOR_MINIMIZE)) == 0)
        {
            decorDel.add(XA_OL_DECOR_CLOSE);
        }
        if (decorDel.size() == 0) {
            insLog.finer("Deleting OL_DECOR");
            XA_OL_DECOR_DEL.DeleteProperty(window);
        } else {
            if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
                insLog.finer("Setting OL_DECOR to " + decorDel);
            }
            XA_OL_DECOR_DEL.setAtomListProperty(window, decorDel);
        }
    }

    /*
     * Set MWM decorations.  Set MWM functions depending on resizability.
     */
    static void setMotifDecor(XWindow window, boolean resizable, int decorations, int functions) {
        /* Apparently some WMs don't implement MWM_*_ALL semantic correctly */
        if ((decorations & MWMConstants.MWM_DECOR_ALL) != 0
            && (decorations != MWMConstants.MWM_DECOR_ALL))
        {
            decorations = normalizeMotifDecor(decorations);
        }
        if ((functions & MWMConstants.MWM_FUNC_ALL) != 0
            && (functions != MWMConstants.MWM_FUNC_ALL))
        {
            functions = normalizeMotifFunc(functions);
        }

        PropMwmHints hints = window.getMWMHints();
        hints.set_flags(hints.get_flags() |
                        MWMConstants.MWM_HINTS_FUNCTIONS |
                        MWMConstants.MWM_HINTS_DECORATIONS);
        hints.set_functions(functions);
        hints.set_decorations(decorations);

        if (stateLog.isLoggable(PlatformLogger.Level.FINER)) {
            stateLog.finer("Setting MWM_HINTS to " + hints);
        }
        window.setMWMHints(hints);
    }

    /*
     * Under some window managers if shell is already mapped, we MUST
     * unmap and later remap in order to effect the changes we make in the
     * window manager decorations.
     *
     * N.B.  This unmapping / remapping of the shell exposes a bug in
     * X/Motif or the Motif Window Manager.  When you attempt to map a
     * widget which is positioned (partially) off-screen, the window is
     * relocated to be entirely on screen. Good idea.  But if both the x
     * and the y coordinates are less than the origin (0,0), the first
     * (re)map will move the window to the origin, and any subsequent
     * (re)map will relocate the window at some other point on the screen.
     * I have written a short Motif test program to discover this bug.
     * This should occur infrequently and it does not cause any real
     * problem.  So for now we'll let it be.
     */
    static boolean needRemap(XDecoratedPeer window) {
        // Don't remap EmbeddedFrame,
        // e.g. for TrayIcon it causes problems.
        return !window.isEmbedded();
    }

    /*
     * Set decoration hints on the shell to wdata->decor adjusted
     * appropriately if not resizable.
     */
    static void setShellDecor(XDecoratedPeer window) {
        int decorations = window.getDecorations();
        int functions = window.getFunctions();
        boolean resizable = window.isResizable();

        if (!resizable) {
            if ((decorations & MWMConstants.MWM_DECOR_ALL) != 0) {
                decorations |= MWMConstants.MWM_DECOR_RESIZEH | MWMConstants.MWM_DECOR_MAXIMIZE;
            } else {
                decorations &= ~(MWMConstants.MWM_DECOR_RESIZEH | MWMConstants.MWM_DECOR_MAXIMIZE);
            }
        }
        setMotifDecor(window, resizable, decorations, functions);
        setOLDecor(window, resizable, decorations);

        /* Some WMs need remap to redecorate the window */
        if (window.isShowing() && needRemap(window)) {
            /*
             * Do the re/mapping at the Xlib level.  Since we essentially
             * work around a WM bug we don't want this hack to be exposed
             * to Intrinsics (i.e. don't mess with grabs, callbacks etc).
             */
            window.xSetVisible(false);
            XToolkit.XSync();
            window.xSetVisible(true);
        }
    }

    /*
     * Make specified shell resizable.
     */
    static void setShellResizable(XDecoratedPeer window) {
        if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
            insLog.fine("Setting shell resizable " + window);
        }
        XToolkit.awtLock();
        try {
            Rectangle shellBounds;
            if (getWMID() != UNITY_COMPIZ_WM) {
                shellBounds = window.getShellBounds();
                shellBounds.translate(-window.currentInsets.left,
                                      -window.currentInsets.top);
            } else {
                shellBounds = window.getDimensions().getScreenBounds();
            }
            window.updateSizeHints(window.getDimensions());
            requestWMExtents(window.getWindow());
            XlibWrapper.XMoveResizeWindow(XToolkit.getDisplay(),
                                          window.getShell(),
                                          window.scaleUp(shellBounds.x),
                                          window.scaleUp(shellBounds.y),
                                          window.scaleUp(shellBounds.width),
                                          window.scaleUp(shellBounds.height));
            /* REMINDER: will need to revisit when setExtendedStateBounds is added */
            //Fix for 4320050: Minimum size for java.awt.Frame is not being enforced.
            //We need to update frame's minimum size, not to reset it
            removeSizeHints(window, XUtilConstants.PMaxSize);
            window.updateMinimumSize();

            /* Restore decorations */
            setShellDecor(window);
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /*
     * Make specified shell non-resizable.
     * If justChangeSize is false, update decorations as well.
     * @param shellBounds bounds of the shell window
     */
    static void setShellNotResizable(XDecoratedPeer window, WindowDimensions newDimensions, Rectangle shellBounds,
                                     boolean justChangeSize)
    {
        if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
            insLog.fine("Setting non-resizable shell " + window + ", dimensions " + newDimensions +
                        ", shellBounds " + shellBounds +", just change size: " + justChangeSize);
        }
        XToolkit.awtLock();
        try {
            /* Fix min/max size hints at the specified values */
            if (!shellBounds.isEmpty()) {
                window.updateSizeHints(newDimensions);
                requestWMExtents(window.getWindow());
                XToolkit.XSync();
                XlibWrapper.XMoveResizeWindow(XToolkit.getDisplay(),
                                              window.getShell(),
                                              window.scaleUp(shellBounds.x),
                                              window.scaleUp(shellBounds.y),
                                              window.scaleUp(shellBounds.width),
                                              window.scaleUp(shellBounds.height));
            }
            if (!justChangeSize) {  /* update decorations */
                setShellDecor(window);
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

/*****************************************************************************\
 * Protocols support
 */
    private HashMap<Class<?>, Collection<?>> protocolsMap = new HashMap<Class<?>, Collection<?>>();
    /**
     * Returns all protocols supporting given protocol interface
     */
    <T> Collection<T> getProtocols(Class<T> protocolInterface) {
        @SuppressWarnings("unchecked")
        Collection<T> res = (Collection<T>) protocolsMap.get(protocolInterface);
        if (res != null) {
            return res;
        } else {
            return new LinkedList<T>();
        }
    }

    private <T> void addProtocol(Class<T> protocolInterface, T protocol) {
        Collection<T> protocols = getProtocols(protocolInterface);
        protocols.add(protocol);
        protocolsMap.put(protocolInterface, protocols);
    }

    boolean supportsDynamicLayout() {
        int wm = getWMID();
        switch (wm) {
          case XWM.ENLIGHTEN_WM:
          case XWM.KDE2_WM:
          case XWM.SAWFISH_WM:
          case XWM.ICE_WM:
          case XWM.METACITY_WM:
              return true;
          case XWM.OPENLOOK_WM:
          case XWM.MOTIF_WM:
          case XWM.CDE_WM:
              return false;
          default:
              return false;
        }
    }


    /**
     * Check if state is supported.
     * Note that a compound state is always reported as not supported.
     * Note also that MAXIMIZED_BOTH is considered not a compound state.
     * Therefore, a compound state is just ICONIFIED | anything else.
     *
     */
    @SuppressWarnings("fallthrough")
    boolean supportsExtendedState(int state) {
        switch (state) {
          case Frame.MAXIMIZED_VERT:
          case Frame.MAXIMIZED_HORIZ:
              /*
               * WMs that talk NET/WIN protocol, but do not support
               * unidirectional maximization.
               */
              if (getWMID() == METACITY_WM) {
                  /* "This is a deliberate policy decision." -hp */
                  return false;
              }
              /* FALLTROUGH */
          case Frame.MAXIMIZED_BOTH:
              for (XStateProtocol proto : getProtocols(XStateProtocol.class)) {
                  if (proto.supportsState(state)) {
                      return true;
                  }
              }
              /* FALLTROUGH */
          default:
              return false;
        }
    }

/*****************************************************************************\
 *
 * Reading state from different protocols
 *
\*****************************************************************************/


    int getExtendedState(XWindowPeer window) {
        int state = 0;
        for (XStateProtocol proto : getProtocols(XStateProtocol.class)) {
            state |= proto.getState(window);
        }
        if (state != 0) {
            return state;
        } else {
            return Frame.NORMAL;
        }
    }

/*****************************************************************************\
 *
 * Notice window state change when WM changes a property on the window ...
 *
\*****************************************************************************/


    /*
     * Check if property change is a window state protocol message.
     */
    boolean isStateChange(XDecoratedPeer window, XPropertyEvent e) {
        if (!window.isShowing()) {
            stateLog.finer("Window is not showing");
            return false;
        }

        int wm_state = window.getWMState();
        if (wm_state == XUtilConstants.WithdrawnState) {
            stateLog.finer("WithdrawnState");
            return false;
        } else {
            if (stateLog.isLoggable(PlatformLogger.Level.FINER)) {
                stateLog.finer("Window WM_STATE is " + wm_state);
            }
        }
        boolean is_state_change = false;
        if (e.get_atom() == XA_WM_STATE.getAtom()) {
            is_state_change = true;
        }

        for (XStateProtocol proto : getProtocols(XStateProtocol.class)) {
            is_state_change |= proto.isStateChange(e);
            if (stateLog.isLoggable(PlatformLogger.Level.FINEST)) {
                stateLog.finest(proto + ": is state changed = " + is_state_change);
            }
        }
        return is_state_change;
    }

    /*
     * Returns current state (including extended) of a given window.
     */
    int getState(XDecoratedPeer window) {
        int res = 0;
        final int wm_state = window.getWMState();
        if (wm_state == XUtilConstants.IconicState) {
            res = Frame.ICONIFIED;
        } else {
            res = Frame.NORMAL;
        }
        res |= getExtendedState(window);
        return res;
    }

/*****************************************************************************\
 *
 * Setting/changing window state ...
 *
\*****************************************************************************/

    /**
     * Moves window to the specified layer, layer is one of the constants defined
     * in XLayerProtocol
     */
    void setLayer(XWindowPeer window, int layer) {
        for (XLayerProtocol proto : getProtocols(XLayerProtocol.class)) {
            if (proto.supportsLayer(layer)) {
                proto.setLayer(window, layer);
            }
        }
        XToolkit.XSync();
    }

    void setExtendedState(XWindowPeer window, int state) {
        for (XStateProtocol proto : getProtocols(XStateProtocol.class)) {
            if (proto.supportsState(state)) {
                proto.setState(window, state);
                break;
            }
        }

        if (!window.isShowing()) {
            /*
             * Purge KWM bits.
             * Not really tested with KWM, only with WindowMaker.
             */
            XToolkit.awtLock();
            try {
                XlibWrapper.XDeleteProperty(XToolkit.getDisplay(),
                                            window.getWindow(),
                                            XA_KWM_WIN_ICONIFIED.getAtom());
                XlibWrapper.XDeleteProperty(XToolkit.getDisplay(),
                                            window.getWindow(),
                                            XA_KWM_WIN_MAXIMIZED.getAtom());
            }
            finally {
                XToolkit.awtUnlock();
            }
        }
        XToolkit.XSync();
    }


    /*
     * Work around for 4775545.
     *
     * If WM exits while the top-level is shaded, the shaded hint remains
     * on the top-level properties.  When WM restarts and sees the shaded
     * window it can reparent it into a "pre-shaded" decoration frame
     * (Metacity does), and our insets logic will go crazy, b/c it will
     * see a huge nagative bottom inset.  There's no clean solution for
     * this, so let's just be weasels and drop the shaded hint if we
     * detect that WM exited.  NB: we are in for a race condition with WM
     * restart here.  NB2: e.g. WindowMaker saves the state in a private
     * property that this code knows nothing about, so this workaround is
     * not effective; other WMs might play similar tricks.
     */
    void unshadeKludge(XDecoratedPeer window) {
        assert(window.isShowing());

        for (XStateProtocol proto : getProtocols(XStateProtocol.class)) {
            proto.unshadeKludge(window);
        }
        XToolkit.XSync();
    }

    static boolean inited = false;
    static void init() {
        if (inited) {
            return;
        }

        initAtoms();
        getWM();
        inited = true;
    }

    void initializeProtocols() {
        XNETProtocol net_protocol = g_net_protocol;
        if (net_protocol != null) {
            if (!net_protocol.active()) {
                net_protocol = null;
            } else {
                if (net_protocol.doStateProtocol()) {
                    addProtocol(XStateProtocol.class, net_protocol);
                }
                if (net_protocol.doLayerProtocol()) {
                    addProtocol(XLayerProtocol.class, net_protocol);
                }
            }
        }

        XWINProtocol win = g_win_protocol;
        if (win != null) {
            if (win.active()) {
                if (win.doStateProtocol()) {
                    addProtocol(XStateProtocol.class, win);
                }
                if (win.doLayerProtocol()) {
                    addProtocol(XLayerProtocol.class, win);
                }
            }
        }
    }

    HashMap<Class<?>, Insets> storedInsets = new HashMap<>();
    Insets guessInsets(XDecoratedPeer window) {
        Insets res = storedInsets.get(window.getClass());
        if (res == null) {
            switch (WMID) {
              case ENLIGHTEN_WM:
                  res = new Insets(19, 4, 4, 4);
                  break;
              case CDE_WM:
                  res = new Insets(28, 6, 6, 6);
                  break;
              case NO_WM:
              case LG3D_WM:
                  res = zeroInsets;
                  break;
              case UNITY_COMPIZ_WM:
                  res = new Insets(28, 1, 1, 1);
                  break;
              case MOTIF_WM:
              case OPENLOOK_WM:
              default:
                  res = defaultInsets;
            }
        }
        if (insLog.isLoggable(PlatformLogger.Level.FINEST)) {
            insLog.finest("WM guessed insets: " + res);
        }
        return res;
    }
    /*
     * Some buggy WMs ignore window gravity when processing
     * ConfigureRequest and position window as if the gravity is Static.
     * We work around this in MWindowPeer.pReshape().
     *
     * Starting with 1.5 we have introduced an Environment variable
     * _JAVA_AWT_WM_STATIC_GRAVITY that can be set to indicate to Java
     * explicitly that the WM has this behaviour, example is FVWM.
     */

    static int awtWMStaticGravity = -1;
    static boolean configureGravityBuggy() {

        if (awtWMStaticGravity == -1) {
            awtWMStaticGravity = (XToolkit.getEnv("_JAVA_AWT_WM_STATIC_GRAVITY") != null) ? 1 : 0;
        }

        if (awtWMStaticGravity == 1) {
            return true;
        }

        switch(getWMID()) {
          case XWM.ICE_WM:
              /*
               * See bug #228981 at IceWM's SourceForge pages.
               * Latest stable version 1.0.8-6 still has this problem.
               */
              /**
               * Version 1.2.2 doesn't have this problem
               */
              // Detect IceWM version
              if (g_net_protocol != null) {
                  String wm_name = g_net_protocol.getWMName();
                  Pattern pat = Pattern.compile("^IceWM (\\d+)\\.(\\d+)\\.(\\d+).*$");
                  try {
                      Matcher match = pat.matcher(wm_name);
                      if (match.matches()) {
                          int v1 = Integer.parseInt(match.group(1));
                          int v2 = Integer.parseInt(match.group(2));
                          int v3 = Integer.parseInt(match.group(3));
                          return !(v1 > 1 || (v1 == 1 && (v2 > 2 || (v2 == 2 && v3 >=2))));
                      }
                  } catch (Exception e) {
                      return true;
                  }
              }
              return true;
          case XWM.ENLIGHTEN_WM:
              /* At least E16 is buggy. */
              return true;
          default:
              return false;
        }
    }

    /*
     * @return if WM implements the insets property - returns insets with values
     * specified in that property, null otherwise.
     */
    public static Insets getInsetsFromExtents(long window) {
        if (window == XConstants.None) {
            return null;
        }
        XNETProtocol net_protocol = getWM().getNETProtocol();
        if (net_protocol != null && net_protocol.active()) {
            Insets insets = getInsetsFromProp(window, XA_NET_FRAME_EXTENTS);
            if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
                insLog.fine("_NET_FRAME_EXTENTS: {0}", insets);
            }

            if (insets != null) {
                return insets;
            }
        }
        switch(getWMID()) {
          case XWM.KDE2_WM:
              return getInsetsFromProp(window, XA_KDE_NET_WM_FRAME_STRUT);
          case XWM.ENLIGHTEN_WM:
              return getInsetsFromProp(window, XA_E_FRAME_SIZE);
          default:
              return null;
        }
    }

    /**
     * Helper function reads property of type CARDINAL[4] = { left, right, top, bottom }
     * and converts it to Insets object.
     */
    public static Insets getInsetsFromProp(long window, XAtom atom) {
        if (window == XConstants.None) {
            return null;
        }

        WindowPropertyGetter getter =
            new WindowPropertyGetter(window, atom,
                                     0, 4, false, XAtom.XA_CARDINAL);
        try {
            if (getter.execute() != XConstants.Success
                || getter.getData() == 0
                || getter.getActualType() != XAtom.XA_CARDINAL
                || getter.getActualFormat() != 32)
            {
                return null;
            } else {
                return new Insets((int)Native.getCard32(getter.getData(), 2), // top
                                  (int)Native.getCard32(getter.getData(), 0), // left
                                  (int)Native.getCard32(getter.getData(), 3), // bottom
                                  (int)Native.getCard32(getter.getData(), 1)); // right
            }
        } finally {
            getter.dispose();
        }
    }

    /**
     * Asks WM to fill Frame Extents (insets) for the window.
     */
    public static void requestWMExtents(long window) {
        if (window == XConstants.None) { // not initialized
            return;
        }

        log.fine("Requesting FRAME_EXTENTS");

        XClientMessageEvent msg = new XClientMessageEvent();
        msg.zero();
        msg.set_type(XConstants.ClientMessage);
        msg.set_display(XToolkit.getDisplay());
        msg.set_window(window);
        msg.set_format(32);
        XToolkit.awtLock();
        try {
            XNETProtocol net_protocol = getWM().getNETProtocol();
            if (net_protocol != null && net_protocol.active()) {
                msg.set_message_type(XA_NET_REQUEST_FRAME_EXTENTS.getAtom());
                XlibWrapper.XSendEvent(XToolkit.getDisplay(), XToolkit.getDefaultRootWindow(),
                                       false,
                                       XConstants.SubstructureRedirectMask | XConstants.SubstructureNotifyMask,
                                       msg.getPData());
            }
            if (getWMID() == XWM.KDE2_WM) {
                msg.set_message_type(XA_KDE_NET_WM_FRAME_STRUT.getAtom());
                XlibWrapper.XSendEvent(XToolkit.getDisplay(), XToolkit.getDefaultRootWindow(),
                                       false,
                                       XConstants.SubstructureRedirectMask | XConstants.SubstructureNotifyMask,
                                       msg.getPData());
            }
            // XXX: should we wait for response? XIfEvent() would be useful here :)
        } finally {
            XToolkit.awtUnlock();
            msg.dispose();
        }
    }

    /* syncTopLEvelPos() is necessary to insure that the window manager has in
     * fact moved us to our final position relative to the reParented WM window.
     * We have noted a timing window which our shell has not been moved so we
     * screw up the insets thinking they are 0,0.  Wait (for a limited period of
     * time to let the WM hava a chance to move us.
     * @param window window ID of the shell, assuming it is the window
     * which will NOT have zero coordinates after the complete
     * reparenting
     */
    boolean syncTopLevelPos(long window, XWindowAttributes attrs) {
        int tries = 0;
        XToolkit.awtLock();
        try {
            do {
                XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(), window, attrs.pData);
                if (attrs.get_x() != 0 || attrs.get_y() != 0) {
                    return true;
                }
                tries++;
                XToolkit.XSync();
            } while (tries < 50);
        }
        finally {
            XToolkit.awtUnlock();
        }
        return false;
    }

    Insets getInsets(XDecoratedPeer win, long window, long parent) {
        /*
         * Unfortunately the concept of "insets" borrowed to AWT
         * from Win32 is *absolutely*, *unbelievably* foreign to
         * X11.  Few WMs provide the size of frame decor
         * (i.e. insets) in a property they set on the client
         * window, so we check if we can get away with just
         * peeking at it.  [Future versions of wm-spec might add a
         * standardized hint for this].
         *
         * Otherwise we do some special casing.  Actually the
         * fallback code ("default" case) seems to cover most of
         * the existing WMs (modulo Reparent/Configure order
         * perhaps?).
         *
         * Fallback code tries to account for the two most common cases:
         *
         * . single reparenting
         *       parent window is the WM frame
         *       [twm, olwm, sawfish]
         *
         * . double reparenting
         *       parent is a lining exactly the size of the client
         *       grandpa is the WM frame
         *       [mwm, e!, kwin, fvwm2 ... ]
         */
        Insets correctWM = XWM.getInsetsFromExtents(window);
        if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
            insLog.finer("Got insets from property: {0}", correctWM);
        }

        if (correctWM == null) {
            correctWM = new Insets(0,0,0,0);

            correctWM.top = -1;
            correctWM.left = -1;

            XWindowAttributes lwinAttr = new XWindowAttributes();
            XWindowAttributes pattr = new XWindowAttributes();
            try {
                switch (XWM.getWMID()) {
                  /* should've been done in awt_wm_getInsetsFromProp */
                  case XWM.ENLIGHTEN_WM: {
                      /* enlightenment does double reparenting */
                      syncTopLevelPos(parent, lwinAttr);
                      correctWM.left = lwinAttr.get_x();
                      correctWM.top = lwinAttr.get_y();
                      /*
                       * Now get the actual dimensions of the parent window
                       * resolve the difference.  We can't rely on the left
                       * to be equal to right or bottom...  Enlightment
                       * breaks that assumption.
                       */
                      XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(),
                                                       XlibUtil.getParentWindow(parent),
                                                       pattr.pData);
                      correctWM.right = pattr.get_width() -
                          (lwinAttr.get_width() + correctWM.left);
                      correctWM.bottom = pattr.get_height() -
                          (lwinAttr.get_height() + correctWM.top);

                      break;
                  }
                  case XWM.ICE_WM: // for 1.2.2.
                  case XWM.KDE2_WM: /* should've been done in getInsetsFromProp */
                  case XWM.CDE_WM:
                  case XWM.MOTIF_WM: {
                      /* these are double reparenting too */
                      if (syncTopLevelPos(parent, lwinAttr)) {
                          correctWM.top = lwinAttr.get_y();
                          correctWM.left = lwinAttr.get_x();
                          correctWM.right = correctWM.left;
                          correctWM.bottom = correctWM.left;
                      } else {
                          return null;
                      }
                      break;
                  }
                  case XWM.SAWFISH_WM:
                  case XWM.OPENLOOK_WM: {
                      /* single reparenting */
                      syncTopLevelPos(window, lwinAttr);
                      correctWM.top    = lwinAttr.get_y();
                      correctWM.left   = lwinAttr.get_x();
                      correctWM.right  = correctWM.left;
                      correctWM.bottom = correctWM.left;
                      break;
                  }
                  case XWM.OTHER_WM:
                  default: {                /* this is very similar to the E! case above */
                      if (insLog.isLoggable(PlatformLogger.Level.FINEST)) {
                          insLog.finest("Getting correct insets for OTHER_WM/default, parent: {0}", parent);
                      }
                      syncTopLevelPos(parent, lwinAttr);
                      int status = XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(),
                                                                    window, lwinAttr.pData);
                      status = XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(),
                                                                parent, pattr.pData);
                      if (lwinAttr.get_root() == parent) {
                          insLog.finest("our parent is root so insets should be zero");
                          correctWM = new Insets(0, 0, 0, 0);
                          break;
                      }

                      /*
                       * Check for double-reparenting WM.
                       *
                       * If the parent is exactly the same size as the
                       * top-level assume taht it's the "lining" window and
                       * that the grandparent is the actual frame (NB: we
                       * have already handled undecorated windows).
                       *
                       * XXX: what about timing issues that syncTopLevelPos
                       * is supposed to work around?
                       */
                      if (lwinAttr.get_x() == 0 && lwinAttr.get_y() == 0
                          && lwinAttr.get_width()+2*lwinAttr.get_border_width() == pattr.get_width()
                          && lwinAttr.get_height()+2*lwinAttr.get_border_width() == pattr.get_height())
                      {
                          if (insLog.isLoggable(PlatformLogger.Level.FINEST)) {
                              insLog.finest("Double reparenting detected, pattr({2})={0}, lwinAttr({3})={1}",
                                        lwinAttr, pattr, parent, window);
                          }
                          lwinAttr.set_x(pattr.get_x());
                          lwinAttr.set_y(pattr.get_y());
                          lwinAttr.set_border_width(lwinAttr.get_border_width()+pattr.get_border_width());

                          final long grand_parent = XlibUtil.getParentWindow(parent);

                          if (grand_parent == lwinAttr.get_root()) {
                              // This is not double-reparenting in a
                              // general sense - we simply don't have
                              // correct insets - return null to try to
                              // get insets later
                              return null;
                          } else {
                              parent = grand_parent;
                              XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(),
                                                               parent,
                                                               pattr.pData);
                          }
                      }
                      /*
                       * XXX: To be absolutely correct, we'd need to take
                       * parent's border-width into account too, but the
                       * rest of the code is happily unaware about border
                       * widths and inner/outer distinction, so for the time
                       * being, just ignore it.
                       */
                      if (insLog.isLoggable(PlatformLogger.Level.FINEST)) {
                          insLog.finest("Attrs before calculation: pattr({2})={0}, lwinAttr({3})={1}",
                                    lwinAttr, pattr, parent, window);
                      }
                      correctWM = new Insets(lwinAttr.get_y() + lwinAttr.get_border_width(),
                                             lwinAttr.get_x() + lwinAttr.get_border_width(),
                                             pattr.get_height() - (lwinAttr.get_y() + lwinAttr.get_height() + 2*lwinAttr.get_border_width()),
                                             pattr.get_width() -  (lwinAttr.get_x() + lwinAttr.get_width() + 2*lwinAttr.get_border_width()));
                      break;
                  } /* default */
                } /* switch (runningWM) */
            } finally {
                lwinAttr.dispose();
                pattr.dispose();
            }
        }

        correctWM.top = win.scaleUp(correctWM.top);
        correctWM.bottom = win.scaleUp(correctWM.bottom);
        correctWM.left = win.scaleUp(correctWM.left);
        correctWM.right = win.scaleUp(correctWM.right);

        if (storedInsets.get(win.getClass()) == null) {
            storedInsets.put(win.getClass(), correctWM);
        }
        return correctWM;
    }
    boolean isDesktopWindow( long w ) {
        if (g_net_protocol != null) {
            XAtomList wtype = XAtom.get("_NET_WM_WINDOW_TYPE").getAtomListPropertyList( w );
            return wtype.contains( XAtom.get("_NET_WM_WINDOW_TYPE_DESKTOP") );
        } else {
            return false;
        }
    }

    public XNETProtocol getNETProtocol() {
        return g_net_protocol;
    }

    /**
     * Sets _NET_WN_ICON property on the window using the arrays of
     * raster-data for icons. If icons is null, removes _NET_WM_ICON
     * property.
     * This method invokes XNETProtocol.setWMIcon() for WMs that
     * support NET protocol.
     *
     * @return true if hint was modified successfully, false otherwise
     */
    public boolean setNetWMIcon(XWindowPeer window, java.util.List<IconInfo> icons) {
        if (g_net_protocol != null && g_net_protocol.active()) {
            g_net_protocol.setWMIcons(window, icons);
            return getWMID() != ICE_WM;
        }
        return false;
    }
}
