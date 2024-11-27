/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf16View.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <LibUnicode/CharacterTypes.h>
#include <LibUnicode/Segmentation.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodeData.h>
#endif

namespace Unicode {

template<typename ViewType>
static size_t code_unit_length(ViewType const& view)
{
    if constexpr (IsSame<ViewType, Utf8View>)
        return view.byte_length();
    else if constexpr (IsSame<ViewType, Utf16View>)
        return view.length_in_code_units();
    else if constexpr (IsSame<ViewType, Utf32View>)
        return view.length();
    else
        static_assert(DependentFalse<ViewType>);
}

template<typename ViewType, typename CodeUnitIterator>
static size_t code_unit_offset_of(ViewType const& view, CodeUnitIterator const& it)
{
    if constexpr (IsSame<ViewType, Utf8View>)
        return view.byte_offset_of(it);
    else if constexpr (IsSame<ViewType, Utf16View>)
        return view.code_unit_offset_of(it);
    else if constexpr (IsSame<ViewType, Utf32View>)
        return view.iterator_offset(it);
    else
        static_assert(DependentFalse<ViewType>);
}

template<typename ViewType>
static void for_each_grapheme_segmentation_boundary_impl([[maybe_unused]] ViewType const& view, [[maybe_unused]] SegmentationCallback callback)
{
#if ENABLE_UNICODE_DATA
    using GBP = GraphemeBreakProperty;

    // https://www.unicode.org/reports/tr29/#Grapheme_Cluster_Boundary_Rules
    if (view.is_empty())
        return;

    auto has_any_gbp = [](u32 code_point, auto&&... properties) {
        return (code_point_has_grapheme_break_property(code_point, properties) || ...);
    };

    auto skip_incb_extend_linker_sequence = [&](auto& it) {
        while (true) {
            if (it == view.end() || !code_point_has_property(*it, Property::InCB_Extend))
                return;

            auto next_it = it;
            ++next_it;

            if (next_it == view.end() || !code_point_has_property(*next_it, Property::InCB_Linker))
                return;

            it = next_it;
            ++it;
        }
    };

    // GB1
    if (callback(0) == IterationDecision::Break)
        return;

    if (code_unit_length(view) > 1) {
        auto it = view.begin();
        auto code_point = *it;
        u32 next_code_point = 0;
        auto current_ri_chain = 0;

        for (++it; it != view.end(); ++it, code_point = next_code_point) {
            next_code_point = *it;

            // GB9c
            if (code_point_has_property(code_point, Property::InCB_Consonant)) {
                auto it_copy = it;
                skip_incb_extend_linker_sequence(it_copy);

                if (it_copy != view.end() && code_point_has_property(*it_copy, Property::InCB_Linker)) {
                    ++it_copy;
                    skip_incb_extend_linker_sequence(it_copy);

                    if (it_copy != view.end() && code_point_has_property(*it_copy, Property::InCB_Consonant)) {
                        next_code_point = *it_copy;
                        it = it_copy;
                        continue;
                    }
                }
            }

            // GB11
            if (code_point_has_property(code_point, Property::Extended_Pictographic) && has_any_gbp(next_code_point, GBP::Extend, GBP::ZWJ)) {
                auto it_copy = it;

                while (it_copy != view.end() && has_any_gbp(*it_copy, GBP::Extend))
                    ++it_copy;

                if (it_copy != view.end() && has_any_gbp(*it_copy, GBP::ZWJ)) {
                    ++it_copy;

                    if (it_copy != view.end() && code_point_has_property(*it_copy, Property::Extended_Pictographic)) {
                        next_code_point = *it_copy;
                        it = it_copy;
                        continue;
                    }
                }
            }

            auto code_point_is_cr = has_any_gbp(code_point, GBP::CR);
            auto next_code_point_is_lf = has_any_gbp(next_code_point, GBP::LF);

            // GB3
            if (code_point_is_cr && next_code_point_is_lf)
                continue;
            // GB4, GB5
            if (code_point_is_cr || next_code_point_is_lf || has_any_gbp(next_code_point, GBP::CR, GBP::Control) || has_any_gbp(code_point, GBP::LF, GBP::Control)) {
                if (callback(code_unit_offset_of(view, it)) == IterationDecision::Break)
                    return;
                continue;
            }

            auto next_code_point_is_v = has_any_gbp(next_code_point, GBP::V);
            auto next_code_point_is_t = has_any_gbp(next_code_point, GBP::T);

            // GB6
            if (has_any_gbp(code_point, GBP::L) && (next_code_point_is_v || has_any_gbp(next_code_point, GBP::L, GBP::LV, GBP::LVT)))
                continue;
            // GB7
            if ((next_code_point_is_v || next_code_point_is_t) && has_any_gbp(code_point, GBP::LV, GBP::V))
                continue;
            // GB8
            if (next_code_point_is_t && has_any_gbp(code_point, GBP::LVT, GBP::T))
                continue;

            // GB9
            if (has_any_gbp(next_code_point, GBP::Extend, GBP::ZWJ))
                continue;
            // GB9a
            if (has_any_gbp(next_code_point, GBP::SpacingMark))
                continue;
            // GB9b
            if (has_any_gbp(code_point, GBP::Prepend))
                continue;

            auto code_point_is_ri = has_any_gbp(code_point, GBP::Regional_Indicator);
            current_ri_chain = code_point_is_ri ? current_ri_chain + 1 : 0;

            // GB12, GB13
            if (code_point_is_ri && has_any_gbp(next_code_point, GBP::Regional_Indicator) && current_ri_chain % 2 == 1)
                continue;

            // GB999
            if (callback(code_unit_offset_of(view, it)) == IterationDecision::Break)
                return;
        }
    }

    // GB2
    callback(code_unit_length(view));
#endif
}

void for_each_grapheme_segmentation_boundary(Utf8View const& view, SegmentationCallback callback)
{
    for_each_grapheme_segmentation_boundary_impl(view, move(callback));
}

void for_each_grapheme_segmentation_boundary(Utf16View const& view, SegmentationCallback callback)
{
    for_each_grapheme_segmentation_boundary_impl(view, move(callback));
}

void for_each_grapheme_segmentation_boundary(Utf32View const& view, SegmentationCallback callback)
{
    for_each_grapheme_segmentation_boundary_impl(view, move(callback));
}

template<typename ViewType>
static void for_each_word_segmentation_boundary_impl([[maybe_unused]] ViewType const& view, [[maybe_unused]] SegmentationCallback callback)
{
#if ENABLE_UNICODE_DATA
    using WBP = WordBreakProperty;

    // https://www.unicode.org/reports/tr29/#Word_Boundary_Rules
    if (view.is_empty())
        return;

    auto has_any_wbp = [](u32 code_point, auto&&... properties) {
        return (code_point_has_word_break_property(code_point, properties) || ...);
    };

    // WB1
    if (callback(0) == IterationDecision::Break)
        return;

    if (code_unit_length(view) > 1) {
        auto it = view.begin();
        auto code_point = *it;
        u32 next_code_point;
        Optional<u32> previous_code_point;
        auto current_ri_chain = 0;

        for (++it; it != view.end(); ++it, previous_code_point = code_point, code_point = next_code_point) {
            next_code_point = *it;

            auto code_point_is_cr = has_any_wbp(code_point, WBP::CR);
            auto next_code_point_is_lf = has_any_wbp(next_code_point, WBP::LF);

            // WB3
            if (code_point_is_cr && next_code_point_is_lf)
                continue;
            // WB3a, WB3b
            if (code_point_is_cr || next_code_point_is_lf || has_any_wbp(next_code_point, WBP::CR, WBP::Newline) || has_any_wbp(code_point, WBP::LF, WBP::Newline)) {
                if (callback(code_unit_offset_of(view, it)) == IterationDecision::Break)
                    return;
                continue;
            }
            // WB3c
            if (has_any_wbp(code_point, WBP::ZWJ) && code_point_has_property(next_code_point, Property::Extended_Pictographic))
                continue;
            // WB3d
            if (has_any_wbp(code_point, WBP::WSegSpace) && has_any_wbp(next_code_point, WBP::WSegSpace))
                continue;

            // WB4
            if (has_any_wbp(next_code_point, WBP::Format, WBP::Extend, WBP::ZWJ))
                continue;

            auto code_point_is_hebrew_letter = has_any_wbp(code_point, WBP::Hebrew_Letter);
            auto code_point_is_ah_letter = code_point_is_hebrew_letter || has_any_wbp(code_point, WBP::ALetter);
            auto next_code_point_is_hebrew_letter = has_any_wbp(next_code_point, WBP::Hebrew_Letter);
            auto next_code_point_is_ah_letter = next_code_point_is_hebrew_letter || has_any_wbp(next_code_point, WBP::ALetter);

            // WB5
            if (code_point_is_ah_letter && next_code_point_is_ah_letter)
                continue;

            Optional<u32> next_next_code_point;
            if (it != view.end()) {
                auto it_copy = it;
                ++it_copy;
                if (it_copy != view.end())
                    next_next_code_point = *it_copy;
            }
            bool next_next_code_point_is_hebrew_letter = next_next_code_point.has_value() && has_any_wbp(*next_next_code_point, WBP::Hebrew_Letter);
            bool next_next_code_point_is_ah_letter = next_next_code_point_is_hebrew_letter || (next_next_code_point.has_value() && has_any_wbp(*next_next_code_point, WBP::ALetter));

            auto next_code_point_is_mid_num_let_q = has_any_wbp(next_code_point, WBP::MidNumLet, WBP::Single_Quote);

            // WB6
            if (code_point_is_ah_letter && next_next_code_point_is_ah_letter && (next_code_point_is_mid_num_let_q || has_any_wbp(next_code_point, WBP::MidLetter)))
                continue;

            auto code_point_is_mid_num_let_q = has_any_wbp(code_point, WBP::MidNumLet, WBP::Single_Quote);
            auto previous_code_point_is_hebrew_letter = previous_code_point.has_value() && has_any_wbp(*previous_code_point, WBP::Hebrew_Letter);
            auto previous_code_point_is_ah_letter = previous_code_point_is_hebrew_letter || (previous_code_point.has_value() && has_any_wbp(*previous_code_point, WBP::ALetter));

            // WB7
            if (previous_code_point_is_ah_letter && next_code_point_is_ah_letter && (code_point_is_mid_num_let_q || has_any_wbp(code_point, WBP::MidLetter)))
                continue;
            // WB7a
            if (code_point_is_hebrew_letter && has_any_wbp(next_code_point, WBP::Single_Quote))
                continue;
            // WB7b
            if (code_point_is_hebrew_letter && next_next_code_point_is_hebrew_letter && has_any_wbp(next_code_point, WBP::Double_Quote))
                continue;
            // WB7c
            if (previous_code_point_is_hebrew_letter && next_code_point_is_hebrew_letter && has_any_wbp(code_point, WBP::Double_Quote))
                continue;

            auto code_point_is_numeric = has_any_wbp(code_point, WBP::Numeric);
            auto next_code_point_is_numeric = has_any_wbp(next_code_point, WBP::Numeric);

            // WB8
            if (code_point_is_numeric && next_code_point_is_numeric)
                continue;
            // WB9
            if (code_point_is_ah_letter && next_code_point_is_numeric)
                continue;
            // WB10
            if (code_point_is_numeric && next_code_point_is_ah_letter)
                continue;

            auto previous_code_point_is_numeric = previous_code_point.has_value() && has_any_wbp(*previous_code_point, WBP::Numeric);

            // WB11
            if (previous_code_point_is_numeric && next_code_point_is_numeric && (code_point_is_mid_num_let_q || has_any_wbp(code_point, WBP::MidNum)))
                continue;

            bool next_next_code_point_is_numeric = next_next_code_point.has_value() && has_any_wbp(*next_next_code_point, WBP::Numeric);

            // WB12
            if (code_point_is_numeric && next_next_code_point_is_numeric && (next_code_point_is_mid_num_let_q || has_any_wbp(next_code_point, WBP::MidNum)))
                continue;

            auto code_point_is_katakana = has_any_wbp(code_point, WBP::Katakana);
            auto next_code_point_is_katakana = has_any_wbp(next_code_point, WBP::Katakana);

            // WB13
            if (code_point_is_katakana && next_code_point_is_katakana)
                continue;

            auto code_point_is_extend_num_let = has_any_wbp(code_point, WBP::ExtendNumLet);

            // WB13a
            if ((code_point_is_ah_letter || code_point_is_numeric || code_point_is_katakana || code_point_is_extend_num_let) && has_any_wbp(next_code_point, WBP::ExtendNumLet))
                continue;
            // WB13b
            if (code_point_is_extend_num_let && (next_code_point_is_ah_letter || next_code_point_is_numeric || next_code_point_is_katakana))
                continue;

            auto code_point_is_ri = has_any_wbp(code_point, WBP::Regional_Indicator);
            current_ri_chain = code_point_is_ri ? current_ri_chain + 1 : 0;

            // WB15, WB16
            if (code_point_is_ri && has_any_wbp(next_code_point, WBP::Regional_Indicator) && current_ri_chain % 2 == 1)
                continue;

            // WB999
            if (callback(code_unit_offset_of(view, it)) == IterationDecision::Break)
                return;
        }
    }

    // WB2
    callback(code_unit_length(view));
#endif
}

void for_each_word_segmentation_boundary(Utf8View const& view, SegmentationCallback callback)
{
    for_each_word_segmentation_boundary_impl(view, move(callback));
}

void for_each_word_segmentation_boundary(Utf16View const& view, SegmentationCallback callback)
{
    for_each_word_segmentation_boundary_impl(view, move(callback));
}

void for_each_word_segmentation_boundary(Utf32View const& view, SegmentationCallback callback)
{
    for_each_word_segmentation_boundary_impl(view, move(callback));
}

template<typename ViewType>
static void for_each_sentence_segmentation_boundary_impl([[maybe_unused]] ViewType const& view, [[maybe_unused]] SegmentationCallback callback)
{
#if ENABLE_UNICODE_DATA
    using SBP = SentenceBreakProperty;

    // https://www.unicode.org/reports/tr29/#Sentence_Boundary_Rules
    if (view.is_empty())
        return;

    auto has_any_sbp = [](u32 code_point, auto&&... properties) {
        return (code_point_has_sentence_break_property(code_point, properties) || ...);
    };

    // SB1
    if (callback(0) == IterationDecision::Break)
        return;

    if (code_unit_length(view) > 1) {
        auto it = view.begin();
        auto code_point = *it;
        u32 next_code_point;
        Optional<u32> previous_code_point;
        enum class TerminatorSequenceState {
            None,
            Term,
            Close,
            Sp
        } terminator_sequence_state { TerminatorSequenceState::None };
        auto term_was_a_term = false;

        for (++it; it != view.end(); ++it, previous_code_point = code_point, code_point = next_code_point) {
            next_code_point = *it;

            auto code_point_is_cr = has_any_sbp(code_point, SBP::CR);
            auto next_code_point_is_lf = has_any_sbp(next_code_point, SBP::LF);

            // SB3
            if (code_point_is_cr && next_code_point_is_lf)
                continue;

            auto code_point_is_para_sep = code_point_is_cr || has_any_sbp(code_point, SBP::LF, SBP::Sep);

            // SB4
            if (code_point_is_para_sep) {
                if (callback(code_unit_offset_of(view, it)) == IterationDecision::Break)
                    return;
                continue;
            }

            // SB5
            if (has_any_sbp(next_code_point, SBP::Format, SBP::Extend))
                continue;

            auto code_point_is_a_term = has_any_sbp(code_point, SBP::ATerm);

            // SB6
            if (code_point_is_a_term && has_any_sbp(next_code_point, SBP::Numeric))
                continue;
            // SB7
            if (code_point_is_a_term && previous_code_point.has_value() && has_any_sbp(*previous_code_point, SBP::Upper, SBP::Lower) && has_any_sbp(next_code_point, SBP::Upper))
                continue;

            if (code_point_is_a_term || has_any_sbp(code_point, SBP::STerm)) {
                terminator_sequence_state = TerminatorSequenceState::Term;
                term_was_a_term = code_point_is_a_term;
            } else if (terminator_sequence_state >= TerminatorSequenceState::Term && terminator_sequence_state <= TerminatorSequenceState::Close && has_any_sbp(code_point, SBP::Close)) {
                terminator_sequence_state = TerminatorSequenceState::Close;
            } else if (terminator_sequence_state >= TerminatorSequenceState::Term && has_any_sbp(code_point, SBP::Sp)) {
                terminator_sequence_state = TerminatorSequenceState::Sp;
            } else {
                terminator_sequence_state = TerminatorSequenceState::None;
            }

            // SB8
            if (terminator_sequence_state >= TerminatorSequenceState::Term && term_was_a_term) {
                auto it_copy = it;
                bool illegal_sequence = false;
                for (auto sequence_code_point = *it_copy; it_copy != view.end(); ++it_copy) {
                    if (has_any_sbp(sequence_code_point, SBP::Close, SBP::SContinue, SBP::Numeric, SBP::Sp, SBP::Format, SBP::Extend))
                        continue;
                    illegal_sequence = has_any_sbp(sequence_code_point, SBP::Lower);
                }
                if (illegal_sequence)
                    continue;
            }

            // SB8a
            if (terminator_sequence_state >= TerminatorSequenceState::Term && (has_any_sbp(next_code_point, SBP::SContinue, SBP::STerm, SBP::ATerm)))
                continue;

            auto next_code_point_is_sp = has_any_sbp(next_code_point, SBP::Sp);
            auto next_code_point_is_para_sep = has_any_sbp(next_code_point, SBP::Sep, SBP::CR, SBP::LF);

            // SB9
            if (terminator_sequence_state >= TerminatorSequenceState::Term && terminator_sequence_state <= TerminatorSequenceState::Close && (next_code_point_is_sp || next_code_point_is_para_sep || has_any_sbp(next_code_point, SBP::Close)))
                continue;

            // SB10
            if (terminator_sequence_state >= TerminatorSequenceState::Term && (next_code_point_is_sp || next_code_point_is_para_sep))
                continue;

            // SB11
            if (terminator_sequence_state >= TerminatorSequenceState::Term)
                if (callback(code_unit_offset_of(view, it)) == IterationDecision::Break)
                    return;

            // SB998
        }
    }

    // SB2
    callback(code_unit_length(view));
#endif
}

void for_each_sentence_segmentation_boundary(Utf8View const& view, SegmentationCallback callback)
{
    for_each_sentence_segmentation_boundary_impl(view, move(callback));
}

void for_each_sentence_segmentation_boundary(Utf16View const& view, SegmentationCallback callback)
{
    for_each_sentence_segmentation_boundary_impl(view, move(callback));
}

void for_each_sentence_segmentation_boundary(Utf32View const& view, SegmentationCallback callback)
{
    for_each_sentence_segmentation_boundary_impl(view, move(callback));
}

}
