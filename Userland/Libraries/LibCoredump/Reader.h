/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/MappedFile.h>
#include <AK/Noncopyable.h>
#include <AK/OwnPtr.h>
#include <LibELF/Core.h>
#include <LibELF/Image.h>

namespace Coredump {

class Reader {
    AK_MAKE_NONCOPYABLE(Reader);
    AK_MAKE_NONMOVABLE(Reader);

public:
    static OwnPtr<Reader> create(const String&);
    ~Reader();

    template<typename Func>
    void for_each_memory_region_info(Func func) const;

    template<typename Func>
    void for_each_thread_info(Func func) const;

    const ELF::Image& image() const { return m_coredump_image; }

    Optional<FlatPtr> peek_memory(FlatPtr address) const;
    ELF::Core::MemoryRegionInfo const* first_region_for_object(StringView object_name) const;
    const ELF::Core::MemoryRegionInfo* region_containing(FlatPtr address) const;

    struct LibraryData {
        String name;
        FlatPtr base_address { 0 };
        NonnullRefPtr<MappedFile> file;
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
    Reader(ReadonlyBytes);

    static ByteBuffer decompress_coredump(const ReadonlyBytes&);

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

    ByteBuffer m_coredump_buffer;
    ELF::Image m_coredump_image;
    ssize_t m_notes_segment_index { -1 };
};

template<typename Func>
void Reader::for_each_memory_region_info(Func func) const
{
    for (NotesEntryIterator it((const u8*)m_coredump_image.program_header(m_notes_segment_index).raw_data()); !it.at_end(); it.next()) {
        if (it.type() != ELF::Core::NotesEntryHeader::Type::MemoryRegionInfo)
            continue;
        auto& memory_region_info = reinterpret_cast<const ELF::Core::MemoryRegionInfo&>(*it.current());
        IterationDecision decision = func(memory_region_info);
        if (decision == IterationDecision::Break)
            return;
    }
}

template<typename Func>
void Reader::for_each_thread_info(Func func) const
{
    for (NotesEntryIterator it((const u8*)m_coredump_image.program_header(m_notes_segment_index).raw_data()); !it.at_end(); it.next()) {
        if (it.type() != ELF::Core::NotesEntryHeader::Type::ThreadInfo)
            continue;
        auto& thread_info = reinterpret_cast<const ELF::Core::ThreadInfo&>(*it.current());
        IterationDecision decision = func(thread_info);
        if (decision == IterationDecision::Break)
            return;
    }
}

}
