/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Utf16View.h>
#include <LibJS/Runtime/Intl/Segmenter.h>
#include <LibJS/Runtime/Object.h>

namespace JS::Intl {

class SegmentIterator final : public Object {
    JS_OBJECT(SegmentIterator, Object);

public:
    static SegmentIterator* create(GlobalObject&, Segmenter&, Utf16View const&, Segments const&);

    SegmentIterator(GlobalObject&, Segmenter&, Utf16View const&, Segments const&);
    virtual ~SegmentIterator() override = default;

    Segmenter const& iterating_segmenter() const { return m_iterating_segmenter; }
    Utf16View const& iterated_string() const { return m_iterated_string; }
    size_t iterated_string_next_segment_code_unit_index() const { return m_iterated_string_next_segment_code_unit_index; }
    void set_iterated_string_next_segment_code_unit_index(size_t index) { m_iterated_string_next_segment_code_unit_index = index; }

    Segments const& segments() { return m_segments; }

private:
    virtual void visit_edges(Cell::Visitor&) override;

    Segmenter& m_iterating_segmenter;                            // [[IteratingSegmenter]]
    Utf16View m_iterated_string;                                 // [[IteratedString]]
    size_t m_iterated_string_next_segment_code_unit_index { 0 }; // [[IteratedStringNextSegmentCodeUnitIndex]]

    Segments const& m_segments;
};

}
