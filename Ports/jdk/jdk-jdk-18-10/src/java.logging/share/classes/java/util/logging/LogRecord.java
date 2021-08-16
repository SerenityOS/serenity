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

package java.util.logging;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.io.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Clock;
import java.util.function.Predicate;
import static jdk.internal.logger.SurrogateLogger.isFilteredFrame;

/**
 * LogRecord objects are used to pass logging requests between
 * the logging framework and individual log Handlers.
 * <p>
 * When a LogRecord is passed into the logging framework it
 * logically belongs to the framework and should no longer be
 * used or updated by the client application.
 * <p>
 * Note that if the client application has not specified an
 * explicit source method name and source class name, then the
 * LogRecord class will infer them automatically when they are
 * first accessed (due to a call on getSourceMethodName or
 * getSourceClassName) by analyzing the call stack.  Therefore,
 * if a logging Handler wants to pass off a LogRecord to another
 * thread, or to transmit it over RMI, and if it wishes to subsequently
 * obtain method name or class name information it should call
 * one of getSourceClassName or getSourceMethodName to force
 * the values to be filled in.
 * <p>
 * <b> Serialization notes:</b>
 * <ul>
 * <li>The LogRecord class is serializable.
 *
 * <li> Because objects in the parameters array may not be serializable,
 * during serialization all objects in the parameters array are
 * written as the corresponding Strings (using Object.toString).
 *
 * <li> The ResourceBundle is not transmitted as part of the serialized
 * form, but the resource bundle name is, and the recipient object's
 * readObject method will attempt to locate a suitable resource bundle.
 *
 * </ul>
 *
 * @since 1.4
 */

public class LogRecord implements java.io.Serializable {
    private static final AtomicLong globalSequenceNumber
        = new AtomicLong();

    /**
     * Logging message level
     */
    private Level level;

    /**
     * Sequence number
     */
    private long sequenceNumber;

    /**
     * Class that issued logging call
     */
    private String sourceClassName;

    /**
     * Method that issued logging call
     */
    private String sourceMethodName;

    /**
     * Non-localized raw message text
     */
    private String message;

    /**
     * Thread ID for thread that issued logging call.
     */
    private int threadID;

    /**
     * long value of Thread ID for thread that issued logging call.
     */
    private long longThreadID;

    /**
     * The Throwable (if any) associated with log message
     */
    private Throwable thrown;

    /**
     * Name of the source Logger.
     */
    private String loggerName;

    /**
     * Resource bundle name to localized log message.
     */
    private String resourceBundleName;

    /**
     * Event time.
     * @since 9
     */
    private Instant instant;

    /**
     * @serialField level Level Logging message level
     * @serialField sequenceNumber long Sequence number
     * @serialField sourceClassName String Class that issued logging call
     * @serialField sourceMethodName String Method that issued logging call
     * @serialField message String Non-localized raw message text
     * @serialField threadID int this is deprecated and is available for backward compatibility.
     *              Values may have been synthesized. If present, {@code longThreadID} represents
     *              the actual thread id.
     * @serialField longThreadID long Thread ID for thread that issued logging call
     * @serialField millis long Truncated event time in milliseconds since 1970
     *              - calculated as getInstant().toEpochMilli().
     *               The event time instant can be reconstructed using
     * <code>Instant.ofEpochSecond(millis/1000, (millis % 1000) * 1000_000 + nanoAdjustment)</code>
     * @serialField nanoAdjustment int Nanoseconds adjustment to the millisecond of
     *              event time - calculated as getInstant().getNano() % 1000_000
     *               The event time instant can be reconstructed using
     * <code>Instant.ofEpochSecond(millis/1000, (millis % 1000) * 1000_000 + nanoAdjustment)</code>
     *              <p>
     *              Since: 9
     * @serialField thrown Throwable The Throwable (if any) associated with log
     *              message
     * @serialField loggerName String Name of the source Logger
     * @serialField resourceBundleName String Resource bundle name to localized
     *              log message
     */
    @Serial
    private static final ObjectStreamField[] serialPersistentFields =
        new ObjectStreamField[] {
            new ObjectStreamField("level", Level.class),
            new ObjectStreamField("sequenceNumber", long.class),
            new ObjectStreamField("sourceClassName", String.class),
            new ObjectStreamField("sourceMethodName", String.class),
            new ObjectStreamField("message", String.class),
            new ObjectStreamField("threadID", int.class),
            new ObjectStreamField("longThreadID", long.class),
            new ObjectStreamField("millis", long.class),
            new ObjectStreamField("nanoAdjustment", int.class),
            new ObjectStreamField("thrown", Throwable.class),
            new ObjectStreamField("loggerName", String.class),
            new ObjectStreamField("resourceBundleName", String.class),
        };

