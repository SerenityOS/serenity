/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * {@code Handler} that buffers requests in a circular buffer in memory.
 * <p>
 * Normally this {@code Handler} simply stores incoming {@code LogRecords}
 * into its memory buffer and discards earlier records.  This buffering
 * is very cheap and avoids formatting costs.  On certain trigger
 * conditions, the {@code MemoryHandler} will push out its current buffer
 * contents to a target {@code Handler}, which will typically publish
 * them to the outside world.
 * <p>
 * There are three main models for triggering a push of the buffer:
 * <ul>
 * <li>
 * An incoming {@code LogRecord} has a type that is greater than
 * a pre-defined level, the {@code pushLevel}. </li>
 * <li>
 * An external class calls the {@code push} method explicitly. </li>
 * <li>
 * A subclass overrides the {@code log} method and scans each incoming
 * {@code LogRecord} and calls {@code push} if a record matches some
 * desired criteria. </li>
 * </ul>
 * <p>
 * <b>Configuration:</b>
 * By default each {@code MemoryHandler} is initialized using the following
 * {@code LogManager} configuration properties where {@code <handler-name>}
 * refers to the fully-qualified class name of the handler.
 * If properties are not defined
 * (or have invalid values) then the specified default values are used.
 * If no default value is defined then a RuntimeException is thrown.
 * <ul>
 * <li>   &lt;handler-name&gt;.level
 *        specifies the level for the {@code Handler}
 *        (defaults to {@code Level.ALL}). </li>
 * <li>   &lt;handler-name&gt;.filter
 *        specifies the name of a {@code Filter} class to use
 *        (defaults to no {@code Filter}). </li>
 * <li>   &lt;handler-name&gt;.size
 *        defines the buffer size (defaults to 1000). </li>
 * <li>   &lt;handler-name&gt;.push
 *        defines the {@code pushLevel} (defaults to {@code level.SEVERE}). </li>
 * <li>   &lt;handler-name&gt;.target
 *        specifies the name of the target {@code Handler } class.
 *        (no default). </li>
 * </ul>
 * <p>
 * For example, the properties for {@code MemoryHandler} would be:
 * <ul>
 * <li>   java.util.logging.MemoryHandler.level=INFO </li>
 * <li>   java.util.logging.MemoryHandler.formatter=java.util.logging.SimpleFormatter </li>
 * </ul>
 * <p>
 * For a custom handler, e.g. com.foo.MyHandler, the properties would be:
 * <ul>
 * <li>   com.foo.MyHandler.level=INFO </li>
 * <li>   com.foo.MyHandler.formatter=java.util.logging.SimpleFormatter </li>
 * </ul>
 *
 * @since 1.4
 */

public class MemoryHandler extends Handler {
    private static final int DEFAULT_SIZE = 1000;
    private volatile Level pushLevel;
    private int size;
    private Handler target;
    private LogRecord buffer[];
    int start, count;

    /**
     * Create a {@code MemoryHandler} and configure it based on
     * {@code LogManager} configuration properties.
     */
    public MemoryHandler() {
        // configure with specific defaults for MemoryHandler
        super(Level.ALL, new SimpleFormatter(), null);

        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();
        pushLevel = manager.getLevelProperty(cname +".push", Level.SEVERE);
        size = manager.getIntProperty(cname + ".size", DEFAULT_SIZE);
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        String targetName = manager.getProperty(cname+".target");
        if (targetName == null) {
            throw new RuntimeException("The handler " + cname
                    + " does not specify a target");
        }
        Class<?> clz;
        try {
            clz = ClassLoader.getSystemClassLoader().loadClass(targetName);
            @SuppressWarnings("deprecation")
            Object o = clz.newInstance();
            target = (Handler) o;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("MemoryHandler can't load handler target \"" + targetName + "\"" , e);
        }
        init();
    }

    // Initialize.  Size is a count of LogRecords.
    private void init() {
        buffer = new LogRecord[size];
        start = 0;
        count = 0;
    }

