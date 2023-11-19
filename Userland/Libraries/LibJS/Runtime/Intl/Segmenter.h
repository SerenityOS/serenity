/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibJS/Runtime/Object.h>

namespace JS::Intl {

class Segmenter final : public Object {
    JS_OBJECT(Segmenter, Object);
    JS_DECLARE_ALLOCATOR(Segmenter);

public:
    enum class SegmenterGranularity {
        Grapheme,
        Word,
        Sentence,
    };

    virtual ~Segmenter() override = default;

    String const& locale() const { return m_locale; }
    void set_locale(String locale) { m_locale = move(locale); }

    SegmenterGranularity segmenter_granularity() const { return m_segmenter_granularity; }
    void set_segmenter_granularity(StringView);
    StringView segmenter_granularity_string() const;

private:
    explicit Segmenter(Object& prototype);

    String m_locale;                                                                 // [[Locale]]
    SegmenterGranularity m_segmenter_granularity { SegmenterGranularity::Grapheme }; // [[SegmenterGranularity]]
};

ThrowCompletionOr<NonnullGCPtr<Object>> create_segment_data_object(VM&, Segmenter const&, Utf16View const&, double start_index, double end_index);

enum class Direction {
    Before,
    After,
};
double find_boundary(Segmenter const&, Utf16View const&, double start_index, Direction);

}