    private transient boolean needToInferCaller;
    private transient Object parameters[];
    private transient ResourceBundle resourceBundle;

    /**
     * Synthesizes a pseudo unique integer value from a long {@code id} value.
     * For backward compatibility with previous releases,the returned integer is
     * such that for any positive long less than or equals to {@code Integer.MAX_VALUE},
     * the returned integer is equal to the original value.
     * Otherwise - it is synthesized with a best effort hashing algorithm,
     * and the returned value is negative.
     * Calling this method multiple times with the same value always yields the same result.
     *
     * @return thread id
     */

    private int shortThreadID(long id) {
        if (id >= 0 && id <= Integer.MAX_VALUE)
            return (int) id;
        int hash = Long.hashCode(id);
        return hash < 0 ? hash : (-1 - hash);
    }

    /**
     * Construct a LogRecord with the given level and message values.
     * <p>
     * The sequence property will be initialized with a new unique value.
     * These sequence values are allocated in increasing order within a VM.
     * <p>
     * Since JDK 9, the event time is represented by an {@link Instant}.
     * The instant property will be initialized to the {@linkplain
     * Instant#now() current instant}, using the best available
     * {@linkplain Clock#systemUTC() clock} on the system.
     * <p>
     * The thread ID property will be initialized with a unique ID for
     * the current thread.
     * <p>
     * All other properties will be initialized to "null".
     *
     * @param level  a logging level value
     * @param msg  the raw non-localized logging message (may be null)
     * @see java.time.Clock#systemUTC()
     */
    public LogRecord(Level level, String msg) {
        this.level = Objects.requireNonNull(level);
        message = msg;
        // Assign a thread ID and a unique sequence number.
        sequenceNumber = globalSequenceNumber.getAndIncrement();
        long id = Thread.currentThread().getId();
        // threadID is deprecated and this value is synthesised for backward compatibility
        threadID = shortThreadID(id);
        longThreadID = id;
        instant = Instant.now();
        needToInferCaller = true;
    }

    /**
     * Get the source Logger's name.
     *
     * @return source logger name (may be null)
     */
    public String getLoggerName() {
        return loggerName;
    }

    /**
     * Set the source Logger's name.
     *
     * @param name   the source logger name (may be null)
     */
    public void setLoggerName(String name) {
        loggerName = name;
    }

    /**
     * Get the localization resource bundle
     * <p>
     * This is the ResourceBundle that should be used to localize
     * the message string before formatting it.  The result may
     * be null if the message is not localizable, or if no suitable
     * ResourceBundle is available.
     * @return the localization resource bundle
     */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    /**
     * Set the localization resource bundle.
     *
     * @param bundle  localization bundle (may be null)
     */
    public void setResourceBundle(ResourceBundle bundle) {
        resourceBundle = bundle;
    }

    /**
     * Get the localization resource bundle name
     * <p>
     * This is the name for the ResourceBundle that should be
     * used to localize the message string before formatting it.
     * The result may be null if the message is not localizable.
     * @return the localization resource bundle name
     */
    public String getResourceBundleName() {
        return resourceBundleName;
    }

