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
#include <AK/NonnullRefPtr.h>
#include <AK/OwnPtr.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibELF/Image.h>

#ifdef KERNEL
#    include <Kernel/VirtualAddress.h>
namespace Kernel {
class Region;
}
#endif

namespace ELF {

class Loader : public RefCounted<Loader> {
public:
    static NonnullRefPtr<Loader> create(const u8* data, size_t size, bool verbose_logging = true) { return adopt(*new Loader(data, size, verbose_logging)); }
    ~Loader();

    bool load();
#if defined(KERNEL)
    Function<void*(VirtualAddress, size_t, size_t, bool, bool, const String&)> alloc_section_hook;
    Function<void*(size_t, size_t)> tls_section_hook;
    Function<void*(VirtualAddress, size_t, size_t, size_t, bool r, bool w, bool x, const String&)> map_section_hook;
#endif
    VirtualAddress entry() const
    {
        return m_image.entry();
    }
    const Image& image() const { return m_image; }
    Optional<Image::Symbol> find_demangled_function(const String& name) const;

    bool has_symbols() const { return m_symbol_count; }

    String symbolicate(u32 address, u32* offset = nullptr) const;
    Optional<Image::Symbol> find_symbol(u32 address, u32* offset = nullptr) const;

private:
    explicit Loader(const u8*, size_t, bool verbose_logging);

    bool layout();

    Image m_image;

    size_t m_symbol_count { 0 };

    struct SortedSymbol {
        u32 address;
        StringView name;
#ifndef KERNEL
        String demangled_name;
        Optional<Image::Symbol> symbol;
#endif
    };
#ifdef KERNEL
    mutable OwnPtr<Kernel::Region> m_sorted_symbols_region;
#else
    mutable Vector<SortedSymbol> m_sorted_symbols;
#endif
};

} // end namespace ELF
