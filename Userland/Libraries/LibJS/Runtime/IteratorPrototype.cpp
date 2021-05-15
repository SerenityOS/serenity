/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorPrototype.h>

namespace JS {

IteratorPrototype::IteratorPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void IteratorPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);
    define_native_function(global_object.vm().well_known_symbol_iterator(), symbol_iterator, 0, Attribute::Writable | Attribute::Enumerable);
}

IteratorPrototype::~IteratorPrototype()
{
}

JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::symbol_iterator)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    return this_object;
}

}
