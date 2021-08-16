/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package jdk.tools.jlink.internal.plugins;

import java.util.Map;
import java.util.function.Function;

import jdk.tools.jlink.internal.ResourcePoolManager.ResourcePoolImpl;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.internal.ImagePluginStack;
import jdk.tools.jlink.internal.ResourcePoolManager;
import jdk.tools.jlink.internal.ResourcePrevisitor;
import jdk.tools.jlink.internal.StringTable;

/**
 *
 * ZIP and String Sharing compression plugin
 */
public final class DefaultCompressPlugin extends AbstractPlugin implements ResourcePrevisitor {
    public static final String FILTER = "filter";
    public static final String LEVEL_0 = "0";
    public static final String LEVEL_1 = "1";
    public static final String LEVEL_2 = "2";

    private StringSharingPlugin ss;
    private ZipPlugin zip;

    public DefaultCompressPlugin() {
        super("compress");
    }

    @Override
    public ResourcePool transform(ResourcePool in, ResourcePoolBuilder out) {
        if (ss != null && zip != null) {
            ResourcePoolManager resMgr = new ImagePluginStack.OrderedResourcePoolManager(
                    in.byteOrder(), ((ResourcePoolImpl)in).getStringTable());
            return zip.transform(ss.transform(in, resMgr.resourcePoolBuilder()), out);
        } else if (ss != null) {
            return ss.transform(in, out);
        } else if (zip != null) {
            return zip.transform(in, out);
        } else {
            in.transformAndCopy(Function.identity(), out);
            return out.build();
        }
    }

    @Override
    public void previsit(ResourcePool resources, StringTable strings) {
        if (ss != null) {
            ss.previsit(resources, strings);
        }
    }

    @Override
    public Category getType() {
        return Category.COMPRESSOR;
    }

    @Override
    public boolean hasArguments() {
        return true;
    }

    @Override
    public void configure(Map<String, String> config) {
        ResourceFilter resFilter = ResourceFilter.includeFilter(config.get(FILTER));
        String level = config.get(getName());
        if (level != null) {
            switch (level) {
                case LEVEL_0:
                    ss = null;
                    zip = null;
                    break;
                case LEVEL_1:
                    ss = new StringSharingPlugin(resFilter);
                    break;
                case LEVEL_2:
                    zip = new ZipPlugin(resFilter);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid compression level " + level);
            }
        } else {
            throw new IllegalArgumentException("Invalid compression level " + level);
        }
    }
}
