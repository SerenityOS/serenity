/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Modifier;
import java.util.ArrayList;

import jdk.vm.ci.meta.JavaTypeProfile.ProfiledType;

/**
 * This profile object represents the type profile at a specific BCI. The precision of the supplied
 * values may vary, but a runtime that provides this information should be aware that it will be
 * used to guide performance-critical decisions like speculative inlining, etc.
 */
public final class JavaTypeProfile extends AbstractJavaProfile<ProfiledType, ResolvedJavaType> {

    private static final ProfiledType[] EMPTY_ARRAY = new ProfiledType[0];

    private final TriState nullSeen;

    public JavaTypeProfile(TriState nullSeen, double notRecordedProbability, ProfiledType[] pitems) {
        super(notRecordedProbability, pitems);
        this.nullSeen = nullSeen;
    }

    /**
     * Returns whether a null value was at the type check.
     */
    public TriState getNullSeen() {
        return nullSeen;
    }

    /**
     * A list of types for which the runtime has recorded probability information. Note that this
     * includes both positive and negative types where a positive type is a subtype of the checked
     * type and a negative type is not.
     */
    public ProfiledType[] getTypes() {
        return getItems();
    }

    public JavaTypeProfile restrict(JavaTypeProfile otherProfile) {
        if (otherProfile.getNotRecordedProbability() > 0.0) {
            // Not useful for restricting since there is an unknown set of types occurring.
            return this;
        }

        if (this.getNotRecordedProbability() > 0.0) {
            // We are unrestricted, so the other profile is always a better estimate.
            return otherProfile;
        }

        ArrayList<ProfiledType> result = new ArrayList<>();
        for (int i = 0; i < getItems().length; i++) {
            ProfiledType ptype = getItems()[i];
            ResolvedJavaType type = ptype.getItem();
            if (otherProfile.isIncluded(type)) {
                result.add(ptype);
            }
        }

        TriState newNullSeen = (otherProfile.getNullSeen() == TriState.FALSE) ? TriState.FALSE : getNullSeen();
        double newNotRecorded = getNotRecordedProbability();
        return createAdjustedProfile(result, newNullSeen, newNotRecorded);
    }

    public JavaTypeProfile restrict(ResolvedJavaType declaredType, boolean nonNull) {
        ArrayList<ProfiledType> result = new ArrayList<>();
        for (int i = 0; i < getItems().length; i++) {
            ProfiledType ptype = getItems()[i];
            ResolvedJavaType type = ptype.getItem();
            if (declaredType.isAssignableFrom(type)) {
                result.add(ptype);
            }
        }

        TriState newNullSeen = (nonNull) ? TriState.FALSE : getNullSeen();
        double newNotRecorded = this.getNotRecordedProbability();
        // Assume for the types not recorded, the incompatibility rate is the same.
        if (getItems().length != 0) {
            newNotRecorded *= ((double) result.size() / (double) getItems().length);
        }
        return createAdjustedProfile(result, newNullSeen, newNotRecorded);
    }

    private JavaTypeProfile createAdjustedProfile(ArrayList<ProfiledType> result, TriState newNullSeen, double newNotRecorded) {
        if (result.size() != this.getItems().length || newNotRecorded != getNotRecordedProbability() || newNullSeen != getNullSeen()) {
            if (result.size() == 0) {
                return new JavaTypeProfile(newNullSeen, 1.0, EMPTY_ARRAY);
            }
            double factor;
            if (result.size() == this.getItems().length) {
                /* List of types did not change, no need to recompute probabilities. */
                factor = 1.0;
            } else {
                double probabilitySum = 0.0;
                for (int i = 0; i < result.size(); i++) {
                    probabilitySum += result.get(i).getProbability();
                }
                probabilitySum += newNotRecorded;

                factor = 1.0 / probabilitySum; // Normalize to 1.0
                assert factor >= 1.0;
            }
            ProfiledType[] newResult = new ProfiledType[result.size()];
            for (int i = 0; i < newResult.length; ++i) {
                ProfiledType curType = result.get(i);
                newResult[i] = new ProfiledType(curType.getItem(), Math.min(1.0, curType.getProbability() * factor));
            }
            double newNotRecordedTypeProbability = Math.min(1.0, newNotRecorded * factor);
            return new JavaTypeProfile(newNullSeen, newNotRecordedTypeProbability, newResult);
        }
        return this;
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other) && nullSeen.equals(((JavaTypeProfile) other).nullSeen);
    }

    @Override
    public int hashCode() {
        return nullSeen.hashCode() + super.hashCode();
    }

    public static class ProfiledType extends AbstractProfiledItem<ResolvedJavaType> {

        public ProfiledType(ResolvedJavaType type, double probability) {
            super(type, probability);
            assert type.isArray() || type.isConcrete() : type + " " + Modifier.toString(type.getModifiers());
        }

        /**
         * Returns the type for this profile entry.
         */
        public ResolvedJavaType getType() {
            return getItem();
        }

        @Override
        public String toString() {
            return String.format("%.6f#%s", probability, item);
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("JavaTypeProfile<nullSeen=").append(getNullSeen()).append(", types=[");
        for (int j = 0; j < getTypes().length; j++) {
            if (j != 0) {
                buf.append(", ");
            }
            ProfiledType ptype = getTypes()[j];
            buf.append(String.format("%.6f:%s", ptype.getProbability(), ptype.getType()));
        }
        return buf.append(String.format("], notRecorded:%.6f>", getNotRecordedProbability())).toString();
    }

    /**
     * Returns {@code true} if all types seen at this location have been recorded in the profile.
     */
    public boolean allTypesRecorded() {
        return this.getNotRecordedProbability() == 0.0;
    }

    /**
     * Returns the single monormorphic type representing this profile or {@code null} if no such
     * type exists.
     */
    public ResolvedJavaType asSingleType() {
        if (allTypesRecorded() && this.getTypes().length == 1) {
            return getTypes()[0].getType();
        }
        return null;
    }
}
