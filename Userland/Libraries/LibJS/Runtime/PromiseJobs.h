/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/JobCallback.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Promise.h>

namespace JS {

struct PromiseJob {
    Function<ThrowCompletionOr<Value>()> job;
    Realm* realm { nullptr };
};

// NOTE: These return a PromiseJob to prevent awkward casting at call sites.
PromiseJob create_promise_reaction_job(GlobalObject&, PromiseReaction&, Value argument);
PromiseJob create_promise_resolve_thenable_job(GlobalObject&, Promise&, Value thenable, JobCallback then);

}
