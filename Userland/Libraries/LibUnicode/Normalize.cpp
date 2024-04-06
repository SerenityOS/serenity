/*
 * Copyright (c) 2022, mat
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Find.h>
#include <AK/QuickSort.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibUnicode/CharacterTypes.h>
#include <LibUnicode/Normalize.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodeData.h>
#else
struct Unicode::CodePointDecomposition { };
#endif

namespace Unicode {

Optional<CodePointDecomposition const> __attribute__((weak)) code_point_decomposition(u32) { return {}; }
Optional<u32> __attribute__((weak)) code_point_composition(u32, u32) { return {}; }

NormalizationForm normalization_form_from_string(StringView form)
{
    if (form == "NFD"sv)
        return NormalizationForm::NFD;
    if (form == "NFC"sv)
        return NormalizationForm::NFC;
    if (form == "NFKD"sv)
        return NormalizationForm::NFKD;
    if (form == "NFKC"sv)
        return NormalizationForm::NFKC;
    VERIFY_NOT_REACHED();
}

StringView normalization_form_to_string(NormalizationForm form)
{
    switch (form) {
    case NormalizationForm::NFD:
        return "NFD"sv;
    case NormalizationForm::NFC:
        return "NFC"sv;
    case NormalizationForm::NFKD:
        return "NFKD"sv;
    case NormalizationForm::NFKC:
        return "NFKC"sv;
    }
    VERIFY_NOT_REACHED();
}

ALWAYS_INLINE static bool is_starter(u32 code_point)
{
    return Unicode::canonical_combining_class(code_point) == 0;
}

// From https://www.unicode.org/versions/Unicode15.0.0/ch03.pdf#G56669
static constexpr u32 HANGUL_SYLLABLE_BASE = 0xAC00;
static constexpr u32 HANGUL_LEADING_BASE = 0x1100;
static constexpr u32 HANGUL_VOWEL_BASE = 0x1161;
static constexpr u32 HANGUL_TRAILING_BASE = 0x11A7;
static constexpr u32 HANGUL_LEADING_COUNT = 19;
static constexpr u32 HANGUL_VOWEL_COUNT = 21;
static constexpr u32 HANGUL_TRAILING_COUNT = 28;
// NCount in the standard.
static constexpr u32 HANGUL_BLOCK_COUNT = HANGUL_VOWEL_COUNT * HANGUL_TRAILING_COUNT;
static constexpr u32 HANGUL_SYLLABLE_COUNT = HANGUL_LEADING_COUNT * HANGUL_BLOCK_COUNT;

ALWAYS_INLINE static bool is_hangul_code_point(u32 code_point)
{
    return code_point >= HANGUL_SYLLABLE_BASE && code_point < HANGUL_SYLLABLE_BASE + HANGUL_SYLLABLE_COUNT;
}

ALWAYS_INLINE static bool is_hangul_leading(u32 code_point)
{
    return code_point >= HANGUL_LEADING_BASE && code_point < HANGUL_LEADING_BASE + HANGUL_LEADING_COUNT;
}

ALWAYS_INLINE static bool is_hangul_vowel(u32 code_point)
{
    return code_point >= HANGUL_VOWEL_BASE && code_point < HANGUL_VOWEL_BASE + HANGUL_VOWEL_COUNT;
}

ALWAYS_INLINE static bool is_hangul_trailing(u32 code_point)
{
    return code_point >= HANGUL_TRAILING_BASE && code_point < HANGUL_TRAILING_BASE + HANGUL_TRAILING_COUNT;
}

// https://www.unicode.org/versions/Unicode15.0.0/ch03.pdf#G56669
static void decompose_hangul_code_point(u32 code_point, Vector<u32>& code_points_output)
{
    auto const index = code_point - HANGUL_SYLLABLE_BASE;

    auto const leading_index = index / HANGUL_BLOCK_COUNT;
    auto const vowel_index = (index % HANGUL_BLOCK_COUNT) / HANGUL_TRAILING_COUNT;
    auto const trailing_index = index % HANGUL_TRAILING_COUNT;

    auto const leading_part = HANGUL_LEADING_BASE + leading_index;
    auto const vowel_part = HANGUL_VOWEL_BASE + vowel_index;
    auto const trailing_part = HANGUL_TRAILING_BASE + trailing_index;

    code_points_output.append(leading_part);
    code_points_output.append(vowel_part);
    if (trailing_index != 0)
        code_points_output.append(trailing_part);
}

// L, V and LV, T Hangul Syllable Composition
// https://www.unicode.org/versions/Unicode15.0.0/ch03.pdf#G59688
static u32 combine_hangul_code_points(u32 a, u32 b)
{
    if (is_hangul_leading(a) && is_hangul_vowel(b)) {
        auto const leading_index = a - HANGUL_LEADING_BASE;
        auto const vowel_index = b - HANGUL_VOWEL_BASE;
        auto const leading_vowel_index = leading_index * HANGUL_BLOCK_COUNT + vowel_index * HANGUL_TRAILING_COUNT;
        return HANGUL_SYLLABLE_BASE + leading_vowel_index;
    }
    // LV characters are the first in each "T block", so use this check to avoid combining LVT with T.
    if (is_hangul_code_point(a) && (a - HANGUL_SYLLABLE_BASE) % HANGUL_TRAILING_COUNT == 0 && is_hangul_trailing(b)) {
        return a + b - HANGUL_TRAILING_BASE;
    }
    return 0;
}

static u32 combine_code_points([[maybe_unused]] u32 a, [[maybe_unused]] u32 b)
{
#if ENABLE_UNICODE_DATA
    auto composition = code_point_composition(a, b);
    if (composition.has_value())
        return composition.value();
#endif

    return 0;
}

enum class UseCompatibility {
    Yes,
    No
};

static void decompose_code_point(u32 code_point, Vector<u32>& code_points_output, [[maybe_unused]] UseCompatibility use_compatibility)
{
    if (is_hangul_code_point(code_point))
        return decompose_hangul_code_point(code_point, code_points_output);

#if ENABLE_UNICODE_DATA
    auto const mapping = Unicode::code_point_decomposition(code_point);
    if (mapping.has_value() && (mapping->tag == CompatibilityFormattingTag::Canonical || use_compatibility == UseCompatibility::Yes)) {
        for (auto code_point : mapping->decomposition) {
            decompose_code_point(code_point, code_points_output, use_compatibility);
        }
    } else {
        code_points_output.append(code_point);
    }
#endif
}

// This can be any sorting algorithm that maintains order (like std::stable_sort),
// however bubble sort is easier to implement, so go with it (for now).
template<typename T, typename LessThan>
void bubble_sort(Span<T> span, LessThan less_than)
{
    for (size_t i = 0; i < span.size() - 1; ++i) {
        for (size_t j = 0; j < span.size() - 1 - i; ++j) {
            if (!less_than(span[j], span[j + 1]))
                swap(span[j], span[j + 1]);
        }
    }
}

// The Canonical Ordering Algorithm, as specified in Version 15.0.0 of the Unicode Standard.
// See Section 3.11, D109; and UAX #15 https://unicode.org/reports/tr15
// https://www.unicode.org/versions/Unicode15.0.0/ch03.pdf#G49591
static void canonical_ordering_algorithm(Span<u32> code_points)
{
    for (size_t i = 0; i < code_points.size(); ++i) {
        if (!is_starter(code_points[i])) {
            auto starter = find_if(code_points.begin() + i, code_points.end(), is_starter);
            auto const span_size = static_cast<size_t>(starter - (code_points.begin() + i));
            // Nothing to reorder, so continue.
            if (span_size <= 1)
                continue;
            Span<u32> const span { code_points.data() + i, span_size };

            bubble_sort(span, [](u32 a, u32 b) {
                // Use <= to keep ordering.
                return Unicode::canonical_combining_class(a) <= Unicode::canonical_combining_class(b);
            });

            // Skip over span we just sorted.
            i += span_size - 1;
        }
    }
}

// See Section 3.11, D115 of Version 15.0.0 of the Unicode Standard.
static bool is_blocked(Span<u32> code_points, size_t a, size_t c)
{
    if (a == c - 1)
        return false;
    auto const c_combining_class = Unicode::canonical_combining_class(code_points[c]);
    auto const b_combining_class = Unicode::canonical_combining_class(code_points[c - 1]);
    return b_combining_class >= c_combining_class;
}

// The Canonical Composition Algorithm, as specified in Version 15.0.0 of the Unicode Standard.
// See Section 3.11, D117; and UAX #15 https://unicode.org/reports/tr15
// https://www.unicode.org/versions/Unicode15.0.0/ch03.pdf#G50628
static void canonical_composition_algorithm(Vector<u32>& code_points)
{
    if (code_points.size() <= 1)
        return;
    ssize_t last_starter = is_starter(code_points[0]) ? 0 : -1;
    for (size_t i = 1; i < code_points.size(); ++i) {
        auto const current_character = code_points[i];
        // R1. Seek back (left) to find the last Starter L preceding C in the character sequence
        if (last_starter == -1) {
            if (is_starter(current_character))
                last_starter = i;
            continue;
        }
        // R2. If there is such an L, and C is not blocked from L,
        //     and there exists a Primary Composite P which is canonically equivalent to <L, C>,
        //     then replace L by P in the sequence and delete C from the sequence.
        if (is_blocked(code_points.span(), last_starter, i)) {
            if (is_starter(current_character))
                last_starter = i;
            continue;
        }

        auto composite = combine_hangul_code_points(code_points[last_starter], current_character);

        if (composite == 0)
            composite = combine_code_points(code_points[last_starter], current_character);

        if (composite == 0) {
            if (is_starter(current_character))
                last_starter = i;
            continue;
        }

        code_points[last_starter] = composite;
        code_points.remove(i);
        --i;
    }
}

static Vector<u32> normalize_nfd(Utf8View string)
{
    Vector<u32> result;
    for (auto const code_point : string)
        decompose_code_point(code_point, result, UseCompatibility::No);

    canonical_ordering_algorithm(result);
    return result;
}

static Vector<u32> normalize_nfc(Utf8View string)
{
    auto result = normalize_nfd(string);
    canonical_composition_algorithm(result);

    return result;
}

static Vector<u32> normalize_nfkd(Utf8View string)
{
    Vector<u32> result;
    for (auto const code_point : string)
        decompose_code_point(code_point, result, UseCompatibility::Yes);

    canonical_ordering_algorithm(result);
    return result;
}

static Vector<u32> normalize_nfkc(Utf8View string)
{
    auto result = normalize_nfkd(string);
    canonical_composition_algorithm(result);

    return result;
}

static Vector<u32> normalize_implementation(Utf8View string, NormalizationForm form)
{
    switch (form) {
    case NormalizationForm::NFD:
        return normalize_nfd(string);
    case NormalizationForm::NFC:
        return normalize_nfc(string);
    case NormalizationForm::NFKD:
        return normalize_nfkd(string);
    case NormalizationForm::NFKC:
        return normalize_nfkc(string);
    }
    VERIFY_NOT_REACHED();
}

String normalize(StringView string, NormalizationForm form)
{
    auto const code_points = normalize_implementation(Utf8View { string }, form);

    StringBuilder builder;
    for (auto code_point : code_points)
        builder.append_code_point(code_point);

    return MUST(builder.to_string());
}

}
