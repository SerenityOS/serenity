/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Streams/AbstractOperations.h>
#include <LibWeb/Streams/ReadableStream.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#is-readable-stream-locked
bool is_readable_stream_locked(ReadableStream const& stream)
{
    // 1. If stream.[[reader]] is undefined, return false.
    if (stream.reader() == nullptr)
        return false;

    // 2. Return true.
    return true;
}

}
