/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <AK/Function.h>
#include <AK/HashTable.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <LibCompress/Gzip.h>
#include <LibCoredump/Reader.h>
#include <LibFileSystem/FileSystem.h>
#include <signal.h>
#include <string.h>
#include <unistd.h>

namespace Coredump {

OwnPtr<Reader> Reader::create(StringView path)
{
    auto file_or_error = Core::MappedFile::map(path);
    if (file_or_error.is_error())
        return {};

    if (!Compress::GzipDecompressor::is_likely_compressed(file_or_error.value()->bytes())) {
        // It's an uncompressed coredump.
        return AK::adopt_own_if_nonnull(new (nothrow) Reader(file_or_error.release_value()));
    }

    auto decompressed_data = decompress_coredump(file_or_error.value()->bytes());
    if (!decompressed_data.has_value())
        return {};
    return adopt_own_if_nonnull(new (nothrow) Reader(decompressed_data.release_value()));
}

Reader::Reader(ByteBuffer buffer)
    : Reader(buffer.bytes())
{
    m_coredump_buffer = move(buffer);
}

Reader::Reader(NonnullOwnPtr<Core::MappedFile> file)
    : Reader(file->bytes())
{
    m_mapped_file = move(file);
}

Reader::Reader(ReadonlyBytes coredump_bytes)
    : m_coredump_bytes(coredump_bytes)
    , m_coredump_image(m_coredump_bytes)
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
    VERIFY(m_notes_segment_index != -1);
}

Optional<ByteBuffer> Reader::decompress_coredump(ReadonlyBytes raw_coredump)
{
    auto decompressed_coredump = Compress::GzipDecompressor::decompress_all(raw_coredump);
    if (!decompressed_coredump.is_error())
        return decompressed_coredump.release_value();

    // If we didn't manage to decompress it, try and parse it as decompressed coredump
    auto bytebuffer = ByteBuffer::copy(raw_coredump);
    if (bytebuffer.is_error())
        return {};
    return bytebuffer.release_value();
}

Reader::NotesEntryIterator::NotesEntryIterator(u8 const* notes_data)
    : m_current(bit_cast<const ELF::Core::NotesEntry*>(notes_data))
    , start(notes_data)
{
}

ELF::Core::NotesEntryHeader::Type Reader::NotesEntryIterator::type() const
{
    VERIFY(m_current->header.type == ELF::Core::NotesEntryHeader::Type::ProcessInfo
        || m_current->header.type == ELF::Core::NotesEntryHeader::Type::MemoryRegionInfo
        || m_current->header.type == ELF::Core::NotesEntryHeader::Type::ThreadInfo
        || m_current->header.type == ELF::Core::NotesEntryHeader::Type::Metadata
        || m_current->header.type == ELF::Core::NotesEntryHeader::Type::Null);
    return m_current->header.type;
}

const ELF::Core::NotesEntry* Reader::NotesEntryIterator::current() const
{
    return m_current;
}

