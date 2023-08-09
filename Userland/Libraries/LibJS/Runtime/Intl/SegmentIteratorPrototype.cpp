/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf16View.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/SegmentIteratorPrototype.h>
#include <LibJS/Runtime/Intl/Segments.h>
#include <LibJS/Runtime/Iterator.h>

namespace JS::Intl {

// 18.6.2 The %SegmentIteratorPrototype% Object, https://tc39.es/ecma402/#sec-%segmentiteratorprototype%-object
SegmentIteratorPrototype::SegmentIteratorPrototype(Realm& realm)
    : PrototypeObject(realm.intrinsics().iterator_prototype())
{
}

void SegmentIteratorPrototype::initialize(Realm& realm)
{
    Base::initialize(realm);

    auto& vm = this->vm();

    // 18.6.2.2 %SegmentIteratorPrototype% [ @@toStringTag ], https://tc39.es/ecma402/#sec-%segmentiteratorprototype%.@@tostringtag
    define_direct_property(vm.well_known_symbol_to_string_tag(), PrimitiveString::create(vm, "Segmenter String Iterator"_string), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(realm, vm.names.next, next, 0, attr);
}

// 18.6.2.1 %SegmentIteratorPrototype%.next ( ), https://tc39.es/ecma402/#sec-%segmentiteratorprototype%.next
JS_DEFINE_NATIVE_FUNCTION(SegmentIteratorPrototype::next)
{
    // 1. Let iterator be the this value.
    // 2. Perform ? RequireInternalSlot(iterator, [[IteratingSegmenter]]).
    auto iterator = TRY(typed_this_object(vm));

    // 3. Let segmenter be iterator.[[IteratingSegmenter]].
    auto const& segmenter = iterator->iterating_segmenter();

    // 4. Let string be iterator.[[IteratedString]].
    auto const& string = iterator->iterated_string();

    // 5. Let startIndex be iterator.[[IteratedStringNextSegmentCodeUnitIndex]].
    auto start_index = iterator->iterated_string_next_segment_code_unit_index();

    // 6. Let endIndex be ! FindBoundary(segmenter, string, startIndex, after).
    auto end_index = find_boundary(segmenter, string, start_index, Direction::After);

    // 7. If endIndex is not finite, then
    if (!Value(end_index).is_finite_number()) {
        // a. Return CreateIterResultObject(undefined, true).
        return create_iterator_result_object(vm, js_undefined(), true);
    }

    // 8. Set iterator.[[IteratedStringNextSegmentCodeUnitIndex]] to endIndex.
    iterator->set_iterated_string_next_segment_code_unit_index(end_index);

    // 9. Let segmentData be ! CreateSegmentDataObject(segmenter, string, startIndex, endIndex).
    auto segment_data = TRY(create_segment_data_object(vm, segmenter, string, start_index, end_index));

    // 10. Return CreateIterResultObject(segmentData, false).
    return create_iterator_result_object(vm, segment_data, false);
}

}
