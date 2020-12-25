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

//#define Loader_DEBUG

namespace ELF {

Loader::Loader(const u8* buffer, size_t size, String&&, bool verbose_logging)
    : m_image(buffer, size, verbose_logging)
{
    if (m_image.is_valid())
        m_symbol_count = m_image.symbol_count();
}

Loader::~Loader()
{
}

#ifndef KERNEL
Optional<Image::Symbol> Loader::find_symbol(u32 address, u32* out_offset) const
{
    if (!m_symbol_count)
        return {};

    SortedSymbol* sorted_symbols = nullptr;
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

String Loader::symbolicate(u32 address, u32* out_offset) const
{
    if (!m_symbol_count) {
        if (out_offset)
            *out_offset = 0;
        return "??";
    }
    SortedSymbol* sorted_symbols = nullptr;

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

    for (size_t i = 0; i < m_symbol_count; ++i) {
        if (sorted_symbols[i].address > address) {
            if (i == 0) {
                if (out_offset)
                    *out_offset = 0;
                return "!!";
            }
            auto& symbol = sorted_symbols[i - 1];

            auto& demangled_name = symbol.demangled_name;
            if (demangled_name.is_null()) {
                demangled_name = demangle(symbol.name);
            }

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

#endif

} // end namespace ELF
