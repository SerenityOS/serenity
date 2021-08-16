/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.platform;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.Processor;
import javax.tools.JavaFileManager;

import com.sun.source.util.Plugin;

/**A description of settings needed for a particular {@code --release name} option.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public interface PlatformDescription extends Closeable {

    JavaFileManager getFileManager();

    /**Returns the source version that should be selected.
     * Equivalent to {@code -source N} on the command line.
     *
     * @return the required source version
     */
    String getSourceVersion();

    /**Returns the target version that should be selected.
     * Equivalent to {@code -target N} on the command line.
     *
     * @return the required target version
     */
    String getTargetVersion();

    /**Returns the description of annotation Processor or javac Plugin that should be enabled
     * in the current javac run.
     *
     * @param <T> either Processor or Plugin
     */
    interface PluginInfo<T> {

        /**Returns the name of the Processor or Plugin.
         *
         * @return name of the Processor or Plugin.
         */
        String getName();

        /**Returns the options for the Processor or Plugin. For plugin, the map will be converted to a collection
         * by concatenating the map's keys, the '=' sign and the map's values.
         *
         *
         * @return the options
         */
        Map<String, String> getOptions();

        /**Returns the Processor or Plugin instance.
         *
         * @return the Processor or Plugin instance.
         */
        T getPlugin();
    }

    /**Returns the annotation Processors that should be enabled in the current javac run.
     *
     * @return annotation Processors
     */
    List<PluginInfo<Processor>> getAnnotationProcessors();

    /**Returns the javac Plugins that should be enabled in the current javac run.
     *
     * @return javac Plugins
     */
    List<PluginInfo<Plugin>> getPlugins();

    /**Returns the additional command line options that should be passed to javac.
     *
     * @return additional command line options
     */
    List<String> getAdditionalOptions();

    /**Perform cleanup for this PlatformProvider - this instance won't be used after this method
     * is called.
     *
     * @throws IOException if unexpected problems occur during closing this provider
     */
    void close() throws IOException;
}
