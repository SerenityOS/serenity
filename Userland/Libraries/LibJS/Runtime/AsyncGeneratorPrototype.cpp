/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AsyncGeneratorPrototype.h>

namespace JS {

// 27.6.1 Properties of the AsyncGenerator Prototype Object, https://tc39.es/ecma262/#sec-properties-of-asyncgenerator-prototype
AsyncGeneratorPrototype::AsyncGeneratorPrototype(Realm& realm)
    : PrototypeObject(*realm.intrinsics().async_iterator_prototype())
{
}

void AsyncGeneratorPrototype::initialize(Realm& realm)
{
    auto& vm = this->vm();
    Object::initialize(realm);

    // 27.6.1.5 AsyncGenerator.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-asyncgenerator-prototype-tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "AsyncGenerator"), Attribute::Configurable);
}

}
