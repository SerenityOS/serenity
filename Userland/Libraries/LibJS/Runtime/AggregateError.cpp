/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AggregateError.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

AggregateError* AggregateError::create(GlobalObject& global_object)
{
    return global_object.heap().allocate<AggregateError>(global_object, *global_object.aggregate_error_prototype());
}

AggregateError::AggregateError(Object& prototype)
    : Error(prototype)
{
}

}
