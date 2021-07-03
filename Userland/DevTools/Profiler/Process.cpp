/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Process.h"

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

HashMap<String, OwnPtr<MappedObject>> g_mapped_object_cache;

static MappedObject* get_or_create_mapped_object(const String& path)
{
    if (auto it = g_mapped_object_cache.find(path); it != g_mapped_object_cache.end())
        return it->value.ptr();

    auto file_or_error = MappedFile::map(path);
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

void LibraryMetadata::handle_mmap(FlatPtr base, size_t size, const String& name)
{
    String path;
    if (name.contains("Loader.so"))
        path = "Loader.so";
    else if (!name.contains(":"))
        return;
    else
        path = name.substring(0, name.view().find_first_of(":").value());

    String full_path;
    if (name.contains(".so"))
        full_path = String::formatted("/usr/lib/{}", path);
    else
        full_path = path;

    auto* mapped_object = get_or_create_mapped_object(full_path);
    if (!mapped_object) {
        full_path = String::formatted("/usr/local/lib/{}", path);
        mapped_object = get_or_create_mapped_object(full_path);
        if (!mapped_object)
            return;
    }

    FlatPtr text_base {};
    mapped_object->elf.for_each_program_header([&](const ELF::Image::ProgramHeader& ph) {
        if (ph.is_executable())
            text_base = ph.vaddr().get();
        return IterationDecision::Continue;
    });

    m_libraries.set(name, adopt_own(*new Library { base, size, name, text_base, mapped_object }));
}

String LibraryMetadata::Library::symbolicate(FlatPtr ptr, u32* offset) const
{
    if (!object)
        return String::formatted("?? <{:p}>", ptr);

    return object->elf.symbolicate(ptr - base + text_base, offset);
}

const LibraryMetadata::Library* LibraryMetadata::library_containing(FlatPtr ptr) const
{
    for (auto& it : m_libraries) {
        auto& library = *it.value;
        if (ptr >= library.base && ptr < (library.base + library.size))
            return &library;
    }
    return nullptr;
}

}
