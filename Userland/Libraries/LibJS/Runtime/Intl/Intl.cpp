/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DisplayNamesConstructor.h>
#include <LibJS/Runtime/Intl/Intl.h>

namespace JS::Intl {

// 8 The Intl Object, https://tc39.es/ecma402/#intl-object
Intl::Intl(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void Intl::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 8.1.1 Intl[ @@toStringTag ], https://tc39.es/ecma402/#sec-Intl-toStringTag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl"), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_direct_property(vm.names.DisplayNames, global_object.intl_display_names_constructor(), attr);
}

}
