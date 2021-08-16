/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.sound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceReceiver;
import javax.sound.midi.MidiDeviceTransmitter;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;


/**
 * Abstract AbstractMidiDevice class representing functionality shared by
 * MidiInDevice and MidiOutDevice objects.
 *
 * @author David Rivas
 * @author Kara Kytle
 * @author Matthias Pfisterer
 * @author Florian Bomers
 */
abstract class AbstractMidiDevice implements MidiDevice, ReferenceCountingDevice {

    private ArrayList<Receiver> receiverList;

    private TransmitterList transmitterList;

    // lock to protect receiverList and transmitterList
    // from simultaneous creation and destruction
    // reduces possibility of deadlock, compared to
    // synchronizing to the class instance
    private final Object traRecLock = new Object();

    // DEVICE ATTRIBUTES

    private final MidiDevice.Info info;

    // DEVICE STATE

    private volatile boolean open;
    private int openRefCount;

    /** List of Receivers and Transmitters that opened the device implicitely.
     */
    private List<Object> openKeepingObjects;

    /**
     * This is the device handle returned from native code.
     */
    protected volatile long id;

    /**
     * Constructs an AbstractMidiDevice with the specified info object.
     * @param info the description of the device
     */
    /*
     * The initial mode and only supported mode default to OMNI_ON_POLY.
     */
    protected AbstractMidiDevice(MidiDevice.Info info) {
        this.info = info;
        openRefCount = 0;
    }

    // MIDI DEVICE METHODS

    @Override
    public final MidiDevice.Info getDeviceInfo() {
        return info;
    }

    /** Open the device from an application program.
     * Setting the open reference count to -1 here prevents Transmitters and Receivers that
     * opened the device implicitly from closing it. The only way to close the device after
     * this call is a call to close().
     */
    @Override
    public final void open() throws MidiUnavailableException {
        synchronized(this) {
            openRefCount = -1;
            doOpen();
        }
    }

    /** Open the device implicitly.
     * This method is intended to be used by AbstractReceiver
     * and BasicTransmitter. Actually, it is called by getReceiverReferenceCounting() and
     * getTransmitterReferenceCounting(). These, in turn, are called by MidiSytem on calls to
     * getReceiver() and getTransmitter(). The former methods should pass the Receiver or
     * Transmitter just created as the object parameter to this method. Storing references to
     * these objects is necessary to be able to decide later (when it comes to closing) if
     * R/T's are ones that opened the device implicitly.
     *
     * @object The Receiver or Transmitter instance that triggered this implicit open.
     */
    private void openInternal(Object object) throws MidiUnavailableException {
        synchronized(this) {
            if (openRefCount != -1) {
                openRefCount++;
                getOpenKeepingObjects().add(object);
            }
            // double calls to doOpens() will be catched by the open flag.
            doOpen();
        }
    }

    private void doOpen() throws MidiUnavailableException {
        synchronized(this) {
            if (! isOpen()) {
                implOpen();
                open = true;
            }
        }
    }

    @Override
    public final void close() {
        synchronized (this) {
            doClose();
            openRefCount = 0;
        }
    }

    /** Close the device for an object that implicitely opened it.
     * This method is intended to be used by Transmitter.close() and Receiver.close().
     * Those methods should pass this for the object parameter. Since Transmitters or Receivers
     * do not know if their device has been opened implicitely because of them, they call this
     * method in any case. This method now is able to seperate Receivers/Transmitters that opened
     * the device implicitely from those that didn't by looking up the R/T in the
     * openKeepingObjects list. Only if the R/T is contained there, the reference count is
     * reduced.
     *
     * @param object The object that might have been opening the device implicitely (for now,
     * this may be a Transmitter or receiver).
     */
    public final void closeInternal(Object object) {
        synchronized(this) {
            if (getOpenKeepingObjects().remove(object)) {
                if (openRefCount > 0) {
                    openRefCount--;
                    if (openRefCount == 0) {
                        doClose();
                    }
                }
            }
        }
    }

    public final void doClose() {
        synchronized(this) {
            if (isOpen()) {
                implClose();
                open = false;
            }
        }
    }

    @Override
    public final boolean isOpen() {
        return open;
    }

    protected void implClose() {
        synchronized (traRecLock) {
            if (receiverList != null) {
                // close all receivers
                for(int i = 0; i < receiverList.size(); i++) {
                    receiverList.get(i).close();
                }
                receiverList.clear();
            }
            if (transmitterList != null) {
                // close all transmitters
                transmitterList.close();
            }
        }
    }

    /**
     * This implementation always returns -1.
     * Devices that actually provide this should over-ride
     * this method.
     */
    @Override
    public long getMicrosecondPosition() {
        return -1;
    }

