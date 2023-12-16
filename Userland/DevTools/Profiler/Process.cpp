/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Process.h"
#include <LibFileSystem/FileSystem.h>

namespace Profiler {

Thread* Process::find_thread(pid_t tid, EventSerialNumber serial)
{
    auto it = threads.find(tid);
    if (it == threads.end())
        return nullptr;
    for (auto& thread : it->value) {
        if (thread.start_valid < serial && (thread.end_valid == EventSerialNumber {} || thread.end_valid > serial))
            return &thread;
    }
    return nullptr;
}

void Process::handle_thread_create(pid_t tid, EventSerialNumber serial)
{
    auto it = threads.find(tid);
    if (it == threads.end()) {
        threads.set(tid, {});
        it = threads.find(tid);
    }

    auto thread = Thread { tid, serial, {} };
    it->value.append(move(thread));
}

void Process::handle_thread_exit(pid_t tid, EventSerialNumber serial)
{
    auto* thread = find_thread(tid, serial);
    if (!thread)
        return;
    thread->end_valid = serial;
}

HashMap<ByteString, OwnPtr<MappedObject>> g_mapped_object_cache;

static MappedObject* get_or_create_mapped_object(ByteString const& path)
{
    if (auto it = g_mapped_object_cache.find(path); it != g_mapped_object_cache.end())
        return it->value.ptr();

    auto file_or_error = Core::MappedFile::map(path);
    if (file_or_error.is_error()) {
        g_mapped_object_cache.set(path, {});
        return nullptr;
    }
    auto elf = ELF::Image(file_or_error.value()->bytes());
    if (!elf.is_valid()) {
        g_mapped_object_cache.set(path, {});
        return nullptr;
    }
    auto new_mapped_object = adopt_own(*new MappedObject {
        .file = file_or_error.release_value(),
        .elf = elf,
    });
    auto* ptr = new_mapped_object.ptr();
    g_mapped_object_cache.set(path, move(new_mapped_object));
    return ptr;
}

void LibraryMetadata::handle_mmap(FlatPtr base, size_t size, ByteString const& name)
{
    StringView path;
    if (name.contains("Loader.so"sv))
        path = "Loader.so"sv;
    else if (!name.contains(':'))
        return;
    else
        path = name.substring_view(0, name.view().find(':').value());

    // Each loaded object has at least 4 segments associated with it: .rodata, .text, .relro, .data.
    // We only want to create a single LibraryMetadata object for each library, so we need to update the
    // associated base address and size as new regions are discovered.

    // We don't allocate a temporary String object if an entry already exists.
    // This assumes that ByteString::hash and StringView::hash return the same result.
    auto string_view_compare = [&path](auto& entry) { return path == entry.key.view(); };
    if (auto existing_it = m_libraries.find(path.hash(), string_view_compare); existing_it != m_libraries.end()) {
        auto& entry = *existing_it->value;
        entry.base = min(entry.base, base);
        entry.size = max(entry.size + size, base - entry.base + size);
    } else {
        ByteString path_string = path.to_byte_string();
        ByteString full_path;
        if (path_string.starts_with('/'))
            full_path = path_string;
        else if (FileSystem::looks_like_shared_library(path_string))
            full_path = ByteString::formatted("/usr/lib/{}", path);
        else
            full_path = path_string;

        auto* mapped_object = get_or_create_mapped_object(full_path);
        if (!mapped_object) {
            full_path = ByteString::formatted("/usr/local/lib/{}", path);
            mapped_object = get_or_create_mapped_object(full_path);
            if (!mapped_object)
                return;
        }
        m_libraries.set(path_string, adopt_own(*new Library { base, size, path_string, mapped_object, {} }));
    }
}

Debug::DebugInfo const& LibraryMetadata::Library::load_debug_info(FlatPtr base_address) const
{
    if (debug_info == nullptr)
        debug_info = make<Debug::DebugInfo>(object->elf, ByteString::empty(), base_address);
    return *debug_info.ptr();
}

ByteString LibraryMetadata::Library::symbolicate(FlatPtr ptr, u32* offset) const
{
    if (!object)
        return ByteString::formatted("?? <{:p}>", ptr);

    return object->elf.symbolicate(ptr - base, offset);
}

LibraryMetadata::Library const* LibraryMetadata::library_containing(FlatPtr ptr) const
{
    for (auto& it : m_libraries) {
        auto& library = *it.value;
        if (ptr >= library.base && ptr < (library.base + library.size))
            return &library;
    }
    return nullptr;
}

}
