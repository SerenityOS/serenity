/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/Forward.h>

namespace Web::Bindings::IDL {

bool is_an_array_index(JS::GlobalObject&, JS::PropertyKey const&);
Optional<ByteBuffer> get_buffer_source_copy(JS::Object const& buffer_source);

}
