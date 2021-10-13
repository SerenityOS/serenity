/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Realm.h>

namespace JS {

ThrowCompletionOr<Value> get_wrapped_value(GlobalObject&, Realm& caller_realm, Value);

}
