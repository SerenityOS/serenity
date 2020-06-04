/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
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

#include <AK/ByteBuffer.h>
#include <AK/OwnPtr.h>
#include <AK/Result.h>
#include <AK/StringView.h>

namespace Gfx {

class TTFont;

enum class TTFIndexToLocFormat {
    Offset16,
    Offset32,
};

class TTFHead {
private:
    TTFHead() {}
    TTFHead(ByteBuffer&& slice)
        : m_slice(move(slice))
    {
        ASSERT(m_slice.size() >= 54);
        dbg() << "HEAD:"
              << "\n  units_per_em: " << units_per_em()
              << "\n  xmin: " << xmin()
              << "\n  ymin: " << ymin()
              << "\n  xmax: " << xmax()
              << "\n  ymax: " << ymax()
              << "\n  lowest_recommended_ppem: " << lowest_recommended_ppem();
    }
    u16 units_per_em() const;
    i16 xmin() const;
    i16 ymin() const;
    i16 xmax() const;
    i16 ymax() const;
    u16 lowest_recommended_ppem() const;
    Result<TTFIndexToLocFormat, i16> index_to_loc_format() const;

    ByteBuffer m_slice;
    bool m_is_init;

    friend TTFont;
};

class TTFont {
public:
    static OwnPtr<TTFont> load_from_file(const StringView& path, unsigned index);

private:
    TTFont(AK::ByteBuffer&& buffer, u32 offset);

    AK::ByteBuffer m_buffer;
    TTFHead m_head;
};

}
