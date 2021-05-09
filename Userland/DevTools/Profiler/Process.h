/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/MappedFile.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <LibELF/Image.h>

namespace Profiler {

struct MappedObject {
    NonnullRefPtr<MappedFile> file;
    ELF::Image elf;
};

extern HashMap<String, OwnPtr<MappedObject>> g_mapped_object_cache;

class LibraryMetadata {
public:
    struct Library {
        FlatPtr base;
        size_t size;
        String name;
        FlatPtr text_base;
        MappedObject* object { nullptr };

        String symbolicate(FlatPtr, u32* offset) const;
    };

    void handle_mmap(FlatPtr base, size_t size, const String& name);
    const Library* library_containing(FlatPtr) const;

private:
    mutable HashMap<String, NonnullOwnPtr<Library>> m_libraries;
};

struct Thread {
    pid_t tid;
    u64 start_valid;
    u64 end_valid { 0 };

    bool valid_at(u64 timestamp) const
    {
        return timestamp >= start_valid && (end_valid == 0 || timestamp <= end_valid);
    }
};

struct Process {
    pid_t pid {};
    String executable;
    HashMap<int, Vector<Thread>> threads {};
    LibraryMetadata library_metadata {};
    u64 start_valid;
    u64 end_valid { 0 };

    Thread* find_thread(pid_t tid, u64 timestamp);
    void handle_thread_create(pid_t tid, u64 timestamp);
    void handle_thread_exit(pid_t tid, u64 timestamp);

    bool valid_at(u64 timestamp) const
    {
        return timestamp >= start_valid && (end_valid == 0 || timestamp <= end_valid);
    }
};

}
