/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xalan.internal.xsltc.compiler.util;

import com.sun.org.apache.bcel.internal.generic.LocalVariableGen;
import com.sun.org.apache.bcel.internal.generic.Type;

/**
 * @author Jacek Ambroziak
 */
final class SlotAllocator {

    private int   _firstAvailableSlot;
    private int   _size = 8;
    private int   _free = 0;
    private int[] _slotsTaken = new int[_size];

    public void initialize(LocalVariableGen[] vars) {
        final int length = vars.length;
        int slot = 0, size, index;

        for (int i = 0; i < length; i++) {
            size  = vars[i].getType().getSize();
            index = vars[i].getIndex();
            slot  = Math.max(slot, index + size);
        }
        _firstAvailableSlot = slot;
    }

    public int allocateSlot(Type type) {
        final int size = type.getSize();
        final int limit = _free;
        int slot = _firstAvailableSlot, where = 0;

        if (_free + size > _size) {
            final int[] array = new int[_size *= 2];
            for (int j = 0; j < limit; j++)
                array[j] = _slotsTaken[j];
            _slotsTaken = array;
        }

        while (where < limit) {
            if (slot + size <= _slotsTaken[where]) {
                // insert
                for (int j = limit - 1; j >= where; j--)
                    _slotsTaken[j + size] = _slotsTaken[j];
                break;
            }
            else {
                slot = _slotsTaken[where++] + 1;
            }
        }

        for (int j = 0; j < size; j++)
            _slotsTaken[where + j] = slot + j;

        _free += size;
        return slot;
    }

    public void releaseSlot(LocalVariableGen lvg) {
        final int size = lvg.getType().getSize();
        final int slot = lvg.getIndex();
        final int limit = _free;

        for (int i = 0; i < limit; i++) {
            if (_slotsTaken[i] == slot) {
                int j = i + size;
                while (j < limit) {
                    _slotsTaken[i++] = _slotsTaken[j++];
                }
                _free -= size;
                return;
            }
        }
        String state = "Variable slot allocation error"+
                       "(size="+size+", slot="+slot+", limit="+limit+")";
        ErrorMsg err = new ErrorMsg(ErrorMsg.INTERNAL_ERR, state);
        throw new Error(err.toString());
    }
}
