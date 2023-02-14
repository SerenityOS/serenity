/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Function.h>
#include <AK/IterationDecision.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace Unicode {

using SegmentationCallback = Function<IterationDecision(size_t)>;

void for_each_grapheme_segmentation_boundary(Utf8View const&, SegmentationCallback);
void for_each_grapheme_segmentation_boundary(Utf16View const&, SegmentationCallback);
void for_each_grapheme_segmentation_boundary(Utf32View const&, SegmentationCallback);

template<typename ViewType>
Vector<size_t> find_grapheme_segmentation_boundaries(ViewType const& view)
{
    Vector<size_t> boundaries;

    for_each_grapheme_segmentation_boundary(view, [&](auto boundary) {
        boundaries.append(boundary);
        return IterationDecision::Continue;
    });

    return boundaries;
}

void for_each_word_segmentation_boundary(Utf8View const&, SegmentationCallback);
void for_each_word_segmentation_boundary(Utf16View const&, SegmentationCallback);
void for_each_word_segmentation_boundary(Utf32View const&, SegmentationCallback);

template<typename ViewType>
Vector<size_t> find_word_segmentation_boundaries(ViewType const& view)
{
    Vector<size_t> boundaries;

    for_each_word_segmentation_boundary(view, [&](auto boundary) {
        boundaries.append(boundary);
        return IterationDecision::Continue;
    });

    return boundaries;
}

void for_each_sentence_segmentation_boundary(Utf8View const&, SegmentationCallback);
void for_each_sentence_segmentation_boundary(Utf16View const&, SegmentationCallback);
void for_each_sentence_segmentation_boundary(Utf32View const&, SegmentationCallback);

template<typename ViewType>
Vector<size_t> find_sentence_segmentation_boundaries(ViewType const& view)
{
    Vector<size_t> boundaries;

    for_each_sentence_segmentation_boundary(view, [&](auto boundary) {
        boundaries.append(boundary);
        return IterationDecision::Continue;
    });

    return boundaries;
}

}
