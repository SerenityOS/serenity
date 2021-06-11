/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AggregateError.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

AggregateError* AggregateError::create(GlobalObject& global_object, String const& message, Vector<Value> const& errors)
{
    auto& vm = global_object.vm();
    auto* error = global_object.heap().allocate<AggregateError>(global_object, *global_object.aggregate_error_prototype());
    u8 attr = Attribute::Writable | Attribute::Configurable;
    if (!message.is_null())
        error->define_property(vm.names.message, js_string(vm, message), attr);
    error->define_property(vm.names.errors, Array::create_from(global_object, errors), attr);
    return error;
}

AggregateError::AggregateError(Object& prototype)
    : Object(prototype)
{
}

}
