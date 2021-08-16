/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.management.jfr;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.management.AttributeChangeNotification;
import javax.management.AttributeNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.StandardEmitterMBean;

import jdk.jfr.Configuration;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.FlightRecorderListener;
import jdk.jfr.FlightRecorderPermission;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.jfr.internal.management.ManagementSupport;
import jdk.jfr.internal.management.StreamManager;

// Instantiated by service provider
final class FlightRecorderMXBeanImpl extends StandardEmitterMBean implements FlightRecorderMXBean, NotificationEmitter {

    final class MXBeanListener implements FlightRecorderListener {
        private final NotificationListener listener;
        private final NotificationFilter filter;
        private final Object handback;
        @SuppressWarnings("removal")
        private final AccessControlContext context;

        @SuppressWarnings("removal")
        public MXBeanListener(NotificationListener listener, NotificationFilter filter, Object handback) {
            this.context = AccessController.getContext();
            this.listener = listener;
            this.filter = filter;
            this.handback = handback;
        }

        @SuppressWarnings("removal")
        public void recordingStateChanged(Recording recording) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    sendNotification(createNotication(recording));
                    return null;
                }
            }, context);
        }
    }

    private static final String ATTRIBUTE_RECORDINGS = "Recordings";
    private static final String OPTION_MAX_SIZE = "maxSize";
    private static final String OPTION_MAX_AGE = "maxAge";
    private static final String OPTION_NAME = "name";
    private static final String OPTION_DISK = "disk";
    private static final String OPTION_DUMP_ON_EXIT = "dumpOnExit";
    private static final String OPTION_DURATION = "duration";
    private static final String OPTION_DESTINATION = "destination";
    private static final List<String> OPTIONS = Arrays.asList(new String[] { OPTION_DUMP_ON_EXIT, OPTION_DURATION, OPTION_NAME, OPTION_MAX_AGE, OPTION_MAX_SIZE, OPTION_DISK, OPTION_DESTINATION, });
    private final StreamManager streamHandler = new StreamManager();
    private final Map<Long, Object> changes = new ConcurrentHashMap<>();
    private final AtomicLong sequenceNumber = new AtomicLong();
    private final List<MXBeanListener> listeners = new CopyOnWriteArrayList<>();
    private FlightRecorder recorder;

    FlightRecorderMXBeanImpl() {
        super(FlightRecorderMXBean.class, true, new NotificationBroadcasterSupport(createNotificationInfo()));
    }

    @Override
    public void startRecording(long id) {
        MBeanUtils.checkControl();
        getExistingRecording(id).start();
    }

    @Override
    public boolean stopRecording(long id) {
        MBeanUtils.checkControl();
        return getExistingRecording(id).stop();
    }

    @Override
    public void closeRecording(long id) {
        MBeanUtils.checkControl();
        getExistingRecording(id).close();
    }

    @Override
    public long openStream(long id, Map<String, String> options) throws IOException {
        MBeanUtils.checkControl();
        if (!FlightRecorder.isInitialized()) {
            throw new IllegalArgumentException("No recording available with id " + id);
        }
        // Make local copy to prevent concurrent modification
        Map<String, String> s = options == null ? new HashMap<>() : new HashMap<>(options);
        Instant starttime = MBeanUtils.parseTimestamp(s.get("startTime"), Instant.MIN);
        Instant endtime = MBeanUtils.parseTimestamp(s.get("endTime"), Instant.MAX);
        int blockSize = MBeanUtils.parseBlockSize(s.get("blockSize"), StreamManager.DEFAULT_BLOCK_SIZE);
        String version = s.get("streamVersion");
        if (version != null) {
            if ("1.0".equals(version)) {
                Recording r = getRecording(id);
                return streamHandler.createOngoing(r, blockSize, starttime, endtime).getId();
            }
            throw new IllegalArgumentException("Unsupported stream version " + version);
        }

        InputStream is = getExistingRecording(id).getStream(starttime, endtime);
        if (is == null) {
            throw new IOException("No recording data available");
        }
        return streamHandler.create(is, blockSize).getId();
    }

    @Override
    public void closeStream(long streamIdentifier) throws IOException {
        MBeanUtils.checkControl();
        streamHandler.getStream(streamIdentifier).close();
    }

    @Override
    public byte[] readStream(long streamIdentifier) throws IOException {
        MBeanUtils.checkMonitor();
        return streamHandler.getStream(streamIdentifier).read();
    }

    @Override
    public List<RecordingInfo> getRecordings() {
        MBeanUtils.checkMonitor();
        if (!FlightRecorder.isInitialized()) {
            return Collections.emptyList();
        }
        return MBeanUtils.transformList(getRecorder().getRecordings(), RecordingInfo::new);
    }

    @Override
    public List<ConfigurationInfo> getConfigurations() {
        MBeanUtils.checkMonitor();
        return MBeanUtils.transformList(Configuration.getConfigurations(), ConfigurationInfo::new);
    }

    @Override
    public List<EventTypeInfo> getEventTypes() {
        MBeanUtils.checkMonitor();
        @SuppressWarnings("removal")
        List<EventType> eventTypes = AccessController.doPrivileged(new PrivilegedAction<List<EventType>>() {
            @Override
            public List<EventType> run() {
                return ManagementSupport.getEventTypes();
            }
        }, null, new FlightRecorderPermission("accessFlightRecorder"));

        return MBeanUtils.transformList(eventTypes, EventTypeInfo::new);
    }

    @Override
    public Map<String, String> getRecordingSettings(long recording) throws IllegalArgumentException {
        MBeanUtils.checkMonitor();
        return getExistingRecording(recording).getSettings();
    }

    @Override
    public void setRecordingSettings(long recording, Map<String, String> values) throws IllegalArgumentException {
        Objects.requireNonNull(values);
        MBeanUtils.checkControl();
        getExistingRecording(recording).setSettings(values);
    }

    @SuppressWarnings("removal")
    @Override
    public long newRecording() {
        MBeanUtils.checkControl();
        getRecorder(); // ensure notification listener is setup
        return AccessController.doPrivileged(new PrivilegedAction<Recording>() {
            @Override
            public Recording run() {
                return new Recording();
            }
        }, null, new FlightRecorderPermission("accessFlightRecorder")).getId();
    }

    @Override
    public long takeSnapshot() {
        MBeanUtils.checkControl();
        return getRecorder().takeSnapshot().getId();
    }

    @Override
    public void setConfiguration(long recording, String configuration) throws IllegalArgumentException {
        Objects.requireNonNull(configuration);
        MBeanUtils.checkControl();
        try {
            Configuration c = Configuration.create(new StringReader(configuration));
            getExistingRecording(recording).setSettings(c.getSettings());
        } catch (IOException | ParseException e) {
            throw new IllegalArgumentException("Could not parse configuration", e);
        }
    }

    @Override
    public void setPredefinedConfiguration(long recording, String configurationName) throws IllegalArgumentException {
        Objects.requireNonNull(configurationName);
        MBeanUtils.checkControl();
        Recording r = getExistingRecording(recording);
        for (Configuration c : Configuration.getConfigurations()) {
            if (c.getName().equals(configurationName)) {
                r.setSettings(c.getSettings());
                return;
            }
        }
        throw new IllegalArgumentException("Could not find configuration with name " + configurationName);
    }

    @Override
    public void copyTo(long recording, String path) throws IOException {
        Objects.requireNonNull(path);
        MBeanUtils.checkControl();
        getExistingRecording(recording).dump(Paths.get(path));
    }

    @Override
    public void setRecordingOptions(long recording, Map<String, String> options) throws IllegalArgumentException {
        Objects.requireNonNull(options);
        MBeanUtils.checkControl();
        // Make local copy to prevent concurrent modification
        Map<String, String> ops = new HashMap<String, String>(options);
        for (Map.Entry<String, String> entry : ops.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (!(key instanceof String)) {
                throw new IllegalArgumentException("Option key must not be null, or other type than " + String.class);
            }
            if (!OPTIONS.contains(key)) {
                throw new IllegalArgumentException("Unknown recording option: " + key + ". Valid options are " + OPTIONS + ".");
            }
            if (value != null && !(value instanceof String)) {
                throw new IllegalArgumentException("Incorrect value for option " + key + ". Values must be of type " + String.class + " .");
            }
        }

        Recording r = getExistingRecording(recording);
        validateOption(ops, OPTION_DUMP_ON_EXIT, MBeanUtils::booleanValue);
        validateOption(ops, OPTION_DISK, MBeanUtils::booleanValue);
        validateOption(ops, OPTION_NAME, Function.identity());
        validateOption(ops, OPTION_MAX_AGE, MBeanUtils::duration);
        validateOption(ops, OPTION_MAX_SIZE, MBeanUtils::size);
        validateOption(ops, OPTION_DURATION, MBeanUtils::duration);
        validateOption(ops, OPTION_DESTINATION, x -> MBeanUtils.destination(r, x));

        // All OK, now set them.atomically
        setOption(ops, OPTION_DUMP_ON_EXIT, "false", MBeanUtils::booleanValue, x -> r.setDumpOnExit(x));
        setOption(ops, OPTION_DISK, "true", MBeanUtils::booleanValue, x -> r.setToDisk(x));
        setOption(ops, OPTION_NAME, String.valueOf(r.getId()), Function.identity(), x -> r.setName(x));
        setOption(ops, OPTION_MAX_AGE, null, MBeanUtils::duration, x -> r.setMaxAge(x));
        setOption(ops, OPTION_MAX_SIZE, "0", MBeanUtils::size, x -> r.setMaxSize(x));
        setOption(ops, OPTION_DURATION, null, MBeanUtils::duration, x -> r.setDuration(x));
        setOption(ops, OPTION_DESTINATION, null, x -> MBeanUtils.destination(r, x), x -> setOptionDestination(r, x));
    }

    @Override
    public Map<String, String> getRecordingOptions(long recording) throws IllegalArgumentException {
        MBeanUtils.checkMonitor();
        Recording r = getExistingRecording(recording);
        Map<String, String> options = new HashMap<>(10);
        options.put(OPTION_DUMP_ON_EXIT, String.valueOf(r.getDumpOnExit()));
        options.put(OPTION_DISK, String.valueOf(r.isToDisk()));
        options.put(OPTION_NAME, String.valueOf(r.getName()));
        options.put(OPTION_MAX_AGE, ManagementSupport.formatTimespan(r.getMaxAge(), " "));
        Long maxSize = r.getMaxSize();
        options.put(OPTION_MAX_SIZE, String.valueOf(maxSize == null ? "0" : maxSize.toString()));
        options.put(OPTION_DURATION, ManagementSupport.formatTimespan(r.getDuration(), " "));
        options.put(OPTION_DESTINATION, ManagementSupport.getDestinationOriginalText(r));
        return options;
    }

    @Override
    public long cloneRecording(long id, boolean stop) throws IllegalStateException, SecurityException {
        MBeanUtils.checkControl();
        return getRecording(id).copy(stop).getId();
    }

    @Override
    public ObjectName getObjectName() {
        return MBeanUtils.createObjectName();
    }

    private Recording getExistingRecording(long id) {
        if (FlightRecorder.isInitialized()) {
            Recording recording = getRecording(id);
            if (recording != null) {
                return recording;
            }
        }
        throw new IllegalArgumentException("No recording available with id " + id);
    }

    private Recording getRecording(long id) {
        List<Recording> recs = getRecorder().getRecordings();
        return recs.stream().filter(r -> r.getId() == id).findFirst().orElse(null);
    }

    private static <T, U> void setOption(Map<String, String> options, String name, String defaultValue, Function<String, U> converter, Consumer<U> setter) {
        if (!options.containsKey(name)) {
            return;
        }
        String v = options.get(name);
        if (v == null) {
            v = defaultValue;
        }
        try {
            setter.accept(converter.apply(v));
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Not a valid value for option '" + name + "'. " + iae.getMessage());
        }
    }

    private static void setOptionDestination(Recording recording, String destination){
        try {
            Path pathDestination = null;
            if(destination != null){
                pathDestination = Paths.get(destination);
            }
            recording.setDestination(pathDestination);
        } catch (IOException e) {
            IllegalArgumentException iae = new IllegalArgumentException("Not a valid destination " + destination);
            iae.addSuppressed(e);
            throw iae;
        }
    }

    private static <T, U> void validateOption(Map<String, String> options, String name, Function<String, U> validator) {
        try {
            String v = options.get(name);
            if (v == null) {
                return; // OK, will set default
            }
            validator.apply(v);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Not a valid value for option '" + name + "'. " + iae.getMessage());
        }
    }

    @SuppressWarnings("removal")
    private FlightRecorder getRecorder() throws SecurityException {
        // Synchronize on some private object that is always available
        synchronized (streamHandler) {
            if (recorder == null) {
                recorder = AccessController.doPrivileged(new PrivilegedAction<FlightRecorder>() {
                    @Override
                    public FlightRecorder run() {
                        return FlightRecorder.getFlightRecorder();
                    }
                }, null, new FlightRecorderPermission("accessFlightRecorder"));
            }
            return recorder;
        }
    }

    private static MBeanNotificationInfo[] createNotificationInfo() {
        String[] types = new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE };
        String name = AttributeChangeNotification.class.getName();
        String description = "Notifies if the RecordingState has changed for one of the recordings, for example if a recording starts or stops";
        MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);
        return new MBeanNotificationInfo[] { info };
    }

    @SuppressWarnings("removal")
    @Override
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
        MXBeanListener mxbeanListener = new MXBeanListener(listener, filter, handback);
        listeners.add(mxbeanListener);
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run(){
                FlightRecorder.addListener(mxbeanListener);
                return null;
            }
        }, null, new FlightRecorderPermission("accessFlightRecorder"));
        super.addNotificationListener(listener, filter, handback);
    }

    @Override
    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        removeListeners( x -> listener == x.listener);
        super.removeNotificationListener(listener);
    }

    @Override
    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
        removeListeners( x -> listener == x.listener && filter == x.filter && handback == x.handback);
        super.removeNotificationListener(listener, filter, handback);
    }

    private void removeListeners(Predicate<MXBeanListener> p) {
        List<MXBeanListener> toBeRemoved = new ArrayList<>(listeners.size());
        for (MXBeanListener l : listeners) {
            if (p.test(l)) {
                toBeRemoved.add(l);
                FlightRecorder.removeListener(l);
            }
        }
        listeners.removeAll(toBeRemoved);
    }

    private Notification createNotication(Recording recording) {
        try {
            Long id = recording.getId();
            Object oldValue = changes.get(recording.getId());
            Object newValue = getAttribute(ATTRIBUTE_RECORDINGS);
            if (recording.getState() != RecordingState.CLOSED) {
                changes.put(id, newValue);
            } else {
                changes.remove(id);
            }
            return new AttributeChangeNotification(getObjectName(), sequenceNumber.incrementAndGet(), System.currentTimeMillis(), "Recording " + recording.getName() + " is "
                    + recording.getState(), ATTRIBUTE_RECORDINGS, newValue.getClass().getName(), oldValue, newValue);
        } catch (AttributeNotFoundException | MBeanException | ReflectionException e) {
            throw new RuntimeException("Could not create notifcation for FlightRecorderMXBean. " + e.getMessage(), e);
        }
    }
}
