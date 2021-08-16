/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8224184
 * @summary Control Char <UNDEF> check for pty
 * @modules jdk.internal.le/jdk.internal.org.jline.terminal
 *          jdk.internal.le/jdk.internal.org.jline.terminal.impl
 * @requires (os.family == "linux") | (os.family == "aix")
 */

import java.util.List;
import jdk.internal.org.jline.terminal.Attributes;
import jdk.internal.org.jline.terminal.Attributes.ControlChar;
import jdk.internal.org.jline.terminal.Attributes.LocalFlag;
import jdk.internal.org.jline.terminal.impl.ExecPty;

public class ExecPtyGetFlagsToSetTest extends ExecPty {
    public ExecPtyGetFlagsToSetTest(String name, boolean system) {
        super(name, system);
    }

    @Override
    protected List<String> getFlagsToSet(Attributes attr, Attributes current) {
        return super.getFlagsToSet(attr, current);
    }

    public static void main(String[] args) {
        ExecPtyGetFlagsToSetTest testPty =
            new ExecPtyGetFlagsToSetTest("stty", true);

        Attributes attr = new Attributes();
        Attributes current = new Attributes();

        current.setLocalFlag(LocalFlag.ICANON, false);
        current.setControlChar(ControlChar.VMIN, 1);
        current.setControlChar(ControlChar.VTIME, 0);

        attr.setLocalFlag(LocalFlag.ICANON, true);
        attr.setControlChar(ControlChar.VMIN, -1);
        attr.setControlChar(ControlChar.VTIME, -1);

        List<String> commands = testPty.getFlagsToSet(attr, current);
        String result = String.join(" ", commands);
        if (!result.equals("icanon")) {
            throw new RuntimeException("stty options contains invalid value.");
        }
    }
}
