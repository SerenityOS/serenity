/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/SegmentIteratorPrototype.h>
#include <LibJS/Runtime/Intl/Segments.h>

namespace JS::Intl {

// 18.6.2 The %SegmentIteratorPrototype% Object, https://tc39.es/ecma402/#sec-%segmentiteratorprototype%-object
SegmentIteratorPrototype::SegmentIteratorPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.iterator_prototype())
{
}

void SegmentIteratorPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 18.6.2.2 %SegmentIteratorPrototype% [ @@toStringTag ], https://tc39.es/ecma402/#sec-%segmentiteratorprototype%.@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), "Segmenter String Iterator"), Attribute::Configurable);
}

}
