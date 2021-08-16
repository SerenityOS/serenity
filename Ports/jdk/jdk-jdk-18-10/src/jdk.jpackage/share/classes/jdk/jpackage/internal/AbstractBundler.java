/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;


/**
 * AbstractBundler
 *
 * This is the base class all bundlers extend from.
 * It contains methods and parameters common to all bundlers.
 * The concrete implementations are in the platform specific bundlers.
 */
abstract class AbstractBundler implements Bundler {

    static final BundlerParamInfo<Path> IMAGES_ROOT =
            new StandardBundlerParam<>(
            "imagesRoot",
            Path.class,
            params ->
                StandardBundlerParam.TEMP_ROOT.fetchFrom(params).resolve("images"),
            (s, p) -> null);

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void cleanup(Map<String, ? super Object> params) {
        try {
            IOUtils.deleteRecursive(
                    StandardBundlerParam.TEMP_ROOT.fetchFrom(params));
        } catch (IOException e) {
            Log.verbose(e.getMessage());
        }
    }
}
