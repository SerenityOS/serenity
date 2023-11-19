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
    JS_DECLARE_ALLOCATOR(Segments);

public:
    static NonnullGCPtr<Segments> create(Realm&, Segmenter&, Utf16String);

    virtual ~Segments() override = default;

    Segmenter& segments_segmenter() const { return m_segments_segmenter; }

    Utf16View segments_string() const { return m_segments_string.view(); }

private:
    Segments(Realm&, Segmenter&, Utf16String);

    virtual void visit_edges(Cell::Visitor&) override;

    NonnullGCPtr<Segmenter> m_segments_segmenter; // [[SegmentsSegmenter]]
    Utf16String m_segments_string;                // [[SegmentsString]]
};

}
