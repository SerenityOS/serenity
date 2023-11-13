/*
 * Copyright (c) 2023, Jesús Lapastora <cyber.gsuscode@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Collection of utilities to produce an in-memory ELF file in the same format
// as the host.

#include <AK/FixedArray.h>
#include <AK/Span.h>
#include <AK/Vector.h>
#include <LibELF/ELFABI.h>

namespace ELF {

// Represents an ELF Section that is optionally bound to some data.
struct Section {
    Elf64_Shdr header;
    Optional<ReadonlyBytes> data {};

    explicit Section(Elf64_Shdr header)
        : header(header)
    {
    }
    Section(ReadonlyBytes data, Elf64_Shdr header)
        : header(header)
        , data(data)
    {
    }
};

// Receives a list of sections, and writes the following layout:
// <elf layout> <section headers> <section data>
//
// Both the section headers & the data for those sections will be written in the
// exact order as they appear in the list.
// If a `Section` contains data, then its `sh_offset` is set to the offset in
// the final image, and `sh_size` to the size of the specified data. `Section`s
// that do not contain data will have their `sh_offset` set to the end offset of
// the section that comes right before them.
//
// Notes on the ELF Header:
// The elf header is mostly filled by this function. It needs help in a couple
// of fields: `e_shstrndx` and `e_type`.
//
// - `shstrndx` is the index of the `Section` that contains the section name
// string table.
// - `image_type` is the image file type: ET_CORE, ET_REL, ET_EXEC, etc.
FixedArray<u8> build_elf_image(u64 shstrndx, Elf64_Quarter image_type, ReadonlySpan<Section> sections);

// Takes care of tracking section header indices and their order
struct SectionTable {

    struct Index {
        u64 index;

        constexpr explicit Index(u64 index)
            : index(index)
        {
        }

        constexpr u64 raw_index() const noexcept { return index; }
    };

    ReadonlySpan<Section> span() const noexcept { return m_sections.span(); }

    // Appends a default-intialized header with no data. The client is
    // responsible for initializing the header before producing the final image.
    Index reserve() noexcept
    {
        return append(Section(Elf64_Shdr()));
    }

    // Appends a Section and returns the index to refer to it.
    Index append(Section section) noexcept
    {
        auto const index = m_sections.size();
        m_sections.append(move(section));
        return Index(index);
    }
    template<typename... Args>
    Index empend(Args&&... args) noexcept
    {
        auto const index = m_sections.size();
        m_sections.empend(forward<Args>(args)...);
        return Index(index);
    }

    // Calls `header_builder` with a reference to the Section header, so that
    // the builder can initialize it.
    // Returns the index for the section.
    template<typename Builder>
    Index build_nobits(Builder header_builder)
    {
        auto index = reserve();
        build_nobits_at(index, move(header_builder));
        return index;
    }

    // Creates a null section header. Useful for avoiding index 0 for the text
    // section, since if we use 0 for its index then symbols that relate to
    // .text will be misinterpreted as related to an 'undefined' section.
    Index build_null()
    {
        Elf64_Shdr header {};
        header.sh_type = SHT_NULL;
        header.sh_name = 0;
        return empend(header);
    }

    // Same as `build_nobits`, but writes an already reserved header instead of
    // creating a new one.
    template<typename Builder>
    void build_nobits_at(Index at, Builder header_builder)
    {
        Elf64_Shdr header {};
        header.sh_type = SHT_NOBITS;
        header_builder(header);
        new (&m_sections[at.raw_index()]) Section(header);
    }

    // Reinterprets `typed_data` as a byte slice, and calls `header_builder`
    // with a reference to the Section header to be initialized.
    // Sets the header's `sh_entsize` to `sizeof(T)` before calling the builder,
    // so it can be overridden if required.
    // Returns the index for the section.
    template<typename T, typename Builder>
    Index build(ReadonlySpan<T> typed_data, Builder header_builder)
    {
        auto index = reserve();
        build_at(index, move(typed_data), move(header_builder));
        return index;
    }

    // Same as `build`, but writes an already reserved header instead of
    // creating a new one.
    template<typename T, typename Builder>
    void build_at(Index at, ReadonlySpan<T> typed_data, Builder header_builder)
    {
        Elf64_Shdr header {};
        header.sh_entsize = sizeof(T);
        header_builder(static_cast<Elf64_Shdr&>(header));
        ReadonlyBytes data = ReadonlyBytes {
            reinterpret_cast<u8 const*>(typed_data.offset(0)),
            typed_data.size() * sizeof(T),
        };
        new (&m_sections[at.raw_index()]) Section(data, header);
    }

    // Makes header editing available after construction. The reference is valid
    // until another header is added.
    Elf64_Shdr& header_at(Index index) noexcept { return m_sections[index.raw_index()].header; }

private:
    Vector<Section> m_sections;
};

struct StringTable {
    // Inserts the given string into the table, giving back the offset it begins
    // at. The string must not contain any zeroes.
    u32 insert(StringView str) noexcept;

    // Emits the section information for the current state, so that it can be
    // merged into an ELF image.
    Section emit_section(u32 name_index) const noexcept;

    // Like `emit_section`, but writes the section directly into the builder.
    // Returns the index for the section.
    SectionTable::Index emit_into_builder(u32 name_index, SectionTable& builder) const noexcept;

private:
    Vector<u8> m_data;
};

};
