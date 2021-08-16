/*
 * Copyright (c) 2000, 2003, Oracle and/or its affiliates. All rights reserved.
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

package java.util.prefs;

import java.io.NotSerializableException;

/**
 * An event emitted by a {@code Preferences} node to indicate that
 * a preference has been added, removed or has had its value changed.<p>
 *
 * Note, that although PreferenceChangeEvent inherits Serializable interface
 * from EventObject, it is not intended to be Serializable. Appropriate
 * serialization methods are implemented to throw NotSerializableException.
 *
 * @author  Josh Bloch
 * @see Preferences
 * @see PreferenceChangeListener
 * @see NodeChangeEvent
 * @since   1.4
 * @serial exclude
 */
public class PreferenceChangeEvent extends java.util.EventObject {

    /**
     * Key of the preference that changed.
     *
     * @serial
     */
    private String key;

    /**
     * New value for preference, or {@code null} if it was removed.
     *
     * @serial
     */
    private String newValue;

    /**
     * Constructs a new {@code PreferenceChangeEvent} instance.
     *
     * @param node  The Preferences node that emitted the event.
     * @param key  The key of the preference that was changed.
     * @param newValue  The new value of the preference, or {@code null}
     *                  if the preference is being removed.
     */
    public PreferenceChangeEvent(Preferences node, String key,
                                 String newValue) {
        super(node);
        this.key = key;
        this.newValue = newValue;
    }

    /**
     * Returns the preference node that emitted the event.
     *
     * @return  The preference node that emitted the event.
     */
    public Preferences getNode() {
        return (Preferences) getSource();
    }

    /**
     * Returns the key of the preference that was changed.
     *
     * @return  The key of the preference that was changed.
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the new value for the preference.
     *
     * @return  The new value for the preference, or {@code null} if the
     *          preference was removed.
     */
    public String getNewValue() {
        return newValue;
    }

    /**
     * Throws NotSerializableException, since NodeChangeEvent objects
     * are not intended to be serializable.
     */
     private void writeObject(java.io.ObjectOutputStream out)
                                               throws NotSerializableException {
         throw new NotSerializableException("Not serializable.");
     }

    /**
     * Throws NotSerializableException, since PreferenceChangeEvent objects
     * are not intended to be serializable.
     */
     private void readObject(java.io.ObjectInputStream in)
                                               throws NotSerializableException {
         throw new NotSerializableException("Not serializable.");
     }

    // Defined so that this class isn't flagged as a potential problem when
    // searches for missing serialVersionUID fields are done.
    private static final long serialVersionUID = 793724513368024975L;
}
