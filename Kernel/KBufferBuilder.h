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

#include <AK/String.h>
#include <Kernel/KBuffer.h>
#include <stdarg.h>

namespace Kernel {

class KBufferBuilder {
public:
    using OutputType = KBuffer;

    explicit KBufferBuilder();
    ~KBufferBuilder() { }

    void append(const StringView&);
    void append(char);
    void append(const char*, int);
    void appendf(const char*, ...);
    void appendvf(const char*, va_list);

    void append_escaped_for_json(const StringView&);

    template<typename... Parameters>
    void appendff(StringView fmtstr, const Parameters&... parameters)
    {
        // FIXME: This is really not the way to go about it, but vformat expects a
        //        StringBuilder. Why does this class exist anyways?
        append(String::formatted(fmtstr, parameters...));
    }

    KBuffer build();

private:
    bool can_append(size_t) const;
    u8* insertion_ptr() { return m_buffer.data() + m_size; }

    KBuffer m_buffer;
    size_t m_size { 0 };
};

}
