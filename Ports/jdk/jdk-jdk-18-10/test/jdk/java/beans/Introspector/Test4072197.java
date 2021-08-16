/*
 * Copyright (c) 1997, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4072197
 * @summary Tests for null in the array of listener method names
 * @author Graham Hamilton
 */

import java.awt.event.ActionListener;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.util.EventObject;

public class Test4072197 {
    public static void main(String[] args) {
        try {
            new EventSetDescriptor(
                    SourceClass.class,
                    "action",
                    Listener.class,
                    new String[] {"action", "event", null, "dummy"},
                    "addActionListener",
                    "removeActionListener"
            );
        } catch (IntrospectionException exception) {
            throw new Error("unexpected exception", exception);
        } catch (NullPointerException exception) {
            return; // we caught the NullPointerException as expected
        }
        throw new Error("NullPointerException expected");
    }

    public static class SourceClass {
        public void addActionListener(ActionListener listener) {
        }

        public void removeActionListener(ActionListener listener) {
        }
    }

    public static class Listener {
        public void action(EventObject event) {
        }

        public void event(EventObject event) {
        }

        public void dummy(EventObject event) {
        }
    }
}
