/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Assertions.h>
#include <AK/Checked.h>
#include <AK/Types.h>

namespace AK {

class Utf32View {
public:
    Utf32View() { }
    Utf32View(const u32* code_points, size_t length)
        : m_code_points(code_points)
        , m_length(length)
    {
        ASSERT(code_points || length == 0);
    }

    const u32* code_points() const { return m_code_points; }
    bool is_empty() const { return m_length == 0; }
    size_t length() const { return m_length; }

    Utf32View substring_view(size_t offset, size_t length) const
    {
        if (length == 0)
            return {};
        ASSERT(offset < m_length);
        ASSERT(!Checked<size_t>::addition_would_overflow(offset, length));
        ASSERT((offset + length) <= m_length);
        return Utf32View(m_code_points + offset, length);
    }

private:
    const u32* m_code_points { nullptr };
    size_t m_length { 0 };
};

}

using AK::Utf32View;
