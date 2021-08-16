/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package bug.javax.sound.sampled;

import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * @test
 * @bug 8221436
 * @run main/othervm bug.javax.sound.sampled.ToString
 */
public final class ToString {

    public static void main(final String[] args) {

        // Behavior of toString() methods is not specified, the checks in
        // this test just defends against accidental changes.

        String custom = new Line.Info(ToString.class).toString();
        if (!custom.contains("bug.javax.sound.sampled")) {
            throw new RuntimeException("Package is missing: " + custom);
        }
        String ints = new Line.Info(int.class).toString();
        if (ints.contains("javax.sound.sampled")) {
            throw new RuntimeException("Package is present: " + ints);
        }
        String array = new Line.Info(int[].class).toString();
        if (array.contains("javax.sound.sampled")) {
            throw new RuntimeException("Package is present: " + array);
        }

        String line = new Line.Info(Line.class).toString();
        if (!line.equals("interface Line")) {
            throw new RuntimeException("Wrong string: " + line);
        }
        String target = new Line.Info(TargetDataLine.class).toString();
        if (!target.equals("interface TargetDataLine")) {
            throw new RuntimeException("Wrong string: " + target);
        }
        String source = new Line.Info(SourceDataLine.class).toString();
        if (!source.equals("interface SourceDataLine")) {
            throw new RuntimeException("Wrong string: " + source);
        }
        String port = new Line.Info(Port.class).toString();
        if (!port.equals("interface Port")) {
            throw new RuntimeException("Wrong string: " + port);
        }
        String data = new Line.Info(DataLine.class).toString();
        if (!data.equals("interface DataLine")) {
            throw new RuntimeException("Wrong string: " + data);
        }
        String mixer = new Line.Info(Mixer.class).toString();
        if (!mixer.equals("interface Mixer")) {
            throw new RuntimeException("Wrong string: " + mixer);
        }
        String clip = new Line.Info(Clip.class).toString();
        if (!clip.equals("interface Clip")) {
            throw new RuntimeException("Wrong string: " + clip);
        }

        String dataLine = new DataLine.Info(DataLine.class, null, 0).toString();
        if (!dataLine.contains("interface DataLine")) {
            throw new RuntimeException("Wrong string: " + dataLine);
        }
    }
}
