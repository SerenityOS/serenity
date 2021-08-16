/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.lang.System.LoggerFinder;

/**
 * A custom class loader which can hide the registered LoggerProvider
 * depending on the value of a test.logger.hidesProvider system property.
 * @author danielfuchs
 */
public class SystemClassLoader extends ClassLoader {

    final public boolean hidesProvider;

    public SystemClassLoader() {
        hidesProvider = Boolean.getBoolean("test.logger.hidesProvider");
    }
    public SystemClassLoader(ClassLoader parent) {
        super(parent);
        hidesProvider = Boolean.getBoolean("test.logger.hidesProvider");
    }

    boolean accept(String name) {
        final boolean res = !name.endsWith(LoggerFinder.class.getName());
        if (res == false) {
            System.out.println("Hiding " + name);
        }
        return res;
    }

    @Override
    public URL getResource(String name) {
        if (hidesProvider && !accept(name)) {
            return null;
        } else {
            return super.getResource(name);
        }
    }

    class Enumerator implements Enumeration<URL> {
        final Enumeration<URL> enumeration;
        volatile URL next;
        Enumerator(Enumeration<URL> enumeration) {
            this.enumeration = enumeration;
        }

        @Override
        public boolean hasMoreElements() {
            if (next != null) return true;
            if (!enumeration.hasMoreElements()) return false;
            if (hidesProvider == false) return true;
            next = enumeration.nextElement();
            if (accept(next.getPath())) return true;
            next = null;
            return hasMoreElements();
        }

        @Override
        public URL nextElement() {
            final URL res = next == null ? enumeration.nextElement() : next;
            next = null;
            if (hidesProvider == false || accept(res.getPath())) return res;
            return nextElement();
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        final Enumeration<URL> enumeration = super.getResources(name);
        return hidesProvider ? new Enumerator(enumeration) : enumeration;
    }



}
