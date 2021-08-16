/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.dnd;

import java.awt.Component;
import java.awt.Point;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import java.awt.dnd.DnDConstants;

import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.InvalidDnDOperationException;

import java.awt.dnd.peer.DropTargetContextPeer;

import java.util.HashSet;
import java.util.Map;
import java.util.Arrays;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.DropTargetContextAccessor;
import sun.util.logging.PlatformLogger;

import java.io.IOException;
import java.io.InputStream;

import sun.awt.AppContext;
import sun.awt.AWTPermissions;
import sun.awt.SunToolkit;
import sun.awt.datatransfer.DataTransferer;
import sun.awt.datatransfer.ToolkitThreadBlockedHandler;

/**
 * <p>
 * The SunDropTargetContextPeer class is the generic class responsible for handling
 * the interaction between a windowing systems DnD system and Java.
 * </p>
 *
 * @since 1.3.1
 *
 */

public abstract class SunDropTargetContextPeer implements DropTargetContextPeer, Transferable {

    /*
     * A boolean constant that requires the peer to wait until the
     * SunDropTargetEvent is processed and return the status back
     * to the native code.
     */
    public static final boolean DISPATCH_SYNC = true;
    private   DropTarget              currentDT;
    private   DropTargetContext       currentDTC;
    private   long[]                  currentT;
    private   int                     currentA;   // target actions
    private   int                     currentSA;  // source actions
    private   int                     currentDA;  // current drop action
    private   int                     previousDA;

    private   long                    nativeDragContext;

    private   Transferable            local;

    private boolean                   dragRejected = false;

    protected int                     dropStatus   = STATUS_NONE;
    protected boolean                 dropComplete = false;

    // The flag is used to monitor whether the drop action is
    // handled by a user. That allows to distinct during
    // which operation getTransferData() method is invoked.
    boolean                           dropInProcess = false;

    /*
     * global lock
     */

    protected static final Object _globalLock = new Object();

    private static final PlatformLogger dndLog = PlatformLogger.getLogger("sun.awt.dnd.SunDropTargetContextPeer");

    /*
     * a primitive mechanism for advertising intra-JVM Transferables
     */

    protected static Transferable         currentJVMLocalSourceTransferable = null;

    public static void setCurrentJVMLocalSourceTransferable(Transferable t) throws InvalidDnDOperationException {
        synchronized(_globalLock) {
            if (t != null && currentJVMLocalSourceTransferable != null) {
                    throw new InvalidDnDOperationException();
            } else {
                currentJVMLocalSourceTransferable = t;
            }
        }
    }

    /**
     * obtain the transferable iff the operation is in the same VM
     */

    private static Transferable getJVMLocalSourceTransferable() {
        return currentJVMLocalSourceTransferable;
    }

    /*
     * constants used by dropAccept() or dropReject()
     */

    protected static final int STATUS_NONE   =  0; // none pending
    protected static final int STATUS_WAIT   =  1; // drop pending
    protected static final int STATUS_ACCEPT =  2;
    protected static final int STATUS_REJECT = -1;

    /**
     * create the peer
     */

    public SunDropTargetContextPeer() {
        super();
    }

    /**
     * @return the DropTarget associated with this peer
     */

    public DropTarget getDropTarget() { return currentDT; }

    /**
     * @param actions set the current actions
     */

    public synchronized void setTargetActions(int actions) {
        currentA = actions &
            (DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK);
    }

    /**
     * @return the current target actions
     */

    public int getTargetActions() {
        return currentA;
    }

    /**
     * get the Transferable associated with the drop
     */

    public Transferable getTransferable() {
        return this;
    }

    /**
     * @return current DataFlavors available
     */
    // NOTE: This method may be called by privileged threads.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!

    public DataFlavor[] getTransferDataFlavors() {
        final Transferable    localTransferable = local;

        if (localTransferable != null) {
            return localTransferable.getTransferDataFlavors();
        } else {
            return DataTransferer.getInstance().getFlavorsForFormatsAsArray
                (currentT, DataTransferer.adaptFlavorMap
                    (currentDT.getFlavorMap()));
        }
    }