    /**
     * Create a {@code MemoryHandler}.
     * <p>
     * The {@code MemoryHandler} is configured based on {@code LogManager}
     * properties (or their default values) except that the given {@code pushLevel}
     * argument and buffer size argument are used.
     *
     * @param target  the Handler to which to publish output.
     * @param size    the number of log records to buffer (must be greater than zero)
     * @param pushLevel  message level to push on
     *
     * @throws IllegalArgumentException if {@code size is <= 0}
     */
    public MemoryHandler(Handler target, int size, Level pushLevel) {
        // configure with specific defaults for MemoryHandler
        super(Level.ALL, new SimpleFormatter(), null);

        if (target == null || pushLevel == null) {
            throw new NullPointerException();
        }
        if (size <= 0) {
            throw new IllegalArgumentException();
        }
        this.target = target;
        this.pushLevel = pushLevel;
        this.size = size;
        init();
    }

    /**
     * Store a {@code LogRecord} in an internal buffer.
     * <p>
     * If there is a {@code Filter}, its {@code isLoggable}
     * method is called to check if the given log record is loggable.
     * If not we return.  Otherwise the given record is copied into
     * an internal circular buffer.  Then the record's level property is
     * compared with the {@code pushLevel}. If the given level is
     * greater than or equal to the {@code pushLevel} then {@code push}
     * is called to write all buffered records to the target output
     * {@code Handler}.
     *
     * @param  record  description of the log event. A null record is
     *                 silently ignored and is not published
     */
    @Override
    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        int ix = (start+count)%buffer.length;
        buffer[ix] = record;
        if (count < buffer.length) {
            count++;
        } else {
            start++;
            start %= buffer.length;
        }
        if (record.getLevel().intValue() >= pushLevel.intValue()) {
            push();
        }
    }

    /**
     * Push any buffered output to the target {@code Handler}.
     * <p>
     * The buffer is then cleared.
     */
    public synchronized void push() {
        for (int i = 0; i < count; i++) {
            int ix = (start+i)%buffer.length;
            LogRecord record = buffer[ix];
            target.publish(record);
        }
        // Empty the buffer.
        start = 0;
        count = 0;
    }

    /**
     * Causes a flush on the target {@code Handler}.
     * <p>
     * Note that the current contents of the {@code MemoryHandler}
     * buffer are <b>not</b> written out.  That requires a "push".
     */
    @Override
    public void flush() {
        target.flush();
    }

    /**
     * Close the {@code Handler} and free all associated resources.
     * This will also close the target {@code Handler}.
     *
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     */
    @Override
    public void close() throws SecurityException {
        target.close();
        setLevel(Level.OFF);
    }

    /**
     * Set the {@code pushLevel}.  After a {@code LogRecord} is copied
     * into our internal buffer, if its level is greater than or equal to
     * the {@code pushLevel}, then {@code push} will be called.
     *
     * @param newLevel the new value of the {@code pushLevel}
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     */
    public synchronized void setPushLevel(Level newLevel) throws SecurityException {
        if (newLevel == null) {
            throw new NullPointerException();
        }
        checkPermission();
        pushLevel = newLevel;
    }

    /**
     * Get the {@code pushLevel}.
     *
     * @return the value of the {@code pushLevel}
     */
    public Level getPushLevel() {
        return pushLevel;
    }

    /**
     * Check if this {@code Handler} would actually log a given
     * {@code LogRecord} into its internal buffer.
     * <p>
     * This method checks if the {@code LogRecord} has an appropriate level and
     * whether it satisfies any {@code Filter}.  However it does <b>not</b>
     * check whether the {@code LogRecord} would result in a "push" of the
     * buffer contents. It will return false if the {@code LogRecord} is null.
     *
     * @param record  a {@code LogRecord} (may be null).
     * @return true if the {@code LogRecord} would be logged.
     *
     */
    @Override
    public boolean isLoggable(LogRecord record) {
        return super.isLoggable(record);
    }
}
