/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.datatransfer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import sun.datatransfer.DataFlavorUtil;

/**
 * A class that implements a mechanism to transfer data using cut/copy/paste
 * operations.
 * <p>
 * {@link FlavorListener}s may be registered on an instance of the Clipboard
 * class to be notified about changes to the set of {@link DataFlavor}s
 * available on this clipboard (see {@link #addFlavorListener}).
 *
 * @author Amy Fowler
 * @author Alexander Gerasimov
 * @see java.awt.Toolkit#getSystemClipboard
 * @see java.awt.Toolkit#getSystemSelection
 * @since 1.1
 */
public class Clipboard {

    String name;

    /**
     * The owner of the clipboard.
     */
    protected ClipboardOwner owner;

    /**
     * Contents of the clipboard.
     */
    protected Transferable contents;

    /**
     * An aggregate of flavor listeners registered on this local clipboard.
     *
     * @since 1.5
     */
    private Set<FlavorListener> flavorListeners;

    /**
     * A set of {@code DataFlavor}s that is available on this local clipboard.
     * It is used for tracking changes of {@code DataFlavor}s available on this
     * clipboard.
     *
     * @since 1.5
     */
    private Set<DataFlavor> currentDataFlavors;

    /**
     * Creates a clipboard object.
     *
     * @param  name for the clipboard
     * @see java.awt.Toolkit#getSystemClipboard
     */
    public Clipboard(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this clipboard object.
     *
     * @return the name of this clipboard object
     * @see java.awt.Toolkit#getSystemClipboard
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the current contents of the clipboard to the specified transferable
     * object and registers the specified clipboard owner as the owner of the
     * new contents.
     * <p>
     * If there is an existing owner different from the argument {@code owner},
     * that owner is notified that it no longer holds ownership of the clipboard
     * contents via an invocation of {@code ClipboardOwner.lostOwnership()} on
     * that owner. An implementation of {@code setContents()} is free not to
     * invoke {@code lostOwnership()} directly from this method. For example,
     * {@code lostOwnership()} may be invoked later on a different thread. The
     * same applies to {@code FlavorListener}s registered on this clipboard.
     * <p>
     * The method throws {@code IllegalStateException} if the clipboard is
     * currently unavailable. For example, on some platforms, the system
     * clipboard is unavailable while it is accessed by another application.
     *
     * @param  contents the transferable object representing the clipboard
     *         content
     * @param  owner the object which owns the clipboard content
     * @throws IllegalStateException if the clipboard is currently unavailable
     * @see java.awt.Toolkit#getSystemClipboard
     */
    public synchronized void setContents(Transferable contents, ClipboardOwner owner) {
        final ClipboardOwner oldOwner = this.owner;
        final Transferable oldContents = this.contents;

        this.owner = owner;
        this.contents = contents;

        if (oldOwner != null && oldOwner != owner) {
            DataFlavorUtil.getDesktopService().invokeOnEventThread(() ->
                    oldOwner.lostOwnership(Clipboard.this, oldContents));
        }
        fireFlavorsChanged();
    }

    /**
     * Returns a transferable object representing the current contents of the
     * clipboard. If the clipboard currently has no contents, it returns
     * {@code null}. The parameter Object requestor is not currently used. The
     * method throws {@code IllegalStateException} if the clipboard is currently
     * unavailable. For example, on some platforms, the system clipboard is
     * unavailable while it is accessed by another application.
     *
     * @param  requestor the object requesting the clip data (not used)
     * @return the current transferable object on the clipboard
     * @throws IllegalStateException if the clipboard is currently unavailable
     * @see java.awt.Toolkit#getSystemClipboard
     */
    public synchronized Transferable getContents(Object requestor) {
        return contents;
    }

    /**
     * Returns an array of {@code DataFlavor}s in which the current contents of
     * this clipboard can be provided. If there are no {@code DataFlavor}s
     * available, this method returns a zero-length array.
     *
     * @return an array of {@code DataFlavor}s in which the current contents of
     *         this clipboard can be provided
     * @throws IllegalStateException if this clipboard is currently unavailable
     * @since 1.5
     */
    public DataFlavor[] getAvailableDataFlavors() {
        Transferable cntnts = getContents(null);
        if (cntnts == null) {
            return new DataFlavor[0];
        }
        return cntnts.getTransferDataFlavors();
    }

    /**
     * Returns whether or not the current contents of this clipboard can be
     * provided in the specified {@code DataFlavor}.
     *
     * @param  flavor the requested {@code DataFlavor} for the contents
     * @return {@code true} if the current contents of this clipboard can be
     *         provided in the specified {@code DataFlavor}; {@code false}
     *         otherwise
     * @throws NullPointerException if {@code flavor} is {@code null}
     * @throws IllegalStateException if this clipboard is currently unavailable
     * @since 1.5
     */
    public boolean isDataFlavorAvailable(DataFlavor flavor) {
        if (flavor == null) {
            throw new NullPointerException("flavor");
        }

        Transferable cntnts = getContents(null);
        if (cntnts == null) {
            return false;
        }
        return cntnts.isDataFlavorSupported(flavor);
    }

    /**
     * Returns an object representing the current contents of this clipboard in
     * the specified {@code DataFlavor}. The class of the object returned is
     * defined by the representation class of {@code flavor}.
     *
     * @param  flavor the requested {@code DataFlavor} for the contents
     * @return an object representing the current contents of this clipboard in
     *         the specified {@code DataFlavor}
     * @throws NullPointerException if {@code flavor} is {@code null}
     * @throws IllegalStateException if this clipboard is currently unavailable
     * @throws UnsupportedFlavorException if the requested {@code DataFlavor} is
     *         not available
     * @throws IOException if the data in the requested {@code DataFlavor} can
     *         not be retrieved
     * @see DataFlavor#getRepresentationClass
     * @since 1.5
     */
    public Object getData(DataFlavor flavor)
        throws UnsupportedFlavorException, IOException {
        if (flavor == null) {
            throw new NullPointerException("flavor");
        }

        Transferable cntnts = getContents(null);
        if (cntnts == null) {
            throw new UnsupportedFlavorException(flavor);
        }
        return cntnts.getTransferData(flavor);
    }

    /**
     * Registers the specified {@code FlavorListener} to receive
     * {@code FlavorEvent}s from this clipboard. If {@code listener} is
     * {@code null}, no exception is thrown and no action is performed.
     *
     * @param  listener the listener to be added
     * @see #removeFlavorListener
     * @see #getFlavorListeners
     * @see FlavorListener
     * @see FlavorEvent
     * @since 1.5
     */
    public synchronized void addFlavorListener(FlavorListener listener) {
        if (listener == null) {
            return;
        }

        if (flavorListeners == null) {
            flavorListeners = new HashSet<>();
            currentDataFlavors = getAvailableDataFlavorSet();
        }

        flavorListeners.add(listener);
    }

    /**
     * Removes the specified {@code FlavorListener} so that it no longer
     * receives {@code FlavorEvent}s from this {@code Clipboard}. This method
     * performs no function, nor does it throw an exception, if the listener
     * specified by the argument was not previously added to this
     * {@code Clipboard}. If {@code listener} is {@code null}, no exception is
     * thrown and no action is performed.
     *
     * @param  listener the listener to be removed
     * @see #addFlavorListener
     * @see #getFlavorListeners
     * @see FlavorListener
     * @see FlavorEvent
     * @since 1.5
     */
    public synchronized void removeFlavorListener(FlavorListener listener) {
        if (listener == null || flavorListeners == null) {
            return;
        }
        flavorListeners.remove(listener);
    }

    /**
     * Returns an array of all the {@code FlavorListener}s currently registered
     * on this {@code Clipboard}.
     *
     * @return all of this clipboard's {@code FlavorListener}s or an empty array
     *         if no listeners are currently registered
     * @see #addFlavorListener
     * @see #removeFlavorListener
     * @see FlavorListener
     * @see FlavorEvent
     * @since 1.5
     */
    public synchronized FlavorListener[] getFlavorListeners() {
        return flavorListeners == null ? new FlavorListener[0] :
            flavorListeners.toArray(new FlavorListener[flavorListeners.size()]);
    }

    /**
     * Checks change of the {@code DataFlavor}s and, if necessary, notifies all
     * listeners that have registered interest for notification on
     * {@code FlavorEvent}s.
     *
     * @since 1.5
     */
    private void fireFlavorsChanged() {
        if (flavorListeners == null) {
            return;
        }

        Set<DataFlavor> prevDataFlavors = currentDataFlavors;
        currentDataFlavors = getAvailableDataFlavorSet();
        if (Objects.equals(prevDataFlavors, currentDataFlavors)) {
            return;
        }
        flavorListeners.forEach(listener ->
                DataFlavorUtil.getDesktopService().invokeOnEventThread(() ->
                        listener.flavorsChanged(new FlavorEvent(Clipboard.this))));
    }

    /**
     * Returns a set of {@code DataFlavor}s currently available on this
     * clipboard.
     *
     * @return a set of {@code DataFlavor}s currently available on this
     *         clipboard
     * @since 1.5
     */
    private Set<DataFlavor> getAvailableDataFlavorSet() {
        Set<DataFlavor> set = new HashSet<>();
        Transferable contents = getContents(null);
        if (contents != null) {
            DataFlavor[] flavors = contents.getTransferDataFlavors();
            if (flavors != null) {
                set.addAll(Arrays.asList(flavors));
            }
        }
        return set;
    }
}
