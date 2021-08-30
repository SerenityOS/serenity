/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <Kernel/KBuffer.h>
#include <stdarg.h>

namespace Kernel {

class KBufferBuilder {
public:
    using OutputType = KBuffer;

    KBufferBuilder();
    KBufferBuilder(KBufferBuilder&&) = default;
    ~KBufferBuilder() = default;

    void append(const StringView&);
    void append(char);
    void append(const char*, int);

    void append_escaped_for_json(const StringView&);
    void append_bytes(ReadonlyBytes);

    template<typename... Parameters>
    void appendff(CheckedFormatString<Parameters...>&& fmtstr, const Parameters&... parameters)
    {
        // FIXME: This really not ideal, but vformat expects StringBuilder.
        StringBuilder builder;
        AK::VariadicFormatParams variadic_format_params { parameters... };
        vformat(builder, fmtstr.view(), variadic_format_params);
        append_bytes(builder.string_view().bytes());
    }

    bool flush();
    OwnPtr<KBuffer> build();

private:
    bool check_expand(size_t);
    u8* insertion_ptr()
    {
        if (!m_buffer)
            return nullptr;
        return m_buffer->data() + m_size;
    }

    RefPtr<KBufferImpl> m_buffer;
    size_t m_size { 0 };
};

}
