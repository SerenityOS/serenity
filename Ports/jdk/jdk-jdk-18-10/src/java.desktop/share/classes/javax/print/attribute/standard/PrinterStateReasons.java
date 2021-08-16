/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.print.attribute.standard;

import java.io.Serial;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.print.attribute.Attribute;
import javax.print.attribute.PrintServiceAttribute;

/**
 * Class {@code PrinterStateReasons} is a printing attribute class, a set of
 * enumeration values, that provides additional information about the printer's
 * current state, i.e., information that augments the value of the printer's
 * {@link PrinterState PrinterState} attribute.
 * <p>
 * Instances of {@link PrinterStateReason PrinterStateReason} do not appear in a
 * Print Service's attribute set directly. Rather, a {@code PrinterStateReasons}
 * attribute appears in the Print Service's attribute set. The
 * {@code PrinterStateReasons} attribute contains zero, one, or more than one
 * {@link PrinterStateReason PrinterStateReason} objects which pertain to the
 * Print Service's status, and each
 * {@link PrinterStateReason PrinterStateReason} object is associated with a
 * {@link Severity Severity} level of {@code REPORT} (least severe),
 * {@code WARNING}, or {@code ERROR} (most severe). The printer adds a
 * {@link PrinterStateReason PrinterStateReason} object to the Print Service's
 * {@code PrinterStateReasons} attribute when the corresponding condition
 * becomes true of the printer, and the printer removes the
 * {@link PrinterStateReason PrinterStateReason} object again when the
 * corresponding condition becomes false, regardless of whether the Print
 * Service's overall {@link PrinterState PrinterState} also changed.
 * <p>
 * Class PrinterStateReasons inherits its implementation from class
 * {@link HashMap java.util.HashMap}. Each entry in the map consists of a
 * {@link PrinterStateReason PrinterStateReason} object (key) mapping to a
 * {@link Severity Severity} object (value):
 * <p>
 * Unlike most printing attributes which are immutable once constructed, class
 * {@code PrinterStateReasons} is designed to be mutable; you can add
 * {@link PrinterStateReason PrinterStateReason} objects to an existing
 * {@code PrinterStateReasons} object and remove them again. However, like class
 * {@link HashMap java.util.HashMap}, class {@code PrinterStateReasons} is not
 * multiple thread safe. If a {@code PrinterStateReasons} object will be used by
 * multiple threads, be sure to synchronize its operations (e.g., using a
 * synchronized map view obtained from class {@link java.util.Collections
 * java.util.Collections}).
 * <p>
 * <b>IPP Compatibility:</b> The string values returned by each individual
 * {@link PrinterStateReason PrinterStateReason} object's and the associated
 * {@link Severity Severity} object's {@code toString()} methods, concatenated
 * together with a hyphen ({@code "-"}) in between, gives the IPP keyword value.
 * The category name returned by {@code getName()} gives the IPP attribute name.
 *
 * @author Alan Kaminsky
 */
