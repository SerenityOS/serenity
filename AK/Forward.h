/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Types.h>

namespace AK {

class Bitmap;
class BufferStream;
class ByteBuffer;
class DebugLogStream;
class IPv4Address;
class JsonArray;
class JsonObject;
class JsonValue;
class LogStream;
class SharedBuffer;
class String;
class StringBuilder;
class StringImpl;
class StringView;
class URL;
class Utf8View;

template<typename T>
class Atomic;

template<typename T>
class SinglyLinkedList;

template<typename T>
class DoublyLinkedList;

template<typename T>
class InlineLinkedList;

template<typename T, size_t capacity>
class CircularQueue;

template<typename T>
struct Traits;

template<typename T, typename = Traits<T>>
class HashTable;

template<typename K, typename V, typename = Traits<K>>
class HashMap;

template<typename T>
class Badge;

template<typename T>
class FixedArray;

template<typename>
class Function;

template<typename Out, typename... In>
class Function<Out(In...)>;

template<typename T>
class NonnullRefPtr;

template<typename T>
class NonnullOwnPtr;

template<typename T>
class Optional;

template<typename T>
class RefPtr;

template<typename T>
class OwnPtr;

template<typename T>
class WeakPtr;

template<typename T, size_t inline_capacity = 0>
class Vector;

}

using AK::Atomic;
using AK::Badge;
using AK::Bitmap;
using AK::BufferStream;
using AK::ByteBuffer;
using AK::CircularQueue;
using AK::DebugLogStream;
using AK::DoublyLinkedList;
using AK::FixedArray;
using AK::Function;
using AK::HashMap;
using AK::HashTable;
using AK::InlineLinkedList;
using AK::IPv4Address;
using AK::JsonArray;
using AK::JsonObject;
using AK::JsonValue;
using AK::LogStream;
using AK::NonnullOwnPtr;
using AK::NonnullRefPtr;
using AK::Optional;
using AK::OwnPtr;
using AK::RefPtr;
using AK::SharedBuffer;
using AK::SinglyLinkedList;
using AK::String;
using AK::StringBuilder;
using AK::StringImpl;
using AK::StringView;
using AK::Traits;
using AK::URL;
using AK::Utf8View;
using AK::Vector;
