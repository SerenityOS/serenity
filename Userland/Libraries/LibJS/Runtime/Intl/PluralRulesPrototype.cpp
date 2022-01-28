/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/PluralRulesPrototype.h>

namespace JS::Intl {

// 16.4 Properties of the Intl.PluralRules Prototype Object, https://tc39.es/ecma402/#sec-properties-of-intl-pluralrules-prototype-object
PluralRulesPrototype::PluralRulesPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void PluralRulesPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 16.4.2 Intl.PluralRules.prototype [ @@toStringTag ], https://tc39.es/ecma402/#sec-intl.pluralrules.prototype-tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.PluralRules"sv), Attribute::Configurable);
}

}
