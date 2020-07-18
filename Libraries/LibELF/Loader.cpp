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

Loader::Loader(const u8* buffer, size_t size)
    : m_image(buffer, size)
{
    m_symbol_count = m_image.symbol_count();
}

Loader::~Loader()
{
}

Optional<AuxiliaryData> Loader::load()
{
#ifdef Loader_DEBUG
    m_image.dump();
#endif
    if (!m_image.is_valid())
        return {};

    if (m_image.is_dynamic()) {
        if (!layout_dynamic())
            return {};
    } else {
        ASSERT(m_image.is_executable());
        if (!layout_static())
            return {};
    }

    AuxiliaryData aux;
    aux.program_headers = (u32)m_base_address + m_image.program_headers_offset();
    aux.num_program_headers = m_image.program_header_count();
    aux.entry_point = m_image.entry().offset((u32)m_base_address).get();
    aux.base_address = (u32)m_base_address;
    aux.tls_section_size = m_tls_section_size;
    aux.text_segment_size = m_text_segment_size;
    return aux;
}

#define ALIGN_ROUND_UP(x, align) ((((size_t)(x)) + align - 1) & (~(align - 1)))

bool Loader::layout_dynamic()
{
#ifndef KERNEL
    ASSERT_NOT_REACHED();
#else

    Optional<Image::ProgramHeader> text_header;
    Optional<Image::ProgramHeader> data_header;
    Optional<Image::ProgramHeader> dynamic_header;

    m_image.for_each_program_header([&](const Image::ProgramHeader& program_header) {
        if (program_header.type() == PT_LOAD) {
            if (program_header.is_executable()) {
                text_header = program_header;
                m_text_segment_size = program_header.size_in_memory();
            } else {
                ASSERT(program_header.is_readable() && program_header.is_writable());
            }
            data_header = program_header;
        } else if (program_header.type() == PT_DYNAMIC) {
            dynamic_header = program_header;
        } else if (program_header.type() == PT_TLS) {
            m_tls_section_size = program_header.size_in_memory();
        }
    });

    ASSERT(text_header.has_value());
    ASSERT(data_header.has_value());
    ASSERT(dynamic_header.has_value());

    m_base_address = map_section_hook(
        {},
        text_header.value().size_in_memory(),
        text_header.value().alignment(),
        text_header.value().offset(),
        text_header.value().is_readable(),
        text_header.value().is_writable(),
        text_header.value().is_executable(),
        String::format("elf-map-%s%s%s", text_header.value().is_readable() ? "r" : "", text_header.value().is_writable() ? "w" : "", text_header.value().is_executable() ? "x" : ""));
    if (!m_base_address) {
        return false;
    }

    u32 text_segment_size = ALIGN_ROUND_UP(text_header.value().size_in_memory(), text_header.value().alignment());

    // TODO: use this
    // void* dynamic_section_address = dynamic_header.value().vaddr().offset((u32)m_base_address).as_ptr();

    void* data_segment_begin = alloc_section_hook(
        VirtualAddress((u32)m_base_address + text_segment_size),
        data_header.value().size_in_memory(),
        data_header.value().alignment(),
        data_header.value().is_readable(),
        data_header.value().is_writable(),
        String::format("elf-alloc-%s%s", data_header.value().is_readable() ? "r" : "", data_header.value().is_writable() ? "w" : ""));

    if (!data_segment_begin) {
        return false;
    }

    VirtualAddress data_segment_actual_addr = data_header.value().vaddr().offset((u32)m_base_address);
    copy_to_user(data_segment_actual_addr.as_ptr(), (const u8*)data_header.value().raw_data(), data_header.value().size_in_image());
    return true;
#endif
}

bool Loader::layout_static()
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
            do_memcpy(tls_image, program_header.raw_data(), program_header.size_in_image());
#endif
            return;
        }
        if (program_header.type() != PT_LOAD)
            return;
#ifdef Loader_DEBUG
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
    if (failed)
        return {};
    return !failed;
}

char* Loader::symbol_ptr(const char* name) const
{
    char* found_ptr = nullptr;
    m_image.for_each_symbol([&](const Image::Symbol symbol) {
        if (symbol.type() != STT_FUNC)
            return IterationDecision::Continue;
        if (symbol.name() != name)
            return IterationDecision::Continue;
        if (m_image.is_executable())
            found_ptr = (char*)(size_t)symbol.value();
        else
            ASSERT_NOT_REACHED();
        return IterationDecision::Break;
    });
    return found_ptr;
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
