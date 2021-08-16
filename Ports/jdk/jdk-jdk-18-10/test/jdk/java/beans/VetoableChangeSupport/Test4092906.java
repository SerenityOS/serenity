/*
 * Copyright (c) 1998, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4092906
 * @summary Tests that hasListeners() is not failed across serialization
 * @author Graham Hamilton
 */

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Test4092906 {
    private static final String PUBLIC = "public";
    private static final String PRIVATE = "private";

    public static void main(String[] args) {
        VetoableChangeSupport pcs = new VetoableChangeSupport(args);
        pcs.addVetoableChangeListener(PUBLIC, new PublicListener());
        pcs.addVetoableChangeListener(PRIVATE, new PrivateListener());

        if (!pcs.hasListeners(PUBLIC)) {
            throw new Error("no public listener");
        }
        if (!pcs.hasListeners(PRIVATE)) {
            throw new Error("no private listener");
        }

        try {
            // serialize into byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream output = new ObjectOutputStream(baos);
            output.writeObject(pcs);
            output.flush();
            // deserialize from byte array
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream input = new ObjectInputStream(bais);
            pcs = (VetoableChangeSupport) input.readObject();
        } catch (Exception exception) {
            throw new Error("unexpected exception", exception);
        }

        if (!pcs.hasListeners(PUBLIC)) {
            throw new Error("no public listener");
        }
        if (pcs.hasListeners(PRIVATE)) {
            throw new Error("unexpected private listener");
        }
    }

    public static class PublicListener implements VetoableChangeListener, Serializable {
        public void vetoableChange(PropertyChangeEvent event) throws PropertyVetoException {
        }
    }

    private static class PrivateListener implements VetoableChangeListener {
        public void vetoableChange(PropertyChangeEvent event) throws PropertyVetoException {
        }
    }
}