public final class PrinterStateReasons
    extends HashMap<PrinterStateReason,Severity>
    implements PrintServiceAttribute
{

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -3731791085163619457L;

    /**
     * Construct a new, empty printer state reasons attribute; the underlying
     * hash map has the default initial capacity and load factor.
     */
    public PrinterStateReasons() {
        super();
    }

    /**
     * Construct a new, empty printer state reasons attribute; the underlying
     * hash map has the given initial capacity and the default load factor.
     *
     * @param  initialCapacity initial capacity
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public PrinterStateReasons(int initialCapacity) {
        super (initialCapacity);
    }

    /**
     * Construct a new, empty printer state reasons attribute; the underlying
     * hash map has the given initial capacity and load factor.
     *
     * @param  initialCapacity initial capacity
     * @param  loadFactor load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public PrinterStateReasons(int initialCapacity, float loadFactor) {
        super (initialCapacity, loadFactor);
    }

    /**
     * Construct a new printer state reasons attribute that contains the same
     * {@link PrinterStateReason PrinterStateReason}-to-{@link Severity
     * Severity} mappings as the given map. The underlying hash map's initial
     * capacity and load factor are as specified in the superclass constructor
     * {@link HashMap#HashMap(Map) HashMap(Map)}.
     *
     * @param  map map to copy
     * @throws NullPointerException if {@code map} is {@code null} or if any key
     *         or value in {@code map} is {@code null}
     * @throws ClassCastException if any key in {@code map} is not an instance
     *         of class {@link PrinterStateReason PrinterStateReason} or if any
     *         value in {@code map} is not an instance of class
     *         {@link Severity Severity}
     */
    public PrinterStateReasons(Map<PrinterStateReason,Severity> map) {
        this();
        for (Map.Entry<PrinterStateReason,Severity> e : map.entrySet())
            put(e.getKey(), e.getValue());
    }

    /**
     * Adds the given printer state reason to this printer state reasons
     * attribute, associating it with the given severity level. If this printer
     * state reasons attribute previously contained a mapping for the given
     * printer state reason, the old value is replaced.
     *
     * @param  reason printer state reason. This must be an instance of class
     *         {@link PrinterStateReason PrinterStateReason}
     * @param  severity severity of the printer state reason. This must be an
     *         instance of class {@link Severity Severity}
     * @return previous severity associated with the given printer state reason,
     *         or {@code null} if the given printer state reason was not
     *         present
     * @throws NullPointerException if {@code reason} is {@code null} or
     *         {@code severity} is {@code null}
     * @throws ClassCastException if {@code reason} is not an instance of class
     *         {@link PrinterStateReason PrinterStateReason} or if
     *         {@code severity} is not an instance of class
     *         {@link Severity Severity}
     * @since 1.5
     */
    public Severity put(PrinterStateReason reason, Severity severity) {
        if (reason == null) {
            throw new NullPointerException("reason is null");
        }
        if (severity == null) {
            throw new NullPointerException("severity is null");
        }
        return super.put(reason, severity);
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code PrinterStateReasons}, the category is class
     * {@code PrinterStateReasons} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return PrinterStateReasons.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code PrinterStateReasons}, the category name is
     * {@code "printer-state-reasons"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "printer-state-reasons";
    }

    /**
     * Obtain an unmodifiable set view of the individual printer state reason
     * attributes at the given severity level in this
     * {@code PrinterStateReasons} attribute. Each element in the set view is a
     * {@link PrinterStateReason PrinterStateReason} object. The only elements
     * in the set view are the {@link PrinterStateReason PrinterStateReason}
     * objects that map to the given severity value. The set view is backed by
     * this {@code PrinterStateReasons} attribute, so changes to this
     * {@code PrinterStateReasons} attribute are reflected in the set view. The
     * set view does not support element insertion or removal. The set view's
     * iterator does not support element removal.
     *
     * @param  severity severity level
     * @return set view of the individual
     *         {@link PrinterStateReason PrinterStateReason} attributes at the
     *         given {@link Severity Severity} level
     * @throws NullPointerException if {@code severity} is {@code null}
     */
    public Set<PrinterStateReason> printerStateReasonSet(Severity severity) {
        if (severity == null) {
            throw new NullPointerException("severity is null");
        }
        return new PrinterStateReasonSet (severity, entrySet());
    }

    private class PrinterStateReasonSet
        extends AbstractSet<PrinterStateReason>
    {
        private Severity mySeverity;

        private Set<Map.Entry<PrinterStateReason, Severity>> myEntrySet;

        public PrinterStateReasonSet(Severity severity,
                                     Set<Map.Entry<PrinterStateReason, Severity>> entrySet) {
            mySeverity = severity;
            myEntrySet = entrySet;
        }

        public int size() {
            int result = 0;
            Iterator<PrinterStateReason> iter = iterator();
            while (iter.hasNext()) {
                iter.next();
                ++ result;
            }
            return result;
        }

        public Iterator<PrinterStateReason> iterator() {
            return new PrinterStateReasonSetIterator(mySeverity,
                                                     myEntrySet.iterator());
        }
    }

    private class PrinterStateReasonSetIterator implements Iterator<PrinterStateReason> {
        private Severity mySeverity;
        private Iterator<Map.Entry<PrinterStateReason, Severity>> myIterator;
        private Map.Entry<PrinterStateReason, Severity> myEntry;

        public PrinterStateReasonSetIterator(Severity severity,
                                             Iterator<Map.Entry<PrinterStateReason, Severity>> iterator) {
            mySeverity = severity;
            myIterator = iterator;
            goToNext();
        }

        private void goToNext() {
            myEntry = null;
            while (myEntry == null && myIterator.hasNext()) {
                myEntry = myIterator.next();
                if (myEntry.getValue() != mySeverity) {
                    myEntry = null;
                }
            }
        }

        public boolean hasNext() {
            return myEntry != null;
        }

        public PrinterStateReason next() {
            if (myEntry == null) {
                throw new NoSuchElementException();
            }
            PrinterStateReason result = myEntry.getKey();
            goToNext();
            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
