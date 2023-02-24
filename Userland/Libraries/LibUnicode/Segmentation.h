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
#include <AK/Optional.h>
#include <AK/Types.h>

namespace Unicode {

using SegmentationCallback = Function<IterationDecision(size_t)>;

void for_each_grapheme_segmentation_boundary(Utf8View const&, SegmentationCallback);
void for_each_grapheme_segmentation_boundary(Utf16View const&, SegmentationCallback);
void for_each_grapheme_segmentation_boundary(Utf32View const&, SegmentationCallback);

template<typename ViewType>
Optional<size_t> next_grapheme_segmentation_boundary(ViewType const& view, size_t index)
{
    Optional<size_t> result;

    for_each_grapheme_segmentation_boundary(view, [&](auto boundary) {
        if (boundary > index) {
            result = boundary;
            return IterationDecision::Break;
        }

        return IterationDecision::Continue;
    });

    return result;
}

template<typename ViewType>
Optional<size_t> previous_grapheme_segmentation_boundary(ViewType const& view, size_t index)
{
    Optional<size_t> result;

    for_each_grapheme_segmentation_boundary(view, [&](auto boundary) {
        if (boundary < index) {
            result = boundary;
            return IterationDecision::Continue;
        }

        return IterationDecision::Break;
    });

    return result;
}

void for_each_word_segmentation_boundary(Utf8View const&, SegmentationCallback);
void for_each_word_segmentation_boundary(Utf16View const&, SegmentationCallback);
void for_each_word_segmentation_boundary(Utf32View const&, SegmentationCallback);

template<typename ViewType>
Optional<size_t> next_word_segmentation_boundary(ViewType const& view, size_t index)
{
    Optional<size_t> result;

    for_each_word_segmentation_boundary(view, [&](auto boundary) {
        if (boundary > index) {
            result = boundary;
            return IterationDecision::Break;
        }

        return IterationDecision::Continue;
    });

    return result;
}

template<typename ViewType>
Optional<size_t> previous_word_segmentation_boundary(ViewType const& view, size_t index)
{
    Optional<size_t> result;

    for_each_word_segmentation_boundary(view, [&](auto boundary) {
        if (boundary < index) {
            result = boundary;
            return IterationDecision::Continue;
        }

        return IterationDecision::Break;
    });

    return result;
}

void for_each_sentence_segmentation_boundary(Utf8View const&, SegmentationCallback);
void for_each_sentence_segmentation_boundary(Utf16View const&, SegmentationCallback);
void for_each_sentence_segmentation_boundary(Utf32View const&, SegmentationCallback);

template<typename ViewType>
Optional<size_t> next_sentence_segmentation_boundary(ViewType const& view, size_t index)
{
    Optional<size_t> result;

    for_each_sentence_segmentation_boundary(view, [&](auto boundary) {
        if (boundary > index) {
            result = boundary;
            return IterationDecision::Break;
        }

        return IterationDecision::Continue;
    });

    return result;
}

template<typename ViewType>
Optional<size_t> previous_sentence_segmentation_boundary(ViewType const& view, size_t index)
{
    Optional<size_t> result;

    for_each_sentence_segmentation_boundary(view, [&](auto boundary) {
        if (boundary < index) {
            result = boundary;
            return IterationDecision::Continue;
        }

        return IterationDecision::Break;
    });

    return result;
}

}
