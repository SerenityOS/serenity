/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/Function.h>
#include <AK/ScopeGuard.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace PDF {

class Reader {
public:
    explicit Reader(ReadonlyBytes bytes)
        : m_bytes(bytes)
    {
    }

    ALWAYS_INLINE ReadonlyBytes bytes() const { return m_bytes; }
    ALWAYS_INLINE size_t offset() const { return m_offset; }

    bool done() const
    {
        if (m_forwards)
            return offset() >= bytes().size();
        return m_offset < 0;
    }

    size_t remaining() const
    {
        if (done())
            return 0;

        if (m_forwards)
            return bytes().size() - offset() - 1;
        return offset() + 1;
    }

    void move_by(size_t count)
    {
        if (m_forwards) {
            m_offset += static_cast<ssize_t>(count);
        } else {
            m_offset -= static_cast<ssize_t>(count);
        }
    }

    template<typename T = char>
    T read()
    {
        T value = reinterpret_cast<const T*>(m_bytes.offset(m_offset))[0];
        move_by(sizeof(T));
        return value;
    }

    char peek(size_t shift = 0) const
    {
        auto offset = m_offset + shift * (m_forwards ? 1 : -1);
        return static_cast<char>(m_bytes.at(offset));
    }

    template<typename... T>
    bool matches_any(T... elements) const
    {
        if (done())
            return false;
        auto ch = peek();
        return ((ch == elements) || ...);
    }

    bool matches(char ch) const
    {
        return !done() && peek() == ch;
    }

    bool matches(char const* chars) const
    {
        String string(chars);
        if (remaining() < string.length())
            return false;

        if (!m_forwards)
            string = string.reverse();

        for (size_t i = 0; i < string.length(); i++) {
            if (peek(i) != string[i])
                return false;
        }

        return true;
    }

    template<typename T = char>
    void move_to(size_t offset)
    {
        VERIFY(offset < m_bytes.size());
        m_offset = static_cast<ssize_t>(offset);
    }

    void move_until(char ch)
    {
        while (!done() && peek() != ch)
            move_by(1);
    }

    void move_until(Function<bool(char)> predicate)
    {
        while (!done() && !predicate(peek()))
            move_by(1);
    }

    ALWAYS_INLINE void move_while(Function<bool(char)> predicate)
    {
        move_until([&predicate](char t) { return !predicate(t); });
    }

    ALWAYS_INLINE void set_reading_forwards() { m_forwards = true; }
    ALWAYS_INLINE void set_reading_backwards() { m_forwards = false; }

    ALWAYS_INLINE void save() { m_saved_offsets.append(m_offset); }
    ALWAYS_INLINE void load() { m_offset = m_saved_offsets.take_last(); }
    ALWAYS_INLINE void discard() { m_saved_offsets.take_last(); }

#ifdef PDF_DEBUG
    void dump_state() const
    {
        dbgln("Reader State (offset={} size={})", offset(), bytes().size());

        size_t from = max(0, static_cast<int>(offset()) - 10);
        size_t to = min(bytes().size() - 1, offset() + 10);

        for (auto i = from; i <= to; i++) {
            char value = static_cast<char>(bytes().at(i));
            auto line = String::formatted("  {}: '{}' (value={:3d}) ", i, value, static_cast<u8>(value));
            if (i == offset()) {
                dbgln("{} <<< current location, forwards={}", line, m_forwards);
            } else {
                dbgln("{}", line);
            }
        }
        dbgln();
    }
#endif

private:
    ReadonlyBytes m_bytes;
    ssize_t m_offset { 0 };
    Vector<ssize_t> m_saved_offsets;
    bool m_forwards { true };
};

}