    /**
     * Set the localization resource bundle name.
     *
     * @param name  localization bundle name (may be null)
     */
    public void setResourceBundleName(String name) {
        resourceBundleName = name;
    }

    /**
     * Get the logging message level, for example Level.SEVERE.
     * @return the logging message level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Set the logging message level, for example Level.SEVERE.
     * @param level the logging message level
     */
    public void setLevel(Level level) {
        if (level == null) {
            throw new NullPointerException();
        }
        this.level = level;
    }

    /**
     * Get the sequence number.
     * <p>
     * Sequence numbers are normally assigned in the LogRecord
     * constructor, which assigns unique sequence numbers to
     * each new LogRecord in increasing order.
     * @return the sequence number
     */
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Set the sequence number.
     * <p>
     * Sequence numbers are normally assigned in the LogRecord constructor,
     * so it should not normally be necessary to use this method.
     * @param seq the sequence number
     */
    public void setSequenceNumber(long seq) {
        sequenceNumber = seq;
    }

    /**
     * Get the  name of the class that (allegedly) issued the logging request.
     * <p>
     * Note that this sourceClassName is not verified and may be spoofed.
     * This information may either have been provided as part of the
     * logging call, or it may have been inferred automatically by the
     * logging framework.  In the latter case, the information may only
     * be approximate and may in fact describe an earlier call on the
     * stack frame.
     * <p>
     * May be null if no information could be obtained.
     *
     * @return the source class name
     */
    public String getSourceClassName() {
        if (needToInferCaller) {
            inferCaller();
        }
        return sourceClassName;
    }

    /**
     * Set the name of the class that (allegedly) issued the logging request.
     *
     * @param sourceClassName the source class name (may be null)
     */
    public void setSourceClassName(String sourceClassName) {
        this.sourceClassName = sourceClassName;
        needToInferCaller = false;
    }

    /**
     * Get the  name of the method that (allegedly) issued the logging request.
     * <p>
     * Note that this sourceMethodName is not verified and may be spoofed.
     * This information may either have been provided as part of the
     * logging call, or it may have been inferred automatically by the
     * logging framework.  In the latter case, the information may only
     * be approximate and may in fact describe an earlier call on the
     * stack frame.
     * <p>
     * May be null if no information could be obtained.
     *
     * @return the source method name
     */
    public String getSourceMethodName() {
        if (needToInferCaller) {
            inferCaller();
        }
        return sourceMethodName;
    }

    /**
     * Set the name of the method that (allegedly) issued the logging request.
     *
     * @param sourceMethodName the source method name (may be null)
     */
    public void setSourceMethodName(String sourceMethodName) {
        this.sourceMethodName = sourceMethodName;
        needToInferCaller = false;
    }

    /**
     * Get the "raw" log message, before localization or formatting.
     * <p>
     * May be null, which is equivalent to the empty string "".
     * <p>
     * This message may be either the final text or a localization key.
     * <p>
     * During formatting, if the source logger has a localization
     * ResourceBundle and if that ResourceBundle has an entry for
     * this message string, then the message string is replaced
     * with the localized value.
     *
     * @return the raw message string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the "raw" log message, before localization or formatting.
     *
     * @param message the raw message string (may be null)
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get the parameters to the log message.
     *
     * @return the log message parameters.  May be null if
     *                  there are no parameters.
     */
    public Object[] getParameters() {
        return parameters;
    }

    /**
     * Set the parameters to the log message.
     *
     * @param parameters the log message parameters. (may be null)
     */
    public void setParameters(Object parameters[]) {
        this.parameters = parameters;
    }

    /**
     * Get an identifier for the thread where the message originated.
     * <p>
     * This is a thread identifier within the Java VM and may or
     * may not map to any operating system ID.
     *
     * @deprecated  Values returned by this method may be synthesized,
     *              and may not correspond to the actual {@linkplain Thread#getId() thread id},
     *              use {@link #getLongThreadID()} instead.
     * @return thread ID
     */
    @Deprecated(since = "16")
    public int getThreadID() {
        return threadID;
    }

