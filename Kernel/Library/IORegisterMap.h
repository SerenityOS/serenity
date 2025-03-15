/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Types.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/kstdio.h>

namespace Kernel {

enum IOSize {
    Byte = 1,
    Word = 2,
    DWord = 4,
};

template<typename T>
consteval IOSize io_size_for()
{
    if constexpr (sizeof(T) == 1)
        return IOSize::Byte;
    else if constexpr (sizeof(T) == 2)
        return IOSize::Word;
    else if constexpr (sizeof(T) == 4)
        return IOSize::DWord;
    else
        static_assert(false, "Invalid IO size");
}

template<Enum E, E L, typename T, bool _Debug = false>
struct IOReg {
    static constexpr E Location = L;
    static constexpr IOSize Size = io_size_for<T>();
    static constexpr bool Debug = _Debug;
    using Type = T;

    template<typename... Args>
    static void DebugPrint(Type value, CheckedFormatString<Args...>&& format, Args&&... args)
    {
        if constexpr (!Debug)
            return;

        StringBuilder builder;
        builder.appendff(forward<CheckedFormatString<Args...>>(format), forward<Args>(args)...);

        using Underlying = Conditional<Size == IOSize::Byte, u8, Conditional<Size == IOSize::Word, u16, u32>>;
        constexpr auto FormatWidth = to_underlying(Size) * 2;
        dbg(forward<CheckedFormatString<Args...>>(format), forward<Args>(args)...);
        if constexpr (AK::HasFormatter<Type> && !IsSame<Type, Underlying>)
            builder.appendff(" {}({:#0{}x})\n", value, bit_cast<Underlying>(value), FormatWidth);
        else
            builder.appendff(" {:#0{}x}\n", bit_cast<Underlying>(value), FormatWidth);

        dbgputstr(builder.string_view());
    }
};

template<Enum E, E L, typename T, size_t _Count, size_t _Stride, bool _Debug = false>
requires(_Count > 0 && _Stride >= sizeof(T))
struct IORegArray : IOReg<E, L, T, _Debug> {
    static constexpr size_t Count = _Count;
    static constexpr size_t Stride = _Stride;
};

template<typename T, typename E>
concept RegisterEntry = IsEnum<E> && requires {
    typename T::Type;
    { T::Location } -> SameAsIgnoringCVReference<E>;
    { T::Size } -> SameAsIgnoringCVReference<IOSize>;
};
template<typename T, typename E>
concept RegisterArray = RegisterEntry<T, E> && requires {
    { T::Stride } -> SameAsIgnoringCVReference<size_t>;
    { T::Count } -> SameAsIgnoringCVReference<size_t>;
};

template<Enum E, RegisterEntry<E>... Entries>
class IORegisterMap {
    template<E Reg, RegisterEntry<E> First, RegisterEntry<E>... Rest>
    static consteval auto find_entry(First, Rest...)
    {
        if constexpr (First::Location == Reg)
            return First();
        else if constexpr (sizeof...(Rest) > 0)
            return find_entry<Reg>(Rest()...);
        else
            static_assert(false, "Register not found");
    }

public:
    explicit IORegisterMap(NonnullOwnPtr<IOWindow> window)
        : m_window(move(window))
    {
    }

    template<E Reg, typename Entry = decltype(find_entry<Reg>(declval<Entries>()...))>
    requires(!RegisterArray<Entry, E>)
    auto read()
    {
        typename Entry::Type value;
        if constexpr (Entry::Size == IOSize::Byte)
            value = bit_cast<typename Entry::Type>(m_window->read8(static_cast<u64>(Reg)));
        else if constexpr (Entry::Size == IOSize::Word)
            value = bit_cast<typename Entry::Type>(m_window->read16(static_cast<u64>(Reg)));
        else if constexpr (Entry::Size == IOSize::DWord)
            value = bit_cast<typename Entry::Type>(m_window->read32(static_cast<u64>(Reg)));
        Entry::DebugPrint(value, "Read {:#04x}:", to_underlying(Entry::Location));
        return value;
    }

