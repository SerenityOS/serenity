/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <Kernel/KBuffer.h>
#include <stdarg.h>

namespace Kernel {

class KBufferBuilder {
    AK_MAKE_NONCOPYABLE(KBufferBuilder);

public:
    using OutputType = KBuffer;

    static ErrorOr<KBufferBuilder> try_create();

    KBufferBuilder(KBufferBuilder&&) = default;
    KBufferBuilder& operator=(KBufferBuilder&&) = default;
    ~KBufferBuilder() = default;

    ErrorOr<void> append(StringView);
    ErrorOr<void> append(char);
    ErrorOr<void> append(const char*, int);

    ErrorOr<void> append_escaped_for_json(StringView);
    ErrorOr<void> append_bytes(ReadonlyBytes);

    template<typename... Parameters>
    ErrorOr<void> appendff(CheckedFormatString<Parameters...>&& fmtstr, const Parameters&... parameters)
    {
        // FIXME: This really not ideal, but vformat expects StringBuilder.
        StringBuilder builder;
        AK::VariadicFormatParams variadic_format_params { parameters... };
        TRY(vformat(builder, fmtstr.view(), variadic_format_params));
        return append_bytes(builder.string_view().bytes());
    }

    bool flush();
    OwnPtr<KBuffer> build();

    ReadonlyBytes bytes() const
    {
        if (!m_buffer)
            return {};
        return m_buffer->bytes();
    }

private:
    explicit KBufferBuilder(NonnullOwnPtr<KBuffer>);

    bool check_expand(size_t);
    u8* insertion_ptr()
    {
        if (!m_buffer)
            return nullptr;
        return m_buffer->data() + m_size;
    }

    OwnPtr<KBuffer> m_buffer;
    size_t m_size { 0 };
};

}
