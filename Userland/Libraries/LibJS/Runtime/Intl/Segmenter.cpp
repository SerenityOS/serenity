/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Intl/Segmenter.h>

namespace JS::Intl {

// 18 Segmenter Objects, https://tc39.es/ecma402/#segmenter-objects
Segmenter::Segmenter(Object& prototype)
    : Object(prototype)
{
}

void Segmenter::set_segmenter_granularity(StringView segmenter_granularity)
{
    if (segmenter_granularity == "grapheme"sv)
        m_segmenter_granularity = SegmenterGranularity::Grapheme;
    else if (segmenter_granularity == "word"sv)
        m_segmenter_granularity = SegmenterGranularity::Word;
    else if (segmenter_granularity == "sentence"sv)
        m_segmenter_granularity = SegmenterGranularity::Sentence;
    else
        VERIFY_NOT_REACHED();
}

StringView Segmenter::segmenter_granularity_string() const
{
    switch (m_segmenter_granularity) {
    case SegmenterGranularity::Grapheme:
        return "grapheme"sv;
    case SegmenterGranularity::Word:
        return "word"sv;
    case SegmenterGranularity::Sentence:
        return "sentence"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

}
