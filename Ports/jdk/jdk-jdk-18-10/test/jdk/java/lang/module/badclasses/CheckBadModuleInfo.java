/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.InvalidModuleDescriptorException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Uses the ModuleDescriptor.read API to read the module-info.class in the
 * ${test.classes} directory and expects InvalidModuleDescriptorException
 * to be thrown.
 */
public class CheckBadModuleInfo {
    public static void main(String[] args) throws IOException {
        Path mi = Path.of(System.getProperty("test.classes"), "module-info.class");
        try (InputStream in = Files.newInputStream(mi)) {
            try {
                ModuleDescriptor descriptor = ModuleDescriptor.read(in);
                System.out.println(descriptor);
                throw new RuntimeException("InvalidModuleDescriptorException expected");
            } catch (InvalidModuleDescriptorException e) {
                // expected
                System.out.println(e);
            }
        }
    }
}