    /** Return the maximum number of Receivers supported by this device.
        Depending on the return value of hasReceivers(), this method returns either 0 or -1.
        Subclasses should rather override hasReceivers() than override this method.
     */
    @Override
    public final int getMaxReceivers() {
        if (hasReceivers()) {
            return -1;
        } else {
            return 0;
        }
    }

    /** Return the maximum number of Transmitters supported by this device.
        Depending on the return value of hasTransmitters(), this method returns either 0 or -1.
        Subclasses should override hasTransmitters().
     */
    @Override
    public final int getMaxTransmitters() {
        if (hasTransmitters()) {
            return -1;
        } else {
            return 0;
        }
    }

    /** Retrieve a Receiver for this device.
        This method returns the value returned by createReceiver(), if it doesn't throw
        an exception. Subclasses should rather override createReceiver() than override
        this method.
        If createReceiver returns a Receiver, it is added to the internal list
        of Receivers (see getReceiversList)
     */
    @Override
    public final Receiver getReceiver() throws MidiUnavailableException {
        Receiver receiver;
        synchronized (traRecLock) {
            receiver = createReceiver(); // may throw MidiUnavailableException
            getReceiverList().add(receiver);
        }
        return receiver;
    }

    @Override
    @SuppressWarnings("unchecked") // Cast of result of clone
    public final List<Receiver> getReceivers() {
        List<Receiver> recs;
        synchronized (traRecLock) {
            if (receiverList == null) {
                recs = Collections.unmodifiableList(new ArrayList<Receiver>(0));
            } else {
                recs = Collections.unmodifiableList
                    ((List<Receiver>) (receiverList.clone()));
            }
        }
        return recs;
    }

    /**
     * This implementation uses createTransmitter, which may throw an exception.
     * If a transmitter is returned in createTransmitter, it is added to the internal
     * TransmitterList
     */
    @Override
    public final Transmitter getTransmitter() throws MidiUnavailableException {
        Transmitter transmitter;
        synchronized (traRecLock) {
            transmitter = createTransmitter(); // may throw MidiUnavailableException
            getTransmitterList().add(transmitter);
        }
        return transmitter;
    }

    @Override
    @SuppressWarnings("unchecked") // Cast of result of clone
    public final List<Transmitter> getTransmitters() {
        List<Transmitter> tras;
        synchronized (traRecLock) {
            if (transmitterList == null
                || transmitterList.transmitters.size() == 0) {
                tras = Collections.unmodifiableList(new ArrayList<Transmitter>(0));
            } else {
                tras = Collections.unmodifiableList((List<Transmitter>) (transmitterList.transmitters.clone()));
            }
        }
        return tras;
    }

    final long getId() {
        return id;
    }

    // REFERENCE COUNTING

    /** Retrieve a Receiver and open the device implicitly.
        This method is called by MidiSystem.getReceiver().
     */
    @Override
    public final Receiver getReceiverReferenceCounting()
            throws MidiUnavailableException {
        /* Keep this order of commands! If getReceiver() throws an exception,
           openInternal() should not be called!
        */
        Receiver receiver;
        synchronized (traRecLock) {
            receiver = getReceiver();
            AbstractMidiDevice.this.openInternal(receiver);
        }
        return receiver;
    }

    /** Retrieve a Transmitter and open the device implicitly.
        This method is called by MidiSystem.getTransmitter().
     */
    @Override
    public final Transmitter getTransmitterReferenceCounting()
            throws MidiUnavailableException {
        /* Keep this order of commands! If getTransmitter() throws an exception,
           openInternal() should not be called!
        */
        Transmitter transmitter;
        synchronized (traRecLock) {
            transmitter = getTransmitter();
            AbstractMidiDevice.this.openInternal(transmitter);
        }
        return transmitter;
    }

    /** Return the list of objects that have opened the device implicitely.
     */
    private synchronized List<Object> getOpenKeepingObjects() {
        if (openKeepingObjects == null) {
            openKeepingObjects = new ArrayList<>();
        }
        return openKeepingObjects;
    }

    // RECEIVER HANDLING METHODS

    /** Return the internal list of Receivers, possibly creating it first.
     */
    private List<Receiver> getReceiverList() {
        synchronized (traRecLock) {
            if (receiverList == null) {
                receiverList = new ArrayList<>();
            }
        }
        return receiverList;
    }

    /** Returns if this device supports Receivers.
        Subclasses that use Receivers should override this method to
        return true. They also should override createReceiver().

        @return true, if the device supports Receivers, false otherwise.
    */
    protected boolean hasReceivers() {
        return false;
    }

