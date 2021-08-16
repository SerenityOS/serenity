/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.Patch;

/**
 * A simple instrument that is made of other ModelInstrument, ModelPerformer
 * objects.
 *
 * @author Karl Helgason
 */
public class SimpleInstrument extends ModelInstrument {

    private static class SimpleInstrumentPart {
        ModelPerformer[] performers;
        int keyFrom;
        int keyTo;
        int velFrom;
        int velTo;
        int exclusiveClass;
    }
    protected int preset = 0;
    protected int bank = 0;
    protected boolean percussion = false;
    protected String name = "";
    protected List<SimpleInstrumentPart> parts = new ArrayList<>();

    public SimpleInstrument() {
        super(null, null, null, null);
    }

    public void clear() {
        parts.clear();
    }

    public void add(ModelPerformer[] performers, int keyFrom, int keyTo,
            int velFrom, int velTo, int exclusiveClass) {
        SimpleInstrumentPart part = new SimpleInstrumentPart();
        part.performers = performers;
        part.keyFrom = keyFrom;
        part.keyTo = keyTo;
        part.velFrom = velFrom;
        part.velTo = velTo;
        part.exclusiveClass = exclusiveClass;
        parts.add(part);
    }

    public void add(ModelPerformer[] performers, int keyFrom, int keyTo,
            int velFrom, int velTo) {
        add(performers, keyFrom, keyTo, velFrom, velTo, -1);
    }

    public void add(ModelPerformer[] performers, int keyFrom, int keyTo) {
        add(performers, keyFrom, keyTo, 0, 127, -1);
    }

    public void add(ModelPerformer[] performers) {
        add(performers, 0, 127, 0, 127, -1);
    }

    public void add(ModelPerformer performer, int keyFrom, int keyTo,
            int velFrom, int velTo, int exclusiveClass) {
        add(new ModelPerformer[]{performer}, keyFrom, keyTo, velFrom, velTo,
                exclusiveClass);
    }

    public void add(ModelPerformer performer, int keyFrom, int keyTo,
            int velFrom, int velTo) {
        add(new ModelPerformer[]{performer}, keyFrom, keyTo, velFrom, velTo);
    }

    public void add(ModelPerformer performer, int keyFrom, int keyTo) {
        add(new ModelPerformer[]{performer}, keyFrom, keyTo);
    }

    public void add(ModelPerformer performer) {
        add(new ModelPerformer[]{performer});
    }

    public void add(ModelInstrument ins, int keyFrom, int keyTo, int velFrom,
            int velTo, int exclusiveClass) {
        add(ins.getPerformers(), keyFrom, keyTo, velFrom, velTo, exclusiveClass);
    }

    public void add(ModelInstrument ins, int keyFrom, int keyTo, int velFrom,
            int velTo) {
        add(ins.getPerformers(), keyFrom, keyTo, velFrom, velTo);
    }

    public void add(ModelInstrument ins, int keyFrom, int keyTo) {
        add(ins.getPerformers(), keyFrom, keyTo);
    }

    public void add(ModelInstrument ins) {
        add(ins.getPerformers());
    }

    @Override
    public ModelPerformer[] getPerformers() {

        int percount = 0;
        for (SimpleInstrumentPart part : parts)
            if (part.performers != null)
                percount += part.performers.length;

        ModelPerformer[] performers = new ModelPerformer[percount];
        int px = 0;
        for (SimpleInstrumentPart part : parts) {
            if (part.performers != null) {
                for (ModelPerformer mperfm : part.performers) {
                    ModelPerformer performer = new ModelPerformer();
                    performer.setName(getName());
                    performers[px++] = performer;

                    performer.setDefaultConnectionsEnabled(
                            mperfm.isDefaultConnectionsEnabled());
                    performer.setKeyFrom(mperfm.getKeyFrom());
                    performer.setKeyTo(mperfm.getKeyTo());
                    performer.setVelFrom(mperfm.getVelFrom());
                    performer.setVelTo(mperfm.getVelTo());
                    performer.setExclusiveClass(mperfm.getExclusiveClass());
                    performer.setSelfNonExclusive(mperfm.isSelfNonExclusive());
                    performer.setReleaseTriggered(mperfm.isReleaseTriggered());
                    if (part.exclusiveClass != -1)
                        performer.setExclusiveClass(part.exclusiveClass);
                    if (part.keyFrom > performer.getKeyFrom())
                        performer.setKeyFrom(part.keyFrom);
                    if (part.keyTo < performer.getKeyTo())
                        performer.setKeyTo(part.keyTo);
                    if (part.velFrom > performer.getVelFrom())
                        performer.setVelFrom(part.velFrom);
                    if (part.velTo < performer.getVelTo())
                        performer.setVelTo(part.velTo);
                    performer.getOscillators().addAll(mperfm.getOscillators());
                    performer.getConnectionBlocks().addAll(
                            mperfm.getConnectionBlocks());
                }
            }
        }

        return performers;
    }

    @Override
    public Object getData() {
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ModelPatch getPatch() {
        return new ModelPatch(bank, preset, percussion);
    }

    public void setPatch(Patch patch) {
        if (patch instanceof ModelPatch && ((ModelPatch)patch).isPercussion()) {
            percussion = true;
            bank = patch.getBank();
            preset = patch.getProgram();
        } else {
            percussion = false;
            bank = patch.getBank();
            preset = patch.getProgram();
        }
    }
}
