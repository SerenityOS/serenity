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

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to define how to synthesize audio in universal maner
 * for both SF2 and DLS instruments.
 *
 * @author Karl Helgason
 */
public final class ModelPerformer {

    private final List<ModelOscillator> oscillators = new ArrayList<>();
    private List<ModelConnectionBlock> connectionBlocks = new ArrayList<>();

    private int keyFrom = 0;
    private int keyTo = 127;
    private int velFrom = 0;
    private int velTo = 127;
    private int exclusiveClass = 0;
    private boolean releaseTrigger = false;
    private boolean selfNonExclusive = false;
    private Object userObject = null;
    private boolean addDefaultConnections = true;
    private String name = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ModelConnectionBlock> getConnectionBlocks() {
        return connectionBlocks;
    }

    public void setConnectionBlocks(List<ModelConnectionBlock> connectionBlocks) {
        this.connectionBlocks = connectionBlocks;
    }

    public List<ModelOscillator> getOscillators() {
        return oscillators;
    }

    public int getExclusiveClass() {
        return exclusiveClass;
    }

    public void setExclusiveClass(int exclusiveClass) {
        this.exclusiveClass = exclusiveClass;
    }

    public boolean isSelfNonExclusive() {
        return selfNonExclusive;
    }

    public void setSelfNonExclusive(boolean selfNonExclusive) {
        this.selfNonExclusive = selfNonExclusive;
    }

    public int getKeyFrom() {
        return keyFrom;
    }

    public void setKeyFrom(int keyFrom) {
        this.keyFrom = keyFrom;
    }

    public int getKeyTo() {
        return keyTo;
    }

    public void setKeyTo(int keyTo) {
        this.keyTo = keyTo;
    }

    public int getVelFrom() {
        return velFrom;
    }

    public void setVelFrom(int velFrom) {
        this.velFrom = velFrom;
    }

    public int getVelTo() {
        return velTo;
    }

    public void setVelTo(int velTo) {
        this.velTo = velTo;
    }

    public boolean isReleaseTriggered() {
        return releaseTrigger;
    }

    public void setReleaseTriggered(boolean value) {
        this.releaseTrigger = value;
    }

    public Object getUserObject() {
        return userObject;
    }

    public void setUserObject(Object object) {
        userObject = object;
    }

    public boolean isDefaultConnectionsEnabled() {
        return addDefaultConnections;
    }

    public void setDefaultConnectionsEnabled(boolean addDefaultConnections) {
        this.addDefaultConnections = addDefaultConnections;
    }
}
