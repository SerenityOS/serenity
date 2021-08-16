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

package nsk.jdi.HiddenClass.events;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.nio.file.Files;
import java.nio.file.Paths;

import nsk.share.classload.ClassLoadUtils;

/* Interface for tested hidden class to implement. */
interface HCInterf {
    void hcMethod();
}

/* Hidden class definition used to define tested hidden class
 * with lookup.defineHiddenClass. */
public class HiddenClass implements HCInterf {
    static String hcField = "<Not initialized>";
    static String getHCField() { return hcField; }

    private String getClassName() {
        return this.getClass().getName();
    }

    public void hcMethod() {
        hcField = getClassName();
        if (hcField.indexOf("HiddenClass") == -1) {
            throw new RuntimeException("Debuggee: Unexpected HiddenClass name: " + hcField);
        }
    }

    public static Class<?> defineHiddenClass() throws Exception {
        final String HC_NAME = HiddenClass.class.getName();
        final String HC_PATH = ClassLoadUtils.getClassPath(HC_NAME) + File.separator +
                               HC_NAME.replace(".", File.separator) + ".class";
        Class<?> hc = defineHiddenClass(HC_PATH);
        return hc;
    }

    private static Class<?> defineHiddenClass(String classFileName) throws Exception {
        try {
            Lookup lookup = MethodHandles.lookup();
            byte[] bytes = Files.readAllBytes(Paths.get(classFileName));
            Class<?> hc = lookup.defineHiddenClass(bytes, false).lookupClass();
            return hc;
        } catch (Exception ex) {
            throw ex;
        }
    }
}
