/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteReader.h>
#include <AK/HashMap.h>
#include <AK/Noncopyable.h>
#include <AK/OwnPtr.h>
#include <LibCore/MappedFile.h>
#include <LibELF/Core.h>
#include <LibELF/Image.h>

namespace Coredump {

struct MemoryRegionInfo {
    ELF::Core::NotesEntryHeader header;
    uint64_t region_start;
    uint64_t region_end;
    uint16_t program_header_index;
    StringView region_name;

    StringView object_name() const
    {
        if (region_name.contains("Loader.so"))
            return "Loader.so"sv;
        auto maybe_colon_index = region_name.find(':');
        if (!maybe_colon_index.has_value())
            return {};
        return region_name.substring_view(0, *maybe_colon_index);
    }
};

class Reader {
    AK_MAKE_NONCOPYABLE(Reader);
    AK_MAKE_NONMOVABLE(Reader);

public:
    static OwnPtr<Reader> create(StringView);
    ~Reader();

    template<typename Func>
    void for_each_memory_region_info(Func func) const;

    struct LibraryInfo {
        String name;
        String path;
        FlatPtr base_address { 0 };
    };

    void for_each_library(Function<void(LibraryInfo)> func) const;

    template<typename Func>
    void for_each_thread_info(Func func) const;

    const ELF::Image& image() const { return m_coredump_image; }

    Optional<FlatPtr> peek_memory(FlatPtr address) const;
    Optional<MemoryRegionInfo> first_region_for_object(StringView object_name) const;
    Optional<MemoryRegionInfo> region_containing(FlatPtr address) const;

    struct LibraryData {
        String name;
        FlatPtr base_address { 0 };
        NonnullRefPtr<Core::MappedFile> file;
        ELF::Image lib_elf;
    };
    const LibraryData* library_containing(FlatPtr address) const;

    int process_pid() const;
    u8 process_termination_signal() const;
    String process_executable_path() const;
    Vector<String> process_arguments() const;
    Vector<String> process_environment() const;
    HashMap<String, String> metadata() const;

private:
    explicit Reader(ReadonlyBytes);
    explicit Reader(ByteBuffer);
    explicit Reader(NonnullRefPtr<Core::MappedFile>);

    static Optional<ByteBuffer> decompress_coredump(ReadonlyBytes);

    class NotesEntryIterator {
    public:
        NotesEntryIterator(const u8* notes_data);

        ELF::Core::NotesEntryHeader::Type type() const;
        const ELF::Core::NotesEntry* current() const;

        void next();
        bool at_end() const;

    private:
        const ELF::Core::NotesEntry* m_current { nullptr };
        const u8* start { nullptr };
    };

    // Private as we don't need anyone poking around in this JsonObject
    // manually - we know very well what should be included and expose that
    // as getters with the appropriate (non-JsonValue) types.
    const JsonObject process_info() const;

    // For uncompressed coredumps, we keep the MappedFile
    RefPtr<Core::MappedFile> m_mapped_file;

    // For compressed coredumps, we decompress them into a ByteBuffer
    ByteBuffer m_coredump_buffer;

    ReadonlyBytes m_coredump_bytes;

    ELF::Image m_coredump_image;
    ssize_t m_notes_segment_index { -1 };
};

template<typename Func>
void Reader::for_each_memory_region_info(Func func) const
{
    NotesEntryIterator it(bit_cast<const u8*>(m_coredump_image.program_header(m_notes_segment_index).raw_data()));
    for (; !it.at_end(); it.next()) {
        if (it.type() != ELF::Core::NotesEntryHeader::Type::MemoryRegionInfo)
            continue;
        ELF::Core::MemoryRegionInfo raw_memory_region_info;
        ReadonlyBytes raw_data {
            it.current(),
            sizeof(raw_memory_region_info),
        };
        ByteReader::load(raw_data.data(), raw_memory_region_info);

        MemoryRegionInfo memory_region_info {
            raw_memory_region_info.header,
            raw_memory_region_info.region_start,
            raw_memory_region_info.region_end,
            raw_memory_region_info.program_header_index,
            { bit_cast<const char*>(raw_data.offset_pointer(raw_data.size())) },
        };
        IterationDecision decision = func(memory_region_info);
        if (decision == IterationDecision::Break)
            return;
    }
}

template<typename Func>
void Reader::for_each_thread_info(Func func) const
{
    NotesEntryIterator it(bit_cast<const u8*>(m_coredump_image.program_header(m_notes_segment_index).raw_data()));
    for (; !it.at_end(); it.next()) {
        if (it.type() != ELF::Core::NotesEntryHeader::Type::ThreadInfo)
            continue;
        ELF::Core::ThreadInfo thread_info;
        ByteReader::load(bit_cast<const u8*>(it.current()), thread_info);

        IterationDecision decision = func(thread_info);
        if (decision == IterationDecision::Break)
            return;
    }
}

}
