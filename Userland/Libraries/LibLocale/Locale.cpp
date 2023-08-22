/*
 * Copyright (c) 2021-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AllOf.h>
#include <AK/GenericLexer.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibLocale/DateTimeFormat.h>
#include <LibLocale/Locale.h>
#include <LibUnicode/CharacterTypes.h>

namespace Locale {

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

bool is_type_identifier(StringView identifier)
{
    // type = alphanum{3,8} (sep alphanum{3,8})*
    GenericLexer lexer { identifier };

    while (true) {
        auto type = consume_next_segment(lexer, lexer.tell() > 0);
        if (!type.has_value())
            break;
        if (!is_single_type(*type))
            return false;
    }

    return lexer.is_eof() && (lexer.tell() > 0);
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
                language_id.language = MUST(String::from_utf8(*segment));
            } else if (is_unicode_script_subtag(*segment)) {
                state = ParseState::ParsingRegion;
                language_id.script = MUST(String::from_utf8(*segment));
            } else {
                return {};
            }
            break;

        case ParseState::ParsingScript:
            if (is_unicode_script_subtag(*segment)) {
                state = ParseState::ParsingRegion;
                language_id.script = MUST(String::from_utf8(*segment));
                break;
            }

            state = ParseState::ParsingRegion;
            [[fallthrough]];

        case ParseState::ParsingRegion:
            if (is_unicode_region_subtag(*segment)) {
                state = ParseState::ParsingVariant;
                language_id.region = MUST(String::from_utf8(*segment));
                break;
            }

            state = ParseState::ParsingVariant;
            [[fallthrough]];

        case ParseState::ParsingVariant:
            if (is_unicode_variant_subtag(*segment)) {
                language_id.variants.append(MUST(String::from_utf8(*segment)));
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
                locale_extension.attributes.append(MUST(String::from_utf8(*segment)));
                break;
            }

            state = ParseState::ParsingKeyword;
            [[fallthrough]];

        case ParseState::ParsingKeyword: {
            // keyword = key (sep type)?
            Keyword keyword { .key = MUST(String::from_utf8(*segment)) };
            Vector<StringView> keyword_values;

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

                keyword_values.append(*type);
            }

            StringBuilder builder;
            builder.join('-', keyword_values);
            keyword.value = MUST(builder.to_string());

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
            TransformedField field { .key = MUST(String::from_utf8(*segment)) };
            Vector<StringView> field_values;

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

                field_values.append(*value);
            }

            if (field_values.is_empty())
                return {};

            StringBuilder builder;
            builder.join('-', field_values);
            field.value = MUST(builder.to_string());

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
    Vector<StringView> other_values;

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

        other_values.append(*segment);
    }

    if (other_values.is_empty())
        return {};

    StringBuilder builder;
    builder.join('-', other_values);
    other_extension.value = MUST(builder.to_string());

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

    auto parse_values = [&]() {
        Vector<String> extensions;

        while (true) {
            auto segment = consume_next_segment(lexer);
            if (!segment.has_value())
                break;

            if ((segment->length() < 1) || (segment->length() > 8) || !all_of(*segment, is_ascii_alphanumeric)) {
                lexer.retreat(segment->length() + 1);
                break;
            }

            extensions.append(MUST(String::from_utf8(*segment)));
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

static void perform_hard_coded_key_value_substitutions(StringView key, String& value)
{
    // FIXME: In the XML export of CLDR, there are some aliases defined in the following files:
    // https://github.com/unicode-org/cldr-staging/blob/master/production/common/bcp47/calendar.xml
    // https://github.com/unicode-org/cldr-staging/blob/master/production/common/bcp47/collation.xml
    // https://github.com/unicode-org/cldr-staging/blob/master/production/common/bcp47/measure.xml
    // https://github.com/unicode-org/cldr-staging/blob/master/production/common/bcp47/timezone.xml
    // https://github.com/unicode-org/cldr-staging/blob/master/production/common/bcp47/transform.xml
    //
    // There isn't yet a counterpart in the JSON export. See: https://unicode-org.atlassian.net/browse/CLDR-14571
    Optional<StringView> result;

    if (key == "ca"sv) {
        if (value == "islamicc"sv)
            result = "islamic-civil"sv;
        else if (value == "ethiopic-amete-alem"sv)
            result = "ethioaa"sv;
    } else if (key.is_one_of("kb"sv, "kc"sv, "kh"sv, "kk"sv, "kn"sv) && (value == "yes"sv)) {
        result = "true"sv;
    } else if (key == "ks"sv) {
        if (value == "primary"sv)
            result = "level1"sv;
        else if (value == "tertiary"sv)
            result = "level3"sv;
        // Note: There are also aliases for "secondary", "quaternary", "quarternary", and "identical",
        // but those are semantically incorrect values (they are too long), so they can be skipped.
    } else if ((key == "m0"sv) && (value == "names"sv)) {
        result = "prprname"sv;
    } else if ((key == "ms"sv) && (value == "imperial"sv)) {
        result = "uksystem"sv;
    } else if (key == "tz"sv) {
        // Formatter disabled because this block is easier to read / check against timezone.xml as one-liners.
        // clang-format off
        if (value == "aqams"sv) result = "nzakl"sv;
        else if (value == "cnckg"sv) result = "cnsha"sv;
        else if (value == "cnhrb"sv) result = "cnsha"sv;
        else if (value == "cnkhg"sv) result = "cnurc"sv;
        else if (value == "cuba"sv) result = "cuhav"sv;
        else if (value == "egypt"sv) result = "egcai"sv;
        else if (value == "eire"sv) result = "iedub"sv;
        else if (value == "est"sv) result = "utcw05"sv;
        else if (value == "gmt0"sv) result = "gmt"sv;
        else if (value == "hongkong"sv) result = "hkhkg"sv;
        else if (value == "hst"sv) result = "utcw10"sv;
        else if (value == "iceland"sv) result = "isrey"sv;
        else if (value == "iran"sv) result = "irthr"sv;
        else if (value == "israel"sv) result = "jeruslm"sv;
        else if (value == "jamaica"sv) result = "jmkin"sv;
        else if (value == "japan"sv) result = "jptyo"sv;
        else if (value == "kwajalein"sv) result = "mhkwa"sv;
        else if (value == "libya"sv) result = "lytip"sv;
        else if (value == "mst"sv) result = "utcw07"sv;
        else if (value == "navajo"sv) result = "usden"sv;
        else if (value == "poland"sv) result = "plwaw"sv;
        else if (value == "portugal"sv) result = "ptlis"sv;
        else if (value == "prc"sv) result = "cnsha"sv;
        else if (value == "roc"sv) result = "twtpe"sv;
        else if (value == "rok"sv) result = "krsel"sv;
        else if (value == "singapore"sv) result = "sgsin"sv;
        else if (value == "turkey"sv) result = "trist"sv;
        else if (value == "uct"sv) result = "utc"sv;
        else if (value == "usnavajo"sv) result = "usden"sv;
        else if (value == "zulu"sv) result = "utc"sv;
        // clang-format on
    }

    if (result.has_value())
        value = MUST(String::from_utf8(*result));
}

void canonicalize_unicode_extension_values(StringView key, String& value, bool remove_true)
{
    value = MUST(value.to_lowercase());
    perform_hard_coded_key_value_substitutions(key, value);

    // Note: The spec says to remove "true" type and tfield values but that is believed to be a bug in the spec
    // because, for tvalues, that would result in invalid syntax:
    //     https://unicode-org.atlassian.net/browse/CLDR-14318
    // This has also been noted by test262:
    //     https://github.com/tc39/test262/blob/18bb955771669541c56c28748603f6afdb2e25ff/test/intl402/Intl/getCanonicalLocales/transformed-ext-canonical.js
    if (remove_true && (value == "true"sv)) {
        value = {};
        return;
    }

    if (key.is_one_of("sd"sv, "rg"sv)) {
        if (auto alias = resolve_subdivision_alias(value); alias.has_value()) {
            auto aliases = alias->split_view(' ');

            // FIXME: Subdivision subtags do not appear in the CLDR likelySubtags.json file.
            //        Implement the spec's recommendation of using just the first alias for now,
            //        but we should determine if there's anything else needed here.
            value = MUST(String::from_utf8(aliases[0]));
        }
    }
}

static void transform_unicode_locale_id_to_canonical_syntax(LocaleID& locale_id)
{
    auto canonicalize_language = [&](LanguageID& language_id, bool force_lowercase) {
        language_id.language = MUST(language_id.language->to_lowercase());
        if (language_id.script.has_value())
            language_id.script = MUST(language_id.script->to_titlecase());
        if (language_id.region.has_value())
            language_id.region = MUST(language_id.region->to_uppercase());
        for (auto& variant : language_id.variants)
            variant = MUST(variant.to_lowercase());

        resolve_complex_language_aliases(language_id);

        if (auto alias = resolve_language_alias(*language_id.language); alias.has_value()) {
            auto language_alias = parse_unicode_language_id(*alias);
            VERIFY(language_alias.has_value());

            language_id.language = move(language_alias->language);
            if (!language_id.script.has_value() && language_alias->script.has_value())
                language_id.script = move(language_alias->script);
            if (!language_id.region.has_value() && language_alias->region.has_value())
                language_id.region = move(language_alias->region);
            if (language_id.variants.is_empty() && !language_alias->variants.is_empty())
                language_id.variants = move(language_alias->variants);
        }

        if (language_id.script.has_value()) {
            if (auto alias = resolve_script_tag_alias(*language_id.script); alias.has_value())
                language_id.script = MUST(String::from_utf8(*alias));
        }

        if (language_id.region.has_value()) {
            if (auto alias = resolve_territory_alias(*language_id.region); alias.has_value())
                language_id.region = resolve_most_likely_territory_alias(language_id, *alias);
        }

        quick_sort(language_id.variants);

        for (auto& variant : language_id.variants) {
            variant = MUST(variant.to_lowercase());
            if (auto alias = resolve_variant_alias(variant); alias.has_value())
                variant = MUST(String::from_utf8(*alias));
        }

        if (force_lowercase) {
            if (language_id.script.has_value())
                language_id.script = MUST(language_id.script->to_lowercase());
            if (language_id.region.has_value())
                language_id.region = MUST(language_id.region->to_lowercase());
        }
    };

    canonicalize_language(locale_id.language_id, false);

    quick_sort(locale_id.extensions, [](auto const& left, auto const& right) {
        auto key = [](auto const& extension) {
            return extension.visit(
                [](LocaleExtension const&) { return 'u'; },
                [](TransformedExtension const&) { return 't'; },
                [](OtherExtension const& ext) { return static_cast<char>(to_ascii_lowercase(ext.key)); });
        };

        return key(left) < key(right);
    });

    for (auto& extension : locale_id.extensions) {
        extension.visit(
            [&](LocaleExtension& ext) {
                for (auto& attribute : ext.attributes)
                    attribute = MUST(attribute.to_lowercase());

                for (auto& keyword : ext.keywords) {
                    keyword.key = MUST(keyword.key.to_lowercase());
                    canonicalize_unicode_extension_values(keyword.key, keyword.value, true);
                }

                quick_sort(ext.attributes);
                quick_sort(ext.keywords, [](auto const& a, auto const& b) { return a.key < b.key; });
            },
            [&](TransformedExtension& ext) {
                if (ext.language.has_value())
                    canonicalize_language(*ext.language, true);

                for (auto& field : ext.fields) {
                    field.key = MUST(field.key.to_lowercase());
                    canonicalize_unicode_extension_values(field.key, field.value, false);
                }

                quick_sort(ext.fields, [](auto const& a, auto const& b) { return a.key < b.key; });
            },
            [&](OtherExtension& ext) {
                ext.key = static_cast<char>(to_ascii_lowercase(ext.key));
                ext.value = MUST(ext.value.to_lowercase());
            });
    }

    for (auto& extension : locale_id.private_use_extensions)
        extension = MUST(extension.to_lowercase());
}

Optional<String> canonicalize_unicode_locale_id(LocaleID& locale_id)
{
    // https://unicode.org/reports/tr35/#Canonical_Unicode_Locale_Identifiers
    StringBuilder builder;

    auto append_sep_and_string = [&](Optional<String> const& string) {
        if (!string.has_value() || string->is_empty())
            return;
        builder.appendff("-{}", *string);
    };

    if (!locale_id.language_id.language.has_value())
        return {};

    transform_unicode_locale_id_to_canonical_syntax(locale_id);

    builder.append(MUST(locale_id.language_id.language->to_lowercase()));
    append_sep_and_string(locale_id.language_id.script);
    append_sep_and_string(locale_id.language_id.region);
    for (auto const& variant : locale_id.language_id.variants)
        append_sep_and_string(variant);

    for (auto const& extension : locale_id.extensions) {
        extension.visit(
            [&](LocaleExtension const& ext) {
                builder.append("-u"sv);

                for (auto const& attribute : ext.attributes)
                    append_sep_and_string(attribute);
                for (auto const& keyword : ext.keywords) {
                    append_sep_and_string(keyword.key);
                    append_sep_and_string(keyword.value);
                }
            },
            [&](TransformedExtension const& ext) {
                builder.append("-t"sv);

                if (ext.language.has_value()) {
                    append_sep_and_string(ext.language->language);
                    append_sep_and_string(ext.language->script);
                    append_sep_and_string(ext.language->region);
                    for (auto const& variant : ext.language->variants)
                        append_sep_and_string(variant);
                }

                for (auto const& field : ext.fields) {
                    append_sep_and_string(field.key);
                    append_sep_and_string(field.value);
                }
            },
            [&](OtherExtension const& ext) {
                builder.appendff("-{:c}", to_ascii_lowercase(ext.key));
                append_sep_and_string(ext.value);
            });
    }

    if (!locale_id.private_use_extensions.is_empty()) {
        builder.append("-x"sv);
        for (auto const& extension : locale_id.private_use_extensions)
            append_sep_and_string(extension);
    }

    return MUST(builder.to_string());
}

StringView default_locale()
{
    return "en"sv;
}

bool is_locale_available(StringView locale)
{
    return locale_from_string(locale).has_value();
}

Style style_from_string(StringView style)
{
    if (style == "narrow"sv)
        return Style::Narrow;
    if (style == "short"sv)
        return Style::Short;
    if (style == "long"sv)
        return Style::Long;
    VERIFY_NOT_REACHED();
}

StringView style_to_string(Style style)
{
    switch (style) {
    case Style::Narrow:
        return "narrow"sv;
    case Style::Short:
        return "short"sv;
    case Style::Long:
        return "long"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

ReadonlySpan<StringView> __attribute__((weak)) get_available_keyword_values(StringView) { return {}; }
ReadonlySpan<StringView> __attribute__((weak)) get_available_calendars() { return {}; }
ReadonlySpan<StringView> __attribute__((weak)) get_available_collation_case_orderings() { return {}; }
ReadonlySpan<StringView> __attribute__((weak)) get_available_collation_numeric_orderings() { return {}; }
ReadonlySpan<StringView> __attribute__((weak)) get_available_collation_types() { return {}; }
ReadonlySpan<StringView> __attribute__((weak)) get_available_currencies() { return {}; }
ReadonlySpan<StringView> __attribute__((weak)) get_available_hour_cycles() { return {}; }
ReadonlySpan<StringView> __attribute__((weak)) get_available_number_systems() { return {}; }
Optional<Locale> __attribute__((weak)) locale_from_string(StringView) { return {}; }
Optional<Language> __attribute__((weak)) language_from_string(StringView) { return {}; }
Optional<Territory> __attribute__((weak)) territory_from_string(StringView) { return {}; }
Optional<ScriptTag> __attribute__((weak)) script_tag_from_string(StringView) { return {}; }
Optional<Currency> __attribute__((weak)) currency_from_string(StringView) { return {}; }
Optional<DateField> __attribute__((weak)) date_field_from_string(StringView) { return {}; }
Optional<ListPatternType> __attribute__((weak)) list_pattern_type_from_string(StringView) { return {}; }
Optional<Key> __attribute__((weak)) key_from_string(StringView) { return {}; }
Optional<KeywordCalendar> __attribute__((weak)) keyword_ca_from_string(StringView) { return {}; }
Optional<KeywordCollation> __attribute__((weak)) keyword_co_from_string(StringView) { return {}; }
Optional<KeywordHours> __attribute__((weak)) keyword_hc_from_string(StringView) { return {}; }
Optional<KeywordColCaseFirst> __attribute__((weak)) keyword_kf_from_string(StringView) { return {}; }
Optional<KeywordColNumeric> __attribute__((weak)) keyword_kn_from_string(StringView) { return {}; }
Optional<KeywordNumbers> __attribute__((weak)) keyword_nu_from_string(StringView) { return {}; }
Vector<StringView> __attribute__((weak)) get_keywords_for_locale(StringView, StringView) { return {}; }
Optional<StringView> __attribute__((weak)) get_preferred_keyword_value_for_locale(StringView, StringView) { return {}; }
Optional<DisplayPattern> __attribute__((weak)) get_locale_display_patterns(StringView) { return {}; }
Optional<StringView> __attribute__((weak)) get_locale_language_mapping(StringView, StringView) { return {}; }
Optional<StringView> __attribute__((weak)) get_locale_territory_mapping(StringView, StringView) { return {}; }
Optional<StringView> __attribute__((weak)) get_locale_script_mapping(StringView, StringView) { return {}; }
Optional<StringView> __attribute__((weak)) get_locale_long_currency_mapping(StringView, StringView) { return {}; }
Optional<StringView> __attribute__((weak)) get_locale_short_currency_mapping(StringView, StringView) { return {}; }
Optional<StringView> __attribute__((weak)) get_locale_narrow_currency_mapping(StringView, StringView) { return {}; }
Optional<StringView> __attribute__((weak)) get_locale_numeric_currency_mapping(StringView, StringView) { return {}; }
Optional<StringView> __attribute__((weak)) get_locale_calendar_mapping(StringView, StringView) { return {}; }
Optional<StringView> __attribute__((weak)) get_locale_long_date_field_mapping(StringView, StringView) { return {}; }
Optional<StringView> __attribute__((weak)) get_locale_short_date_field_mapping(StringView, StringView) { return {}; }
Optional<StringView> __attribute__((weak)) get_locale_narrow_date_field_mapping(StringView, StringView) { return {}; }

// https://www.unicode.org/reports/tr35/tr35-39/tr35-general.html#Display_Name_Elements
Optional<String> format_locale_for_display(StringView locale, LocaleID locale_id)
{
    auto language_id = move(locale_id.language_id);
    VERIFY(language_id.language.has_value());

    auto patterns = get_locale_display_patterns(locale);
    if (!patterns.has_value())
        return {};

    auto primary_tag = get_locale_language_mapping(locale, *language_id.language).value_or(*language_id.language);
    Optional<StringView> script;
    Optional<StringView> region;

    if (language_id.script.has_value())
        script = get_locale_script_mapping(locale, *language_id.script).value_or(*language_id.script);
    if (language_id.region.has_value())
        region = get_locale_territory_mapping(locale, *language_id.region).value_or(*language_id.region);

    Optional<String> secondary_tag;

    if (script.has_value() && region.has_value()) {
        secondary_tag = MUST(String::from_utf8(patterns->locale_separator));
        secondary_tag = MUST(secondary_tag->replace("{0}"sv, *script, ReplaceMode::FirstOnly));
        secondary_tag = MUST(secondary_tag->replace("{1}"sv, *region, ReplaceMode::FirstOnly));
    } else if (script.has_value()) {
        secondary_tag = MUST(String::from_utf8(*script));
    } else if (region.has_value()) {
        secondary_tag = MUST(String::from_utf8(*region));
    }

    if (!secondary_tag.has_value())
        return MUST(String::from_utf8(primary_tag));

    auto result = MUST(String::from_utf8(patterns->locale_pattern));
    result = MUST(result.replace("{0}"sv, primary_tag, ReplaceMode::FirstOnly));
    result = MUST(result.replace("{1}"sv, *secondary_tag, ReplaceMode::FirstOnly));

    return result;
}

Optional<ListPatterns> __attribute__((weak)) get_locale_list_patterns(StringView, StringView, Style) { return {}; }
Optional<CharacterOrder> __attribute__((weak)) character_order_from_string(StringView) { return {}; }
StringView __attribute__((weak)) character_order_to_string(CharacterOrder) { return {}; }
Optional<CharacterOrder> __attribute__((weak)) character_order_for_locale(StringView) { return {}; }
Optional<StringView> __attribute__((weak)) resolve_language_alias(StringView) { return {}; }
Optional<StringView> __attribute__((weak)) resolve_territory_alias(StringView) { return {}; }
Optional<StringView> __attribute__((weak)) resolve_script_tag_alias(StringView) { return {}; }
Optional<StringView> __attribute__((weak)) resolve_variant_alias(StringView) { return {}; }
Optional<StringView> __attribute__((weak)) resolve_subdivision_alias(StringView) { return {}; }
void __attribute__((weak)) resolve_complex_language_aliases(LanguageID&) { }
Optional<LanguageID> __attribute__((weak)) add_likely_subtags(LanguageID const&) { return {}; }

Optional<LanguageID> remove_likely_subtags(LanguageID const& language_id)
{
    // https://www.unicode.org/reports/tr35/#Likely_Subtags
    auto return_language_and_variants = [](auto language, auto variants) {
        language.variants = move(variants);
        return language;
    };

    // 1. First get max = AddLikelySubtags(inputLocale). If an error is signaled, return it.
    auto maximized = add_likely_subtags(language_id);
    if (!maximized.has_value())
        return {};

    // 2. Remove the variants from max.
    auto variants = move(maximized->variants);

    // 3. Get the components of the max (languagemax, scriptmax, regionmax).
    auto language_max = maximized->language;
    auto script_max = maximized->script;
    auto region_max = maximized->region;

    // 4. Then for trial in {languagemax, languagemax_regionmax, languagemax_scriptmax}:
    //    If AddLikelySubtags(trial) = max, then return trial + variants.
    auto run_trial = [&](Optional<String> language, Optional<String> script, Optional<String> region) -> Optional<LanguageID> {
        LanguageID trial { .language = move(language), .script = move(script), .region = move(region) };

        if (add_likely_subtags(trial) == maximized)
            return return_language_and_variants(move(trial), move(variants));
        return {};
    };

    if (auto trial = run_trial(language_max, {}, {}); trial.has_value())
        return trial;
    if (auto trial = run_trial(language_max, {}, region_max); trial.has_value())
        return trial;
    if (auto trial = run_trial(language_max, script_max, {}); trial.has_value())
        return trial;

    // 5. If you do not get a match, return max + variants.
    return return_language_and_variants(maximized.release_value(), move(variants));
}

Optional<String> __attribute__((weak)) resolve_most_likely_territory(LanguageID const&) { return {}; }

String resolve_most_likely_territory_alias(LanguageID const& language_id, StringView territory_alias)
{
    auto aliases = territory_alias.split_view(' ');

    if (aliases.size() > 1) {
        auto territory = resolve_most_likely_territory(language_id);
        if (territory.has_value() && aliases.contains_slow(*territory))
            return territory.release_value();
    }

    return MUST(String::from_utf8(aliases[0]));
}

String LanguageID::to_string() const
{
    StringBuilder builder;

    auto append_segment = [&](Optional<String> const& segment) {
        if (!segment.has_value())
            return;
        if (!builder.is_empty())
            builder.append('-');
        builder.append(*segment);
    };

    append_segment(language);
    append_segment(script);
    append_segment(region);
    for (auto const& variant : variants)
        append_segment(variant);

    return MUST(builder.to_string());
}

String LocaleID::to_string() const
{
    StringBuilder builder;

    auto append_segment = [&](auto const& segment) {
        if (segment.is_empty())
            return;
        if (!builder.is_empty())
            builder.append('-');
        builder.append(segment);
    };

    append_segment(language_id.to_string());

    for (auto const& extension : extensions) {
        extension.visit(
            [&](LocaleExtension const& ext) {
                builder.append("-u"sv);
                for (auto const& attribute : ext.attributes)
                    append_segment(attribute);
                for (auto const& keyword : ext.keywords) {
                    append_segment(keyword.key);
                    append_segment(keyword.value);
                }
            },
            [&](TransformedExtension const& ext) {
                builder.append("-t"sv);
                if (ext.language.has_value())
                    append_segment(ext.language->to_string());
                for (auto const& field : ext.fields) {
                    append_segment(field.key);
                    append_segment(field.value);
                }
            },
            [&](OtherExtension const& ext) {
                builder.appendff("-{}", ext.key);
                append_segment(ext.value);
            });
    }

    if (!private_use_extensions.is_empty()) {
        builder.append("-x"sv);
        for (auto const& extension : private_use_extensions)
            append_segment(extension);
    }

    return MUST(builder.to_string());
}

}
