/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.spi.Connection;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;

public class TargetVM implements Runnable {
    private Map<String, Packet> waitingQueue = new HashMap<>(32,0.75f);
    private volatile boolean shouldListen = true;
    private List<EventQueue> eventQueues = Collections.synchronizedList(new ArrayList<>(2));
    private VirtualMachineImpl vm;
    private Connection connection;
    private Thread readerThread;
    private EventController eventController = null;
    private boolean eventsHeld = false;

    /*
     * TO DO: The limit numbers below are somewhat arbitrary and should
     * be configurable in the future.
     */
    static private final int OVERLOADED_QUEUE = 10000;
    static private final int UNDERLOADED_QUEUE = 100;

    TargetVM(VirtualMachineImpl vm, Connection connection) {
        this.vm = vm;
        this.connection = connection;
        this.readerThread = new Thread(vm.threadGroupForJDI(),
                                       this, "JDI Target VM Interface");
        this.readerThread.setDaemon(true);
    }

    void start() {
        readerThread.start();
    }

    private void dumpPacket(Packet packet, boolean sending) {
        String direction = sending ? "Sending" : "Receiving";
        if (sending) {
            vm.printTrace(direction + " Command. id=" + packet.id +
                          ", length=" + packet.data.length +
                          ", commandSet=" + packet.cmdSet +
                          ", command=" + packet.cmd +
                          ", flags=" + packet.flags);
        } else {
            String type = (packet.flags & Packet.Reply) != 0 ?
                          "Reply" : "Event";
            vm.printTrace(direction + " " + type + ". id=" + packet.id +
                          ", length=" + packet.data.length +
                          ", errorCode=" + packet.errorCode +
                          ", flags=" + packet.flags);
        }
        StringBuilder line = new StringBuilder(80);
        line.append("0000: ");
        for (int i = 0; i < packet.data.length; i++) {
            if ((i > 0) && (i % 16 == 0)) {
                vm.printTrace(line.toString());
                line.setLength(0);
                line.append(String.valueOf(i));
                line.append(": ");
                int len = line.length();
                for (int j = 0; j < 6 - len; j++) {
                    line.insert(0, '0');
                }
            }
            int val = 0xff & packet.data[i];
            String str = Integer.toHexString(val);
            if (str.length() == 1) {
                line.append('0');
            }
            line.append(str);
            line.append(' ');
        }
        if (line.length() > 6) {
            vm.printTrace(line.toString());
        }
    }

    public void run() {
        if ((vm.traceFlags & VirtualMachine.TRACE_SENDS) != 0) {
            vm.printTrace("Target VM interface thread running");
        }
        Packet p=null,p2;
        String idString;

        while (shouldListen) {

            boolean done = false;
            try {
                byte b[] = connection.readPacket();
                if (b.length == 0) {
                    done = true;
                }
                p = Packet.fromByteArray(b);
            } catch (IOException e) {
                done = true;
            }

            if (done) {
                shouldListen = false;
                try {
                    connection.close();
                } catch (IOException ioe) { }
                break;
            }

            if ((vm.traceFlags & VirtualMachineImpl.TRACE_RAW_RECEIVES) != 0)  {
                dumpPacket(p, false);
            }

            if ((p.flags & Packet.Reply) == 0) {
                // It's a command
                handleVMCommand(p);
            } else {
                /*if(p.errorCode != Packet.ReplyNoError) {
                    System.err.println("Packet " + p.id + " returned failure = " + p.errorCode);
                }*/

                vm.state().notifyCommandComplete(p.id);
                idString = String.valueOf(p.id);

                synchronized(waitingQueue) {
                    p2 = waitingQueue.get(idString);

                    if (p2 != null)
                        waitingQueue.remove(idString);
                }

                if (p2 == null) {
                    // Whoa! a reply without a sender. Problem.
                    // FIX ME! Need to post an error.

                    System.err.println("Recieved reply with no sender!");
                    continue;
                }
                p2.errorCode = p.errorCode;
                p2.data = p.data;
                p2.replied = true;

                synchronized(p2) {
                    p2.notify();
                }
            }
        }

        // inform the VM mamager that this VM is history
        vm.vmManager.disposeVirtualMachine(vm);
        if (eventController != null) {
            eventController.release();
        }

        // close down all the event queues
        // Closing a queue causes a VMDisconnectEvent to
        // be put onto the queue.
        synchronized(eventQueues) {
            Iterator<EventQueue> iter = eventQueues.iterator();
            while (iter.hasNext()) {
                ((EventQueueImpl)iter.next()).close();
            }
        }

        // indirectly throw VMDisconnectedException to
        // command requesters.
        synchronized(waitingQueue) {
            Iterator<Packet> iter = waitingQueue.values().iterator();
            while (iter.hasNext()) {
                Packet packet = iter.next();
                synchronized(packet) {
                    packet.notify();
                }
            }
            waitingQueue.clear();
        }

        if ((vm.traceFlags & VirtualMachine.TRACE_SENDS) != 0) {
            vm.printTrace("Target VM interface thread exiting");
        }
    }

