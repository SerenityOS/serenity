/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibCoreDump/Backtrace.h>
#include <LibCoreDump/Reader.h>
#include <string.h>

namespace CoreDump {

OwnPtr<Reader> Reader::create(const String& path)
{
    auto mapped_file = make<MappedFile>(path);
    if (!mapped_file->is_valid())
        return nullptr;
    return make<Reader>(move(mapped_file));
}

Reader::Reader(OwnPtr<MappedFile>&& coredump_file)
    : m_coredump_file(move(coredump_file))
    , m_coredump_image((u8*)m_coredump_file->data(), m_coredump_file->size())
{
    size_t index = 0;
    m_coredump_image.for_each_program_header([this, &index](auto pheader) {
        if (pheader.type() == PT_NOTE) {
            m_notes_segment_index = index;
            return IterationDecision::Break;
        }
        ++index;
        return IterationDecision::Continue;
    });
    ASSERT(m_notes_segment_index != -1);
}

Reader::~Reader()
{
}

Reader::NotesEntryIterator::NotesEntryIterator(const u8* notes_data)
    : m_current((const ELF::Core::NotesEntry*)notes_data)
    , start(notes_data)
{
}

ELF::Core::NotesEntryHeader::Type Reader::NotesEntryIterator::type() const
{
    ASSERT(m_current->header.type == ELF::Core::NotesEntryHeader::Type::MemoryRegionInfo
        || m_current->header.type == ELF::Core::NotesEntryHeader::Type::ThreadInfo
        || m_current->header.type == ELF::Core::NotesEntryHeader::Type::Null);
    return m_current->header.type;
}

const ELF::Core::NotesEntry* Reader::NotesEntryIterator::current() const
{
    return m_current;
}

void Reader::NotesEntryIterator::next()
{
    ASSERT(!at_end());
    if (type() == ELF::Core::NotesEntryHeader::Type::ThreadInfo) {
        const ELF::Core::ThreadInfo* current = (const ELF::Core::ThreadInfo*)m_current;
        m_current = (const ELF::Core::NotesEntry*)(current + 1);
        return;
    }
    if (type() == ELF::Core::NotesEntryHeader::Type::MemoryRegionInfo) {
        const ELF::Core::MemoryRegionInfo* current = (const ELF::Core::MemoryRegionInfo*)m_current;
        m_current = (const ELF::Core::NotesEntry*)(current->region_name + strlen(current->region_name) + 1);
        return;
    }
}

bool Reader::NotesEntryIterator::at_end() const
{
    return type() == ELF::Core::NotesEntryHeader::Type::Null;
}

Optional<uint32_t> Reader::peek_memory(FlatPtr address) const
{
    const auto* region = region_containing(address);
    if (!region)
        return {};

    FlatPtr offset_in_region = address - region->region_start;
    const char* region_data = image().program_header(region->program_header_index).raw_data();
    return *(const uint32_t*)(&region_data[offset_in_region]);
}

const ELF::Core::MemoryRegionInfo* Reader::region_containing(FlatPtr address) const
{
    const ELF::Core::MemoryRegionInfo* ret = nullptr;
    for_each_memory_region_info([&ret, address](const ELF::Core::MemoryRegionInfo& region_info) {
        if (region_info.region_start <= address && region_info.region_end >= address) {
            ret = &region_info;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return ret;
}

Backtrace Reader::backtrace() const
{
    return Backtrace(*this);
}

}
