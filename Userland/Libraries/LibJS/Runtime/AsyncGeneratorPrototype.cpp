/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AsyncGeneratorPrototype.h>

namespace JS {

// 27.6.1 Properties of the AsyncGenerator Prototype Object, https://tc39.es/ecma262/#sec-properties-of-asyncgenerator-prototype
AsyncGeneratorPrototype::AsyncGeneratorPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.async_iterator_prototype())
{
}

void AsyncGeneratorPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    // 27.6.1.5 AsyncGenerator.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-asyncgenerator-prototype-tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "AsyncGenerator"), Attribute::Configurable);
}

}
