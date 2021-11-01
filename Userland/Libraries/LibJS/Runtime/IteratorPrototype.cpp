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
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Enumerable;
    define_native_function(*vm.well_known_symbol_iterator(), symbol_iterator, 0, attr);
}

IteratorPrototype::~IteratorPrototype()
{
}

// 27.1.2.1 %IteratorPrototype% [ @@iterator ] ( ), https://tc39.es/ecma262/#sec-%iteratorprototype%-@@iterator
JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::symbol_iterator)
{
    return TRY(vm.this_value(global_object).to_object(global_object));
}

}
