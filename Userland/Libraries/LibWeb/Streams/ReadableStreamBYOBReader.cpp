/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/PromiseCapability.h>
#include <LibWeb/Streams/ReadableStream.h>
#include <LibWeb/Streams/ReadableStreamBYOBReader.h>

namespace Web::Streams {

ReadableStreamBYOBReader::ReadableStreamBYOBReader(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
    , ReadableStreamGenericReaderMixin(realm)
{
}

void ReadableStreamBYOBReader::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    ReadableStreamGenericReaderMixin::visit_edges(visitor);
}

}
