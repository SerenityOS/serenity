/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Format.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibPDF/Error.h>

namespace PDF {

constexpr u64 invalid_byte_offset = NumericLimits<u64>::max();

struct XRefEntry {
    u64 byte_offset { invalid_byte_offset };
    u16 generation_number { 0 };
    bool in_use { false };
    bool compressed { false };
};

struct XRefSection {
    int starting_index;
    int count;
    Vector<XRefEntry> entries;
};

class XRefTable final : public RefCounted<XRefTable> {
public:
    PDFErrorOr<void> merge(XRefTable&& other)
    {
        auto this_size = m_entries.size();
        auto other_size = other.m_entries.size();
        TRY(m_entries.try_ensure_capacity(other_size));

        for (size_t i = 0; i < other_size; i++) {
            auto other_entry = other.m_entries[i];
            if (i >= this_size) {
                m_entries.unchecked_append(other_entry);
                continue;
            }

            auto this_entry = m_entries[i];

            // Only add values that we don't already have.
            if (this_entry.byte_offset == invalid_byte_offset)
                m_entries[i] = other_entry;
        }

        return {};
    }

    void add_section(XRefSection const& section)
    {
        m_entries.ensure_capacity(section.starting_index + section.count);

        for (int i = static_cast<int>(m_entries.size()); i < section.starting_index; i++)
            m_entries.append(XRefEntry {});

        for (auto& entry : section.entries)
            m_entries.append(entry);
    }

    void set_trailer(RefPtr<DictObject> trailer) { m_trailer = trailer; }

    ALWAYS_INLINE Vector<XRefEntry>& entries() { return m_entries; }

    ALWAYS_INLINE RefPtr<DictObject> const& trailer() const { return m_trailer; }

    [[nodiscard]] ALWAYS_INLINE bool has_object(size_t index) const
    {
        return index < m_entries.size() && m_entries[index].byte_offset != invalid_byte_offset;
    }

    [[nodiscard]] ALWAYS_INLINE u64 byte_offset_for_object(size_t index) const
    {
        VERIFY(has_object(index));
        return m_entries[index].byte_offset;
    }

    [[nodiscard]] ALWAYS_INLINE u64 object_stream_for_object(size_t index) const
    {
        return byte_offset_for_object(index);
    }

    [[nodiscard]] ALWAYS_INLINE u16 generation_number_for_object(size_t index) const
    {
        VERIFY(has_object(index));
        return m_entries[index].generation_number;
    }

    [[nodiscard]] ALWAYS_INLINE u16 object_stream_index_for_object(size_t index) const
    {
        return generation_number_for_object(index);
    }

    [[nodiscard]] ALWAYS_INLINE bool is_object_in_use(size_t index) const
    {
        VERIFY(has_object(index));
        return m_entries[index].in_use;
    }

    [[nodiscard]] ALWAYS_INLINE bool is_object_compressed(size_t index) const
    {
        VERIFY(has_object(index));
        return m_entries[index].compressed;
    }

private:
    friend struct AK::Formatter<PDF::XRefTable>;

    Vector<XRefEntry> m_entries;
    RefPtr<DictObject> m_trailer;
};

}

namespace AK {

template<>
struct Formatter<PDF::XRefEntry> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::XRefEntry const& entry)
    {
        return Formatter<StringView>::format(builder,
            ByteString::formatted("XRefEntry {{ offset={} generation={} used={} }}",
                entry.byte_offset,
                entry.generation_number,
                entry.in_use));
    }
};

template<>
struct Formatter<PDF::XRefTable> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& format_builder, PDF::XRefTable const& table)
    {
        StringBuilder builder;
        builder.append("XRefTable {"sv);
        for (auto& entry : table.m_entries)
            builder.appendff("\n  {}", entry);
        builder.append("\n}"sv);
        return Formatter<StringView>::format(format_builder, builder.to_byte_string());
    }
};

}
