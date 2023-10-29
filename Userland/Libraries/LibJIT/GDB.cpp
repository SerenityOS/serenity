/*
 * Copyright (c) 2023, Jesús Lapastora <cyber.gsuscode@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/OwnPtr.h>
#include <AK/Span.h>
#include <LibJIT/GDB.h>

namespace JIT::GDB {

// Declarations from https://sourceware.org/gdb/current/onlinedocs/gdb.html/Declarations.html#Declarations.
enum class JitActions : u32 {
    NoAction = 0,
    RegisterFn = 1,
    UnregisterFn = 2,
};

struct JitCodeEntry {
    JitCodeEntry* next_entry;
    JitCodeEntry* prev_entry;
    char const* symfile_addr;
    u64 symfile_size;
};

struct JitDescriptor {
    u32 version;
    JitActions action_flag;
    JitCodeEntry* relevant_entry;
    JitCodeEntry* first_entry;
};
extern "C" {

// GDB puts a breakpoint in this function.
// Use an __asm__ statement to prevent compilers from inlining/removing this
// function.
// From V8:
// https://github.com/v8/v8/blob/1c7d3a94fc051e0fd69584b930faab448026941e/src/diagnostics/gdb-jit.cc#L1742
void __attribute__((noinline)) __jit_debug_register_code();
void __attribute__((noinline)) __jit_debug_register_code() { __asm__(""); }

// Make sure to specify the version statically, because the debugger may check
// the version before we can set it.
// NOTE: If the JIT is multi-threaded, then it is important that the JIT synchronize any modifications to this global data properly, which can easily be done by putting a global mutex around modifications to these structures.
JitDescriptor __jit_debug_descriptor = { 1, JitActions::NoAction, nullptr, nullptr };
}

static JitCodeEntry* find_code_entry(ReadonlyBytes data)
{
    auto const search_addr = bit_cast<uintptr_t>(data.offset(0));
    for (JitCodeEntry* curr = __jit_debug_descriptor.first_entry; curr != NULL; curr = curr->next_entry) {
        auto const entry_addr = bit_cast<uintptr_t>(curr->symfile_addr);
        if (entry_addr == search_addr) {
            VERIFY(curr->symfile_size == data.size());
            return curr;
        }
    }
    return NULL;
}

void unregister_from_gdb(ReadonlyBytes data)
{
    // Following steps from:
    // https://sourceware.org/gdb/current/onlinedocs/gdb.html/Unregistering-Code.html#Unregistering-Code
    auto may_have_entry = AK::adopt_own_if_nonnull(find_code_entry(data));
    VERIFY(may_have_entry);
    auto entry = may_have_entry.release_nonnull();
    if (entry->prev_entry) {
        entry->prev_entry->next_entry = entry->next_entry;
    }
    if (entry->next_entry) {
        entry->next_entry->prev_entry = entry->prev_entry;
    }
    if (entry == __jit_debug_descriptor.first_entry) {
        __jit_debug_descriptor.first_entry = entry->next_entry;
    }
    __jit_debug_descriptor.relevant_entry = entry;
    __jit_debug_descriptor.action_flag = JitActions::UnregisterFn;
    __jit_debug_register_code();
}

void register_into_gdb(ReadonlyBytes data)
{
    // Following steps from:
    // https://sourceware.org/gdb/current/onlinedocs/gdb.html/Registering-Code.html#Registering-Code
    auto* const leaked_entry = make<JitCodeEntry>().leak_ptr();

    // Add it to the linked list in the JIT descriptor.
    leaked_entry->symfile_addr = reinterpret_cast<char const*>(data.data());
    leaked_entry->symfile_size = data.size();
    leaked_entry->next_entry = __jit_debug_descriptor.first_entry;
    leaked_entry->prev_entry = NULL;
    if (__jit_debug_descriptor.first_entry) {
        VERIFY(__jit_debug_descriptor.first_entry->prev_entry == nullptr);
        __jit_debug_descriptor.first_entry->prev_entry = leaked_entry;
    }
    __jit_debug_descriptor.first_entry = leaked_entry;
    __jit_debug_descriptor.relevant_entry = leaked_entry;
    __jit_debug_descriptor.action_flag = JitActions::RegisterFn;
    __jit_debug_register_code();
}
}
