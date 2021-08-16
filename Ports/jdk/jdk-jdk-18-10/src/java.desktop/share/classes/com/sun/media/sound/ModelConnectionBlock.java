/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.sound;

import java.util.Arrays;

/**
 * Connection blocks are used to connect source variable
 * to a destination variable.
 * For example Note On velocity can be connected to output gain.
 * In DLS this is called articulator and in SoundFonts (SF2) a modulator.
 *
 * @author Karl Helgason
 */
public final class ModelConnectionBlock {

    //
    //   source1 * source2 * scale -> destination
    //
    private static final ModelSource[] no_sources = new ModelSource[0];
    private ModelSource[] sources = no_sources;
    private double scale = 1;
    private ModelDestination destination;

    public ModelConnectionBlock() {
    }

    public ModelConnectionBlock(double scale, ModelDestination destination) {
        this.scale = scale;
        this.destination = destination;
    }

    public ModelConnectionBlock(ModelSource source,
            ModelDestination destination) {
        if (source != null) {
            this.sources = new ModelSource[1];
            this.sources[0] = source;
        }
        this.destination = destination;
    }

    public ModelConnectionBlock(ModelSource source, double scale,
            ModelDestination destination) {
        if (source != null) {
            this.sources = new ModelSource[1];
            this.sources[0] = source;
        }
        this.scale = scale;
        this.destination = destination;
    }

    public ModelConnectionBlock(ModelSource source, ModelSource control,
            ModelDestination destination) {
        if (source != null) {
            if (control == null) {
                this.sources = new ModelSource[1];
                this.sources[0] = source;
            } else {
                this.sources = new ModelSource[2];
                this.sources[0] = source;
                this.sources[1] = control;
            }
        }
        this.destination = destination;
    }

    public ModelConnectionBlock(ModelSource source, ModelSource control,
            double scale, ModelDestination destination) {
        if (source != null) {
            if (control == null) {
                this.sources = new ModelSource[1];
                this.sources[0] = source;
            } else {
                this.sources = new ModelSource[2];
                this.sources[0] = source;
                this.sources[1] = control;
            }
        }
        this.scale = scale;
        this.destination = destination;
    }

    public ModelDestination getDestination() {
        return destination;
    }

    public void setDestination(ModelDestination destination) {
        this.destination = destination;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public ModelSource[] getSources() {
        return Arrays.copyOf(sources, sources.length);
    }

    public void setSources(ModelSource[] source) {
        this.sources = source == null ? no_sources : Arrays.copyOf(source, source.length);
    }

    public void addSource(ModelSource source) {
        ModelSource[] oldsources = sources;
        sources = new ModelSource[oldsources.length + 1];
        System.arraycopy(oldsources, 0, sources, 0, oldsources.length);
        sources[sources.length - 1] = source;
    }
}
