/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.instrument;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/*
 * Copyright 2003 Wily Technology, Inc.
 */

/**
 * Support class for the InstrumentationImpl. Manages the list of registered transformers.
 * Keeps everything in the right order, deals with sync of the list,
 * and actually does the calling of the transformers.
 */
public class TransformerManager
{
    private class TransformerInfo {
        final ClassFileTransformer  mTransformer;
        String                      mPrefix;

        TransformerInfo(ClassFileTransformer transformer) {
            mTransformer = transformer;
            mPrefix = null;
        }

        ClassFileTransformer transformer() {
            return  mTransformer;
        }

        String getPrefix() {
            return mPrefix;
        }

        void setPrefix(String prefix) {
            mPrefix = prefix;
        }
    }

    /**
     * a given instance of this list is treated as immutable to simplify sync;
     * we pay copying overhead whenever the list is changed rather than every time
     * the list is referenced.
     * The array is kept in the order the transformers are added via addTransformer
     * (first added is 0, last added is length-1)
     * Use an array, not a List or other Collection. This keeps the set of classes
     * used by this code to a minimum. We want as few dependencies as possible in this
     * code, since it is used inside the class definition system. Any class referenced here
     * cannot be transformed by Java code.
     */
    private TransformerInfo[]  mTransformerList;

    /***
     * Is this TransformerManager for transformers capable of retransformation?
     */
    private boolean            mIsRetransformable;

    TransformerManager(boolean isRetransformable) {
        mTransformerList    = new TransformerInfo[0];
        mIsRetransformable  = isRetransformable;
    }

    boolean isRetransformable() {
        return mIsRetransformable;
    }

    public synchronized void
    addTransformer( ClassFileTransformer    transformer) {
        TransformerInfo[] oldList = mTransformerList;
        TransformerInfo[] newList = new TransformerInfo[oldList.length + 1];
        System.arraycopy(   oldList,
                            0,
                            newList,
                            0,
                            oldList.length);
        newList[oldList.length] = new TransformerInfo(transformer);
        mTransformerList = newList;
    }

    public synchronized boolean
    removeTransformer(ClassFileTransformer  transformer) {
        boolean                 found           = false;
        TransformerInfo[]       oldList         = mTransformerList;
        int                     oldLength       = oldList.length;
        int                     newLength       = oldLength - 1;

        // look for it in the list, starting at the last added, and remember
        // where it was if we found it
        int matchingIndex   = 0;
        for ( int x = oldLength - 1; x >= 0; x-- ) {
            if ( oldList[x].transformer() == transformer ) {
                found           = true;
                matchingIndex   = x;
                break;
            }
        }

        // make a copy of the array without the matching element
        if ( found ) {
            TransformerInfo[]  newList = new TransformerInfo[newLength];

            // copy up to but not including the match
            if ( matchingIndex > 0 ) {
                System.arraycopy(   oldList,
                                    0,
                                    newList,
                                    0,
                                    matchingIndex);
            }

            // if there is anything after the match, copy it as well
            if ( matchingIndex < (newLength) ) {
                System.arraycopy(   oldList,
                                    matchingIndex + 1,
                                    newList,
                                    matchingIndex,
                                    (newLength) - matchingIndex);
            }
            mTransformerList = newList;
        }
        return found;
    }

    synchronized boolean
    includesTransformer(ClassFileTransformer transformer) {
        for (TransformerInfo info : mTransformerList) {
            if ( info.transformer() == transformer ) {
                return true;
            }
        }
        return false;
    }

    // This function doesn't actually snapshot anything, but should be
    // used to set a local variable, which will snapshot the transformer
    // list because of the copying semantics of mTransformerList (see
    // the comment for mTransformerList).
    private TransformerInfo[]
    getSnapshotTransformerList() {
        return mTransformerList;
    }

    public byte[]
    transform(  Module              module,
                ClassLoader         loader,
                String              classname,
                Class<?>            classBeingRedefined,
                ProtectionDomain    protectionDomain,
                byte[]              classfileBuffer) {
        boolean someoneTouchedTheBytecode = false;

        TransformerInfo[]  transformerList = getSnapshotTransformerList();

        byte[]  bufferToUse = classfileBuffer;

        // order matters, gotta run 'em in the order they were added
        for ( int x = 0; x < transformerList.length; x++ ) {
            TransformerInfo         transformerInfo = transformerList[x];
            ClassFileTransformer    transformer = transformerInfo.transformer();
            byte[]                  transformedBytes = null;

            try {
                transformedBytes = transformer.transform(   module,
                                                            loader,
                                                            classname,
                                                            classBeingRedefined,
                                                            protectionDomain,
                                                            bufferToUse);
            }
            catch (Throwable t) {
                // don't let any one transformer mess it up for the others.
                // This is where we need to put some logging. What should go here? FIXME
            }

            if ( transformedBytes != null ) {
                someoneTouchedTheBytecode = true;
                bufferToUse = transformedBytes;
            }
        }

        // if someone modified it, return the modified buffer.
        // otherwise return null to mean "no transforms occurred"
        byte [] result;
        if ( someoneTouchedTheBytecode ) {
            result = bufferToUse;
        }
        else {
            result = null;
        }

        return result;
    }


    int
    getTransformerCount() {
        TransformerInfo[]  transformerList = getSnapshotTransformerList();
        return transformerList.length;
    }

    boolean
    setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
        TransformerInfo[]  transformerList = getSnapshotTransformerList();

        for ( int x = 0; x < transformerList.length; x++ ) {
            TransformerInfo         transformerInfo = transformerList[x];
            ClassFileTransformer    aTransformer = transformerInfo.transformer();

            if ( aTransformer == transformer ) {
                transformerInfo.setPrefix(prefix);
                return true;
            }
        }
        return false;
    }


    String[]
    getNativeMethodPrefixes() {
        TransformerInfo[]  transformerList = getSnapshotTransformerList();
        String[] prefixes                  = new String[transformerList.length];

        for ( int x = 0; x < transformerList.length; x++ ) {
            TransformerInfo         transformerInfo = transformerList[x];
            prefixes[x] = transformerInfo.getPrefix();
        }
        return prefixes;
    }
}
