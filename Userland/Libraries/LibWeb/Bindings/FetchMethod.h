/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>

namespace Web::Bindings {

JS::ThrowCompletionOr<JS::Value> fetch(JS::VM&);

}
