/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AggregateError.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

AggregateError* AggregateError::create(Realm& realm)
{
    return realm.heap().allocate<AggregateError>(realm, *realm.global_object().aggregate_error_prototype());
}

AggregateError::AggregateError(Object& prototype)
    : Error(prototype)
{
}

}
