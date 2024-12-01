/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Types.h>
#include <Kernel/Library/IOWindow.h>

namespace Kernel {

enum IOSize {
    Byte,
    Word,
    DWord,
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
};

template<typename T, typename E>
concept RegisterEntry = IsEnum<E> && requires {
    typename T::Type;
    { T::Location } -> SameAsIgnoringCVReference<E>;
    { T::Size } -> SameAsIgnoringCVReference<IOSize>;
};

template<Enum E, RegisterEntry<E>... Entries>
class IORegister {
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
    explicit IORegister(NonnullOwnPtr<IOWindow> window)
        : m_window(move(window))
    {
    }

    template<E Reg, typename Entry = decltype(find_entry<Reg>(declval<Entries>()...))>
    requires(Entry::Size == IOSize::Byte)
    auto read()
    {
        auto value = m_window->read8(static_cast<u64>(Reg));
        if constexpr (Entry::Debug)
            dbgln("Read {:#04x}: {:#02x}", (u32)Entry::Location, value);
        return bit_cast<typename Entry::Type>(value);
    }

    template<E Reg, typename Entry = decltype(find_entry<Reg>(declval<Entries>()...))>
    requires(Entry::Size == IOSize::Word)
    auto read()
    {
        auto value = m_window->read16(static_cast<u64>(Reg));
        if constexpr (Entry::Debug)
            dbgln("Read {:#04x}: {:#04x}", (u32)Entry::Location, value);
        return bit_cast<typename Entry::Type>(value);
    }

    template<E Reg, typename Entry = decltype(find_entry<Reg>(declval<Entries>()...))>
    requires(Entry::Size == IOSize::DWord)
    auto read()
    {
        auto value = m_window->read32(static_cast<u64>(Reg));
        if constexpr (Entry::Debug)
            dbgln("Read {:#04x}: {:#08x}", (u32)Entry::Location, value);
        return bit_cast<typename Entry::Type>(value);
    }

    template<E Reg, typename Entry = decltype(find_entry<Reg>(declval<Entries>()...))>
    requires(Entry::Size == IOSize::Byte)
    void write(Entry::Type value)
    {
        auto v = bit_cast<u8>(value);
        if constexpr (Entry::Debug)
            dbgln("Write {:#04x}: {:#02x}", (u32)Entry::Location, v);
        m_window->write8(static_cast<u64>(Reg), v);
    }

    template<E Reg, typename Entry = decltype(find_entry<Reg>(declval<Entries>()...))>
    requires(Entry::Size == IOSize::Word)
    void write(Entry::Type value)
    {
        auto v = bit_cast<u16>(value);
        if constexpr (Entry::Debug)
            dbgln("Write {:#04x}: {:#04x}", (u32)Entry::Location, v);
        m_window->write16(static_cast<u64>(Reg), v);
    }

    template<E Reg, typename Entry = decltype(find_entry<Reg>(declval<Entries>()...))>
    requires(Entry::Size == IOSize::DWord)
    void write(Entry::Type value)
    {
        auto v = bit_cast<u32>(value);
        if constexpr (Entry::Debug)
            dbgln("Write {:#04x}: {:#08x}", (u32)Entry::Location, v);
        m_window->write32(static_cast<u64>(Reg), v);
    }

    IOWindow& window() { return *m_window; }

private:
    NonnullOwnPtr<IOWindow> m_window;
};

}
