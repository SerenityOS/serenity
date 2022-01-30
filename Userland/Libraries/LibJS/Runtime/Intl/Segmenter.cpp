/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BinarySearch.h>
#include <AK/Utf16View.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/Segmenter.h>
#include <LibUnicode/CharacterTypes.h>

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

// 18.7.1 CreateSegmentDataObject ( segmenter, string, startIndex, endIndex ), https://tc39.es/ecma402/#sec-createsegmentdataobject
Object* create_segment_data_object(GlobalObject& global_object, Segmenter const& segmenter, Utf16View const& string, double start_index, double end_index)
{
    auto& vm = global_object.vm();

    // 1. Let len be the length of string.
    auto length = string.length_in_code_units();

    // 2. Assert: startIndex ≥ 0.
    VERIFY(start_index >= 0);

    // 3. Assert: endIndex ≤ len.
    VERIFY(end_index <= length);

    // 4. Assert: startIndex < endIndex.
    VERIFY(start_index < end_index);

    // 5. Let result be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* result = Object::create(global_object, global_object.object_prototype());

    // 6. Let segment be the substring of string from startIndex to endIndex.
    auto segment = string.substring_view(start_index, end_index - start_index);

    // 7. Perform ! CreateDataPropertyOrThrow(result, "segment", segment).
    MUST(result->create_data_property_or_throw(vm.names.segment, js_string(vm, segment)));

    // 8. Perform ! CreateDataPropertyOrThrow(result, "index", 𝔽(startIndex)).
    MUST(result->create_data_property_or_throw(vm.names.index, Value(start_index)));

    // 9. Perform ! CreateDataPropertyOrThrow(result, "input", string).
    MUST(result->create_data_property_or_throw(vm.names.input, js_string(vm, string)));

    // 10. Let granularity be segmenter.[[SegmenterGranularity]].
    auto granularity = segmenter.segmenter_granularity();

    // 11. If granularity is "word", then
    if (granularity == Segmenter::SegmenterGranularity::Word) {
        // a. Let isWordLike be a Boolean value indicating whether the segment in string is "word-like" according to locale segmenter.[[Locale]].
        // TODO

        // b. Perform ! CreateDataPropertyOrThrow(result, "isWordLike", isWordLike).
        MUST(result->create_data_property_or_throw(vm.names.isWordLike, Value(false)));
    }

    // 12. Return result.
    return result;
}

// 18.8.1 FindBoundary ( segmenter, string, startIndex, direction ), https://tc39.es/ecma402/#sec-findboundary
double find_boundary(Segmenter const& segmenter, Utf16View const& string, double start_index, Direction direction, Optional<Vector<size_t>>& boundaries_cache)
{
    // 1. Let locale be segmenter.[[Locale]].
    auto const& locale = segmenter.locale();

    // 2. Let granularity be segmenter.[[SegmenterGranularity]].
    auto granularity = segmenter.segmenter_granularity();

    // 3. Let len be the length of string.
    auto length = string.length_in_code_units();

    // Non-standard, populate boundaries cache
    if (!boundaries_cache.has_value()) {
        switch (granularity) {
        case Segmenter::SegmenterGranularity::Grapheme:
            boundaries_cache = Unicode::find_grapheme_segmentation_boundaries(string);
            break;
        case Segmenter::SegmenterGranularity::Word:
            boundaries_cache = Unicode::find_word_segmentation_boundaries(string);
            break;
        case Segmenter::SegmenterGranularity::Sentence:
            boundaries_cache = Unicode::find_sentence_segmentation_boundaries(string);
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }
    (void)locale; // TODO: Support locale-sensitive boundaries

    // 4. If direction is before, then
    if (direction == Direction::Before) {
        // a. Assert: startIndex ≥ 0.
        VERIFY(start_index >= 0);
        // b. Assert: startIndex < len.
        VERIFY(start_index < length);

        // c. Search string for the last segmentation boundary that is preceded by at most startIndex code units from the beginning, using locale locale and text element granularity granularity.
        size_t boundary_index;
        binary_search(*boundaries_cache, start_index, &boundary_index);

        // d. If a boundary is found, return the count of code units in string preceding it.
        if (boundary_index < boundaries_cache->size())
            return boundaries_cache->at(boundary_index);

        // e. Return 0.
        return 0;
    }

    // 5. Assert: direction is after.
    VERIFY(direction == Direction::After);

    // 6. If len is 0 or startIndex ≥ len, return +∞.
    if (length == 0 || start_index >= length)
        return INFINITY;

    // 7. Search string for the first segmentation boundary that follows the code unit at index startIndex, using locale locale and text element granularity granularity.
    size_t boundary_index;
    binary_search(*boundaries_cache, start_index, &boundary_index);
    ++boundary_index;

    // 8. If a boundary is found, return the count of code units in string preceding it.
    if (boundary_index < boundaries_cache->size())
        return boundaries_cache->at(boundary_index);

    // 9. Return len.
    return length;
}

}