    /**
     * Set an identifier for the thread where the message originated.
     * @param threadID  the thread ID
     *
     * @deprecated  This method doesn't allow to pass a long {@linkplain Thread#getId() thread id},
     *              use {@link #setLongThreadID(long)} instead.
     */
    @Deprecated(since = "16")
    public void setThreadID(int threadID) {
        this.threadID = threadID;
        this.longThreadID = threadID;
    }

    /**
     * Get a thread identifier for the thread where message originated
     *
     * <p>
     * This is a thread identifier within the Java VM and may or
     * may not map to any operating system ID.
     *
     * @return thread ID
     * @since 16
     */
    public long getLongThreadID() {
        return longThreadID;
    }

    /**
     * Set an identifier for the thread where the message originated.
     *
     * @param longThreadID the thread ID
     * @return this LogRecord
     * @since 16
     */
    public LogRecord setLongThreadID(long longThreadID) {
        this.threadID = shortThreadID(longThreadID);
        this.longThreadID = longThreadID;
        return this;
    }

    /**
     * Get truncated event time in milliseconds since 1970.
     *
     * @return truncated event time in millis since 1970
     *
     * @implSpec This is equivalent to calling
     *      {@link #getInstant() getInstant().toEpochMilli()}.
     *
     * @apiNote To get the full nanosecond resolution event time,
     *             use {@link #getInstant()}.
     *
     * @see #getInstant()
     */
    public long getMillis() {
        return instant.toEpochMilli();
    }

    /**
     * Set event time.
     *
     * @param millis event time in millis since 1970.
     *
     * @implSpec This is equivalent to calling
     *      {@link #setInstant(java.time.Instant)
     *      setInstant(Instant.ofEpochMilli(millis))}.
     *
     * @deprecated LogRecord maintains timestamps with nanosecond resolution,
     *             using {@link Instant} values. For this reason,
     *             {@link #setInstant(java.time.Instant) setInstant()}
     *             should be used in preference to {@code setMillis()}.
     *
     * @see #setInstant(java.time.Instant)
     */
    @Deprecated
    public void setMillis(long millis) {
        this.instant = Instant.ofEpochMilli(millis);
    }

    /**
     * Gets the instant that the event occurred.
     *
     * @return the instant that the event occurred.
     *
     * @since 9
     */
    public Instant getInstant() {
        return instant;
    }

    /**
     * Sets the instant that the event occurred.
     * <p>
     * If the given {@code instant} represents a point on the time-line too
     * far in the future or past to fit in a {@code long} milliseconds and
     * nanoseconds adjustment, then an {@code ArithmeticException} will be
     * thrown.
     *
     * @param instant the instant that the event occurred.
     *
     * @throws NullPointerException if {@code instant} is null.
     * @throws ArithmeticException if numeric overflow would occur while
     *         calling {@link Instant#toEpochMilli() instant.toEpochMilli()}.
     *
     * @since 9
     */
    public void setInstant(Instant instant) {
        instant.toEpochMilli();
        this.instant = instant;
    }

    /**
     * Get any throwable associated with the log record.
     * <p>
     * If the event involved an exception, this will be the
     * exception object. Otherwise null.
     *
     * @return a throwable
     */
    public Throwable getThrown() {
        return thrown;
    }

    /**
     * Set a throwable associated with the log event.
     *
     * @param thrown  a throwable (may be null)
     */
    public void setThrown(Throwable thrown) {
        this.thrown = thrown;
    }

    @Serial
    private static final long serialVersionUID = 5372048053134512534L;

