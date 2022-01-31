/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/SegmentIterator.h>
#include <LibJS/Runtime/Intl/Segments.h>
#include <LibJS/Runtime/Intl/SegmentsPrototype.h>

namespace JS::Intl {

// 18.5.2 The %SegmentsPrototype% Object, https://tc39.es/ecma402/#sec-%segmentsprototype%-object
SegmentsPrototype::SegmentsPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void SegmentsPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(*vm.well_known_symbol_iterator(), symbol_iterator, 0, attr);
}

// 18.5.2.2 %SegmentsPrototype% [ @@iterator ] ( ), https://tc39.es/ecma402/#sec-%segmentsprototype%-@@iterator
JS_DEFINE_NATIVE_FUNCTION(SegmentsPrototype::symbol_iterator)
{
    // 1. Let segments be the this value.
    // 2. Perform ? RequireInternalSlot(segments, [[SegmentsSegmenter]]).
    auto* segments = TRY(typed_this_object(global_object));

    // 3. Let segmenter be segments.[[SegmentsSegmenter]].
    auto& segmenter = segments->segments_segmenter();

    // 4. Let string be segments.[[SegmentsString]].
    auto& string = segments->segments_string();

    // 5. Return ! CreateSegmentIterator(segmenter, string).
    return SegmentIterator::create(global_object, segmenter, string);
}

}