    protected void handleVMCommand(Packet p) {
        switch (p.cmdSet) {
            case JDWP.Event.COMMAND_SET:
                handleEventCmdSet(p);
                break;

            default:
                System.err.println("Ignoring cmd " + p.id + "/" +
                                   p.cmdSet + "/" + p.cmd + " from the VM");
                return;
        }
    }

    /* Events should not be constructed on this thread (the thread
     * which reads all data from the transport). This means that the
     * packet cannot be converted to real JDI objects as that may
     * involve further communications with the back end which would
     * deadlock.
     *
     * Instead the whole packet is passed for lazy eval by a queue
     * reading thread.
     */
    protected void handleEventCmdSet(Packet p) {
        EventSet eventSet = new EventSetImpl(vm, p);
        queueEventSet(eventSet);
    }

    private EventController eventController() {
        if (eventController == null) {
            eventController = new EventController();
        }
        return eventController;
    }

    private synchronized void controlEventFlow(int maxQueueSize) {
        if (!eventsHeld && (maxQueueSize > OVERLOADED_QUEUE)) {
            eventController().hold();
            eventsHeld = true;
        } else if (eventsHeld && (maxQueueSize < UNDERLOADED_QUEUE)) {
            eventController().release();
            eventsHeld = false;
        }
    }

    void notifyDequeueEventSet() {
        int maxQueueSize = 0;
        synchronized(eventQueues) {
            Iterator<EventQueue> iter = eventQueues.iterator();
            while (iter.hasNext()) {
                EventQueueImpl queue = (EventQueueImpl)iter.next();
                maxQueueSize = Math.max(maxQueueSize, queue.size());
            }
        }
        controlEventFlow(maxQueueSize);
    }

    private void queueEventSet(EventSet eventSet) {
        int maxQueueSize = 0;

        synchronized(eventQueues) {
            Iterator<EventQueue> iter = eventQueues.iterator();
            while (iter.hasNext()) {
                EventQueueImpl queue = (EventQueueImpl)iter.next();
                queue.enqueue(eventSet);
                maxQueueSize = Math.max(maxQueueSize, queue.size());
            }
        }

        controlEventFlow(maxQueueSize);
    }

    void send(Packet packet) {
        String id = String.valueOf(packet.id);

        synchronized(waitingQueue) {
            waitingQueue.put(id, packet);
        }

        if ((vm.traceFlags & VirtualMachineImpl.TRACE_RAW_SENDS) != 0) {
            dumpPacket(packet, true);
        }

        try {
            connection.writePacket(packet.toByteArray());
        } catch (IOException e) {
            throw new VMDisconnectedException(e.getMessage());
        }
    }

    void waitForReply(Packet packet) {
        synchronized(packet) {
            while ((!packet.replied) && shouldListen) {
                try { packet.wait(); } catch (InterruptedException e) {;}
            }

            if (!packet.replied) {
                throw new VMDisconnectedException();
            }
        }
    }

    void addEventQueue(EventQueueImpl queue) {
        if ((vm.traceFlags & VirtualMachine.TRACE_EVENTS) != 0) {
            vm.printTrace("New event queue added");
        }
        eventQueues.add(queue);
    }

    void stopListening() {
        if ((vm.traceFlags & VirtualMachine.TRACE_EVENTS) != 0) {
            vm.printTrace("Target VM i/f closing event queues");
        }
        shouldListen = false;
        try {
            connection.close();
        } catch (IOException ioe) { }
    }

    private class EventController extends Thread {
        int controlRequest = 0;

        EventController() {
            super(vm.threadGroupForJDI(), "JDI Event Control Thread");
            setDaemon(true);
            setPriority((MAX_PRIORITY + NORM_PRIORITY)/2);
            super.start();
        }

        synchronized void hold() {
            controlRequest++;
            notifyAll();
        }

        synchronized void release() {
            controlRequest--;
            notifyAll();
        }

        public void run() {
            while(true) {
                int currentRequest;
                synchronized(this) {
                    while (controlRequest == 0) {
                        try {wait();} catch (InterruptedException e) {}
                        if (!shouldListen) {
                           return;
                        }
                    }
                    currentRequest = controlRequest;
                    controlRequest = 0;
                }
                try {
                    if (currentRequest > 0) {
                        JDWP.VirtualMachine.HoldEvents.process(vm);
                    } else {
                        JDWP.VirtualMachine.ReleaseEvents.process(vm);
                    }
                } catch (JDWPException e) {
                    /*
                     * Don't want to terminate the thread, so the
                     * stack trace is printed and we continue.
                     */
                    e.toJDIException().printStackTrace(System.err);
                }
            }
        }
    }
}
