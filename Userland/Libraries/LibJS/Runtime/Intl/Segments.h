/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibJS/Runtime/Intl/Segmenter.h>
#include <LibJS/Runtime/Object.h>

namespace JS::Intl {

class Segments final : public Object {
    JS_OBJECT(Segments, Object);

public:
    static Segments* create(GlobalObject&, Segmenter&, String);

    Segments(GlobalObject&, Segmenter&, String);
    virtual ~Segments() override = default;

    Segmenter& segments_segmenter() const { return m_segments_segmenter; }

    String const& segments_string() const { return m_segments_string; }

private:
    virtual void visit_edges(Cell::Visitor&) override;

    Segmenter& m_segments_segmenter; // [[SegmentsSegmenter]]
    String m_segments_string;        // [[SegmentsString]]
};

}
