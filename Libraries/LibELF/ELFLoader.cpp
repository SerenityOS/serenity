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

#include "ELFLoader.h"
#include <AK/Demangle.h>
#include <AK/QuickSort.h>

#ifdef KERNEL
#include <Kernel/VM/MemoryManager.h>
#define do_memcpy copy_to_user
#else
#define do_memcpy memcpy
#endif

//#define ELFLOADER_DEBUG

ELFLoader::ELFLoader(const u8* buffer, size_t size)
    : m_image(buffer, size)
{
}

ELFLoader::~ELFLoader()
{
}

bool ELFLoader::load()
{
#ifdef ELFLOADER_DEBUG
    m_image.dump();
#endif
    if (!m_image.is_valid())
        return false;

    if (!layout())
        return false;

    return true;
}

bool ELFLoader::layout()
{
    bool failed = false;
    m_image.for_each_program_header([&](const ELFImage::ProgramHeader& program_header) {
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
            do_memcpy(tls_image, program_header.raw_data(), program_header.size_in_image());
#endif
            return;
        }
        if (program_header.type() != PT_LOAD)
            return;
#ifdef ELFLOADER_DEBUG
        kprintf("PH: V%p %u r:%u w:%u\n", program_header.vaddr().get(), program_header.size_in_memory(), program_header.is_readable(), program_header.is_writable());
#endif
#ifdef KERNEL
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
            do_memcpy((u8*)allocated_section + page_offset.get(), program_header.raw_data(), program_header.size_in_image());
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

char* ELFLoader::symbol_ptr(const char* name)
{
    char* found_ptr = nullptr;
    m_image.for_each_symbol([&](const ELFImage::Symbol symbol) {
        if (symbol.type() != STT_FUNC)
            return IterationDecision::Continue;
        if (symbol.name() == name)
            return IterationDecision::Continue;
        if (m_image.is_executable())
            found_ptr = (char*)(size_t)symbol.value();
        else
            ASSERT_NOT_REACHED();
        return IterationDecision::Break;
    });
    return found_ptr;
}

String ELFLoader::symbolicate(u32 address, u32* out_offset) const
{
    SortedSymbol* sorted_symbols = nullptr;
#ifdef KERNEL
    if (!m_sorted_symbols_region) {
        m_sorted_symbols_region = MM.allocate_kernel_region(PAGE_ROUND_UP(m_image.symbol_count() * sizeof(SortedSymbol)), "Sorted symbols", Kernel::Region::Access::Read | Kernel::Region::Access::Write);
        sorted_symbols = (SortedSymbol*)m_sorted_symbols_region->vaddr().as_ptr();
        size_t index = 0;
        m_image.for_each_symbol([&](auto& symbol) {
            sorted_symbols[index++] = { symbol.value(), symbol.name() };
            return IterationDecision::Continue;
        });
        quick_sort(sorted_symbols, sorted_symbols + m_image.symbol_count(), [](auto& a, auto& b) {
            return a.address < b.address;
        });
    } else {
        sorted_symbols = (SortedSymbol*)m_sorted_symbols_region->vaddr().as_ptr();
    }
#else
    if (m_sorted_symbols.is_empty()) {
        m_sorted_symbols.ensure_capacity(m_image.symbol_count());
        m_image.for_each_symbol([this](auto& symbol) {
            m_sorted_symbols.append({ symbol.value(), symbol.name() });
            return IterationDecision::Continue;
        });
        quick_sort(m_sorted_symbols.begin(), m_sorted_symbols.end(), [](auto& a, auto& b) {
            return a.address < b.address;
        });
    }
    sorted_symbols = m_sorted_symbols.data();
#endif

    for (size_t i = 0; i < m_image.symbol_count(); ++i) {
        if (sorted_symbols[i].address > address) {
            if (i == 0)
                return "!!";
            auto& symbol = sorted_symbols[i - 1];
            if (out_offset) {
                *out_offset = address - symbol.address;
                return demangle(symbol.name);
            }
            return String::format("%s +%u", demangle(symbol.name).characters(), address - symbol.address);
        }
    }
    return "??";
}