    /**
     * @serialData Serialized fields, followed by a two byte version number
     * (major byte, followed by minor byte), followed by information on
     * the log record parameter array.  If there is no parameter array,
     * then -1 is written.  If there is a parameter array (possible of zero
     * length) then the array length is written as an integer, followed
     * by String values for each parameter.  If a parameter is null, then
     * a null String is written.  Otherwise the output of Object.toString()
     * is written.
     *
     * @param out the {@code ObjectOutputStream} to write to
     *
     * @throws  IOException if I/O errors occur
     */
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        // We have to write serialized fields first.
        ObjectOutputStream.PutField pf = out.putFields();
        pf.put("level", level);
        pf.put("sequenceNumber", sequenceNumber);
        pf.put("sourceClassName", sourceClassName);
        pf.put("sourceMethodName", sourceMethodName);
        pf.put("message", message);
        pf.put("threadID", threadID);
        pf.put("longThreadID", longThreadID);
        pf.put("millis", instant.toEpochMilli());
        pf.put("nanoAdjustment", instant.getNano() % 1000_000);
        pf.put("thrown", thrown);
        pf.put("loggerName", loggerName);
        pf.put("resourceBundleName", resourceBundleName);
        out.writeFields();

        // Write our version number.
        out.writeByte(1);
        out.writeByte(0);
        if (parameters == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(parameters.length);
        // Write string values for the parameters.
        for (Object parameter : parameters) {
            out.writeObject(Objects.toString(parameter, null));
        }
    }

    /**
     * Initializes the LogRecord from deserialized data.
     * <ul>
     * <li>If {@code longThreadID} is present in the serial form, its value
     * takes precedence over {@code threadID} and a value for {@code threadID}
     * is synthesized from it, such that for {@code longThreadID} values between
     * {@code 0} and {@code Integer.MAX_VALUE} inclusive, {@code longThreadID}
     * and {@code threadID} will have the same value. For values outside of this
     * range a negative synthesized value will be deterministically derived
     * from {@code longThreadID}.
     * <li>Otherwise, when only {@code threadID} is
     * present, {@code longThreadID} is initialized with the value of
     * {@code threadID} which may be anything between {@code Integer.MIN_VALUE}
     * and {Integer.MAX_VALUE}.
     * </ul>
     *
     * See {@code writeObject} for a description of the serial form.
     *
     * @param in the {@code ObjectInputStream} to read from
     *
     * @throws  ClassNotFoundException if the class of a serialized object
     *          could not be found.
     * @throws  IOException if an I/O error occurs.
     */
    @Serial
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        // We have to read serialized fields first.
        ObjectInputStream.GetField gf = in.readFields();
        level = (Level) gf.get("level", null);
        sequenceNumber = gf.get("sequenceNumber", 0L);
        sourceClassName = (String) gf.get("sourceClassName", null);
        sourceMethodName = (String) gf.get("sourceMethodName", null);
        message = (String) gf.get("message", null);
        // If longthreadID is not present, it will be initialised with threadID value
        // If longthreadID is present, threadID might have a synthesized value
        int threadID = gf.get("threadID", 0);
        long longThreadID = gf.get("longThreadID", (long)threadID);
        if (threadID != longThreadID)
            threadID = shortThreadID(longThreadID);
        this.threadID = threadID;
        this.longThreadID = longThreadID;
        long millis = gf.get("millis", 0L);
        int nanoOfMilli = gf.get("nanoAdjustment", 0);
        instant = Instant.ofEpochSecond(
            millis / 1000L, (millis % 1000L) * 1000_000L + nanoOfMilli);
        thrown = (Throwable) gf.get("thrown", null);
        loggerName = (String) gf.get("loggerName", null);
        resourceBundleName = (String) gf.get("resourceBundleName", null);

