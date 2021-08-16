/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.openmbean;

/**
 * <p>A Java class can implement this interface to indicate how it is
 * to be converted into a {@code CompositeData} by the MXBean framework.</p>
 *
 * <p>A typical way to use this class is to add extra items to the
 * {@code CompositeData} in addition to the ones that are declared in the
 * {@code CompositeType} supplied by the MXBean framework.  To do this,
 * you must create another {@code CompositeType} that has all the same items,
 * plus your extra items.</p>
 *
 * <p>For example, suppose you have a class {@code Measure} that consists of
 * a String called {@code units} and a {@code value} that is either a
 * {@code long} or a {@code double}.  It might look like this:</p>
 *
 * <pre>
 * public class Measure implements CompositeDataView {
 *     private String units;
 *     private Number value; // a Long or a Double
 *
 *     public Measure(String units, Number value) {
 *         this.units = units;
 *         this.value = value;
 *     }
 *
 *     public static Measure from(CompositeData cd) {
 *         return new Measure((String) cd.get("units"),
 *                            (Number) cd.get("value"));
 *     }
 *
 *     public String getUnits() {
 *         return units;
 *     }
 *
 *     // Can't be called getValue(), because Number is not a valid type
 *     // in an MXBean, so the implied "value" property would be rejected.
 *     public Number _getValue() {
 *         return value;
 *     }
 *
 *     public CompositeData toCompositeData(CompositeType ct) {
 *         try {
 *             {@code List<String> itemNames = new ArrayList<String>(ct.keySet());}
 *             {@code List<String> itemDescriptions = new ArrayList<String>();}
 *             {@code List<OpenType<?>> itemTypes = new ArrayList<OpenType<?>>();}
 *             for (String item : itemNames) {
 *                 itemDescriptions.add(ct.getDescription(item));
 *                 itemTypes.add(ct.getType(item));
 *             }
 *             itemNames.add("value");
 *             itemDescriptions.add("long or double value of the measure");
 *             itemTypes.add((value instanceof Long) ? SimpleType.LONG :
 *                           SimpleType.DOUBLE);
 *             CompositeType xct =
 *                 new CompositeType(ct.getTypeName(),
 *                                   ct.getDescription(),
 *                                   itemNames.toArray(new String[0]),
 *                                   itemDescriptions.toArray(new String[0]),
 *                                   itemTypes.toArray(new OpenType&lt;?&gt;[0]));
 *             CompositeData cd =
 *                 new CompositeDataSupport(xct,
 *                                          new String[] {"units", "value"},
 *                                          new Object[] {units, value});
 *             assert ct.isValue(cd);  // check we've done it right
 *             return cd;
 *         } catch (Exception e) {
 *             throw new RuntimeException(e);
 *         }
 *     }
 * }
 * </pre>
 *
 * <p>The {@code CompositeType} that will appear in the {@code openType} field
 * of the {@link javax.management.Descriptor Descriptor} for an attribute or
 * operation of this type will show only the {@code units} item, but the actual
 * {@code CompositeData} that is generated will have both {@code units} and
 * {@code value}.</p>
 *
 * @see javax.management.MXBean
 *
 * @since 1.6
 */
public interface CompositeDataView {
    /**
     * <p>Return a {@code CompositeData} corresponding to the values in
     * this object.  The returned value should usually be an instance of
     * {@link CompositeDataSupport}, or a class that serializes as a
     * {@code CompositeDataSupport} via a {@code writeReplace} method.
     * Otherwise, a remote client that receives the object might not be
     * able to reconstruct it.
     *
     * @param ct The expected {@code CompositeType} of the returned
     * value.  If the returned value is {@code cd}, then
     * {@code cd.getCompositeType().equals(ct)} should be true.
     * Typically this will be because {@code cd} is a
     * {@link CompositeDataSupport} constructed with {@code ct} as its
     * {@code CompositeType}.
     *
     * @return the {@code CompositeData}.
     */
    public CompositeData toCompositeData(CompositeType ct);
}
