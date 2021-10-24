/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>

namespace Web::Bindings::IDL {

bool is_an_array_index(JS::GlobalObject&, JS::PropertyKey const&);

}
