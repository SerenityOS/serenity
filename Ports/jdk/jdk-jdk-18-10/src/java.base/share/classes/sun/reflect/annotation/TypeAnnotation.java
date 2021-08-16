/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
package sun.reflect.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.AnnotatedElement;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A TypeAnnotation contains all the information needed to transform type
 * annotations on declarations in the class file to actual Annotations in
 * AnnotatedType instances.
 *
 * TypeAnnotaions contain a base Annotation, location info (which lets you
 * distinguish between '@A Inner.@B Outer' in for example nested types),
 * target info and the declaration the TypeAnnotaiton was parsed from.
 */
public final class TypeAnnotation {
    private final TypeAnnotationTargetInfo targetInfo;
    private final LocationInfo loc;
    private final Annotation annotation;
    private final AnnotatedElement baseDeclaration;

    public TypeAnnotation(TypeAnnotationTargetInfo targetInfo,
                          LocationInfo loc,
                          Annotation annotation,
                          AnnotatedElement baseDeclaration) {
        this.targetInfo = targetInfo;
        this.loc = loc;
        this.annotation = annotation;
        this.baseDeclaration = baseDeclaration;
    }

    public TypeAnnotationTargetInfo getTargetInfo() {
        return targetInfo;
    }
    public Annotation getAnnotation() {
        return annotation;
    }
    public AnnotatedElement getBaseDeclaration() {
        return baseDeclaration;
    }
    public LocationInfo getLocationInfo() {
        return loc;
    }

    public static List<TypeAnnotation> filter(TypeAnnotation[] typeAnnotations,
                                              TypeAnnotationTarget predicate) {
        ArrayList<TypeAnnotation> typeAnnos = new ArrayList<>(typeAnnotations.length);
        for (TypeAnnotation t : typeAnnotations)
            if (t.getTargetInfo().getTarget() == predicate)
                typeAnnos.add(t);
        typeAnnos.trimToSize();
        return typeAnnos;
    }

    public static enum TypeAnnotationTarget {
        CLASS_TYPE_PARAMETER,
        METHOD_TYPE_PARAMETER,
        CLASS_EXTENDS,
        CLASS_IMPLEMENTS, // Not in the spec
        CLASS_TYPE_PARAMETER_BOUND,
        METHOD_TYPE_PARAMETER_BOUND,
        FIELD,
        METHOD_RETURN,
        METHOD_RECEIVER,
        METHOD_FORMAL_PARAMETER,
        THROWS,
        /**
         * @since 16
         */
        RECORD_COMPONENT;
    }

    public static final class TypeAnnotationTargetInfo {
        private final TypeAnnotationTarget target;
        private final int count;
        private final int secondaryIndex;
        private static final int UNUSED_INDEX = -2; // this is not a valid index in the 308 spec

        public TypeAnnotationTargetInfo(TypeAnnotationTarget target) {
            this(target, UNUSED_INDEX, UNUSED_INDEX);
        }

        public TypeAnnotationTargetInfo(TypeAnnotationTarget target,
                                        int count) {
            this(target, count, UNUSED_INDEX);
        }

        public TypeAnnotationTargetInfo(TypeAnnotationTarget target,
                                        int count,
                                        int secondaryIndex) {
            this.target = target;
            this.count = count;
            this.secondaryIndex = secondaryIndex;
        }

        public TypeAnnotationTarget getTarget() {
            return target;
        }
        public int getCount() {
            return count;
        }
        public int getSecondaryIndex() {
            return secondaryIndex;
        }

        @Override
        public String toString() {
            return "" + target + ": " + count + ", " + secondaryIndex;
        }
    }

    public static final class LocationInfo {
        private final int depth;
        private final Location[] locations;

        private LocationInfo() {
            this(0, new Location[0]);
        }
        private LocationInfo(int depth, Location[] locations) {
            this.depth = depth;
            this.locations = locations;
        }

        public static final LocationInfo BASE_LOCATION = new LocationInfo();

        public static LocationInfo parseLocationInfo(ByteBuffer buf) {
            int depth = buf.get() & 0xFF;
            if (depth == 0)
                return BASE_LOCATION;
            Location[] locations = new Location[depth];
            for (int i = 0; i < depth; i++) {
                byte tag = buf.get();
                short index = (short)(buf.get() & 0xFF);
                if (!(tag == 0 || tag == 1 | tag == 2 || tag == 3))
                    throw new AnnotationFormatError("Bad Location encoding in Type Annotation");
                if (tag != 3 && index != 0)
                    throw new AnnotationFormatError("Bad Location encoding in Type Annotation");
                locations[i] = new Location(tag, index);
            }
            return new LocationInfo(depth, locations);
        }

        public LocationInfo pushArray() {
            return pushLocation((byte)0, (short)0);
        }

        public LocationInfo pushInner() {
            return pushLocation((byte)1, (short)0);
        }

        public LocationInfo pushWildcard() {
            return pushLocation((byte) 2, (short) 0);
        }

        public LocationInfo pushTypeArg(short index) {
            return pushLocation((byte) 3, index);
        }

        public LocationInfo pushLocation(byte tag, short index) {
            int newDepth = this.depth + 1;
            Location[] res = new Location[newDepth];
            System.arraycopy(this.locations, 0, res, 0, depth);
            res[newDepth - 1] = new Location(tag, (short)(index & 0xFF));
            return new LocationInfo(newDepth, res);
        }

        /**
         * Pops a location matching {@code tag}, or returns {@code null}
         * if no matching location was found.
         */
        public LocationInfo popLocation(byte tag) {
            if (depth == 0 || locations[depth - 1].tag != tag) {
                return null;
            }
            Location[] res = new Location[depth - 1];
            System.arraycopy(locations, 0, res, 0, depth - 1);
            return new LocationInfo(depth - 1, res);
        }

        public TypeAnnotation[] filter(TypeAnnotation[] ta) {
            ArrayList<TypeAnnotation> l = new ArrayList<>(ta.length);
            for (TypeAnnotation t : ta) {
                if (isSameLocationInfo(t.getLocationInfo()))
                    l.add(t);
            }
            return l.toArray(AnnotatedTypeFactory.EMPTY_TYPE_ANNOTATION_ARRAY);
        }

        boolean isSameLocationInfo(LocationInfo other) {
            if (depth != other.depth)
                return false;
            for (int i = 0; i < depth; i++)
                if (!locations[i].isSameLocation(other.locations[i]))
                    return false;
            return true;
        }

        public static final class Location {
            public final byte tag;
            public final short index;

            boolean isSameLocation(Location other) {
                return tag == other.tag && index == other.index;
            }

            public Location(byte tag, short index) {
                this.tag = tag;
                this.index = index;
            }
        }
    }

    @Override
    public String toString() {
        return annotation.toString() + " with Targetnfo: " +
            targetInfo.toString() + " on base declaration: " +
            baseDeclaration.toString();
    }
}
