/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InternalException;
import com.sun.jdi.InvalidStackFrameException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.Location;
import com.sun.jdi.MonitorInfo;
import com.sun.jdi.NativeMethodException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;

public class ThreadReferenceImpl extends ObjectReferenceImpl
                                 implements ThreadReference {
    static final int SUSPEND_STATUS_SUSPENDED = 0x1;
    static final int SUSPEND_STATUS_BREAK = 0x2;

    private int suspendedZombieCount = 0;

    /*
     * Some objects can only be created while a thread is suspended and are valid
     * only while the thread remains suspended.  Examples are StackFrameImpl
     * and MonitorInfoImpl.  When the thread resumes, these objects have to be
     * marked as invalid so that their methods can throw
     * InvalidStackFrameException if they are called.  To do this, such objects
     * register themselves as listeners of the associated thread.  When the
     * thread is resumed, its listeners are notified and mark themselves
     * invalid.
     * Also, note that ThreadReferenceImpl itself caches some info that
     * is valid only as long as the thread is suspended.  When the thread
     * is resumed, that cache must be purged.
     * Lastly, note that ThreadReferenceImpl and its super, ObjectReferenceImpl
     * cache some info that is only valid as long as the entire VM is suspended.
     * If _any_ thread is resumed, this cache must be purged.  To handle this,
     * both ThreadReferenceImpl and ObjectReferenceImpl register themselves as
     * VMListeners so that they get notified when all threads are suspended and
     * when any thread is resumed.
     */

    // This is cached for the life of the thread
    private ThreadGroupReference threadGroup;

    // This is cached only while this one thread is suspended.  Each time
    // the thread is resumed, we abandon the current cache object and
    // create a new initialized one.
    private static class LocalCache {
        JDWP.ThreadReference.Status status = null;
        List<StackFrame> frames = null;
        int framesStart = -1;
        int framesLength = 0;
        int frameCount = -1;
        List<ObjectReference> ownedMonitors = null;
        List<MonitorInfo> ownedMonitorsInfo = null;
        ObjectReference contendedMonitor = null;
        boolean triedCurrentContended = false;
    }

    /*
     * The localCache instance var is set by resetLocalCache to an initialized
     * object as shown above.  This occurs when the ThreadReference
     * object is created, and when the mirrored thread is resumed.
     * The fields are then filled in by the relevant methods as they
     * are called.  A problem can occur if resetLocalCache is called
     * (ie, a resume() is executed) at certain points in the execution
     * of some of these methods - see 6751643.  To avoid this, each
     * method that wants to use this cache must make a local copy of
     * this variable and use that.  This means that each invocation of
     * these methods will use a copy of the cache object that was in
     * effect at the point that the copy was made; if a racy resume
     * occurs, it won't affect the method's local copy.  This means that
     * the values returned by these calls may not match the state of
     * the debuggee at the time the caller gets the values.  EG,
     * frameCount() is called and comes up with 5 frames.  But before
     * it returns this, a resume of the debuggee thread is executed in a
     * different debugger thread.  The thread is resumed and running at
     * the time that the value 5 is returned.  Or even worse, the thread
     * could be suspended again and have a different number of frames, eg, 24,
     * but this call will still return 5.
     */
    private LocalCache localCache;

    private void resetLocalCache() {
        localCache = new LocalCache();
    }

    // This is cached only while all threads in the VM are suspended
    // Yes, someone could change the name of a thread while it is suspended.
    private static class Cache extends ObjectReferenceImpl.Cache {
        String name = null;
    }
    protected ObjectReferenceImpl.Cache newCache() {
        return new Cache();
    }

    // Listeners - synchronized on vm.state()
    private List<WeakReference<ThreadListener>> listeners = new ArrayList<>();

    ThreadReferenceImpl(VirtualMachine aVm, long aRef) {
        super(aVm, aRef);
        resetLocalCache();
        vm.state().addListener(this);
    }

    protected String description() {
        return "ThreadReference " + uniqueID();
    }

    /*
     * VMListener implementation
     */
    public boolean vmNotSuspended(VMAction action) {
        if (action.resumingThread() == null) {
            // all threads are being resumed
            synchronized (vm.state()) {
                processThreadAction(new ThreadAction(this,
                                            ThreadAction.THREAD_RESUMABLE));
            }

        }

        /*
         * Othewise, only one thread is being resumed:
         *   if it is us,
         *      we have already done our processThreadAction to notify our
         *      listeners when we processed the resume.
         *   if it is not us,
         *      we don't want to notify our listeners
         *       because we are not being resumed.
         */
        return super.vmNotSuspended(action);
    }

    /**
     * Note that we only cache the name string while the entire VM is suspended
     * because the name can change via Thread.setName arbitrarily while this
     * thread is running.
     */
    public String name() {
        String name = null;
        try {
            Cache local = (Cache)getCache();

            if (local != null) {
                name = local.name;
            }
            if (name == null) {
                name = JDWP.ThreadReference.Name.process(vm, this).threadName;
                if (local != null) {
                    local.name = name;
                }
            }
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
        return name;
    }

    /*
     * Sends a command to the back end which is defined to do an
     * implicit vm-wide resume.
     */
    PacketStream sendResumingCommand(CommandSender sender) {
        synchronized (vm.state()) {
            processThreadAction(new ThreadAction(this,
                                        ThreadAction.THREAD_RESUMABLE));
            return sender.send();
        }
    }

    public void suspend() {
        try {
            JDWP.ThreadReference.Suspend.process(vm, this);
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
        // Don't consider the thread suspended yet. On reply, notifySuspend()
        // will be called.
    }

    public void resume() {
        /*
         * If it's a zombie, we can just update internal state without
         * going to back end.
         */
        if (suspendedZombieCount > 0) {
            suspendedZombieCount--;
            return;
        }

        PacketStream stream;
        synchronized (vm.state()) {
            processThreadAction(new ThreadAction(this,
                                      ThreadAction.THREAD_RESUMABLE));
            stream = JDWP.ThreadReference.Resume.enqueueCommand(vm, this);
        }
        try {
            JDWP.ThreadReference.Resume.waitForReply(vm, stream);
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
    }

    public int suspendCount() {
        /*
         * If it's a zombie, we maintain the count in the front end.
         */
        if (suspendedZombieCount > 0) {
            return suspendedZombieCount;
        }

        try {
            return JDWP.ThreadReference.SuspendCount.process(vm, this).suspendCount;
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
    }

    public void stop(ObjectReference throwable) throws InvalidTypeException {
        validateMirrorOrNull(throwable);
        // Verify that the given object is a Throwable instance
        List<ReferenceType> list = vm.classesByName("java.lang.Throwable");
        ClassTypeImpl throwableClass = (ClassTypeImpl)list.get(0);
        if ((throwable == null) ||
            !throwableClass.isAssignableFrom(throwable)) {
             throw new InvalidTypeException("Not an instance of Throwable");
        }

        try {
            JDWP.ThreadReference.Stop.process(vm, this,
                                         (ObjectReferenceImpl)throwable);
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
    }

    public void interrupt() {
        try {
            JDWP.ThreadReference.Interrupt.process(vm, this);
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
    }

    private JDWP.ThreadReference.Status jdwpStatus() {
        LocalCache snapshot = localCache;
        JDWP.ThreadReference.Status myStatus = snapshot.status;
        try {
             if (myStatus == null) {
                 myStatus = JDWP.ThreadReference.Status.process(vm, this);
                if ((myStatus.suspendStatus & SUSPEND_STATUS_SUSPENDED) != 0) {
                    // thread is suspended, we can cache the status.
                    snapshot.status = myStatus;
                }
            }
         } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
        return myStatus;
    }

    public int status() {
        return jdwpStatus().threadStatus;
    }

    public boolean isSuspended() {
        return ((suspendedZombieCount > 0) ||
                ((jdwpStatus().suspendStatus & SUSPEND_STATUS_SUSPENDED) != 0));
    }

    public boolean isAtBreakpoint() {
        /*
         * TO DO: This fails to take filters into account.
         */
        try {
            StackFrame frame = frame(0);
            Location location = frame.location();
            List<BreakpointRequest> requests = vm.eventRequestManager().breakpointRequests();
            Iterator<BreakpointRequest> iter = requests.iterator();
            while (iter.hasNext()) {
                BreakpointRequest request = iter.next();
                if (location.equals(request.location())) {
                    return true;
                }
            }
            return false;
        } catch (IndexOutOfBoundsException iobe) {
            return false;  // no frames on stack => not at breakpoint
        } catch (IncompatibleThreadStateException itse) {
            // Per the javadoc, not suspended => return false
            return false;
        }
    }

    public ThreadGroupReference threadGroup() {
        /*
         * Thread group can't change, so it's cached once and for all.
         */
        if (threadGroup == null) {
            try {
                threadGroup = JDWP.ThreadReference.ThreadGroup.
                    process(vm, this).group;
            } catch (JDWPException exc) {
                throw exc.toJDIException();
            }
        }
        return threadGroup;
    }

    public int frameCount() throws IncompatibleThreadStateException  {
        LocalCache snapshot = localCache;
        try {
            if (snapshot.frameCount == -1) {
                snapshot.frameCount = JDWP.ThreadReference.FrameCount
                                          .process(vm, this).frameCount;
            }
        } catch (JDWPException exc) {
            switch (exc.errorCode()) {
            case JDWP.Error.THREAD_NOT_SUSPENDED:
            case JDWP.Error.INVALID_THREAD:   /* zombie */
                throw new IncompatibleThreadStateException();
            default:
                throw exc.toJDIException();
            }
        }
        return snapshot.frameCount;
    }

    public List<StackFrame> frames() throws IncompatibleThreadStateException  {
        return privateFrames(0, -1);
    }

    public StackFrame frame(int index) throws IncompatibleThreadStateException  {
        List<StackFrame> list = privateFrames(index, 1);
        return list.get(0);
    }

    /**
     * Is the requested subrange within what has been retrieved?
     * local is known to be non-null.  Should only be called from
     * a sync method.
     */
    private boolean isSubrange(LocalCache snapshot,
                               int start, int length) {
        if (start < snapshot.framesStart) {
            return false;
        }
        if (length == -1) {
            return (snapshot.framesLength == -1);
        }
        if (snapshot.framesLength == -1) {
            if ((start + length) > (snapshot.framesStart +
                                    snapshot.frames.size())) {
                throw new IndexOutOfBoundsException();
            }
            return true;
        }
        return ((start + length) <= (snapshot.framesStart + snapshot.framesLength));
    }

    public List<StackFrame> frames(int start, int length)
                              throws IncompatibleThreadStateException  {
        if (length < 0) {
            throw new IndexOutOfBoundsException(
                "length must be greater than or equal to zero");
        }
        return privateFrames(start, length);
    }

    /**
     * Private version of frames() allows "-1" to specify all
     * remaining frames.
     */
    synchronized private List<StackFrame> privateFrames(int start, int length)
                              throws IncompatibleThreadStateException  {

        // Lock must be held while creating stack frames so if that two threads
        // do this at the same time, one won't clobber the subset created by the other.
        LocalCache snapshot = localCache;
        try {
            if (snapshot.frames == null || !isSubrange(snapshot, start, length)) {
                JDWP.ThreadReference.Frames.Frame[] jdwpFrames
                    = JDWP.ThreadReference.Frames.
                    process(vm, this, start, length).frames;
                int count = jdwpFrames.length;
                snapshot.frames = new ArrayList<>(count);

                for (int i = 0; i<count; i++) {
                    if (jdwpFrames[i].location == null) {
                        throw new InternalException("Invalid frame location");
                    }
                    StackFrame frame = new StackFrameImpl(vm, this,
                                                          jdwpFrames[i].frameID,
                                                          jdwpFrames[i].location);
                    // Add to the frame list
                    snapshot.frames.add(frame);
                }
                snapshot.framesStart = start;
                snapshot.framesLength = length;
                return Collections.unmodifiableList(snapshot.frames);
            } else {
                int fromIndex = start - snapshot.framesStart;
                int toIndex;
                if (length == -1) {
                    toIndex = snapshot.frames.size() - fromIndex;
                } else {
                    toIndex = fromIndex + length;
                }
                return Collections.unmodifiableList(snapshot.frames.subList(fromIndex, toIndex));
            }
        } catch (JDWPException exc) {
            switch (exc.errorCode()) {
            case JDWP.Error.THREAD_NOT_SUSPENDED:
            case JDWP.Error.INVALID_THREAD:   /* zombie */
                throw new IncompatibleThreadStateException();
            default:
                throw exc.toJDIException();
            }
        }
    }

    public List<ObjectReference> ownedMonitors()  throws IncompatibleThreadStateException  {
        LocalCache snapshot = localCache;
        try {
            if (snapshot.ownedMonitors == null) {
                snapshot.ownedMonitors = Arrays.asList(
                                 (ObjectReference[])JDWP.ThreadReference.OwnedMonitors.
                                         process(vm, this).owned);
                if ((vm.traceFlags & VirtualMachine.TRACE_OBJREFS) != 0) {
                    vm.printTrace(description() +
                                  " temporarily caching owned monitors"+
                                  " (count = " + snapshot.ownedMonitors.size() + ")");
                }
            }
        } catch (JDWPException exc) {
            switch (exc.errorCode()) {
            case JDWP.Error.THREAD_NOT_SUSPENDED:
            case JDWP.Error.INVALID_THREAD:   /* zombie */
                throw new IncompatibleThreadStateException();
            default:
                throw exc.toJDIException();
            }
        }
        return snapshot.ownedMonitors;
    }

    public ObjectReference currentContendedMonitor()
                              throws IncompatibleThreadStateException  {
        LocalCache snapshot = localCache;
        try {
            if (snapshot.contendedMonitor == null &&
                !snapshot.triedCurrentContended) {
                snapshot.contendedMonitor = JDWP.ThreadReference.CurrentContendedMonitor.
                    process(vm, this).monitor;
                snapshot.triedCurrentContended = true;
                if ((snapshot.contendedMonitor != null) &&
                    ((vm.traceFlags & VirtualMachine.TRACE_OBJREFS) != 0)) {
                    vm.printTrace(description() +
                                  " temporarily caching contended monitor"+
                                  " (id = " + snapshot.contendedMonitor.uniqueID() + ")");
                }
            }
        } catch (JDWPException exc) {
            switch (exc.errorCode()) {
            case JDWP.Error.THREAD_NOT_SUSPENDED:
            case JDWP.Error.INVALID_THREAD:   /* zombie */
                throw new IncompatibleThreadStateException();
            default:
                throw exc.toJDIException();
            }
        }
        return snapshot.contendedMonitor;
    }

    public List<MonitorInfo> ownedMonitorsAndFrames()  throws IncompatibleThreadStateException  {
        LocalCache snapshot = localCache;
        try {
            if (snapshot.ownedMonitorsInfo == null) {
                JDWP.ThreadReference.OwnedMonitorsStackDepthInfo.monitor[] minfo;
                minfo = JDWP.ThreadReference.OwnedMonitorsStackDepthInfo.process(vm, this).owned;

                snapshot.ownedMonitorsInfo = new ArrayList<>(minfo.length);

                for (int i=0; i < minfo.length; i++) {
                    MonitorInfo mon = new MonitorInfoImpl(vm, minfo[i].monitor, this, minfo[i].stack_depth);
                    snapshot.ownedMonitorsInfo.add(mon);
                }

                if ((vm.traceFlags & VirtualMachine.TRACE_OBJREFS) != 0) {
                    vm.printTrace(description() +
                                  " temporarily caching owned monitors"+
                                  " (count = " + snapshot.ownedMonitorsInfo.size() + ")");
                    }
                }

        } catch (JDWPException exc) {
            switch (exc.errorCode()) {
            case JDWP.Error.THREAD_NOT_SUSPENDED:
            case JDWP.Error.INVALID_THREAD:   /* zombie */
                throw new IncompatibleThreadStateException();
            default:
                throw exc.toJDIException();
            }
        }
        return snapshot.ownedMonitorsInfo;
    }

    public void popFrames(StackFrame frame) throws IncompatibleThreadStateException {
        // Note that interface-wise this functionality belongs
        // here in ThreadReference, but implementation-wise it
        // belongs in StackFrame, so we just forward it.
        if (!frame.thread().equals(this)) {
            throw new IllegalArgumentException("frame does not belong to this thread");
        }
        if (!vm.canPopFrames()) {
            throw new UnsupportedOperationException(
                "target does not support popping frames");
        }
        ((StackFrameImpl)frame).pop();
    }

    public void forceEarlyReturn(Value returnValue) throws InvalidTypeException,
                                                           ClassNotLoadedException,
                                             IncompatibleThreadStateException {
        if (!vm.canForceEarlyReturn()) {
            throw new UnsupportedOperationException(
                "target does not support the forcing of a method to return early");
        }

        validateMirrorOrNull(returnValue);

        StackFrameImpl sf;
        try {
           sf = (StackFrameImpl)frame(0);
        } catch (IndexOutOfBoundsException exc) {
           throw new InvalidStackFrameException("No more frames on the stack");
        }
        sf.validateStackFrame();
        MethodImpl meth = (MethodImpl)sf.location().method();
        ValueImpl convertedValue  = ValueImpl.prepareForAssignment(returnValue,
                                                                   meth.getReturnValueContainer());

        try {
            JDWP.ThreadReference.ForceEarlyReturn.process(vm, this, convertedValue);
        } catch (JDWPException exc) {
            switch (exc.errorCode()) {
            case JDWP.Error.OPAQUE_FRAME:
                throw new NativeMethodException();
            case JDWP.Error.THREAD_NOT_SUSPENDED:
                throw new IncompatibleThreadStateException(
                         "Thread not suspended");
            case JDWP.Error.THREAD_NOT_ALIVE:
                throw new IncompatibleThreadStateException(
                                     "Thread has not started or has finished");
            case JDWP.Error.NO_MORE_FRAMES:
                throw new InvalidStackFrameException(
                         "No more frames on the stack");
            default:
                throw exc.toJDIException();
            }
        }
    }

    public String toString() {
        return "instance of " + referenceType().name() +
               "(name='" + name() + "', " + "id=" + uniqueID() + ")";
    }

    byte typeValueKey() {
        return JDWP.Tag.THREAD;
    }

    void addListener(ThreadListener listener) {
        synchronized (vm.state()) {
            listeners.add(new WeakReference<>(listener));
        }
    }

    void removeListener(ThreadListener listener) {
        synchronized (vm.state()) {
            Iterator<WeakReference<ThreadListener>> iter = listeners.iterator();
            while (iter.hasNext()) {
                WeakReference<ThreadListener> ref = iter.next();
                if (listener.equals(ref.get())) {
                    iter.remove();
                    break;
                }
            }
        }
    }

    /**
     * Propagate the thread state change information
     * to registered listeners.
     * Must be entered while synchronized on vm.state()
     */
    private void processThreadAction(ThreadAction action) {
        synchronized (vm.state()) {
            Iterator<WeakReference<ThreadListener>> iter = listeners.iterator();
            while (iter.hasNext()) {
                WeakReference<ThreadListener> ref = iter.next();
                ThreadListener listener = ref.get();
                if (listener != null) {
                    switch (action.id()) {
                        case ThreadAction.THREAD_RESUMABLE:
                            if (!listener.threadResumable(action)) {
                                iter.remove();
                            }
                            break;
                    }
                } else {
                    // Listener is unreachable; clean up
                    iter.remove();
                }
            }

            // Discard our local cache
            resetLocalCache();
        }
    }
}
