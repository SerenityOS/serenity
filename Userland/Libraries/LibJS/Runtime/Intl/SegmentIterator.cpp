/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/SegmentIterator.h>
#include <LibJS/Runtime/Intl/Segments.h>

namespace JS::Intl {

// 18.6.1 CreateSegmentIterator ( segmenter, string ), https://tc39.es/ecma402/#sec-createsegmentsobject
NonnullGCPtr<SegmentIterator> SegmentIterator::create(Realm& realm, Segmenter& segmenter, Utf16View const& string, Segments const& segments)
{
    // 1. Let internalSlotsList be « [[IteratingSegmenter]], [[IteratedString]], [[IteratedStringNextSegmentCodeUnitIndex]] ».
    // 2. Let iterator be OrdinaryObjectCreate(%SegmentIteratorPrototype%, internalSlotsList).
    // 3. Set iterator.[[IteratingSegmenter]] to segmenter.
    // 4. Set iterator.[[IteratedString]] to string.
    // 5. Set iterator.[[IteratedStringNextSegmentCodeUnitIndex]] to 0.
    // 6. Return iterator.
    return realm.heap().allocate<SegmentIterator>(realm, realm, segmenter, move(string), segments);
}

// 18.6 Segment Iterator Objects, https://tc39.es/ecma402/#sec-segment-iterator-objects
SegmentIterator::SegmentIterator(Realm& realm, Segmenter& segmenter, Utf16View const& string, Segments const& segments)
    : Object(ConstructWithPrototypeTag::Tag, realm.intrinsics().intl_segment_iterator_prototype())
    , m_iterating_segmenter(segmenter)
    , m_iterated_string(string)
    , m_segments(segments)
{
}

void SegmentIterator::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_iterating_segmenter);
    visitor.visit(m_segments);
}

}
