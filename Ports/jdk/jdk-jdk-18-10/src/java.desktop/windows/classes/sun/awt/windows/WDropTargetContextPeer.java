/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.windows;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import sun.awt.PeerEvent;
import sun.awt.SunToolkit;
import sun.awt.dnd.SunDropTargetContextPeer;
import sun.awt.dnd.SunDropTargetEvent;

/**
 * <p>
 * The WDropTargetContextPeer class is the class responsible for handling
 * the interaction between the win32 DnD system and Java.
 * </p>
 *
 *
 */

final class WDropTargetContextPeer extends SunDropTargetContextPeer {
    /**
     * create the peer typically upcall from C++
     */

    static WDropTargetContextPeer getWDropTargetContextPeer() {
        return new WDropTargetContextPeer();
    }

    /**
     * create the peer
     */

    private WDropTargetContextPeer() {
        super();
    }

    /**
     * upcall to encapsulate file transfer
     */

    private static FileInputStream getFileStream(String file, long stgmedium)
        throws IOException
    {
        return new WDropTargetContextPeerFileStream(file, stgmedium);
    }

    /**
     * upcall to encapsulate IStream buffer transfer
     */

    private static Object getIStream(long istream) throws IOException {
        return new WDropTargetContextPeerIStream(istream);
    }

    @Override
    protected Object getNativeData(long format) {
        return getData(getNativeDragContext(), format);
    }

    /**
     * signal drop complete
     */

    @Override
    protected void doDropDone(boolean success, int dropAction,
                              boolean isLocal) {
        dropDone(getNativeDragContext(), success, dropAction);
    }

    @Override
    protected void eventPosted(final SunDropTargetEvent e) {
        if (e.getID() != SunDropTargetEvent.MOUSE_DROPPED) {
            Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        e.getDispatcher().unregisterAllEvents();
                    }
                };
            // NOTE: this PeerEvent must be a NORM_PRIORITY event to be
            // dispatched after this SunDropTargetEvent, but before the next
            // one, so we should pass zero flags.
            PeerEvent peerEvent = new PeerEvent(e.getSource(), runnable, 0);
            SunToolkit.executeOnEventHandlerThread(peerEvent);
        }
    }

    /**
     * downcall to bind transfer data.
     */

     private native Object getData(long nativeContext, long format);

    /**
     * downcall to notify that drop is complete
     */

     private native void dropDone(long nativeContext, boolean success, int action);
}

/**
 * package private class to handle file transfers
 */

final class WDropTargetContextPeerFileStream extends FileInputStream {

    /**
     * construct file input stream
     */

    WDropTargetContextPeerFileStream(String name, long stgmedium)
        throws FileNotFoundException
    {
        super(name);

        this.stgmedium  = stgmedium;
    }

    /**
     * close
     */

    @Override
    public void close() throws IOException {
        if (stgmedium != 0) {
            super.close();
            freeStgMedium(stgmedium);
            stgmedium = 0;
        }
    }

    /**
     * free underlying windows data structure
     */

    private native void freeStgMedium(long stgmedium);

    /*
     * fields
     */

    private long    stgmedium;
}

/**
 * Package private class to access IStream objects
 */

final class WDropTargetContextPeerIStream extends InputStream {

    /**
     * construct a WDropTargetContextPeerIStream wrapper
     */

    WDropTargetContextPeerIStream(long istream) throws IOException {
        super();

        if (istream == 0) throw new IOException("No IStream");

        this.istream    = istream;
    }

    /**
     * @return bytes available
     */

    @Override
    public int available() throws IOException {
        if (istream == 0) throw new IOException("No IStream");
        return Available(istream);
    }

    private native int Available(long istream);

    /**
     * read
     */

    @Override
    public int read() throws IOException {
        if (istream == 0) throw new IOException("No IStream");
        return Read(istream);
    }

    private native int Read(long istream) throws IOException;

    /**
     * read into buffer
     */

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (istream == 0) throw new IOException("No IStream");
        return ReadBytes(istream, b, off, len);
    }

    private native int ReadBytes(long istream, byte[] b, int off, int len) throws IOException;

    /**
     * close
     */

    @Override
    public void close() throws IOException {
        if (istream != 0) {
            super.close();
            Close(istream);
            istream = 0;
        }
    }

    private native void Close(long istream);

    /*
     * fields
     */

    private long istream;
}
