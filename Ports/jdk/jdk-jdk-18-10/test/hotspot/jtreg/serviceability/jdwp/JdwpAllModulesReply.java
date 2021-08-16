/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.IOException;

/**
 * The JDWP reply to the ALLMODULES command
 */
public class JdwpAllModulesReply extends JdwpReply {

    private int modulesCount;
    private long[] modulesId;

    protected void parseData(DataInputStream ds) throws IOException {
        modulesCount = ds.readInt();
        modulesId = new long[modulesCount];
        for (int nmod = 0; nmod < modulesCount; ++nmod) {
            modulesId[nmod] = readRefId(ds);
        }
    }

    /**
     * Number of modules reported
     *
     * @return modules count
     */
    public int getModulesCount() {
        return modulesCount;
    }

    /**
     * The id of a module reported
     *
     * @param ndx module index in the array of the reported ids
     * @return module id
     */
    public long getModuleId(int ndx) {
        return modulesId[ndx];
    }
}
