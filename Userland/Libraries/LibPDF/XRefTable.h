/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>

namespace PDF {

struct XRefEntry {
    long byte_offset { -1L };
    u16 generation_number { 0 };
    bool in_use { false };
};

struct XRefSection {
    int starting_index;
    int count;
    Vector<XRefEntry> entries;
};

class XRefTable {
public:
    void add_section(const XRefSection& section)
    {
        m_entries.ensure_capacity(section.starting_index + section.count);

        for (int i = static_cast<int>(m_entries.size()); i < section.starting_index; i++)
            m_entries.append(XRefEntry {});

        for (auto& entry : section.entries)
            m_entries.append(entry);
    }

    [[nodiscard]] ALWAYS_INLINE bool has_object(size_t index) const
    {
        return index < m_entries.size() && m_entries[index].byte_offset != -1;
    }

    [[nodiscard]] ALWAYS_INLINE long byte_offset_for_object(size_t index) const
    {
        VERIFY(has_object(index));
        return m_entries[index].byte_offset;
    }

    [[nodiscard]] ALWAYS_INLINE u16 generation_number_for_object(size_t index) const
    {
        VERIFY(has_object(index));
        return m_entries[index].generation_number;
    }

    [[nodiscard]] ALWAYS_INLINE bool is_object_in_use(size_t index) const
    {
        VERIFY(has_object(index));
        return m_entries[index].in_use;
    }

private:
    friend struct AK::Formatter<PDF::XRefTable>;

    Vector<XRefEntry> m_entries;
};

}

namespace AK {

template<>
struct Formatter<PDF::XRefEntry> : Formatter<StringView> {
    void format(FormatBuilder& builder, const PDF::XRefEntry& entry)
    {
        Formatter<StringView>::format(builder,
            String::formatted("XRefEntry {{ offset={} generation={} used={} }}",
                entry.byte_offset,
                entry.generation_number,
                entry.in_use));
    }
};

template<>
struct Formatter<PDF::XRefTable> : Formatter<StringView> {
    void format(FormatBuilder& format_builder, const PDF::XRefTable& table)
    {
        StringBuilder builder;
        builder.append("XRefTable {");
        for (auto& entry : table.m_entries)
            builder.appendff("\n  {}", entry);
        builder.append("\n}");
        Formatter<StringView>::format(format_builder, builder.to_string());
    }
};

}
