/*
 * Copyright (c) 2021, Peter Elliott <pelliott@ualberta.ca>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Utf16View.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibCore/IODevice.h>

namespace X {

template<typename T>
T deserialize(ReadonlyBytes const& bytes, size_t offset);

template<typename T>
T deserialize(ReadonlyBytes const& bytes, size_t offset) requires Integral<T>
{
    return *(bytes.slice(offset, sizeof(T)).data());
}

template<typename T>
T deserialize(ReadonlyBytes const& bytes, size_t offset) requires Integral<UnderlyingType<T>>
{
    return static_cast<T>(deserialize<UnderlyingType<T>>(bytes, offset));
}

template<typename T>
ByteBuffer serialize(T const& item)
{
    ByteBuffer buffer;
    serialize(buffer, item);
    return buffer;
}

template<typename T>
void serialize(ByteBuffer& buffer, T const& item);

template<typename T>
void serialize(ByteBuffer& buffer, T const& item) requires Integral<T> || Integral<UnderlyingType<T>>
{
    buffer.append(&item, sizeof(T));
}

template<typename T>
size_t wire_sizeof(T const& item);

template<typename T>
size_t wire_sizeof(T const&) requires Integral<T> || Integral<UnderlyingType<T>>
{
    return sizeof(T);
}

template<typename T>
class ListOf {
public:
    ListOf() { }
    ListOf(Vector<T> const& list)
        : m_list(list)
    {
    }
    ListOf(std::initializer_list<T> list)
        : m_list(list)
    {
    }
    Vector<T> const& value() const { return m_list; }
    size_t size() const { return m_list.size(); }

    // These functions are sfinae'd out when not appropriate
    Utf8View utf8() const
    {
        return Utf8View(m_list.span());
    }

    Utf16View utf16() const
    {
        return Utf16View(m_list.span());
    }

    static ListOf<T> deserialize_n(ReadonlyBytes const& bytes, size_t offset, size_t n)
    {
        Vector<T> list;
        for (size_t i = 0; i < n; ++i) {
            auto item = deserialize<T>(bytes, offset);
            list.append(item);
            offset += wire_sizeof(item);
        }
        return ListOf<T>(list);
    }

private:
    Vector<T> m_list;
};

template<typename T>
void serialize(ByteBuffer& buffer, ListOf<T> const& list)
{
    for (auto& item : list.value()) {
        serialize<T>(buffer, item);
    }
}

template<typename T>
size_t wire_sizeof(ListOf<T> const& list)
{
    size_t size = 0;
    for (auto& item : list.value()) {
        size += wire_sizeof(item);
    }
    return size;
}

template<typename T>
requires IsEnum<T>
class SetOf {
public:
    void set(const T& key, bool value)
    {
        m_bitset &= ~(1 << to_underlying(key));
        m_bitset |= (value ? 1 : 0) << to_underlying(key);
    }

    bool at(const T& key) const
    {
        return m_bitset & (1 << to_underlying(key));
    }

    UnderlyingType<T> numeric_value() const
    {
        return m_bitset;
    }

private:
    // Enum types that are used in sets should make their underlying type of
    // sufficent size to hold a bitset of all alternatives.
    UnderlyingType<T> m_bitset;
};

template<typename T>
void serialize(ByteBuffer& buffer, SetOf<T> const& set)
{
    serialize<UnderlyingType<T>>(buffer, set.numeric_value());
}

template<typename T>
size_t wire_sizeof(SetOf<T> const&)
{
    return wire_sizeof<T>(static_cast<T>(0));
}

typedef u32 Window;
typedef u32 PixMap;
typedef u32 Cursor;
typedef u32 Font;
typedef u32 GContext;
typedef u32 ColorMap;
typedef u32 VisualID;
typedef u8 KeyCode;

typedef i8 Int8;
typedef i16 Int16;
typedef i32 Int32;
typedef u8 Card8;
typedef u16 Card16;
typedef u32 Card32;

typedef u8 Bool;
typedef char Byte;

typedef ListOf<Card8> String8;
typedef ListOf<Card16> String16;

inline String8 operator"" Xs8(const char* cstring, size_t length)
{
    Vector<Card8> vector;
    for (auto& ch : StringView(cstring, length)) {
        vector.append(ch);
    }
    return String8(vector);
}

enum class Event : Card32 {
    KeyPress = 0,
    KeyRelease = 1,
    ButtonPress = 2,
    ButtonRelease = 3,
    EnterWindow = 4,
    LeaveWindow = 5,
    PointerMotion = 6,
    PointerMotionHint = 7,
    Button1Motion = 8,
    Button2Motion = 9,
    Button3Motion = 10,
    Button4Motion = 11,
    Button5Motion = 12,
    ButtonMotion = 13,
    KeymapState = 14,
    Exposure = 15,
    VisibilityChange = 16,
    StructureNotify = 17,
    ResizeRedirect = 18,
    SubstructureNotify = 19,
    SubstructureRedirect = 20,
    FocusChange = 21,
    PropertyChange = 22,
    ColormapChange = 23,
    OwnerGrabButton = 24,
};

}