    /**
     * @return if the flavor is supported
     */

    public boolean isDataFlavorSupported(DataFlavor df) {
        Transferable localTransferable = local;

        if (localTransferable != null) {
            return localTransferable.isDataFlavorSupported(df);
        } else {
            return DataTransferer.getInstance().getFlavorsForFormats
                (currentT, DataTransferer.adaptFlavorMap
                    (currentDT.getFlavorMap())).
                containsKey(df);
        }
    }

    /**
     * @return the data
     */

    public Object getTransferData(DataFlavor df)
      throws UnsupportedFlavorException, IOException,
        InvalidDnDOperationException
    {

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        try {
            if (!dropInProcess && sm != null) {
                sm.checkPermission(AWTPermissions.ACCESS_CLIPBOARD_PERMISSION);
            }
        } catch (Exception e) {
            Thread currentThread = Thread.currentThread();
            currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, e);
            return null;
        }

        Long lFormat = null;
        Transferable localTransferable = local;

        if (localTransferable != null) {
            return localTransferable.getTransferData(df);
        }

        if (dropStatus != STATUS_ACCEPT || dropComplete) {
            throw new InvalidDnDOperationException("No drop current");
        }

        Map<DataFlavor, Long> flavorMap = DataTransferer.getInstance()
            .getFlavorsForFormats(currentT, DataTransferer.adaptFlavorMap
                (currentDT.getFlavorMap()));

        lFormat = flavorMap.get(df);
        if (lFormat == null) {
            throw new UnsupportedFlavorException(df);
        }

        if (df.isRepresentationClassRemote() &&
            currentDA != DnDConstants.ACTION_LINK) {
            throw new InvalidDnDOperationException("only ACTION_LINK is permissable for transfer of java.rmi.Remote objects");
        }

        final long format = lFormat.longValue();

        Object ret = getNativeData(format);

