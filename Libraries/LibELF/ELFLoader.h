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

#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <LibELF/ELFImage.h>

#ifdef KERNEL
#include <Kernel/VM/VirtualAddress.h>
class Region;
#endif

class ELFLoader {
public:
    explicit ELFLoader(const u8*, size_t);
    ~ELFLoader();

    bool load();
#if defined(KERNEL)
    Function<void*(VirtualAddress, size_t, size_t, bool, bool, const String&)> alloc_section_hook;
    Function<void*(size_t, size_t)> tls_section_hook;
    Function<void*(VirtualAddress, size_t, size_t, size_t, bool r, bool w, bool x, const String&)> map_section_hook;
    VirtualAddress entry() const { return m_image.entry(); }
#endif
    char* symbol_ptr(const char* name);

    bool has_symbols() const { return m_image.symbol_count(); }

    String symbolicate(u32 address, u32* offset = nullptr) const;

private:
    bool layout();
    bool perform_relocations();
    void* lookup(const ELFImage::Symbol&);
    char* area_for_section(const ELFImage::Section&);
    char* area_for_section_name(const char*);

    struct PtrAndSize {
        PtrAndSize() {}
        PtrAndSize(char* p, unsigned s)
            : ptr(p)
            , size(s)
        {
        }

        char* ptr { nullptr };
        unsigned size { 0 };
    };
    ELFImage m_image;

    struct SortedSymbol {
        u32 address;
        StringView name;
    };
#ifdef KERNEL
    mutable OwnPtr<Region> m_sorted_symbols_region;
#else
    mutable Vector<SortedSymbol> m_sorted_symbols;
#endif
};
