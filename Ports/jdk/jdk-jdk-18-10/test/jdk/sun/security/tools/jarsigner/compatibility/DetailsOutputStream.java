/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/*
 * A custom output stream that redirects the testing outputs to a file, called
 * details.out. It also calls another output stream to save some outputs to
 * other files.
 */
public class DetailsOutputStream extends FileOutputStream {

    private PhaseOutputStream phaseOutputStream = new PhaseOutputStream();

    public DetailsOutputStream(String filename) throws FileNotFoundException {
        super(filename != null && !filename.isEmpty() ? filename :
            "details.out", true);
    }

    public void transferPhase() throws IOException {
        if (phaseOutputStream.isCorePhase()) {
            phaseOutputStream.write(HtmlHelper.endHtml());
            phaseOutputStream.write(HtmlHelper.endPre());
        }

        phaseOutputStream.transfer();

        if (phaseOutputStream.isCorePhase()) {
            phaseOutputStream.write(HtmlHelper.startHtml());
            phaseOutputStream.write(HtmlHelper.startPre());
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        super.write(b);
        phaseOutputStream.write(b);
    }

    @Override
    public void write(int b) throws IOException {
        super.write(b);
        phaseOutputStream.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        super.write(b, off, len);
        phaseOutputStream.write(b, off, len);
    }

    public void writeAnchorName(String name, String text) throws IOException {
        super.write((text).getBytes());
        super.write('\n');
        phaseOutputStream.write(HtmlHelper.anchorName(name, text));
        phaseOutputStream.write('\n');
    }
}
