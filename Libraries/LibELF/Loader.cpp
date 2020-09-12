/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "Loader.h"
#include <AK/Demangle.h>
#include <AK/Memory.h>
#include <AK/QuickSort.h>

#ifdef KERNEL
#    include <Kernel/VM/MemoryManager.h>
#    define do_memcpy copy_to_user
#else
#    define do_memcpy memcpy
#endif

//#define Loader_DEBUG

namespace ELF {

Loader::Loader(const u8* buffer, size_t size, bool verbose_logging)
    : m_image(buffer, size, verbose_logging)
{
    if (m_image.is_valid())
        m_symbol_count = m_image.symbol_count();
}

Loader::~Loader()
{
}

bool Loader::load()
{
#ifdef Loader_DEBUG
    m_image.dump();
#endif
    if (!m_image.is_valid())
        return false;

    if (!layout())
        return false;

    return true;
}

bool Loader::layout()
{
    bool failed = false;
    m_image.for_each_program_header([&](const Image::ProgramHeader& program_header) {
        if (program_header.type() == PT_TLS) {
#ifdef KERNEL
            auto* tls_image = tls_section_hook(program_header.size_in_memory(), program_header.alignment());
            if (!tls_image) {
                failed = true;
                return;
            }
            if (!m_image.is_within_image(program_header.raw_data(), program_header.size_in_image())) {
                dbg() << "Shenanigans! ELF PT_TLS header sneaks outside of executable.";
                failed = true;
                return;
            }
            if (!do_memcpy(tls_image, program_header.raw_data(), program_header.size_in_image())) {
                failed = false;
                return;
            }
#endif
            return;
        }
        if (program_header.type() != PT_LOAD)
            return;
#ifdef KERNEL
#    ifdef Loader_DEBUG
        kprintf("PH: V%p %u r:%u w:%u\n", program_header.vaddr().get(), program_header.size_in_memory(), program_header.is_readable(), program_header.is_writable());
#    endif
        if (program_header.is_writable()) {
            auto* allocated_section = alloc_section_hook(
                program_header.vaddr(),
                program_header.size_in_memory(),
                program_header.alignment(),
                program_header.is_readable(),
                program_header.is_writable(),
                String::format("elf-alloc-%s%s", program_header.is_readable() ? "r" : "", program_header.is_writable() ? "w" : ""));
            if (!allocated_section) {
                failed = true;
                return;
            }
            if (!m_image.is_within_image(program_header.raw_data(), program_header.size_in_image())) {
                dbg() << "Shenanigans! Writable ELF PT_LOAD header sneaks outside of executable.";
                failed = true;
                return;
            }
            // It's not always the case with PIE executables (and very well shouldn't be) that the
            // virtual address in the program header matches the one we end up giving the process.
            // In order to copy the data image correctly into memory, we need to copy the data starting at
            // the right initial page offset into the pages allocated for the elf_alloc-XX section.
            // FIXME: There's an opportunity to munmap, or at least mprotect, the padding space between
            //     the .text and .data PT_LOAD sections of the executable.
            //     Accessing it would definitely be a bug.
            auto page_offset = program_header.vaddr();
            page_offset.mask(~PAGE_MASK);
            if (!do_memcpy((u8*)allocated_section + page_offset.get(), program_header.raw_data(), program_header.size_in_image())) {
                failed = false;
                return;
            }
        } else {
            auto* mapped_section = map_section_hook(
                program_header.vaddr(),
                program_header.size_in_memory(),
                program_header.alignment(),
                program_header.offset(),
                program_header.is_readable(),
                program_header.is_writable(),
                program_header.is_executable(),
                String::format("elf-map-%s%s%s", program_header.is_readable() ? "r" : "", program_header.is_writable() ? "w" : "", program_header.is_executable() ? "x" : ""));
            if (!mapped_section) {
                failed = true;
            }
        }
#endif
    });
    return !failed;
}

Optional<Image::Symbol> Loader::find_demangled_function(const String& name) const
{
    Optional<Image::Symbol> found;
    m_image.for_each_symbol([&](const Image::Symbol symbol) {
        if (symbol.type() != STT_FUNC)
            return IterationDecision::Continue;
        auto demangled = demangle(symbol.name());
        auto index_of_paren = demangled.index_of("(");
        if (index_of_paren.has_value()) {
            demangled = demangled.substring(0, index_of_paren.value());
        }
        if (demangled != name)
            return IterationDecision::Continue;
        found = symbol;
        return IterationDecision::Break;
    });
    return found;
}

#ifndef KERNEL
Optional<Image::Symbol> Loader::find_symbol(u32 address, u32* out_offset) const
{
    if (!m_symbol_count)
        return {};

    SortedSymbol* sorted_symbols = nullptr;
#    ifdef KERNEL
    if (!m_sorted_symbols_region) {
        m_sorted_symbols_region = MM.allocate_kernel_region(PAGE_ROUND_UP(m_symbol_count * sizeof(SortedSymbol)), "Sorted symbols", Kernel::Region::Access::Read | Kernel::Region::Access::Write);
        sorted_symbols = (SortedSymbol*)m_sorted_symbols_region->vaddr().as_ptr();
        size_t index = 0;
        m_image.for_each_symbol([&](auto& symbol) {
            sorted_symbols[index++] = { symbol.value(), symbol.name() };
            return IterationDecision::Continue;
        });
        quick_sort(sorted_symbols, sorted_symbols + m_symbol_count, [](auto& a, auto& b) {
            return a.address < b.address;
        });
    } else {
        sorted_symbols = (SortedSymbol*)m_sorted_symbols_region->vaddr().as_ptr();
    }
#    else
    if (m_sorted_symbols.is_empty()) {
        m_sorted_symbols.ensure_capacity(m_symbol_count);
        m_image.for_each_symbol([this](auto& symbol) {
            m_sorted_symbols.append({ symbol.value(), symbol.name(), {}, symbol });
            return IterationDecision::Continue;
        });
        quick_sort(m_sorted_symbols, [](auto& a, auto& b) {
            return a.address < b.address;
        });
    }
    sorted_symbols = m_sorted_symbols.data();
#    endif

    for (size_t i = 0; i < m_symbol_count; ++i) {
        if (sorted_symbols[i].address > address) {
            if (i == 0)
                return {};
            auto& symbol = sorted_symbols[i - 1];
            if (out_offset)
                *out_offset = address - symbol.address;
            return symbol.symbol;
        }
    }
    return {};
}
#endif

String Loader::symbolicate(u32 address, u32* out_offset) const
{
    if (!m_symbol_count) {
        if (out_offset)
            *out_offset = 0;
        return "??";
    }
    SortedSymbol* sorted_symbols = nullptr;
#ifdef KERNEL
    if (!m_sorted_symbols_region) {
        m_sorted_symbols_region = MM.allocate_kernel_region(PAGE_ROUND_UP(m_symbol_count * sizeof(SortedSymbol)), "Sorted symbols", Kernel::Region::Access::Read | Kernel::Region::Access::Write);
        sorted_symbols = (SortedSymbol*)m_sorted_symbols_region->vaddr().as_ptr();
        size_t index = 0;
        m_image.for_each_symbol([&](auto& symbol) {
            sorted_symbols[index++] = { symbol.value(), symbol.name() };
            return IterationDecision::Continue;
        });
        quick_sort(sorted_symbols, sorted_symbols + m_symbol_count, [](auto& a, auto& b) {
            return a.address < b.address;
        });
    } else {
        sorted_symbols = (SortedSymbol*)m_sorted_symbols_region->vaddr().as_ptr();
    }
#else
    if (m_sorted_symbols.is_empty()) {
        m_sorted_symbols.ensure_capacity(m_symbol_count);
        m_image.for_each_symbol([this](auto& symbol) {
            m_sorted_symbols.append({ symbol.value(), symbol.name(), {}, {} });
            return IterationDecision::Continue;
        });
        quick_sort(m_sorted_symbols, [](auto& a, auto& b) {
            return a.address < b.address;
        });
    }
    sorted_symbols = m_sorted_symbols.data();
#endif

    for (size_t i = 0; i < m_symbol_count; ++i) {
        if (sorted_symbols[i].address > address) {
            if (i == 0) {
                if (out_offset)
                    *out_offset = 0;
                return "!!";
            }
            auto& symbol = sorted_symbols[i - 1];

#ifdef KERNEL
            auto demangled_name = demangle(symbol.name);
#else
            auto& demangled_name = symbol.demangled_name;
            if (demangled_name.is_null())
                demangled_name = demangle(symbol.name);
#endif

            if (out_offset) {
                *out_offset = address - symbol.address;
                return demangled_name;
            }
            return String::format("%s +%u", demangled_name.characters(), address - symbol.address);
        }
    }
    if (out_offset)
        *out_offset = 0;
    return "??";
}

} // end namespace ELF
