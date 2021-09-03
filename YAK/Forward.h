/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Types.h>

namespace YAK {

namespace Detail {
template<size_t inline_capacity>
class ByteBuffer;
}

class Bitmap;
using ByteBuffer = YAK::Detail::ByteBuffer<32>;
class GenericLexer;
class IPv4Address;
class JsonArray;
class JsonObject;
class JsonValue;
class StackInfo;
class String;
class StringBuilder;
class StringImpl;
class StringView;
class Time;
class URL;
class FlyString;
class Utf16View;
class Utf32View;
class Utf8View;
class InputStream;
class InputMemoryStream;
class DuplexMemoryStream;
class OutputStream;
class InputBitStream;
class OutputBitStream;
class OutputMemoryStream;

template<size_t Capacity>
class CircularDuplexStream;

template<typename T>
class Span;

template<typename T, size_t Size>
struct Array;

template<typename Container, typename ValueType>
class SimpleIterator;

using ReadonlyBytes = Span<const u8>;
using Bytes = Span<u8>;

template<typename T, YAK::MemoryOrder DefaultMemoryOrder>
class Atomic;

template<typename T>
class SinglyLinkedList;

template<typename T>
class DoublyLinkedList;

template<typename T, size_t capacity>
class CircularQueue;

template<typename T>
struct Traits;

template<typename T, typename TraitsForT = Traits<T>, bool IsOrdered = false>
class HashTable;

template<typename T, typename TraitsForT = Traits<T>>
using OrderedHashTable = HashTable<T, TraitsForT, true>;

template<typename K, typename V, typename KeyTraits = Traits<K>, bool IsOrdered = false>
class HashMap;

template<typename K, typename V, typename KeyTraits = Traits<K>>
using OrderedHashMap = HashMap<K, V, KeyTraits, true>;

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

template<typename T, size_t inline_capacity = 0>
class NonnullRefPtrVector;

template<typename T, size_t inline_capacity = 0>
class NonnullOwnPtrVector;

template<typename T>
class Optional;

template<typename T>
struct RefPtrTraits;

template<typename T, typename PtrTraits = RefPtrTraits<T>>
class RefPtr;

template<typename T>
class OwnPtr;

template<typename T>
class WeakPtr;

template<typename T, size_t inline_capacity = 0>
requires(!IsRvalueReference<T>) class Vector;

}

using YAK::Array;
using YAK::Atomic;
using YAK::Badge;
using YAK::Bitmap;
using YAK::ByteBuffer;
using YAK::Bytes;
using YAK::CircularDuplexStream;
using YAK::CircularQueue;
using YAK::DoublyLinkedList;
using YAK::DuplexMemoryStream;
using YAK::FixedArray;
using YAK::FlyString;
using YAK::Function;
using YAK::GenericLexer;
using YAK::HashMap;
using YAK::HashTable;
using YAK::InputBitStream;
using YAK::InputMemoryStream;
using YAK::InputStream;
using YAK::IPv4Address;
using YAK::JsonArray;
using YAK::JsonObject;
using YAK::JsonValue;
using YAK::NonnullOwnPtr;
using YAK::NonnullOwnPtrVector;
using YAK::NonnullRefPtr;
using YAK::NonnullRefPtrVector;
using YAK::Optional;
using YAK::OutputBitStream;
using YAK::OutputMemoryStream;
using YAK::OutputStream;
using YAK::OwnPtr;
using YAK::ReadonlyBytes;
using YAK::RefPtr;
using YAK::SinglyLinkedList;
using YAK::Span;
using YAK::StackInfo;
using YAK::String;
using YAK::StringBuilder;
using YAK::StringImpl;
using YAK::StringView;
using YAK::Time;
using YAK::Traits;
using YAK::URL;
using YAK::Utf16View;
using YAK::Utf32View;
using YAK::Utf8View;
using YAK::Vector;
