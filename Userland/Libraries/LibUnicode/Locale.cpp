/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AllOf.h>
#include <AK/CharacterTypes.h>
#include <AK/GenericLexer.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibUnicode/Locale.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodeLocale.h>
#endif

namespace Unicode {

bool is_unicode_language_subtag(StringView subtag)
{
    // unicode_language_subtag = alpha{2,3} | alpha{5,8}
    if ((subtag.length() < 2) || (subtag.length() == 4) || (subtag.length() > 8))
        return false;
    return all_of(subtag, is_ascii_alpha);
}

bool is_unicode_script_subtag(StringView subtag)
{
    // unicode_script_subtag = alpha{4}
    if (subtag.length() != 4)
        return false;
    return all_of(subtag, is_ascii_alpha);
}

bool is_unicode_region_subtag(StringView subtag)
{
    // unicode_region_subtag = (alpha{2} | digit{3})
    if (subtag.length() == 2)
        return all_of(subtag, is_ascii_alpha);
    if (subtag.length() == 3)
        return all_of(subtag, is_ascii_digit);
    return false;
}

bool is_unicode_variant_subtag(StringView subtag)
{
    // unicode_variant_subtag = (alphanum{5,8} | digit alphanum{3})
    if ((subtag.length() >= 5) && (subtag.length() <= 8))
        return all_of(subtag, is_ascii_alphanumeric);
    if (subtag.length() == 4)
        return is_ascii_digit(subtag[0]) && all_of(subtag.substring_view(1), is_ascii_alphanumeric);
    return false;
}

static bool is_key(StringView key)
{
    // key = alphanum alpha
    if (key.length() != 2)
        return false;
    return is_ascii_alphanumeric(key[0]) && is_ascii_alpha(key[1]);
}

static bool is_single_type(StringView type)
{
    // type = alphanum{3,8} (sep alphanum{3,8})*
    // Note: Consecutive types are not handled here, that is left to the caller.
    if ((type.length() < 3) || (type.length() > 8))
        return false;
    return all_of(type, is_ascii_alphanumeric);
}

static bool is_attribute(StringView type)
{
    // attribute = alphanum{3,8}
    if ((type.length() < 3) || (type.length() > 8))
        return false;
    return all_of(type, is_ascii_alphanumeric);
}

static bool is_transformed_key(StringView key)
{
    // tkey = alpha digit
    if (key.length() != 2)
        return false;
    return is_ascii_alpha(key[0]) && is_ascii_digit(key[1]);
}

static bool is_single_transformed_value(StringView value)
{
    // tvalue = (sep alphanum{3,8})+
    // Note: Consecutive values are not handled here, that is left to the caller.
    if ((value.length() < 3) || (value.length() > 8))
        return false;
    return all_of(value, is_ascii_alphanumeric);
}

static Optional<StringView> consume_next_segment(GenericLexer& lexer, bool with_separator = true)
{
    constexpr auto is_separator = is_any_of("-_"sv);

    if (with_separator) {
        if (!lexer.next_is(is_separator))
            return {};
        lexer.ignore();
    }

    auto segment = lexer.consume_until(is_separator);
    if (segment.is_empty()) {
        lexer.retreat(with_separator);
        return {};
    }

    return segment;
}

static Optional<LanguageID> parse_unicode_language_id(GenericLexer& lexer)
{
    // https://unicode.org/reports/tr35/#Unicode_language_identifier
    //
    // unicode_language_id = "root"
    //     OR
    // unicode_language_id = ((unicode_language_subtag (sep unicode_script_subtag)?) | unicode_script_subtag)
    //                       (sep unicode_region_subtag)?
    //                       (sep unicode_variant_subtag)*
    LanguageID language_id {};

    if (lexer.consume_specific("root"sv)) {
        language_id.is_root = true;
        return language_id;
    }

    enum class ParseState {
        ParsingLanguageOrScript,
        ParsingScript,
        ParsingRegion,
        ParsingVariant,
        Done,
    };

    auto state = ParseState::ParsingLanguageOrScript;

    while (!lexer.is_eof() && (state != ParseState::Done)) {
        auto segment = consume_next_segment(lexer, state != ParseState::ParsingLanguageOrScript);
        if (!segment.has_value())
            return {};

        switch (state) {
        case ParseState::ParsingLanguageOrScript:
            if (is_unicode_language_subtag(*segment)) {
                state = ParseState::ParsingScript;
                language_id.language = *segment;
            } else if (is_unicode_script_subtag(*segment)) {
                state = ParseState::ParsingRegion;
                language_id.script = *segment;
            } else {
                return {};
            }
            break;

        case ParseState::ParsingScript:
            if (is_unicode_script_subtag(*segment)) {
                state = ParseState::ParsingRegion;
                language_id.script = *segment;
                break;
            }

            state = ParseState::ParsingRegion;
            [[fallthrough]];

        case ParseState::ParsingRegion:
            if (is_unicode_region_subtag(*segment)) {
                state = ParseState::ParsingVariant;
                language_id.region = *segment;
                break;
            }

            state = ParseState::ParsingVariant;
            [[fallthrough]];

        case ParseState::ParsingVariant:
            if (is_unicode_variant_subtag(*segment)) {
                language_id.variants.append(*segment);
            } else {
                lexer.retreat(segment->length() + 1);
                state = ParseState::Done;
            }
            break;

        default:
            VERIFY_NOT_REACHED();
        }
    }

    return language_id;
}

static Optional<LocaleExtension> parse_unicode_locale_extension(GenericLexer& lexer)
{
    // https://unicode.org/reports/tr35/#unicode_locale_extensions
    //
    // unicode_locale_extensions = sep [uU] ((sep keyword)+ | (sep attribute)+ (sep keyword)*)
    LocaleExtension locale_extension {};

    enum class ParseState {
        ParsingAttributeOrKeyword,
        ParsingAttribute,
        ParsingKeyword,
        Done,
    };

    auto state = ParseState::ParsingAttributeOrKeyword;

    while (!lexer.is_eof() && (state != ParseState::Done)) {
        auto segment = consume_next_segment(lexer);
        if (!segment.has_value())
            return {};

        if (state == ParseState::ParsingAttributeOrKeyword)
            state = is_key(*segment) ? ParseState::ParsingKeyword : ParseState::ParsingAttribute;

        switch (state) {
        case ParseState::ParsingAttribute:
            if (is_attribute(*segment)) {
                locale_extension.attributes.append(*segment);
                break;
            }

            state = ParseState::ParsingKeyword;
            [[fallthrough]];

        case ParseState::ParsingKeyword: {
            // keyword = key (sep type)?
            Keyword keyword { .key = *segment };

            if (!is_key(*segment)) {
                lexer.retreat(segment->length() + 1);
                state = ParseState::Done;
                break;
            }

            while (true) {
                auto type = consume_next_segment(lexer);

                if (!type.has_value() || !is_single_type(*type)) {
                    if (type.has_value())
                        lexer.retreat(type->length() + 1);
                    break;
                }

                keyword.types.append(*type);
            }

            locale_extension.keywords.append(move(keyword));
            break;
        }

        default:
            VERIFY_NOT_REACHED();
        }
    }

    if (locale_extension.attributes.is_empty() && locale_extension.keywords.is_empty())
        return {};
    return locale_extension;
}

static Optional<TransformedExtension> parse_transformed_extension(GenericLexer& lexer)
{
    // https://unicode.org/reports/tr35/#transformed_extensions
    //
    // transformed_extensions = sep [tT] ((sep tlang (sep tfield)*) | (sep tfield)+)
    TransformedExtension transformed_extension {};

    enum class ParseState {
        ParsingLanguageOrField,
        ParsingLanguage,
        ParsingField,
        Done,
    };

    auto state = ParseState::ParsingLanguageOrField;

    while (!lexer.is_eof() && (state != ParseState::Done)) {
        auto segment = consume_next_segment(lexer);
        if (!segment.has_value())
            return {};

        if (state == ParseState::ParsingLanguageOrField)
            state = is_unicode_language_subtag(*segment) ? ParseState::ParsingLanguage : ParseState::ParsingField;

        switch (state) {
        case ParseState::ParsingLanguage:
            lexer.retreat(segment->length());

            if (auto language_id = parse_unicode_language_id(lexer); language_id.has_value()) {
                transformed_extension.language = language_id.release_value();
                state = ParseState::ParsingField;
                break;
            }

            return {};

        case ParseState::ParsingField: {
            // tfield = tkey tvalue;
            TransformedField field { .key = *segment };

            if (!is_transformed_key(*segment)) {
                lexer.retreat(segment->length() + 1);
                state = ParseState::Done;
                break;
            }

            while (true) {
                auto value = consume_next_segment(lexer);

                if (!value.has_value() || !is_single_transformed_value(*value)) {
                    if (value.has_value())
                        lexer.retreat(value->length() + 1);
                    break;
                }

                field.values.append(*value);
            }

            if (field.values.is_empty())
                return {};

            transformed_extension.fields.append(move(field));
            break;
        }

        default:
            VERIFY_NOT_REACHED();
        }
    }

    if (!transformed_extension.language.has_value() && transformed_extension.fields.is_empty())
        return {};
    return transformed_extension;
}

static Optional<OtherExtension> parse_other_extension(char key, GenericLexer& lexer)
{
    // https://unicode.org/reports/tr35/#other_extensions
    //
    // other_extensions = sep [alphanum-[tTuUxX]] (sep alphanum{2,8})+ ;
    OtherExtension other_extension { .key = key };

    if (!is_ascii_alphanumeric(key) || (key == 'x') || (key == 'X'))
        return {};

    while (true) {
        auto segment = consume_next_segment(lexer);
        if (!segment.has_value())
            break;

        if ((segment->length() < 2) || (segment->length() > 8) || !all_of(*segment, is_ascii_alphanumeric)) {
            lexer.retreat(segment->length() + 1);
            break;
        }

        other_extension.values.append(*segment);
    }

    if (other_extension.values.is_empty())
        return {};
    return other_extension;
}

static Optional<Extension> parse_extension(GenericLexer& lexer)
{
    // https://unicode.org/reports/tr35/#extensions
    //
    // extensions = unicode_locale_extensions | transformed_extensions | other_extensions
    size_t starting_position = lexer.tell();

    if (auto header = consume_next_segment(lexer); header.has_value() && (header->length() == 1)) {
        switch (char key = (*header)[0]) {
        case 'u':
        case 'U':
            if (auto extension = parse_unicode_locale_extension(lexer); extension.has_value())
                return Extension { extension.release_value() };
            break;

        case 't':
        case 'T':
            if (auto extension = parse_transformed_extension(lexer); extension.has_value())
                return Extension { extension.release_value() };
            break;

        default:
            if (auto extension = parse_other_extension(key, lexer); extension.has_value())
                return Extension { extension.release_value() };
            break;
        }
    }

    lexer.retreat(lexer.tell() - starting_position);
    return {};
}

static Vector<String> parse_private_use_extensions(GenericLexer& lexer)
{
    // https://unicode.org/reports/tr35/#pu_extensions
    //
    // pu_extensions = = sep [xX] (sep alphanum{1,8})+ ;
    size_t starting_position = lexer.tell();

    auto header = consume_next_segment(lexer);
    if (!header.has_value())
        return {};

    auto parse_values = [&]() -> Vector<String> {
        Vector<String> extensions;

        while (true) {
            auto segment = consume_next_segment(lexer);
            if (!segment.has_value())
                break;

            if ((segment->length() < 1) || (segment->length() > 8) || !all_of(*segment, is_ascii_alphanumeric)) {
                lexer.retreat(segment->length() + 1);
                break;
            }

            extensions.append(*segment);
        }

        return extensions;
    };

    if ((header->length() == 1) && (((*header)[0] == 'x') || ((*header)[0] == 'X'))) {
        if (auto extensions = parse_values(); !extensions.is_empty())
            return extensions;
    }

    lexer.retreat(lexer.tell() - starting_position);
    return {};
}

Optional<LanguageID> parse_unicode_language_id(StringView language)
{
    GenericLexer lexer { language };

    auto language_id = parse_unicode_language_id(lexer);
    if (!lexer.is_eof())
        return {};

    return language_id;
}

Optional<LocaleID> parse_unicode_locale_id(StringView locale)
{
    GenericLexer lexer { locale };

    // https://unicode.org/reports/tr35/#Unicode_locale_identifier
    //
    // unicode_locale_id = unicode_language_id
    //                     extensions*
    //                     pu_extensions?
    auto language_id = parse_unicode_language_id(lexer);
    if (!language_id.has_value())
        return {};

    LocaleID locale_id { language_id.release_value() };

    while (true) {
        auto extension = parse_extension(lexer);
        if (!extension.has_value())
            break;
        locale_id.extensions.append(extension.release_value());
    }

    locale_id.private_use_extensions = parse_private_use_extensions(lexer);

    if (!lexer.is_eof())
        return {};

    return locale_id;
}

Optional<String> canonicalize_unicode_locale_id(LocaleID& locale_id)
{
    // https://unicode.org/reports/tr35/#Canonical_Unicode_Locale_Identifiers
    StringBuilder builder;

    enum class Case {
        Upper,
        Lower,
        Title,
    };

    auto append_sep_and_string = [&](Optional<String> const& string, Case case_ = Case::Lower) {
        if (!string.has_value())
            return;
        switch (case_) {
        case Case::Upper:
            builder.appendff("-{}", string->to_uppercase());
            break;
        case Case::Lower:
            builder.appendff("-{}", string->to_lowercase());
            break;
        case Case::Title:
            builder.appendff("-{}", string->to_titlecase());
            break;
        }
    };

    if (!locale_id.language_id.language.has_value())
        return {};

    builder.append(locale_id.language_id.language->to_lowercase());
    append_sep_and_string(locale_id.language_id.script, Case::Title);
    append_sep_and_string(locale_id.language_id.region, Case::Upper);

    quick_sort(locale_id.language_id.variants);
    for (auto const& variant : locale_id.language_id.variants)
        append_sep_and_string(variant);

    quick_sort(locale_id.extensions, [](auto const& left, auto const& right) {
        auto key = [](auto const& extension) {
            return extension.visit(
                [](LocaleExtension const&) { return 'u'; },
                [](TransformedExtension const&) { return 't'; },
                [](OtherExtension const& ext) { return static_cast<char>(to_ascii_lowercase(ext.key)); });
        };

        return key(left) < key(right);
    });

    auto append_key_value_list = [&](auto const& key, auto const& values, bool remove_true_values) {
        append_sep_and_string(key);

        for (auto const& type : values) {
            // Note: The spec says to remove "true" type and tfield values but that is believed to be a bug in the spec
            // because, for tvalues, that would result in invalid syntax:
            //     https://unicode-org.atlassian.net/browse/CLDR-14318
            // This has also been noted by test262:
            //     https://github.com/tc39/test262/blob/18bb955771669541c56c28748603f6afdb2e25ff/test/intl402/Intl/getCanonicalLocales/transformed-ext-canonical.js
            if (remove_true_values && type.equals_ignoring_case("true"sv))
                continue;
            append_sep_and_string(type);
        }
    };

    for (auto& extension : locale_id.extensions) {
        extension.visit(
            [&](LocaleExtension& ext) {
                quick_sort(ext.attributes);
                quick_sort(ext.keywords, [](auto const& a, auto const& b) { return a.key < b.key; });
                builder.append("-u"sv);

                for (auto const& attribute : ext.attributes)
                    append_sep_and_string(attribute);
                for (auto const& keyword : ext.keywords)
                    append_key_value_list(keyword.key, keyword.types, true);
            },
            [&](TransformedExtension& ext) {
                quick_sort(ext.fields, [](auto const& a, auto const& b) { return a.key < b.key; });
                builder.append("-t"sv);

                if (ext.language.has_value()) {
                    append_sep_and_string(ext.language->language);
                    append_sep_and_string(ext.language->script);
                    append_sep_and_string(ext.language->region);

                    quick_sort(ext.language->variants);
                    for (auto const& variant : ext.language->variants)
                        append_sep_and_string(variant);
                }

                for (auto const& field : ext.fields)
                    append_key_value_list(field.key, field.values, false);
            },
            [&](OtherExtension& ext) {
                builder.appendff("-{:c}", to_ascii_lowercase(ext.key));
                for (auto const& value : ext.values)
                    append_sep_and_string(value);
            });
    }

    if (!locale_id.private_use_extensions.is_empty()) {
        builder.append("-x"sv);
        for (auto const& extension : locale_id.private_use_extensions)
            append_sep_and_string(extension);
    }

    return builder.build();
}

String const& default_locale()
{
    static String locale = "en"sv;
    return locale;
}

bool is_locale_available([[maybe_unused]] StringView locale)
{
#if ENABLE_UNICODE_DATA
    return Detail::locale_from_string(locale).has_value();
#else
    return false;
#endif
}

Optional<StringView> get_locale_language_mapping([[maybe_unused]] StringView locale, [[maybe_unused]] StringView language)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_locale_language_mapping(locale, language);
#else
    return {};
#endif
}

Optional<StringView> get_locale_territory_mapping([[maybe_unused]] StringView locale, [[maybe_unused]] StringView territory)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_locale_territory_mapping(locale, territory);
#else
    return {};
#endif
}

Optional<StringView> get_locale_script_mapping([[maybe_unused]] StringView locale, [[maybe_unused]] StringView script)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_locale_script_tag_mapping(locale, script);
#else
    return {};
#endif
}

Optional<StringView> get_locale_currency_mapping([[maybe_unused]] StringView locale, [[maybe_unused]] StringView currency)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_locale_currency_mapping(locale, currency);
#else
    return {};
#endif
}

}
