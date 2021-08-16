/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * An implementation of the Subject class that provides basic functionality
 * for the construction of Subject objects with null Principal elements.
 * This is a helper class for serialization tests tied to bug 8015081
 * (see SubjectNullTests.java).
 */
package jjjjj.security.auth;

import javax.management.remote.JMXPrincipal;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.lang.Exception;
import java.lang.RuntimeException;
import java.security.Principal;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import java.io.FileOutputStream;

public class Subject implements java.io.Serializable {

    private static final long serialVersionUID = -8308522755600156056L;

    Set<Principal> principals;
    private volatile boolean readOnly = false;
    private static final int PRINCIPAL_SET = 1;

    public Subject(Set<? extends Principal> principals) {
        this.principals = Collections.synchronizedSet(new SecureSet<Principal>
                (this, PRINCIPAL_SET, principals));
    }

    public Set<Principal> getPrincipals() {
        return principals;
    }

    private static class SecureSet<E>
            extends AbstractSet<E>
            implements java.io.Serializable {

        private static final long serialVersionUID = 7911754171111800359L;
        private static final ObjectStreamField[] serialPersistentFields = {
                new ObjectStreamField("this$0", Subject.class),
                new ObjectStreamField("elements", LinkedList.class),
                new ObjectStreamField("which", int.class)
        };

        Subject subject;
        LinkedList<E> elements;
        private int which;

        SecureSet(Subject subject, int which, Set<? extends E> set) {
            this.subject = subject;
            this.which = which;
            this.elements = new LinkedList<E>(set);
        }

        public Iterator<E> iterator() {
            return elements.iterator();
        }

        public int size() {
            return elements.size();
        }

        private void writeObject(java.io.ObjectOutputStream oos)
                throws java.io.IOException {

            ObjectOutputStream.PutField fields = oos.putFields();
            fields.put("this$0", subject);
            fields.put("elements", elements);
            fields.put("which", which);
            oos.writeFields();
        }
    }

    public static byte[] enc(Object obj) {
        try {
            ByteArrayOutputStream bout;
            bout = new ByteArrayOutputStream();
            new ObjectOutputStream(bout).writeObject(obj);
            byte[] data = bout.toByteArray();
            for (int i = 0; i < data.length - 5; i++) {
                if (data[i] == 'j' && data[i + 1] == 'j' && data[i + 2] == 'j'
                        && data[i + 3] == 'j' && data[i + 4] == 'j') {
                    System.arraycopy("javax".getBytes(), 0, data, i, 5);
                }
            }
            return data;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
