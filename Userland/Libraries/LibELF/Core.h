/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <sys/arch/regs.h>

#ifndef KERNEL
#    include <AK/ByteString.h>
#endif

namespace ELF::Core {

struct [[gnu::packed]] NotesEntryHeader {
    enum Type : u8 {
        Null = 0, // Terminates segment
        ProcessInfo,
        ThreadInfo,
        MemoryRegionInfo,
        Metadata,
    };
    Type type;
};

struct [[gnu::packed]] NotesEntry {
    NotesEntryHeader header;
    char data[];
};

struct [[gnu::packed]] ProcessInfo {
    NotesEntryHeader header;
    // Information is stored as JSON blob to allow arbitrary
    // number and length of strings/objects/arrays.
    //
    // Keys:
    // - "pid" (int)
    // - "termination_signal" (u8)
    // - "executable_path" (ByteString)
    // - "arguments" (Vector<ByteString>)
    // - "environment" (Vector<ByteString>)
    char json_data[]; // Null terminated
};

struct [[gnu::packed]] ThreadInfo {
    NotesEntryHeader header;
    int tid;
    PtraceRegisters regs;
};

struct [[gnu::packed]] MemoryRegionInfo {
    NotesEntryHeader header;
    uint64_t region_start;
    uint64_t region_end;
    uint16_t program_header_index;
    char region_name[]; // Null terminated

#ifndef KERNEL
    ByteString object_name() const
    {
        StringView memory_region_name { region_name, strlen(region_name) };
        if (memory_region_name.contains("Loader.so"sv))
            return "Loader.so"sv;
        auto maybe_colon_index = memory_region_name.find(':');
        if (!maybe_colon_index.has_value())
            return {};
        return memory_region_name.substring_view(0, *maybe_colon_index).to_byte_string();
    }
#endif
};

struct [[gnu::packed]] Metadata {
    NotesEntryHeader header;
    // Arbitrary metadata, set via SC_set_coredump_metadata.
    // Limited to 16 entries and 16 KiB keys/values by the kernel.
    //
    // Well-known keys:
    // - "assertion": Used by LibC's __assertion_failed() to store assertion info
    // - "pledge_violation": Used by the Kernel's require_promise() to store pledge violation info
    char json_data[]; // Null terminated
};

}
