/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Fetch/Infrastructure/HTTP/Bodies.h>

namespace Web::Fetch::Infrastructure {

Body::Body(JS::Handle<Streams::ReadableStream> stream)
    : m_stream(move(stream))
{
}

Body::Body(JS::Handle<Streams::ReadableStream> stream, SourceType source, Optional<u64> length)
    : m_stream(move(stream))
    , m_source(move(source))
    , m_length(move(length))
{
}

}
