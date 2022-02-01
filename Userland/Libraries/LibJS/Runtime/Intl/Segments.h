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

class Segments final : public Object {
    JS_OBJECT(Segments, Object);

public:
    static Segments* create(GlobalObject&, Segmenter&, Utf16String);

    Segments(GlobalObject&, Segmenter&, Utf16String);
    virtual ~Segments() override = default;

    Segmenter& segments_segmenter() const { return m_segments_segmenter; }

    Utf16View segments_string() const { return m_segments_string.view(); }

    Optional<Vector<size_t>>& boundaries_cache() const { return m_boundaries_cache; }

private:
    virtual void visit_edges(Cell::Visitor&) override;

    Segmenter& m_segments_segmenter; // [[SegmentsSegmenter]]
    Utf16String m_segments_string;   // [[SegmentsString]]

    mutable Optional<Vector<size_t>> m_boundaries_cache;
};

}