    template<E Reg, size_t N, typename Entry = decltype(find_entry<Reg>(declval<Entries>()...))>
    requires(RegisterArray<Entry, E> && (N < Entry::Count))
    auto read()
    {
        typename Entry::Type value;

        if constexpr (Entry::Size == IOSize::Byte)
            value = bit_cast<typename Entry::Type>(m_window->read8(static_cast<u64>(Reg) + Entry::Stride * N));
        else if constexpr (Entry::Size == IOSize::Word)
            value = bit_cast<typename Entry::Type>(m_window->read16(static_cast<u64>(Reg) + Entry::Stride * N));
        else if constexpr (Entry::Size == IOSize::DWord)
            value = bit_cast<typename Entry::Type>(m_window->read32(static_cast<u64>(Reg) + Entry::Stride * N));

        Entry::DebugPrint(value, "Read {:#04x}[{}]:", to_underlying(Entry::Location), N);
        return value;
    }

    template<E Reg, typename Entry = decltype(find_entry<Reg>(declval<Entries>()...))>
    requires(RegisterArray<Entry, E>)
    auto read(size_t index)
    {
        VERIFY(index < Entry::Count);

        typename Entry::Type value;

        if constexpr (Entry::Size == IOSize::Byte)
            value = bit_cast<typename Entry::Type>(m_window->read8(static_cast<u64>(Reg) + Entry::Stride * index));
        else if constexpr (Entry::Size == IOSize::Word)
            value = bit_cast<typename Entry::Type>(m_window->read16(static_cast<u64>(Reg) + Entry::Stride * index));
        else if constexpr (Entry::Size == IOSize::DWord)
            value = bit_cast<typename Entry::Type>(m_window->read32(static_cast<u64>(Reg) + Entry::Stride * index));

        Entry::DebugPrint(value, "Read {:#04x}[{}]:", to_underlying(Reg), index);
        return value;
    }

    template<E Reg, typename Entry = decltype(find_entry<Reg>(declval<Entries>()...))>
    requires(!RegisterArray<Entry, E>)
    void write(Entry::Type value)
    {
        Entry::DebugPrint(value, "Write {:#04x}:", to_underlying(Reg));

        if constexpr (Entry::Size == IOSize::Byte)
            m_window->write8(static_cast<u64>(Reg), bit_cast<u8>(value));
        else if constexpr (Entry::Size == IOSize::Word)
            m_window->write16(static_cast<u64>(Reg), bit_cast<u16>(value));
        else if constexpr (Entry::Size == IOSize::DWord)
            m_window->write32(static_cast<u64>(Reg), bit_cast<u32>(value));
    }

    template<E Reg, size_t N, typename Entry = decltype(find_entry<Reg>(declval<Entries>()...))>
    requires(RegisterArray<Entry, E> && (N < Entry::Count))
    void write(Entry::Type value)
    {
        Entry::DebugPrint(value, "Write {:#04x}[{}]:", to_underlying(Entry::Location), N);

        if constexpr (Entry::Size == IOSize::Byte)
            m_window->write8(static_cast<u64>(Reg) + Entry::Stride * N, bit_cast<u8>(value));
        else if constexpr (Entry::Size == IOSize::Word)
            m_window->write16(static_cast<u64>(Reg) + Entry::Stride * N, bit_cast<u16>(value));
        else if constexpr (Entry::Size == IOSize::DWord)
            m_window->write32(static_cast<u64>(Reg) + Entry::Stride * N, bit_cast<u32>(value));
    }

    template<E Reg, typename Entry = decltype(find_entry<Reg>(declval<Entries>()...))>
    requires(RegisterArray<Entry, E>)
    void write(size_t index, Entry::Type value)
    {
        VERIFY(index < Entry::Count);

        Entry::DebugPrint(value, "Write {:#04x}[{}]:", to_underlying(Entry::Location), index);

        if constexpr (Entry::Size == IOSize::Byte)
            m_window->write8(static_cast<u64>(Reg) + Entry::Stride * index, bit_cast<u8>(value));
        else if constexpr (Entry::Size == IOSize::Word)
            m_window->write16(static_cast<u64>(Reg) + Entry::Stride * index, bit_cast<u16>(value));
        else if constexpr (Entry::Size == IOSize::DWord)
            m_window->write32(static_cast<u64>(Reg) + Entry::Stride * index, bit_cast<u32>(value));
    }

    IOWindow& window() { return *m_window; }

private:
    NonnullOwnPtr<IOWindow> m_window;
};

}