        // Read version number.
        byte major = in.readByte();
        byte minor = in.readByte();
        if (major != 1) {
            throw new IOException("LogRecord: bad version: " + major + "." + minor);
        }
        int len = in.readInt();
        if (len < -1) {
            throw new NegativeArraySizeException();
        } else if (len == -1) {
            parameters = null;
        } else if (len < 255) {
            parameters = new Object[len];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = in.readObject();
            }
        } else {
            List<Object> params = new ArrayList<>(Math.min(len, 1024));
            for (int i = 0; i < len; i++) {
                params.add(in.readObject());
            }
            parameters = params.toArray(new Object[params.size()]);
        }
        // If necessary, try to regenerate the resource bundle.
        if (resourceBundleName != null) {
            try {
                // use system class loader to ensure the ResourceBundle
                // instance is a different instance than null loader uses
                final ResourceBundle bundle =
                    ResourceBundle.getBundle(resourceBundleName,
                        Locale.getDefault(),
                        ClassLoader.getSystemClassLoader());
                resourceBundle = bundle;
            } catch (MissingResourceException ex) {
                // This is not a good place to throw an exception,
                // so we simply leave the resourceBundle null.
                resourceBundle = null;
            }
        }

        needToInferCaller = false;
    }

    // Private method to infer the caller's class and method names
    //
    // Note:
    // For testing purposes - it is possible to customize the process
    // by which LogRecord will infer the source class name and source method name
    // when analyzing the call stack.
    // <p>
    // The system property {@code jdk.logger.packages} can define a comma separated
    // list of strings corresponding to additional package name prefixes that
    // should be ignored when trying to infer the source caller class name.
    // Those stack frames whose {@linkplain StackTraceElement#getClassName()
    // declaring class name} start with one such prefix will be ignored.
    // <p>
    // This is primarily useful when providing utility logging classes wrapping
    // a logger instance, as it makes it possible to instruct LogRecord to skip
    // those utility frames when inferring the caller source class name.
    // <p>
    // The {@code jdk.logger.packages} system property is consulted only once.
    // <p>
    // This property is not standard, implementation specific, and yet
    // undocumented (and thus subject to changes without notice).
    //
    private void inferCaller() {
        needToInferCaller = false;
        // Skip all frames until we have found the first logger frame.
        Optional<StackWalker.StackFrame> frame = new CallerFinder().get();
        frame.ifPresent(f -> {
            setSourceClassName(f.getClassName());
            setSourceMethodName(f.getMethodName());
        });

        // We haven't found a suitable frame, so just punt.  This is
        // OK as we are only committed to making a "best effort" here.
    }

    /*
     * CallerFinder is a stateful predicate.
     */
    @SuppressWarnings("removal")
    static final class CallerFinder implements Predicate<StackWalker.StackFrame> {
        private static final StackWalker WALKER;
        static {
            final PrivilegedAction<StackWalker> action =
                () -> StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
            WALKER = AccessController.doPrivileged(action);
        }

        /**
         * Returns StackFrame of the caller's frame.
         * @return StackFrame of the caller's frame.
         */
        Optional<StackWalker.StackFrame> get() {
            return WALKER.walk((s) -> s.filter(this).findFirst());
        }

        private boolean lookingForLogger = true;
        /**
         * Returns true if we have found the caller's frame, false if the frame
         * must be skipped.
         *
         * @param t The frame info.
         * @return true if we have found the caller's frame, false if the frame
         * must be skipped.
         */
        @Override
        public boolean test(StackWalker.StackFrame t) {
            final String cname = t.getClassName();
            // We should skip all frames until we have found the logger,
            // because these frames could be frames introduced by e.g. custom
            // sub classes of Handler.
            if (lookingForLogger) {
                // the log record could be created for a platform logger
                lookingForLogger = !isLoggerImplFrame(cname);
                return false;
            }
            // Continue walking until we've found the relevant calling frame.
            // Skips logging/logger infrastructure.
            return !isFilteredFrame(t);
        }

        private boolean isLoggerImplFrame(String cname) {
            return (cname.equals("java.util.logging.Logger") ||
                cname.startsWith("sun.util.logging.PlatformLogger"));
        }
    }
}