void Reader::NotesEntryIterator::next()
{
    VERIFY(!at_end());
    switch (type()) {
    case ELF::Core::NotesEntryHeader::Type::ProcessInfo: {
        auto const* current = bit_cast<const ELF::Core::ProcessInfo*>(m_current);
        m_current = bit_cast<const ELF::Core::NotesEntry*>(current->json_data + strlen(current->json_data) + 1);
        break;
    }
    case ELF::Core::NotesEntryHeader::Type::ThreadInfo: {
        auto const* current = bit_cast<const ELF::Core::ThreadInfo*>(m_current);
        m_current = bit_cast<const ELF::Core::NotesEntry*>(current + 1);
        break;
    }
    case ELF::Core::NotesEntryHeader::Type::MemoryRegionInfo: {
        auto const* current = bit_cast<const ELF::Core::MemoryRegionInfo*>(m_current);
        m_current = bit_cast<const ELF::Core::NotesEntry*>(current->region_name + strlen(current->region_name) + 1);
        break;
    }
    case ELF::Core::NotesEntryHeader::Type::Metadata: {
        auto const* current = bit_cast<const ELF::Core::Metadata*>(m_current);
        m_current = bit_cast<const ELF::Core::NotesEntry*>(current->json_data + strlen(current->json_data) + 1);
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

bool Reader::NotesEntryIterator::at_end() const
{
    return type() == ELF::Core::NotesEntryHeader::Type::Null;
}

Optional<FlatPtr> Reader::peek_memory(FlatPtr address) const
{
    auto region = region_containing(address);
    if (!region.has_value())
        return {};

    FlatPtr offset_in_region = address - region->region_start;
    auto* region_data = bit_cast<u8 const*>(image().program_header(region->program_header_index).raw_data());
    FlatPtr value { 0 };
    ByteReader::load(region_data + offset_in_region, value);
    return value;
}

JsonObject const Reader::process_info() const
{
    const ELF::Core::ProcessInfo* process_info_notes_entry = nullptr;
    NotesEntryIterator it(bit_cast<u8 const*>(m_coredump_image.program_header(m_notes_segment_index).raw_data()));
    for (; !it.at_end(); it.next()) {
        if (it.type() != ELF::Core::NotesEntryHeader::Type::ProcessInfo)
            continue;
        process_info_notes_entry = bit_cast<const ELF::Core::ProcessInfo*>(it.current());
        break;
    }
    if (!process_info_notes_entry)
        return {};
    auto const* json_data_ptr = process_info_notes_entry->json_data;
    auto process_info_json_value = JsonValue::from_string({ json_data_ptr, strlen(json_data_ptr) });
    if (process_info_json_value.is_error())
        return {};
    if (!process_info_json_value.value().is_object())
        return {};
    return process_info_json_value.value().as_object();
    // FIXME: Maybe just cache this on the Reader instance after first access.
}

Optional<MemoryRegionInfo> Reader::first_region_for_object(StringView object_name) const
{
    Optional<MemoryRegionInfo> ret;
    for_each_memory_region_info([&ret, &object_name](auto& region_info) {
        if (region_info.object_name() == object_name) {
            ret = region_info;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return ret;
}

Optional<MemoryRegionInfo> Reader::region_containing(FlatPtr address) const
{
    Optional<MemoryRegionInfo> ret;
    for_each_memory_region_info([&ret, address](auto const& region_info) {
        if (region_info.region_start <= address && region_info.region_end >= address) {
            ret = region_info;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return ret;
}

int Reader::process_pid() const
{
    auto process_info = this->process_info();
    auto pid = process_info.get_integer<int>("pid"sv).value_or(0);
    return pid;
}

u8 Reader::process_termination_signal() const
{
    auto process_info = this->process_info();
    auto termination_signal = process_info.get_u8("termination_signal"sv);
    if (!termination_signal.has_value() || *termination_signal <= SIGINVAL || *termination_signal >= NSIG)
        return SIGINVAL;
    return *termination_signal;
}

ByteString Reader::process_executable_path() const
{
    auto process_info = this->process_info();
    auto executable_path = process_info.get_byte_string("executable_path"sv);
    return executable_path.value_or({});
}

Vector<ByteString> Reader::process_arguments() const
{
    auto process_info = this->process_info();
    auto arguments = process_info.get_array("arguments"sv);
    if (!arguments.has_value())
        return {};
    Vector<ByteString> vector;
    arguments->for_each([&](auto& value) {
        if (value.is_string())
            vector.append(value.as_string());
    });
    return vector;
}

Vector<ByteString> Reader::process_environment() const
{
    auto process_info = this->process_info();
    auto environment = process_info.get_array("environment"sv);
    if (!environment.has_value())
        return {};
    Vector<ByteString> vector;
    environment->for_each([&](auto& value) {
        if (value.is_string())
            vector.append(value.as_string());
    });
    return vector;
}

HashMap<ByteString, ByteString> Reader::metadata() const
{
    const ELF::Core::Metadata* metadata_notes_entry = nullptr;
    NotesEntryIterator it(bit_cast<u8 const*>(m_coredump_image.program_header(m_notes_segment_index).raw_data()));
    for (; !it.at_end(); it.next()) {
        if (it.type() != ELF::Core::NotesEntryHeader::Type::Metadata)
            continue;
        metadata_notes_entry = bit_cast<const ELF::Core::Metadata*>(it.current());
        break;
    }
    if (!metadata_notes_entry)
        return {};
    auto const* json_data_ptr = metadata_notes_entry->json_data;
    auto metadata_json_value = JsonValue::from_string({ json_data_ptr, strlen(json_data_ptr) });
    if (metadata_json_value.is_error())
        return {};
    if (!metadata_json_value.value().is_object())
        return {};
    HashMap<ByteString, ByteString> metadata;
    metadata_json_value.value().as_object().for_each_member([&](auto& key, auto& value) {
        metadata.set(key, value.as_string_or({}));
    });
    return metadata;
}

Reader::LibraryData const* Reader::library_containing(FlatPtr address) const
{
    static HashMap<ByteString, OwnPtr<LibraryData>> cached_libs;
    auto region = region_containing(address);
    if (!region.has_value())
        return {};

    auto name = region->object_name();
    ByteString path = resolve_object_path(name);

    if (!cached_libs.contains(path)) {
        auto file_or_error = Core::MappedFile::map(path);
        if (file_or_error.is_error())
            return {};
        auto image = ELF::Image(file_or_error.value()->bytes());
        cached_libs.set(path, make<LibraryData>(name, static_cast<FlatPtr>(region->region_start), file_or_error.release_value(), move(image)));
    }

    auto lib_data = cached_libs.get(path).value();
    return lib_data;
}

ByteString Reader::resolve_object_path(StringView name) const
{
    // TODO: There are other places where similar method is implemented or would be useful.
    //       (e.g. LibSymbolication, Profiler, and DynamicLinker itself)
    //       We should consider creating unified implementation in the future.

    if (name.starts_with('/') || !FileSystem::looks_like_shared_library(name)) {
        return name;
    }

    Vector<ByteString> library_search_directories;

    // If LD_LIBRARY_PATH is present, check its folders first
    for (auto& environment_variable : process_environment()) {
        auto prefix = "LD_LIBRARY_PATH="sv;
        if (environment_variable.starts_with(prefix)) {
            auto ld_library_path = environment_variable.substring_view(prefix.length());

            // FIXME: This code won't handle folders with ":" in the name correctly.
            for (auto directory : ld_library_path.split_view(':')) {
                library_search_directories.append(directory);
            }
        }
    }

    // Add default paths that DynamicLinker uses
    library_search_directories.append("/usr/lib/"sv);
    library_search_directories.append("/usr/local/lib/"sv);

    // Search for the first readable library file
    for (auto& directory : library_search_directories) {
        auto full_path = LexicalPath::join(directory, name).string();

        if (access(full_path.characters(), R_OK) != 0)
            continue;

        return full_path;
    }

    return name;
}

void Reader::for_each_library(Function<void(LibraryInfo)> func) const
{
    HashTable<ByteString> libraries;
    for_each_memory_region_info([&](auto const& region) {
        auto name = region.object_name();
        if (name.is_null() || libraries.contains(name))
            return IterationDecision::Continue;

        libraries.set(name);

        ByteString path = resolve_object_path(name);

        func(LibraryInfo { name, path, static_cast<FlatPtr>(region.region_start) });
        return IterationDecision::Continue;
    });
}

}
