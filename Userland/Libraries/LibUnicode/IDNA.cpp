/*
 * Copyright (c) 2023, Simon Wanner <simon@skyrising.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/String.h>
#include <LibUnicode/CharacterTypes.h>
#include <LibUnicode/IDNA.h>
#include <LibUnicode/Normalize.h>
#include <LibUnicode/Punycode.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/IDNAData.h>
#    include <LibUnicode/UnicodeData.h>
#endif

namespace Unicode::IDNA {

Optional<Mapping> __attribute__((weak)) get_idna_mapping(u32) { return {}; }

struct ProcessingResult {
    Vector<String> result {};
    bool has_error { false };
};

static MappingStatus translate_status(MappingStatus status, UseStd3AsciiRules use_std3_ascii_rules)
{
    switch (status) {
    case MappingStatus::DisallowedStd3Valid:
        return use_std3_ascii_rules == UseStd3AsciiRules::Yes ? MappingStatus::Disallowed : MappingStatus::Valid;
    case MappingStatus::DisallowedStd3Mapped:
        return use_std3_ascii_rules == UseStd3AsciiRules::Yes ? MappingStatus::Disallowed : MappingStatus::Mapped;
    default:
        return status;
    }
}

// https://www.unicode.org/reports/tr46/#Validity_Criteria
static bool is_valid_label(String const& label, CheckHyphens check_hyphens, CheckBidi check_bidi, CheckJoiners check_joiners, UseStd3AsciiRules use_std3_ascii_rules, TransitionalProcessing transitional_processing)
{
    // 1. The label must be in Unicode Normalization Form NFC.
    auto normalized = normalize(label, NormalizationForm::NFC);
    if (normalized != label)
        return false;

    size_t position = 0;
    for (auto code_point : label.code_points()) {
        // 2. If CheckHyphens, the label must not contain a U+002D HYPHEN-MINUS character in both the third and fourth positions.
        if (check_hyphens == CheckHyphens::Yes && code_point == '-' && (position == 2 || position == 3))
            return false;

        // 4. The label must not contain a U+002E ( . ) FULL STOP.
        if (code_point == '.')
            return false;

        // 5. The label must not begin with a combining mark, that is: General_Category=Mark.
        static auto general_category_mark = general_category_from_string("Mark"sv);
        if (position == 0 && general_category_mark.has_value() && code_point_has_general_category(code_point, general_category_mark.value()))
            return false;

        // 6. Each code point in the label must only have certain status values according to Section 5, IDNA Mapping Table:
        Optional<Mapping> mapping = get_idna_mapping(code_point);
        if (!mapping.has_value())
            return false;

        auto status = translate_status(mapping->status, use_std3_ascii_rules);
        if (transitional_processing == TransitionalProcessing::Yes) {
            // 1. For Transitional Processing, each value must be valid.
            if (status != MappingStatus::Valid)
                return false;
        } else {
            // 2. For Nontransitional Processing, each value must be either valid or deviation.
            if (status != MappingStatus::Valid && status != MappingStatus::Deviation)
                return false;
        }
        position++;
    }

    // 3. If CheckHyphens, the label must neither begin nor end with a U+002D HYPHEN-MINUS character.
    if (check_hyphens == CheckHyphens::Yes && (label.starts_with('-') || label.ends_with('-')))
        return false;

    // FIXME: 7. If CheckJoiners, the label must satisify the ContextJ rules from Appendix A, in The Unicode Code Points and Internationalized Domain Names for Applications (IDNA) [IDNA2008].
    (void)check_joiners;

    // FIXME: 8. If CheckBidi, and if the domain name is a  Bidi domain name, then the label must satisfy all six of the numbered conditions in [IDNA2008] RFC 5893, Section 2.
    (void)check_bidi;

    return true;
}

// https://www.unicode.org/reports/tr46/#Processing
static ErrorOr<ProcessingResult> apply_main_processing_steps(Utf8View domain_name, ToAsciiOptions const& options)
{
    bool has_error = false;
    StringBuilder mapped;
    // 1. Map. For each code point in the domain_name string, look up the status value in Section 5, IDNA Mapping Table, and take the following actions:
    for (u32 code_point : domain_name) {
        Optional<Mapping> mapping = get_idna_mapping(code_point);
        if (!mapping.has_value()) {
            has_error = true;
            continue;
        }
        switch (translate_status(mapping->status, options.use_std3_ascii_rules)) {
        // disallowed: Leave the code point unchanged in the string, and record that there was an error.
        case MappingStatus::Disallowed:
            TRY(mapped.try_append_code_point(code_point));
            has_error = true;
            break;
        // ignored: Remove the code point from the string. This is equivalent to mapping the code point to an empty string.
        case MappingStatus::Ignored:
            break;
        // mapped: Replace the code point in the string by the value for the mapping in Section 5, IDNA Mapping Table.
        case MappingStatus::Mapped:
            TRY(mapped.try_append(mapping->mapped_to));
            break;
        // deviation:
        case MappingStatus::Deviation:
            if (options.transitional_processing == TransitionalProcessing::Yes) {
                // If Transitional_Processing, replace the code point in the string by the value for the mapping in Section 5, IDNA Mapping Table .
                TRY(mapped.try_append(mapping->mapped_to));
            } else {
                TRY(mapped.try_append_code_point(code_point));
            }
            break;
        // valid: Leave the code point unchanged in the string.
        case MappingStatus::Valid:
            TRY(mapped.try_append_code_point(code_point));
            break;

        default:
            VERIFY_NOT_REACHED();
        }
    }

    // 2. Normalize. Normalize the domain_name string to Unicode Normalization Form C.
    auto normalized = normalize(mapped.string_view(), NormalizationForm::NFC);

    // 3. Break. Break the string into labels at U+002E ( . ) FULL STOP.
    auto labels = TRY(normalized.split('.', SplitBehavior::KeepEmpty));

    // 4. Convert/Validate. For each label in the domain_name string:
    for (auto& label : labels) {
        // If the label starts with “xn--”:
        if (label.starts_with_bytes("xn--"sv)) {
            // 1. Attempt to convert the rest of the label to Unicode according to Punycode [RFC3492]. If that conversion fails, record that there was an error, and continue with the next label.
            //    Otherwise replace the original label in the string by the results of the conversion.
            auto punycode = Punycode::decode(label.bytes_as_string_view().substring_view(4));
            if (punycode.is_error()) {
                has_error = true;
                continue;
            }

            label = punycode.release_value();

            // 2. Verify that the label meets the validity criteria in Section 4.1, Validity Criteria for Nontransitional Processing.
            //    If any of the validity criteria are not satisfied, record that there was an error.
            if (!is_valid_label(label, options.check_hyphens, options.check_bidi, options.check_joiners, options.use_std3_ascii_rules, TransitionalProcessing::No))
                has_error = true;
        }
        // If the label does not start with “xn--”:
        else {
            // Verify that the label meets the validity criteria in Section 4.1, Validity Criteria for the input Processing choice (Transitional or Nontransitional).
            // If any of the validity criteria are not satisfied, record that there was an error.
            if (!is_valid_label(label, options.check_hyphens, options.check_bidi, options.check_joiners, options.use_std3_ascii_rules, options.transitional_processing))
                has_error = true;
        }
    }

    return ProcessingResult {
        .result = move(labels),
        .has_error = has_error,
    };
}

// https://www.unicode.org/reports/tr46/#ToASCII
ErrorOr<String> to_ascii(Utf8View domain_name, ToAsciiOptions const& options)
{
    // 1. To the input domain_name, apply the Processing Steps in Section 4, Processing, using the input boolean flags Transitional_Processing, CheckHyphens, CheckBidi, CheckJoiners, and UseSTD3ASCIIRules. This may record an error.
    auto processed = TRY(apply_main_processing_steps(domain_name, options));
    bool has_error = processed.has_error;

    // 2. Break the result into labels at U+002E FULL STOP.
    auto labels = move(processed.result);

    // 3. Convert each label with non-ASCII characters into Punycode [RFC3492], and prefix by “xn--”. This may record an error.
    for (auto& label : labels) {
        auto all_ascii = true;
        for (auto code_point : label.code_points()) {
            if (!is_ascii(code_point)) {
                all_ascii = false;
                break;
            }
        }

        if (!all_ascii) {
            auto punycode = Punycode::encode(label);
            if (punycode.is_error()) {
                has_error = true;
                continue;
            }
            auto punycode_result = punycode.release_value();

            StringBuilder builder;
            TRY(builder.try_append("xn--"sv));
            TRY(builder.try_append(punycode_result));
            label = TRY(builder.to_string());
        }
    }

    // 4. If the VerifyDnsLength flag is true, then verify DNS length restrictions. This may record an error. For more information, see [STD13] and [STD3].
    if (options.verify_dns_length == VerifyDnsLength::Yes) {
        // 1. The length of the domain name, excluding the root label and its dot, is from 1 to 253.
        size_t total_length = 0;
        auto* root_label = !labels.is_empty() && labels.last().is_empty() ? &labels.last() : nullptr;
        for (auto& label : labels) {
            // 2. The length of each label is from 1 to 63.
            auto length = label.bytes().size();
            if (label.is_empty() && &label != root_label)
                return Error::from_string_literal("Invalid empty label");
            if (length > 63)
                return Error::from_string_literal("Label too long");
            total_length += length;
        }

        total_length += labels.size() - (root_label ? 2 : 1);
        if (total_length == 0 || total_length > 253)
            return Error::from_string_literal("Domain too long");
    }

    // 5. If an error was recorded in steps 1-4, then the operation has failed and a failure value is returned. No DNS lookup should be done.
    if (has_error)
        return Error::from_string_literal("Invalid domain name");

    // 6. Otherwise join the labels using U+002E FULL STOP as a separator, and return the result.
    return String::join('.', labels);
}

}
