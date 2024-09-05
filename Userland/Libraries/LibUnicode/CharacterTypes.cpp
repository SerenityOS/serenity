/*
 * Copyright (c) 2021-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Platform.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <AK/Utf16View.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <LibUnicode/CharacterTypes.h>
#include <LibUnicode/UnicodeUtils.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodeData.h>
#endif

namespace Unicode {

Optional<ByteString> __attribute__((weak)) code_point_display_name(u32) { return {}; }
Optional<StringView> __attribute__((weak)) code_point_block_display_name(u32) { return {}; }
Optional<StringView> __attribute__((weak)) code_point_abbreviation(u32) { return {}; }
u32 __attribute__((weak)) canonical_combining_class(u32) { return {}; }
ReadonlySpan<BlockName> __attribute__((weak)) block_display_names() { return {}; }

u32 __attribute__((weak)) to_unicode_lowercase(u32 code_point)
{
    return to_ascii_lowercase(code_point);
}

u32 __attribute__((weak)) to_unicode_uppercase(u32 code_point)
{
    return to_ascii_uppercase(code_point);
}

u32 __attribute__((weak)) to_unicode_titlecase(u32 code_point)
{
    return to_ascii_uppercase(code_point);
}

template<typename ViewType>
class CasefoldStringComparator {
public:
    explicit CasefoldStringComparator(ViewType string)
        : m_string(string)
        , m_it(m_string.begin())
    {
    }

    bool has_more_data() const
    {
        return !m_casefolded_code_points.is_empty() || (m_it != m_string.end());
    }

    size_t index() const
    {
        if constexpr (IsSame<ViewType, Utf8View>)
            return m_string.byte_offset_of(m_it);
        else if constexpr (IsSame<ViewType, Utf16View>)
            return m_string.code_unit_offset_of(m_it);
        else if constexpr (IsSame<ViewType, Utf32View>)
            return m_string.iterator_offset(m_it);
        else
            static_assert(DependentFalse<ViewType>);
    }

    u32 next_code_point()
    {
        VERIFY(has_more_data());

        if (m_casefolded_code_points.is_empty()) {
            m_current_code_point = *m_it;
            ++m_it;

            m_casefolded_code_points = Unicode::Detail::casefold_code_point(m_current_code_point);
            VERIFY(!m_casefolded_code_points.is_empty()); // Must at least contain the provided code point.
        }

        auto code_point = m_casefolded_code_points[0];
        m_casefolded_code_points = m_casefolded_code_points.substring_view(1);

        return code_point;
    }

private:
    ViewType m_string;
    typename ViewType::Iterator m_it;

    u32 m_current_code_point { 0 };
    Utf32View m_casefolded_code_points;
};

// https://www.unicode.org/versions/Unicode15.0.0/ch03.pdf#G34145
template<typename ViewType>
bool equals_ignoring_case(ViewType lhs, ViewType rhs)
{
    // A string X is a caseless match for a string Y if and only if:
    //     toCasefold(X) = toCasefold(Y)

    CasefoldStringComparator lhs_comparator { lhs };
    CasefoldStringComparator rhs_comparator { rhs };

    while (lhs_comparator.has_more_data() && rhs_comparator.has_more_data()) {
        if (lhs_comparator.next_code_point() != rhs_comparator.next_code_point())
            return false;
    }

    return !lhs_comparator.has_more_data() && !rhs_comparator.has_more_data();
}

template bool equals_ignoring_case(Utf8View, Utf8View);
template bool equals_ignoring_case(Utf16View, Utf16View);
template bool equals_ignoring_case(Utf32View, Utf32View);

template<typename ViewType>
Optional<size_t> find_ignoring_case(ViewType lhs, ViewType rhs)
{
    CasefoldStringComparator lhs_comparator { lhs };

    while (lhs_comparator.has_more_data()) {
        CasefoldStringComparator rhs_comparator { rhs };

        auto saved_state = lhs_comparator;
        auto matches = true;

        while (lhs_comparator.has_more_data() && rhs_comparator.has_more_data()) {
            if (lhs_comparator.next_code_point() != rhs_comparator.next_code_point()) {
                matches = false;
                break;
            }
        }

        if (matches && !rhs_comparator.has_more_data())
            return saved_state.index();

        lhs_comparator = move(saved_state);
        lhs_comparator.next_code_point();
    }

    return {};
}

template Optional<size_t> find_ignoring_case(Utf8View, Utf8View);
template Optional<size_t> find_ignoring_case(Utf16View, Utf16View);
template Optional<size_t> find_ignoring_case(Utf32View, Utf32View);

Optional<GeneralCategory> __attribute__((weak)) general_category_from_string(StringView) { return {}; }
bool __attribute__((weak)) code_point_has_general_category(u32, GeneralCategory) { return {}; }

bool code_point_has_control_general_category(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    return code_point_has_general_category(code_point, Unicode::GeneralCategory::Control);
#else
    return false;
#endif
}

bool code_point_has_punctuation_general_category(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    return code_point_has_general_category(code_point, Unicode::GeneralCategory::Punctuation);
#else
    return false;
#endif
}

bool code_point_has_separator_general_category(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    return code_point_has_general_category(code_point, Unicode::GeneralCategory::Separator);
#else
    return is_ascii_space(code_point);
#endif
}

bool code_point_has_space_separator_general_category(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    return code_point_has_general_category(code_point, Unicode::GeneralCategory::Space_Separator);
#else
    return is_ascii_space(code_point);
#endif
}

Optional<Property> __attribute__((weak)) property_from_string(StringView) { return {}; }
bool __attribute__((weak)) code_point_has_property(u32, Property) { return {}; }

bool code_point_has_emoji_property(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    return code_point_has_property(code_point, Unicode::Property::Emoji);
#else
    return false;
#endif
}

bool code_point_has_emoji_modifier_base_property(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    return code_point_has_property(code_point, Unicode::Property::Emoji_Modifier_Base);
#else
    return false;
#endif
}

bool code_point_has_emoji_presentation_property(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    return code_point_has_property(code_point, Unicode::Property::Emoji_Presentation);
#else
    return false;
#endif
}

bool code_point_has_identifier_start_property(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    return code_point_has_property(code_point, Unicode::Property::ID_Start);
#else
    return false;
#endif
}

bool code_point_has_identifier_continue_property(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    return code_point_has_property(code_point, Unicode::Property::ID_Continue);
#else
    return false;
#endif
}

bool code_point_has_regional_indicator_property(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    return code_point_has_property(code_point, Unicode::Property::Regional_Indicator);
#else
    return false;
#endif
}

bool code_point_has_variation_selector_property(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    return code_point_has_property(code_point, Unicode::Property::Variation_Selector);
#else
    return false;
#endif
}

bool is_ecma262_property([[maybe_unused]] Property property)
{
#if ENABLE_UNICODE_DATA
    // EMCA-262 only allows a subset of Unicode properties: https://tc39.es/ecma262/#table-binary-unicode-properties
    switch (property) {
    case Unicode::Property::ASCII:
    case Unicode::Property::ASCII_Hex_Digit:
    case Unicode::Property::Alphabetic:
    case Unicode::Property::Any:
    case Unicode::Property::Assigned:
    case Unicode::Property::Bidi_Control:
    case Unicode::Property::Bidi_Mirrored:
    case Unicode::Property::Case_Ignorable:
    case Unicode::Property::Cased:
    case Unicode::Property::Changes_When_Casefolded:
    case Unicode::Property::Changes_When_Casemapped:
    case Unicode::Property::Changes_When_Lowercased:
    case Unicode::Property::Changes_When_NFKC_Casefolded:
    case Unicode::Property::Changes_When_Titlecased:
    case Unicode::Property::Changes_When_Uppercased:
    case Unicode::Property::Dash:
    case Unicode::Property::Default_Ignorable_Code_Point:
    case Unicode::Property::Deprecated:
    case Unicode::Property::Diacritic:
    case Unicode::Property::Emoji:
    case Unicode::Property::Emoji_Component:
    case Unicode::Property::Emoji_Modifier:
    case Unicode::Property::Emoji_Modifier_Base:
    case Unicode::Property::Emoji_Presentation:
    case Unicode::Property::Extended_Pictographic:
    case Unicode::Property::Extender:
    case Unicode::Property::Grapheme_Base:
    case Unicode::Property::Grapheme_Extend:
    case Unicode::Property::Hex_Digit:
    case Unicode::Property::IDS_Binary_Operator:
    case Unicode::Property::IDS_Trinary_Operator:
    case Unicode::Property::ID_Continue:
    case Unicode::Property::ID_Start:
    case Unicode::Property::Ideographic:
    case Unicode::Property::Join_Control:
    case Unicode::Property::Logical_Order_Exception:
    case Unicode::Property::Lowercase:
    case Unicode::Property::Math:
    case Unicode::Property::Noncharacter_Code_Point:
    case Unicode::Property::Pattern_Syntax:
    case Unicode::Property::Pattern_White_Space:
    case Unicode::Property::Quotation_Mark:
    case Unicode::Property::Radical:
    case Unicode::Property::Regional_Indicator:
    case Unicode::Property::Sentence_Terminal:
    case Unicode::Property::Soft_Dotted:
    case Unicode::Property::Terminal_Punctuation:
    case Unicode::Property::Unified_Ideograph:
    case Unicode::Property::Uppercase:
    case Unicode::Property::Variation_Selector:
    case Unicode::Property::White_Space:
    case Unicode::Property::XID_Continue:
    case Unicode::Property::XID_Start:
        return true;
    default:
        return false;
    }
#else
    return false;
#endif
}

Optional<Script> __attribute__((weak)) script_from_string(StringView) { return {}; }
bool __attribute__((weak)) code_point_has_script(u32, Script) { return {}; }
bool __attribute__((weak)) code_point_has_script_extension(u32, Script) { return {}; }

bool __attribute__((weak)) code_point_has_grapheme_break_property(u32, GraphemeBreakProperty) { return {}; }
bool __attribute__((weak)) code_point_has_word_break_property(u32, WordBreakProperty) { return {}; }
bool __attribute__((weak)) code_point_has_sentence_break_property(u32, SentenceBreakProperty) { return {}; }

Optional<BidirectionalClassInternal> __attribute__((weak)) bidirectional_class_internal(u32) { return {}; }

#if ENABLE_UNICODE_DATA
static constexpr BidiClass bidi_class_from_bidirectional_class_interal(BidirectionalClassInternal direction)
{
    switch (direction) {
    case BidirectionalClassInternal::Arabic_Number:
        return BidiClass::ArabicNumber;
    case BidirectionalClassInternal::Paragraph_Separator:
        return BidiClass::BlockSeparator;
    case BidirectionalClassInternal::Boundary_Neutral:
        return BidiClass::BoundaryNeutral;
    case BidirectionalClassInternal::Common_Separator:
        return BidiClass::CommonNumberSeparator;
    case BidirectionalClassInternal::Nonspacing_Mark:
        return BidiClass::DirNonSpacingMark;
    case BidirectionalClassInternal::European_Number:
        return BidiClass::EuropeanNumber;
    case BidirectionalClassInternal::European_Separator:
        return BidiClass::EuropeanNumberSeparator;
    case BidirectionalClassInternal::European_Terminator:
        return BidiClass::EuropeanNumberTerminator;
    case BidirectionalClassInternal::First_Strong_Isolate:
        return BidiClass::FirstStrongIsolate;
    case BidirectionalClassInternal::Left_To_Right:
        return BidiClass::LeftToRight;
    case BidirectionalClassInternal::Left_To_Right_Embedding:
        return BidiClass::LeftToRightEmbedding;
    case BidirectionalClassInternal::Left_To_Right_Isolate:
        return BidiClass::LeftToRightIsolate;
    case BidirectionalClassInternal::Left_To_Right_Override:
        return BidiClass::LeftToRightOverride;
    case BidirectionalClassInternal::Other_Neutral:
        return BidiClass::OtherNeutral;
    case BidirectionalClassInternal::Pop_Directional_Format:
        return BidiClass::PopDirectionalFormat;
    case BidirectionalClassInternal::Pop_Directional_Isolate:
        return BidiClass::PopDirectionalIsolate;
    case BidirectionalClassInternal::Right_To_Left:
        return BidiClass::RightToLeft;
    case BidirectionalClassInternal::Arabic_Letter:
        return BidiClass::RightToLeftArabic;
    case BidirectionalClassInternal::Right_To_Left_Embedding:
        return BidiClass::RightToLeftEmbedding;
    case BidirectionalClassInternal::Right_To_Left_Isolate:
        return BidiClass::RightToLeftIsolate;
    case BidirectionalClassInternal::Right_To_Left_Override:
        return BidiClass::RightToLeftOverride;
    case BidirectionalClassInternal::Segment_Separator:
        return BidiClass::SegmentSeparator;
    case BidirectionalClassInternal::White_Space:
        return BidiClass::WhiteSpaceNeutral;
    }

    VERIFY_NOT_REACHED();
}
#endif

BidiClass bidirectional_class([[maybe_unused]] u32 code_point)
{
#if ENABLE_UNICODE_DATA
    if (auto bidi_class_internal = bidirectional_class_internal(code_point); bidi_class_internal.has_value())
        return bidi_class_from_bidirectional_class_interal(bidi_class_internal.value());
#endif
    return BidiClass::LeftToRight;
}

}
