/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6630275
 * @summary Tests VetoableChangeSupport specification
 * @author Sergey Malenkov
 */

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;

public class Test6630275 {
    private static final String PROPERTY = "property"; // NON-NLS: predefined property name

    public static void main(String[] args) {
        CheckListener first = new CheckListener(false);
        CheckListener second = new CheckListener(true);
        CheckListener third = new CheckListener(false);

        VetoableChangeSupport vcs = new VetoableChangeSupport(Test6630275.class);
        vcs.addVetoableChangeListener(first);
        vcs.addVetoableChangeListener(PROPERTY, first);
        vcs.addVetoableChangeListener(PROPERTY, second);
        vcs.addVetoableChangeListener(PROPERTY, third);
        try {
            vcs.fireVetoableChange(PROPERTY, true, false);
        } catch (PropertyVetoException exception) {
            first.validate();
            second.validate();
            third.validate();
            return; // expected exception
        }
        throw new Error("exception should be thrown");
    }

    private static class CheckListener implements VetoableChangeListener {
        private final boolean veto;
        private boolean odd; // even/odd check for notification

        private CheckListener(boolean veto) {
            this.veto = veto;
        }

        private void validate() {
            if (this.veto != this.odd)
                throw new Error(this.odd
                        ? "undo event expected"
                        : "unexpected undo event");
        }

        public void vetoableChange(PropertyChangeEvent event) throws PropertyVetoException {
            this.odd = !this.odd;
            if (this.veto)
                throw new PropertyVetoException("disable all changes", event);
        }
    }
}
