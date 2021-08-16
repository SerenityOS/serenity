/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#ifndef SHARE_UTILITIES_SIZES_HPP
#define SHARE_UTILITIES_SIZES_HPP

#include "utilities/globalDefinitions.hpp"

// The following two classes are used to represent 'sizes' and 'offsets' in the VM;
// they serve as 'unit' types. ByteSize is used for sizes measured in bytes, while
// WordSize is used for sizes measured in machine words (i.e., 32bit or 64bit words
// depending on platform).
//
// These classes should help doing a transition from (currently) word-size based offsets
// to byte-size based offsets in the VM (this will be important if we desire to pack
// objects more densely in the VM for 64bit machines). Such a transition should proceed
// in two steps to minimize the risk of introducing hard-to-find bugs:
//
// a) first transition the whole VM into a form where all sizes are strongly typed
// b) change all WordSize's to ByteSize's where desired and fix the compilation errors

enum class WordSize : int {};

constexpr WordSize in_WordSize(int size) { return static_cast<WordSize>(size); }
constexpr int      in_words(WordSize x)  { return static_cast<int>(x); }

enum class ByteSize : int {};

constexpr ByteSize in_ByteSize(int size) { return static_cast<ByteSize>(size); }
constexpr int      in_bytes(ByteSize x)  { return static_cast<int>(x); }

constexpr ByteSize operator + (ByteSize x, ByteSize y) { return in_ByteSize(in_bytes(x) + in_bytes(y)); }
constexpr ByteSize operator - (ByteSize x, ByteSize y) { return in_ByteSize(in_bytes(x) - in_bytes(y)); }
constexpr ByteSize operator * (ByteSize x, int      y) { return in_ByteSize(in_bytes(x) * y          ); }

// Use the following #define to get C++ field member offsets

#define byte_offset_of(klass,field)   in_ByteSize((int)offset_of(klass, field))

#endif // SHARE_UTILITIES_SIZES_HPP