        if (ret instanceof byte[]) {
            try {
                return DataTransferer.getInstance().
                    translateBytes((byte[])ret, df, format, this);
            } catch (IOException e) {
                throw new InvalidDnDOperationException(e.getMessage());
            }
        } else if (ret instanceof InputStream) {
            try {
                return DataTransferer.getInstance().
                    translateStream((InputStream)ret, df, format, this);
            } catch (IOException e) {
                throw new InvalidDnDOperationException(e.getMessage());
            }
        } else {
            throw new IOException("no native data was transfered");
        }
    }

    protected abstract Object getNativeData(long format)
      throws IOException;

    /**
     * @return if the transfer is a local one
     */
    public boolean isTransferableJVMLocal() {
        return local != null || getJVMLocalSourceTransferable() != null;
    }

    private int handleEnterMessage(final Component component,
                                   final int x, final int y,
                                   final int dropAction,
                                   final int actions, final long[] formats,
                                   final long nativeCtxt) {
        return postDropTargetEvent(component, x, y, dropAction, actions,
                                   formats, nativeCtxt,
                                   SunDropTargetEvent.MOUSE_ENTERED,
                                   SunDropTargetContextPeer.DISPATCH_SYNC);
    }

    /**
     * actual processing on EventQueue Thread
     */

    protected void processEnterMessage(SunDropTargetEvent event) {
        Component  c    = (Component)event.getSource();
        DropTarget dt   = c.getDropTarget();
        Point      hots = event.getPoint();

        local = getJVMLocalSourceTransferable();
        DropTargetContextAccessor acc =
                AWTAccessor.getDropTargetContextAccessor();
        if (currentDTC != null) { // some wreckage from last time
            acc.reset(currentDTC);
            currentDTC = null;
        }

        if (c.isShowing() && dt != null && dt.isActive()) {
            currentDT  = dt;
            currentDTC = currentDT.getDropTargetContext();

            acc.setDropTargetContextPeer(currentDTC, this);

            currentA   = dt.getDefaultActions();

            try {
                ((DropTargetListener)dt).dragEnter(new DropTargetDragEvent(currentDTC,
                                                                           hots,
                                                                           currentDA,
                                                                           currentSA));
            } catch (Exception e) {
                e.printStackTrace();
                currentDA = DnDConstants.ACTION_NONE;
            }
        } else {
            currentDT  = null;
            currentDTC = null;
            currentDA   = DnDConstants.ACTION_NONE;
            currentSA   = DnDConstants.ACTION_NONE;
            currentA   = DnDConstants.ACTION_NONE;
        }

    }

    /**
     * upcall to handle exit messages
     */

    private void handleExitMessage(final Component component,
                                   final long nativeCtxt) {
        /*
         * Even though the return value is irrelevant for this event, it is
         * dispatched synchronously to fix 4393148 properly.
         */
        postDropTargetEvent(component, 0, 0, DnDConstants.ACTION_NONE,
                            DnDConstants.ACTION_NONE, null, nativeCtxt,
                            SunDropTargetEvent.MOUSE_EXITED,
                            SunDropTargetContextPeer.DISPATCH_SYNC);
    }

    /**
     *
     */

    protected void processExitMessage(SunDropTargetEvent event) {
        Component         c   = (Component)event.getSource();
        DropTarget        dt  = c.getDropTarget();
        DropTargetContext dtc = null;
        DropTargetContextAccessor acc =
                AWTAccessor.getDropTargetContextAccessor();

        if (dt == null) {
            currentDT = null;
            currentT  = null;

            if (currentDTC != null) {
                acc.reset(currentDTC);
            }

            currentDTC = null;

            return;
        }

        if (dt != currentDT) {

            if (currentDTC != null) {
                acc.reset(currentDTC);
            }

            currentDT  = dt;
            currentDTC = dt.getDropTargetContext();

            acc.setDropTargetContextPeer(currentDTC, this);
        }

        dtc = currentDTC;

        if (dt.isActive()) try {
            ((DropTargetListener)dt).dragExit(new DropTargetEvent(dtc));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            currentA  = DnDConstants.ACTION_NONE;
            currentSA = DnDConstants.ACTION_NONE;
            currentDA = DnDConstants.ACTION_NONE;
            currentDT = null;
            currentT  = null;

            acc.reset(currentDTC);
            currentDTC = null;

            local = null;

            dragRejected = false;
        }
    }

    private int handleMotionMessage(final Component component,
                                    final int x, final int y,
                                    final int dropAction,
                                    final int actions, final long[] formats,
                                    final long nativeCtxt) {
        return postDropTargetEvent(component, x, y, dropAction, actions,
                                   formats, nativeCtxt,
                                   SunDropTargetEvent.MOUSE_DRAGGED,
                                   SunDropTargetContextPeer.DISPATCH_SYNC);
    }

    /**
     *
     */

    protected void processMotionMessage(SunDropTargetEvent event,
                                      boolean operationChanged) {
        Component         c    = (Component)event.getSource();
        Point             hots = event.getPoint();
        int               id   = event.getID();
        DropTarget        dt   = c.getDropTarget();
        DropTargetContext dtc  = null;
        DropTargetContextAccessor acc =
                AWTAccessor.getDropTargetContextAccessor();

        if (c.isShowing() && (dt != null) && dt.isActive()) {
            if (currentDT != dt) {
                if (currentDTC != null) {
                    acc.reset(currentDTC);
                }

                currentDT  = dt;
                currentDTC = null;
            }

            dtc = currentDT.getDropTargetContext();
            if (dtc != currentDTC) {
                if (currentDTC != null) {
                    acc.reset(currentDTC);
                }

                currentDTC = dtc;
                acc.setDropTargetContextPeer(currentDTC, this);
            }

            currentA = currentDT.getDefaultActions();

            try {
                DropTargetDragEvent dtde = new DropTargetDragEvent(dtc,
                                                                   hots,
                                                                   currentDA,
                                                                   currentSA);
                DropTargetListener dtl = (DropTargetListener)dt;
                if (operationChanged) {
                    dtl.dropActionChanged(dtde);
                } else {
                    dtl.dragOver(dtde);
                }

                if (dragRejected) {
                    currentDA = DnDConstants.ACTION_NONE;
                }
            } catch (Exception e) {
                e.printStackTrace();
                currentDA = DnDConstants.ACTION_NONE;
            }
        } else {
            currentDA = DnDConstants.ACTION_NONE;
        }
    }

    /**
     * upcall to handle the Drop message
     */

    private void handleDropMessage(final Component component,
                                   final int x, final int y,
                                   final int dropAction, final int actions,
                                   final long[] formats,
                                   final long nativeCtxt) {
        postDropTargetEvent(component, x, y, dropAction, actions,
                            formats, nativeCtxt,
                            SunDropTargetEvent.MOUSE_DROPPED,
                            !SunDropTargetContextPeer.DISPATCH_SYNC);
    }

    /**
     *
     */

    protected void processDropMessage(SunDropTargetEvent event) {
        Component  c    = (Component)event.getSource();
        Point      hots = event.getPoint();
        DropTarget dt   = c.getDropTarget();

        dropStatus   = STATUS_WAIT; // drop pending ACK
        dropComplete = false;

        if (c.isShowing() && dt != null && dt.isActive()) {
            DropTargetContext dtc = dt.getDropTargetContext();

            currentDT = dt;
            DropTargetContextAccessor acc =
                    AWTAccessor.getDropTargetContextAccessor();

            if (currentDTC != null) {
                acc.reset(currentDTC);
            }

            currentDTC = dtc;
            acc.setDropTargetContextPeer(currentDTC, this);
            currentA = dt.getDefaultActions();

            synchronized(_globalLock) {
                if ((local = getJVMLocalSourceTransferable()) != null)
                    setCurrentJVMLocalSourceTransferable(null);
            }

            dropInProcess = true;

            try {
                ((DropTargetListener)dt).drop(new DropTargetDropEvent(dtc,
                                                                      hots,
                                                                      currentDA,
                                                                      currentSA,
                                                                      local != null));
            } finally {
                if (dropStatus == STATUS_WAIT) {
                    rejectDrop();
                } else if (dropComplete == false) {
                    dropComplete(false);
                }
                dropInProcess = false;
            }
        } else {
            rejectDrop();
        }
    }

    protected int postDropTargetEvent(final Component component,
                                      final int x, final int y,
                                      final int dropAction,
                                      final int actions,
                                      final long[] formats,
                                      final long nativeCtxt,
                                      final int eventID,
                                      final boolean dispatchType) {
        AppContext appContext = SunToolkit.targetToAppContext(component);

        EventDispatcher dispatcher =
            new EventDispatcher(this, dropAction, actions, formats, nativeCtxt,
                                dispatchType);

        SunDropTargetEvent event =
            new SunDropTargetEvent(component, eventID, x, y, dispatcher);

        if (dispatchType == SunDropTargetContextPeer.DISPATCH_SYNC) {
            DataTransferer.getInstance().getToolkitThreadBlockedHandler().lock();
        }

        // schedule callback
        SunToolkit.postEvent(appContext, event);

        eventPosted(event);

        if (dispatchType == SunDropTargetContextPeer.DISPATCH_SYNC) {
            while (!dispatcher.isDone()) {
                DataTransferer.getInstance().getToolkitThreadBlockedHandler().enter();
            }

            DataTransferer.getInstance().getToolkitThreadBlockedHandler().unlock();

            // return target's response
            return dispatcher.getReturnValue();
        } else {
            return 0;
        }
    }

    /**
     * acceptDrag
     */

    public synchronized void acceptDrag(int dragOperation) {
        if (currentDT == null) {
            throw new InvalidDnDOperationException("No Drag pending");
        }
        currentDA = mapOperation(dragOperation);
        if (currentDA != DnDConstants.ACTION_NONE) {
            dragRejected = false;
        }
    }

    /**
     * rejectDrag
     */

    public synchronized void rejectDrag() {
        if (currentDT == null) {
            throw new InvalidDnDOperationException("No Drag pending");
        }
        currentDA = DnDConstants.ACTION_NONE;
        dragRejected = true;
    }

    /**
     * acceptDrop
     */

    public synchronized void acceptDrop(int dropOperation) {
        if (dropOperation == DnDConstants.ACTION_NONE)
            throw new IllegalArgumentException("invalid acceptDrop() action");

        if (dropStatus == STATUS_WAIT || dropStatus == STATUS_ACCEPT) {
            currentDA = currentA = mapOperation(dropOperation & currentSA);

            dropStatus   = STATUS_ACCEPT;
            dropComplete = false;
        } else {
            throw new InvalidDnDOperationException("invalid acceptDrop()");
        }
    }

    /**
     * reject Drop
     */

    public synchronized void rejectDrop() {
        if (dropStatus != STATUS_WAIT) {
            throw new InvalidDnDOperationException("invalid rejectDrop()");
        }
        dropStatus = STATUS_REJECT;
        /*
         * Fix for 4285634.
         * The target rejected the drop means that it doesn't perform any
         * drop action. This change is to make Solaris behavior consistent
         * with Win32.
         */
        currentDA = DnDConstants.ACTION_NONE;
        dropComplete(false);
    }

    /**
     * mapOperation
     */

    private int mapOperation(int operation) {
        int[] operations = {
                DnDConstants.ACTION_MOVE,
                DnDConstants.ACTION_COPY,
                DnDConstants.ACTION_LINK,
        };
        int   ret = DnDConstants.ACTION_NONE;

        for (int i = 0; i < operations.length; i++) {
            if ((operation & operations[i]) == operations[i]) {
                    ret = operations[i];
                    break;
            }
        }

        return ret;
    }

    /**
     * signal drop complete
     */

    public synchronized void dropComplete(boolean success) {
        if (dropStatus == STATUS_NONE) {
            throw new InvalidDnDOperationException("No Drop pending");
        }

        if (currentDTC != null) {
            AWTAccessor.getDropTargetContextAccessor().reset(currentDTC);
        }

        currentDT  = null;
        currentDTC = null;
        currentT   = null;
        currentA   = DnDConstants.ACTION_NONE;

        synchronized(_globalLock) {
            currentJVMLocalSourceTransferable = null;
        }

        dropStatus   = STATUS_NONE;
        dropComplete = true;

        try {
            doDropDone(success, currentDA, local != null);
        } finally {
            currentDA = DnDConstants.ACTION_NONE;
            // The native context is invalid after the drop is done.
            // Clear the reference to prohibit access.
            nativeDragContext = 0;
        }
    }

    protected abstract void doDropDone(boolean success,
                                       int dropAction, boolean isLocal);

    protected synchronized long getNativeDragContext() {
        return nativeDragContext;
    }

    protected void eventPosted(SunDropTargetEvent e) {}

    protected void eventProcessed(SunDropTargetEvent e, int returnValue,
                                  boolean dispatcherDone) {}

    protected static class EventDispatcher {

        private final SunDropTargetContextPeer peer;

        // context fields
        private final int dropAction;
        private final int actions;
        private final long[] formats;
        private long nativeCtxt;
        private final boolean dispatchType;
        private boolean dispatcherDone = false;

        // dispatcher state fields
        private int returnValue = 0;
        // set of events to be dispatched by this dispatcher
        private final HashSet<SunDropTargetEvent> eventSet = new HashSet<>(3);

        static final ToolkitThreadBlockedHandler handler =
            DataTransferer.getInstance().getToolkitThreadBlockedHandler();

        EventDispatcher(SunDropTargetContextPeer peer,
                        int dropAction,
                        int actions,
                        long[] formats,
                        long nativeCtxt,
                        boolean dispatchType) {

            this.peer         = peer;
            this.nativeCtxt   = nativeCtxt;
            this.dropAction   = dropAction;
            this.actions      = actions;
            this.formats =
                     (null == formats) ? null : Arrays.copyOf(formats, formats.length);
            this.dispatchType = dispatchType;
        }

        void dispatchEvent(SunDropTargetEvent e) {
            int id = e.getID();

            switch (id) {
            case SunDropTargetEvent.MOUSE_ENTERED:
                dispatchEnterEvent(e);
                break;
            case SunDropTargetEvent.MOUSE_DRAGGED:
                dispatchMotionEvent(e);
                break;
            case SunDropTargetEvent.MOUSE_EXITED:
                dispatchExitEvent(e);
                break;
            case SunDropTargetEvent.MOUSE_DROPPED:
                dispatchDropEvent(e);
                break;
            default:
                throw new InvalidDnDOperationException();
            }
        }

        private void dispatchEnterEvent(SunDropTargetEvent e) {
            synchronized (peer) {

                // store the drop action here to track operation changes
                peer.previousDA = dropAction;

                // setup peer context
                peer.nativeDragContext = nativeCtxt;
                peer.currentT          = formats;
                peer.currentSA         = actions;
                peer.currentDA         = dropAction;
                // To allow data retrieval.
                peer.dropStatus        = STATUS_ACCEPT;
                peer.dropComplete      = false;

                try {
                    peer.processEnterMessage(e);
                } finally {
                    peer.dropStatus        = STATUS_NONE;
                }

                setReturnValue(peer.currentDA);
            }
        }

        private void dispatchMotionEvent(SunDropTargetEvent e) {
            synchronized (peer) {

                boolean operationChanged = peer.previousDA != dropAction;
                peer.previousDA = dropAction;

                // setup peer context
                peer.nativeDragContext = nativeCtxt;
                peer.currentT          = formats;
                peer.currentSA         = actions;
                peer.currentDA         = dropAction;
                // To allow data retrieval.
                peer.dropStatus        = STATUS_ACCEPT;
                peer.dropComplete      = false;

                try {
                    peer.processMotionMessage(e, operationChanged);
                } finally {
                    peer.dropStatus        = STATUS_NONE;
                }

                setReturnValue(peer.currentDA);
            }
        }

        private void dispatchExitEvent(SunDropTargetEvent e) {
            synchronized (peer) {

                // setup peer context
                peer.nativeDragContext = nativeCtxt;

                peer.processExitMessage(e);
            }
        }

        private void dispatchDropEvent(SunDropTargetEvent e) {
            synchronized (peer) {

                // setup peer context
                peer.nativeDragContext = nativeCtxt;
                peer.currentT          = formats;
                peer.currentSA         = actions;
                peer.currentDA         = dropAction;

                peer.processDropMessage(e);
            }
        }

        void setReturnValue(int ret) {
            returnValue = ret;
        }

        int getReturnValue() {
            return returnValue;
        }

        boolean isDone() {
            return eventSet.isEmpty();
        }

        void registerEvent(SunDropTargetEvent e) {
            handler.lock();
            if (!eventSet.add(e) && dndLog.isLoggable(PlatformLogger.Level.FINE)) {
                dndLog.fine("Event is already registered: " + e);
            }
            handler.unlock();
        }

        void unregisterEvent(SunDropTargetEvent e) {
            handler.lock();
            try {
                if (!eventSet.remove(e)) {
                    // This event has already been unregistered.
                    return;
                }
                if (eventSet.isEmpty()) {
                    if (!dispatcherDone && dispatchType == DISPATCH_SYNC) {
                        handler.exit();
                    }
                    dispatcherDone = true;
                }
            } finally {
                handler.unlock();
            }

            try {
                peer.eventProcessed(e, returnValue, dispatcherDone);
            } finally {
                /*
                 * Clear the reference to the native context if all copies of
                 * the original event are processed.
                 */
                if (dispatcherDone) {
                    nativeCtxt = 0;
                    // Fix for 6342381
                    peer.nativeDragContext = 0;

                }
            }
        }

        public void unregisterAllEvents() {
            Object[] events = null;
            handler.lock();
            try {
                events = eventSet.toArray();
            } finally {
                handler.unlock();
            }

            if (events != null) {
                for (int i = 0; i < events.length; i++) {
                    unregisterEvent((SunDropTargetEvent)events[i]);
                }
            }
        }
    }
}
