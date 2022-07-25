/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Fetch/Infrastructure/HTTP/Bodies.h>

namespace Web::Fetch::Infrastructure {

Body::Body(ReadableStreamDummy stream)
    : m_stream(stream)
{
}

Body::Body(ReadableStreamDummy stream, SourceType source, Optional<u64> length)
    : m_stream(stream)
    , m_source(move(source))
    , m_length(move(length))
{
}

}