    /** Create a Receiver object.
        throwing an exception here means that Receivers aren't enabled.
        Subclasses that use Receivers should override this method with
        one that returns objects implementing Receiver.
        Classes overriding this method should also override hasReceivers()
        to return true.
    */
    protected Receiver createReceiver() throws MidiUnavailableException {
        throw new MidiUnavailableException("MIDI IN receiver not available");
    }

    // TRANSMITTER HANDLING

    /** Return the internal list of Transmitters, possibly creating it first.
     */
    final TransmitterList getTransmitterList() {
        synchronized (traRecLock) {
            if (transmitterList == null) {
                transmitterList = new TransmitterList();
            }
        }
        return transmitterList;
    }

    /** Returns if this device supports Transmitters.
        Subclasses that use Transmitters should override this method to
        return true. They also should override createTransmitter().

        @return true, if the device supports Transmitters, false otherwise.
    */
    protected boolean hasTransmitters() {
        return false;
    }

    /** Create a Transmitter object.
        throwing an exception here means that Transmitters aren't enabled.
        Subclasses that use Transmitters should override this method with
        one that returns objects implementing Transmitters.
        Classes overriding this method should also override hasTransmitters()
        to return true.
    */
    protected Transmitter createTransmitter() throws MidiUnavailableException {
        throw new MidiUnavailableException("MIDI OUT transmitter not available");
    }

    protected abstract void implOpen() throws MidiUnavailableException;

    /**
     * close this device if discarded by the garbage collector.
     */
    @Override
    @SuppressWarnings("deprecation")
    protected final void finalize() {
        close();
    }

    /** Base class for Receivers.
        Subclasses that use Receivers must use this base class, since it
        contains magic necessary to manage implicit closing the device.
        This is necessary for Receivers retrieved via MidiSystem.getReceiver()
        (which opens the device implicitely).
     */
    abstract class AbstractReceiver implements MidiDeviceReceiver {
        private volatile boolean open = true;


        /** Deliver a MidiMessage.
            This method contains magic related to the closed state of a
            Receiver. Therefore, subclasses should not override this method.
            Instead, they should implement implSend().
        */
        @Override
        public final synchronized void send(final MidiMessage message,
                                            final long timeStamp) {
            if (!open) {
                throw new IllegalStateException("Receiver is not open");
            }
            implSend(message, timeStamp);
        }

        abstract void implSend(MidiMessage message, long timeStamp);

        /** Close the Receiver.
         * Here, the call to the magic method closeInternal() takes place.
         * Therefore, subclasses that override this method must call
         * 'super.close()'.
         */
        @Override
        public final void close() {
            open = false;
            synchronized (AbstractMidiDevice.this.traRecLock) {
                AbstractMidiDevice.this.getReceiverList().remove(this);
            }
            AbstractMidiDevice.this.closeInternal(this);
        }

        @Override
        public final MidiDevice getMidiDevice() {
            return AbstractMidiDevice.this;
        }

        final boolean isOpen() {
            return open;
        }
    } // class AbstractReceiver


    /**
     * Transmitter base class.
     * This class especially makes sure the device is closed if it
     * has been opened implicitly by a call to MidiSystem.getTransmitter().
     * The logic of doing so is actually in closeInternal().
     *
     * Also, it has some optimizations regarding sending to the Receivers,
     * for known Receivers, and managing itself in the TransmitterList.
     */
    class BasicTransmitter implements MidiDeviceTransmitter {

        private Receiver receiver = null;
        TransmitterList tlist = null;

        protected BasicTransmitter() {
        }

        private void setTransmitterList(TransmitterList tlist) {
            this.tlist = tlist;
        }

        @Override
        public final void setReceiver(Receiver receiver) {
            if (tlist != null && this.receiver != receiver) {
                tlist.receiverChanged(this, this.receiver, receiver);
                this.receiver = receiver;
            }
        }

        @Override
        public final Receiver getReceiver() {
            return receiver;
        }

        /** Close the Transmitter.
         * Here, the call to the magic method closeInternal() takes place.
         * Therefore, subclasses that override this method must call
         * 'super.close()'.
         */
        @Override
        public final void close() {
            AbstractMidiDevice.this.closeInternal(this);
            if (tlist != null) {
                tlist.receiverChanged(this, this.receiver, null);
                tlist.remove(this);
                tlist = null;
            }
        }

        @Override
        public final MidiDevice getMidiDevice() {
            return AbstractMidiDevice.this;
        }

    } // class BasicTransmitter

    /**
     * a class to manage a list of transmitters.
     */
    final class TransmitterList {

        private final ArrayList<Transmitter> transmitters = new ArrayList<>();
        private MidiOutDevice.MidiOutReceiver midiOutReceiver;

