/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * The management interface for the logging facility.
 *
 * {@link java.lang.management.PlatformLoggingMXBean
 * java.lang.management.PlatformLoggingMXBean} is the management interface
 * for logging facility registered in the {@link
 * java.lang.management.ManagementFactory#getPlatformMBeanServer()
 * platform MBeanServer}.
 * It is recommended to use the {@code PlatformLoggingMXBean} obtained via
 * the {@link java.lang.management.ManagementFactory#getPlatformMXBean(Class)
 * ManagementFactory.getPlatformMXBean(PlatformLoggingMXBean.class)} method.
 *
 * @deprecated {@code LoggingMXBean} is no longer a {@link
 * java.lang.management.PlatformManagedObject platform MXBean} and is replaced
 * with {@link java.lang.management.PlatformLoggingMXBean}.
 * It will not register in the platform {@code MBeanServer}.
 * Use {@code ManagementFactory.getPlatformMXBean(PlatformLoggingMXBean.class)}
 * instead.
 *
 * @author  Ron Mann
 * @author  Mandy Chung
 * @since   1.5
 *
 * @see java.lang.management.PlatformLoggingMXBean
 */
@Deprecated(since="9")
public interface LoggingMXBean {

    /**
     * Returns the list of currently registered logger names. This method
     * calls {@link LogManager#getLoggerNames} and returns a list
     * of the logger names.
     *
     * @return A list of {@code String} each of which is a
     *         currently registered {@code Logger} name.
     */
    public java.util.List<String> getLoggerNames();

    /**
     * Gets the name of the log level associated with the specified logger.
     * If the specified logger does not exist, {@code null}
     * is returned.
     * This method first finds the logger of the given name and
     * then returns the name of the log level by calling:
     * <blockquote>
     *   {@link Logger#getLevel Logger.getLevel()}.{@link Level#getName getName()};
     * </blockquote>
     *
     * <p>
     * If the {@code Level} of the specified logger is {@code null},
     * which means that this logger's effective level is inherited
     * from its parent, an empty string will be returned.
     *
     * @param loggerName The name of the {@code Logger} to be retrieved.
     *
     * @return The name of the log level of the specified logger; or
     *         an empty string if the log level of the specified logger
     *         is {@code null}.  If the specified logger does not
     *         exist, {@code null} is returned.
     *
     * @see Logger#getLevel
     */
    public String getLoggerLevel(String loggerName);

    /**
     * Sets the specified logger to the specified new level.
     * If the {@code levelName} is not {@code null}, the level
     * of the specified logger is set to the parsed {@code Level}
     * matching the {@code levelName}.
     * If the {@code levelName} is {@code null}, the level
     * of the specified logger is set to {@code null} and
     * the effective level of the logger is inherited from
     * its nearest ancestor with a specific (non-null) level value.
     *
     * @param loggerName The name of the {@code Logger} to be set.
     *                   Must be non-null.
     * @param levelName The name of the level to set on the specified logger,
     *                 or {@code null} if setting the level to inherit
     *                 from its nearest ancestor.
     *
     * @throws IllegalArgumentException if the specified logger
     * does not exist, or {@code levelName} is not a valid level name.
     *
     * @throws SecurityException if a security manager exists and if
     * the caller does not have LoggingPermission("control").
     *
     * @see Logger#setLevel
     */
    public void setLoggerLevel(String loggerName, String levelName);

    /**
     * Returns the name of the parent for the specified logger.
     * If the specified logger does not exist, {@code null} is returned.
     * If the specified logger is the root {@code Logger} in the namespace,
     * the result will be an empty string.
     *
     * @param loggerName The name of a {@code Logger}.
     *
     * @return the name of the nearest existing parent logger;
     *         an empty string if the specified logger is the root logger.
     *         If the specified logger does not exist, {@code null}
     *         is returned.
     */
    public String getParentLoggerName(String loggerName);
}
