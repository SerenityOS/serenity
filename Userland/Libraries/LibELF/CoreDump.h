/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <LibC/sys/arch/i386/regs.h>

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
    // - "executable_path" (String)
    // - "arguments" (Vector<String>)
    // - "environment" (Vector<String>)
    char json_data[]; // Null terminated
};

struct [[gnu::packed]] ThreadInfo {
    NotesEntryHeader header;
    int tid;
    PtraceRegisters regs;
};

struct [[gnu::packed]] MemoryRegionInfo {
    NotesEntryHeader header;
    uint32_t region_start;
    uint32_t region_end;
    uint16_t program_header_index;
    char region_name[]; // Null terminated

    String object_name() const
    {
        StringView memory_region_name { region_name };
        if (memory_region_name.contains("Loader.so"))
            return "Loader.so";
        if (!memory_region_name.contains(":"))
            return {};
        return memory_region_name.substring_view(0, memory_region_name.find_first_of(":").value()).to_string();
    }
};

struct [[gnu::packed]] Metadata {
    NotesEntryHeader header;
    // Arbitrary metadata, set via SC_set_coredump_metadata.
    // Limited to 16 entries and 16 KiB keys/values by the kernel.
    //
    // Well-known keys:
    // - "assertion": Used by LibC's __assertion_failed() to store assertion info
    // - "pledge_violation": Used by the Kernel's REQUIRE_PROMISE() to store pledge violation info
    char json_data[]; // Null terminated
};

}