        // how many transmitters must be present for optimized
        // handling
        private int optimizedReceiverCount = 0;


        private void add(Transmitter t) {
            synchronized(transmitters) {
                transmitters.add(t);
            }
            if (t instanceof BasicTransmitter) {
                ((BasicTransmitter) t).setTransmitterList(this);
            }
        }

        private void remove(Transmitter t) {
            synchronized(transmitters) {
                int index = transmitters.indexOf(t);
                if (index >= 0) {
                    transmitters.remove(index);
                }
            }
        }

        private void receiverChanged(BasicTransmitter t,
                                     Receiver oldR,
                                     Receiver newR) {
            synchronized(transmitters) {
                // some optimization
                if (midiOutReceiver == oldR) {
                    midiOutReceiver = null;
                }
                if (newR != null) {
                    if ((newR instanceof MidiOutDevice.MidiOutReceiver)
                        && (midiOutReceiver == null)) {
                        midiOutReceiver = ((MidiOutDevice.MidiOutReceiver) newR);
                    }
                }
                optimizedReceiverCount =
                      ((midiOutReceiver!=null)?1:0);
            }
            // more potential for optimization here
        }


        /** closes all transmitters and empties the list */
        void close() {
            synchronized (transmitters) {
                for(int i = 0; i < transmitters.size(); i++) {
                    transmitters.get(i).close();
                }
                transmitters.clear();
            }
        }



        /**
        * Send this message to all receivers
        * status = packedMessage & 0xFF
        * data1 = (packedMessage & 0xFF00) >> 8;
        * data1 = (packedMessage & 0xFF0000) >> 16;
        */
        void sendMessage(int packedMessage, long timeStamp) {
            try {
                synchronized(transmitters) {
                    int size = transmitters.size();
                    if (optimizedReceiverCount == size) {
                        if (midiOutReceiver != null) {
                            midiOutReceiver.sendPackedMidiMessage(packedMessage, timeStamp);
                        }
                    } else {
                        for (int i = 0; i < size; i++) {
                            Receiver receiver = transmitters.get(i).getReceiver();
                            if (receiver != null) {
                                if (optimizedReceiverCount > 0) {
                                    if (receiver instanceof MidiOutDevice.MidiOutReceiver) {
                                        ((MidiOutDevice.MidiOutReceiver) receiver).sendPackedMidiMessage(packedMessage, timeStamp);
                                    } else {
                                        receiver.send(new FastShortMessage(packedMessage), timeStamp);
                                    }
                                } else {
                                    receiver.send(new FastShortMessage(packedMessage), timeStamp);
                                }
                            }
                        }
                    }
                }
            } catch (InvalidMidiDataException e) {
                // this happens when invalid data comes over the wire. Ignore it.
            }
        }

        void sendMessage(byte[] data, long timeStamp) {
            try {
                synchronized(transmitters) {
                    int size = transmitters.size();
                    for (int i = 0; i < size; i++) {
                        Receiver receiver = transmitters.get(i).getReceiver();
                        if (receiver != null) {
                            //$$fb 2002-04-02: SysexMessages are mutable, so
                            // an application could change the contents of this object,
                            // or try to use the object later. So we can't get around object creation
                            // But the array need not be unique for each FastSysexMessage object,
                            // because it cannot be modified.
                            receiver.send(new FastSysexMessage(data), timeStamp);
                        }
                    }
                }
            } catch (InvalidMidiDataException e) {
                // this happens when invalid data comes over the wire. Ignore it.
                return;
            }
        }

        /**
        * Send this message to all transmitters.
        */
        void sendMessage(MidiMessage message, long timeStamp) {
            if (message instanceof FastShortMessage) {
                sendMessage(((FastShortMessage) message).getPackedMsg(), timeStamp);
                return;
            }
            synchronized(transmitters) {
                int size = transmitters.size();
                if (optimizedReceiverCount == size) {
                    if (midiOutReceiver != null) {
                        midiOutReceiver.send(message, timeStamp);
                    }
                } else {
                    for (int i = 0; i < size; i++) {
                        Receiver receiver = transmitters.get(i).getReceiver();
                        if (receiver != null) {
                            //$$fb 2002-04-02: ShortMessages are mutable, so
                            // an application could change the contents of this object,
                            // or try to use the object later.
                            // We violate this spec here, to avoid costly (and gc-intensive)
                            // object creation for potentially hundred of messages per second.
                            // The spec should be changed to allow Immutable MidiMessages
                            // (i.e. throws InvalidStateException or so in setMessage)
                            receiver.send(message, timeStamp);
                        }
                    }
                }
            }
        }
    } // TransmitterList
}
