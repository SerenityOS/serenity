/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package jdk.internal.org.objectweb.asm.commons;

import java.util.ArrayList;
import java.util.List;
import jdk.internal.org.objectweb.asm.Attribute;
import jdk.internal.org.objectweb.asm.ByteVector;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;

/**
 * A ModuleHashes attribute. This attribute is specific to the OpenJDK and may change in the future.
 *
 * @author Remi Forax
 */
public final class ModuleHashesAttribute extends Attribute {

    /** The name of the hashing algorithm. */
    public String algorithm;

    /** A list of module names. */
    public List<String> modules;

    /** The hash of the modules in {@link #modules}. The two lists must have the same size. */
    public List<byte[]> hashes;

    /**
      * Constructs a new {@link ModuleHashesAttribute}.
      *
      * @param algorithm the name of the hashing algorithm.
      * @param modules a list of module names.
      * @param hashes the hash of the modules in 'modules'. The two lists must have the same size.
      */
    public ModuleHashesAttribute(
            final String algorithm, final List<String> modules, final List<byte[]> hashes) {
        super("ModuleHashes");
        this.algorithm = algorithm;
        this.modules = modules;
        this.hashes = hashes;
    }

    /**
      * Constructs an empty {@link ModuleHashesAttribute}. This object can be passed as a prototype to
      * the {@link ClassReader#accept(org.objectweb.asm.ClassVisitor, Attribute[], int)} method.
      */
    public ModuleHashesAttribute() {
        this(null, null, null);
    }

    @Override
    protected Attribute read(
            final ClassReader classReader,
            final int offset,
            final int length,
            final char[] charBuffer,
            final int codeAttributeOffset,
            final Label[] labels) {
        int currentOffset = offset;

        String hashAlgorithm = classReader.readUTF8(currentOffset, charBuffer);
        currentOffset += 2;

        int numModules = classReader.readUnsignedShort(currentOffset);
        currentOffset += 2;

        ArrayList<String> moduleList = new ArrayList<>(numModules);
        ArrayList<byte[]> hashList = new ArrayList<>(numModules);

        for (int i = 0; i < numModules; ++i) {
            String module = classReader.readModule(currentOffset, charBuffer);
            currentOffset += 2;
            moduleList.add(module);

            int hashLength = classReader.readUnsignedShort(currentOffset);
            currentOffset += 2;
            byte[] hash = new byte[hashLength];
            for (int j = 0; j < hashLength; ++j) {
                hash[j] = (byte) (classReader.readByte(currentOffset) & 0xFF);
                currentOffset += 1;
            }
            hashList.add(hash);
        }
        return new ModuleHashesAttribute(hashAlgorithm, moduleList, hashList);
    }

    @Override
    protected ByteVector write(
            final ClassWriter classWriter,
            final byte[] code,
            final int codeLength,
            final int maxStack,
            final int maxLocals) {
        ByteVector byteVector = new ByteVector();
        byteVector.putShort(classWriter.newUTF8(algorithm));
        if (modules == null) {
            byteVector.putShort(0);
        } else {
            int numModules = modules.size();
            byteVector.putShort(numModules);
            for (int i = 0; i < numModules; ++i) {
                String module = modules.get(i);
                byte[] hash = hashes.get(i);
                byteVector
                        .putShort(classWriter.newModule(module))
                        .putShort(hash.length)
                        .putByteArray(hash, 0, hash.length);
            }
        }
        return byteVector;
    }
}
