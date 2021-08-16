/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 4112758
 * @summary Checks that a second invocation of PrintJob.end() does not cause
 * an exception or segmentation violation.
 * @author dpm
 */

import java.awt.*;

public class MultipleEnd {
    public static void main(String[] args) {
        new MultipleEnd().start();
    }
    public void start() {
        new MultipleEndFrame();
    }
}

class MultipleEndFrame extends Frame {
    public MultipleEndFrame() {
        super("MultipleEnd");
        setVisible(true);

        JobAttributes job = new JobAttributes();
        job.setDialog(JobAttributes.DialogType.NONE);
        PrintJob pj  = getToolkit().getPrintJob(this, "MultipleEnd", job, null);
        if (pj != null) {
            pj.end();
            pj.end();
        }
    }
}
