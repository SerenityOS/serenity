/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.meta;

/**
 * This object holds probability information for a set of items that were profiled at a specific
 * BCI. The precision of the supplied values may vary, but a runtime that provides this information
 * should be aware that it will be used to guide performance-critical decisions like speculative
 * inlining, etc.
 *
 * @param <T> a subclass of AbstractProfiledItem
 * @param <U> the class of the items that are profiled at the specific BCI and for which
 *            probabilities are stored. E.g., a ResolvedJavaType or a ResolvedJavaMethod.
 */
public abstract class AbstractJavaProfile<T extends AbstractProfiledItem<U>, U> {

    private final double notRecordedProbability;
    private final T[] pitems;

    /**
     *
     * @param notRecordedProbability
     * @param pitems
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "caller transfers ownership of the `pitems` array parameter")
    public AbstractJavaProfile(double notRecordedProbability, T[] pitems) {
        this.pitems = pitems;
        assert !Double.isNaN(notRecordedProbability);
        this.notRecordedProbability = notRecordedProbability;
        assert isSorted();
        assert totalProbablility() >= 0 && totalProbablility() <= 1.0001 : totalProbablility() + " " + this;
    }

    private double totalProbablility() {
        double total = notRecordedProbability;
        for (T item : pitems) {
            total += item.probability;
        }
        return total;
    }

    /**
     * Determines if an array of profiled items are sorted in descending order of their
     * probabilities.
     */
    private boolean isSorted() {
        for (int i = 1; i < pitems.length; i++) {
            if (pitems[i - 1].getProbability() < pitems[i].getProbability()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the estimated probability of all types that could not be recorded due to profiling
     * limitations.
     *
     * @return double value &ge; 0.0 and &le; 1.0
     */
    public double getNotRecordedProbability() {
        return notRecordedProbability;
    }

    protected T[] getItems() {
        return pitems;
    }

    /**
     * Searches for an entry of a given resolved Java type.
     *
     * @param type the type for which an entry should be searched
     * @return the entry or null if no entry for this type can be found
     */
    public T findEntry(ResolvedJavaType type) {
        if (pitems != null) {
            for (T pt : pitems) {
                if (pt.getItem().equals(type)) {
                    return pt;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.getClass().getName());
        builder.append("[");
        if (pitems != null) {
            for (T pt : pitems) {
                builder.append(pt.toString());
                builder.append(", ");
            }
        }
        builder.append(this.notRecordedProbability);
        builder.append("]");
        return builder.toString();
    }

    public boolean isIncluded(U item) {
        if (this.getNotRecordedProbability() > 0.0) {
            return true;
        } else {
            for (int i = 0; i < getItems().length; i++) {
                T pitem = getItems()[i];
                U curType = pitem.getItem();
                if (curType == item) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AbstractJavaProfile)) {
            return false;
        }
        AbstractJavaProfile<?, ?> that = (AbstractJavaProfile<?, ?>) obj;
        if (that.notRecordedProbability != notRecordedProbability) {
            return false;
        }
        if (that.pitems.length != pitems.length) {
            return false;
        }
        for (int i = 0; i < pitems.length; ++i) {
            if (!pitems[i].equals(that.pitems[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return (int) Double.doubleToLongBits(notRecordedProbability) + pitems.length * 13;
    }
}
