/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/Segmenter.h>
#include <LibJS/Runtime/Intl/SegmenterPrototype.h>

namespace JS::Intl {

// 18.3 Properties of the Intl.Segmenter Prototype Object, https://tc39.es/ecma402/#sec-properties-of-intl-segmenter-prototype-object
SegmenterPrototype::SegmenterPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void SegmenterPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 18.3.2 Intl.Segmenter.prototype [ @@toStringTag ], https://tc39.es/ecma402/#sec-intl.segmenter.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.Segmenter"), Attribute::Configurable);
}

}
