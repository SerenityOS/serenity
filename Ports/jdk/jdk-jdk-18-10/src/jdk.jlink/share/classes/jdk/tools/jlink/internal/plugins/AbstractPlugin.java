/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.tools.jlink.plugin.Plugin;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public abstract class AbstractPlugin implements Plugin {

    static final String DESCRIPTION = "description";
    static final String USAGE = "usage";

    private static final ResourceBundle standardPluginsBundle;

    static {
        Locale locale = Locale.getDefault();
        try {
            standardPluginsBundle = ResourceBundle.getBundle("jdk.tools.jlink."
                    + "resources.plugins", locale);
        } catch (MissingResourceException e) {
            throw new InternalError("Cannot find jlink resource bundle for "
                    + "locale " + locale);
        }
    }

    private final ResourceBundle pluginsBundle;
    private final String name;

    protected AbstractPlugin(String name) {
        this.name = name;
        this.pluginsBundle = standardPluginsBundle;
    }

    protected AbstractPlugin(String name, ResourceBundle bundle) {
        this.name = name;
        this.pluginsBundle = bundle;
    }
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return getMessage(getName() + "." + DESCRIPTION, getName());
    }

    @Override
    public String getUsage() {
        return getMessage(getName() + "." + USAGE, getName());
    }

    @Override
    public String getArgumentsDescription() {
        return PluginsResourceBundle.getArgument(getName());
    }

    protected String getMessage(String key, Object...args) {
       return PluginsResourceBundle.getMessage(this.pluginsBundle, key, args);
    }
}
