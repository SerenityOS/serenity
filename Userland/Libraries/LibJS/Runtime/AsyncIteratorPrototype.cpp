/*
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AsyncIteratorPrototype.h>

namespace JS {

AsyncIteratorPrototype::AsyncIteratorPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void AsyncIteratorPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Enumerable;
    define_native_function(*vm.well_known_symbol_async_iterator(), symbol_async_iterator, 0, attr);
}

// 27.1.3.1 %AsyncIteratorPrototype% [ @@asyncIterator ] ( ), https://tc39.es/ecma262/#sec-asynciteratorprototype-asynciterator
JS_DEFINE_NATIVE_FUNCTION(AsyncIteratorPrototype::symbol_async_iterator)
{
    // 1. Return the this value.
    return vm.this_value(global_object);
}

}
