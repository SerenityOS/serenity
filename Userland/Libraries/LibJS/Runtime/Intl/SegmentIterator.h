/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Intl/Segmenter.h>
#include <LibJS/Runtime/Object.h>

namespace JS::Intl {

class SegmentIterator final : public Object {
    JS_OBJECT(SegmentIterator, Object);

public:
    static SegmentIterator* create(GlobalObject&, Segmenter&, String);

    SegmentIterator(GlobalObject&, Segmenter&, String);
    virtual ~SegmentIterator() override = default;

    Segmenter const& iterating_segmenter() const { return m_iterating_segmenter; }
    String const& iterated_string() const { return m_iterated_string; }
    size_t iterated_string_next_segment_code_unit_index() const { return m_iterated_string_next_segment_code_unit_index; }

private:
    virtual void visit_edges(Cell::Visitor&) override;

    Segmenter& m_iterating_segmenter;                            // [[IteratingSegmenter]]
    String m_iterated_string;                                    // [[IteratedString]]
    size_t m_iterated_string_next_segment_code_unit_index { 0 }; // [[IteratedStringNextSegmentCodeUnitIndex]]
};

}
