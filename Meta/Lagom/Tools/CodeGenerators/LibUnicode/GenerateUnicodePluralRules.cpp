/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/Format.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Variant.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/Stream.h>
#include <LibUnicode/PluralRules.h>

using StringIndexType = u16;

static String format_identifier(StringView owner, String identifier)
{
    identifier = identifier.replace("-"sv, "_"sv, ReplaceMode::All);

    if (all_of(identifier, is_ascii_digit))
        return String::formatted("{}_{}", owner[0], identifier);
    if (is_ascii_lower_alpha(identifier[0]))
        return String::formatted("{:c}{}", to_ascii_uppercase(identifier[0]), identifier.substring_view(1));
    return identifier;
}

struct Relation {
    using Range = Array<u32, 2>;
    using Comparator = Variant<u32, Range>;

    enum class Type {
        Equality,
        Inequality,
    };

    String const& modulus_variable_name() const
    {
        VERIFY(modulus.has_value());

        if (!cached_modulus_variable_name.has_value())
            cached_modulus_variable_name = String::formatted("mod_{}_{}", symbol, *modulus);

        return *cached_modulus_variable_name;
    }

    String const& exponential_variable_name() const
    {
        if (!cached_exponential_variable_name.has_value())
            cached_exponential_variable_name = String::formatted("exp_{}", symbol);

        return *cached_exponential_variable_name;
    }

    void generate_relation(SourceGenerator& generator) const
    {
        auto append_variable_name = [&]() {
            if (modulus.has_value())
                generator.append(modulus_variable_name());
            else if (symbol == 'e' || symbol == 'c')
                generator.append(exponential_variable_name());
            else
                generator.append(String::formatted("ops.{}", Unicode::PluralOperands::symbol_to_variable_name(symbol)));
        };

        auto append_value = [&](u32 value) {
            append_variable_name();
            generator.append(" == "sv);
            generator.append(String::number(value));
        };

        auto append_range = [&](auto const& range) {
            // This check avoids generating "0 <= unsigned_value", which is always true.
            if (range[0] != 0 || Unicode::PluralOperands::symbol_requires_floating_point_modulus(symbol)) {
                generator.append(String::formatted("{} <= ", range[0]));
                append_variable_name();
                generator.append(" && "sv);
            }

            append_variable_name();
            generator.append(String::formatted(" <= {}", range[1]));
        };

        if (type == Type::Inequality)
            generator.append("!"sv);

        generator.append("("sv);

        bool first = true;
        for (auto const& comparator : comparators) {
            generator.append(first ? "("sv : " || ("sv);

            comparator.visit(
                [&](u32 value) { append_value(value); },
                [&](Range const& range) { append_range(range); });

            generator.append(")"sv);
            first = false;
        }

        generator.append(")"sv);
    }

    void generate_precomputed_variables(SourceGenerator& generator, HashTable<String>& generated_variables) const
    {
        // FIXME: How do we handle the exponential symbols? They seem unused by ECMA-402.
        if (symbol == 'e' || symbol == 'c') {
            if (auto variable = exponential_variable_name(); !generated_variables.contains(variable)) {
                generated_variables.set(variable);
                generator.set("variable"sv, move(variable));
                generator.append(R"~~~(
    auto @variable@ = 0;)~~~");
            }
        }

        if (!modulus.has_value())
            return;

        auto variable = modulus_variable_name();
        if (generated_variables.contains(variable))
            return;

        generated_variables.set(variable);
        generator.set("variable"sv, move(variable));
        generator.set("operand"sv, Unicode::PluralOperands::symbol_to_variable_name(symbol));
        generator.set("modulus"sv, String::number(*modulus));

        if (Unicode::PluralOperands::symbol_requires_floating_point_modulus(symbol)) {
            generator.append(R"~~~(
    auto @variable@ = fmod(ops.@operand@, @modulus@);)~~~");
        } else {
            generator.append(R"~~~(
    auto @variable@ = ops.@operand@ % @modulus@;)~~~");
        }
    }

    Type type;
    char symbol { 0 };
    Optional<u32> modulus;
    Vector<Comparator> comparators;

private:
    mutable Optional<String> cached_modulus_variable_name;
    mutable Optional<String> cached_exponential_variable_name;
};

struct Condition {
    void generate_condition(SourceGenerator& generator) const
    {
        for (size_t i = 0; i < relations.size(); ++i) {
            if (i > 0)
                generator.append(" || "sv);

            auto const& conjunctions = relations[i];
            if (conjunctions.size() > 1)
                generator.append("("sv);

            for (size_t j = 0; j < conjunctions.size(); ++j) {
                if (j > 0)
                    generator.append(" && "sv);
                conjunctions[j].generate_relation(generator);
            }

            if (conjunctions.size() > 1)
                generator.append(")"sv);
        }
    }

    void generate_precomputed_variables(SourceGenerator& generator, HashTable<String>& generated_variables) const
    {
        for (auto const& conjunctions : relations) {
            for (auto const& relation : conjunctions)
                relation.generate_precomputed_variables(generator, generated_variables);
        }
    }

    Vector<Vector<Relation>> relations;
};

struct Locale {
    static String generated_method_name(StringView form, StringView locale)
    {
        return String::formatted("{}_plurality_{}", form, format_identifier({}, locale));
    }

    HashMap<String, Condition>& rules_for_form(StringView form)
    {
        if (form == "cardinal")
            return cardinal_rules;
        if (form == "ordinal")
            return ordinal_rules;
        VERIFY_NOT_REACHED();
    }

    HashMap<String, Condition> cardinal_rules;
    HashMap<String, Condition> ordinal_rules;
};

struct UnicodeLocaleData {
    UniqueStringStorage<StringIndexType> unique_strings;

    HashMap<String, Locale> locales;
    Vector<String> categories;
};

static Relation parse_relation(StringView relation)
{
    static constexpr auto equality_operator = " = "sv;
    static constexpr auto inequality_operator = " != "sv;
    static constexpr auto modulus_operator = " % "sv;
    static constexpr auto range_operator = ".."sv;
    static constexpr auto set_operator = ',';

    Relation parsed;

    StringView lhs;
    StringView rhs;

    if (auto index = relation.find(equality_operator); index.has_value()) {
        parsed.type = Relation::Type::Equality;
        lhs = relation.substring_view(0, *index);
        rhs = relation.substring_view(*index + equality_operator.length());
    } else if (auto index = relation.find(inequality_operator); index.has_value()) {
        parsed.type = Relation::Type::Inequality;
        lhs = relation.substring_view(0, *index);
        rhs = relation.substring_view(*index + inequality_operator.length());
    } else {
        VERIFY_NOT_REACHED();
    }

    if (auto index = lhs.find(modulus_operator); index.has_value()) {
        auto symbol = lhs.substring_view(0, *index);
        VERIFY(symbol.length() == 1);

        auto modulus = lhs.substring_view(*index + modulus_operator.length()).to_uint();
        VERIFY(modulus.has_value());

        parsed.symbol = symbol[0];
        parsed.modulus = move(modulus);
    } else {
        VERIFY(lhs.length() == 1);
        parsed.symbol = lhs[0];
    }

    rhs.for_each_split_view(set_operator, false, [&](auto set) {
        if (auto index = set.find(range_operator); index.has_value()) {
            auto range_begin = set.substring_view(0, *index).to_uint();
            VERIFY(range_begin.has_value());

            auto range_end = set.substring_view(*index + range_operator.length()).to_uint();
            VERIFY(range_end.has_value());

            parsed.comparators.empend(Array { *range_begin, *range_end });
        } else {
            auto value = set.to_uint();
            VERIFY(value.has_value());

            parsed.comparators.empend(*value);
        }
    });

    return parsed;
}

// https://unicode.org/reports/tr35/tr35-numbers.html#Plural_rules_syntax
//
// A very simplified view of a plural rule is:
//
//    condition.* ([@integer|@decimal] sample)+
//
// The "sample" being series of integer or decimal values that fit the specified condition. The
// condition may be one or more binary expressions, chained together with "and" or "or" operators.
static void parse_condition(StringView category, StringView rule, HashMap<String, Condition>& rules)
{
    static constexpr auto other_category = "other"sv;
    static constexpr auto disjunction_keyword = " or "sv;
    static constexpr auto conjunction_keyword = " and "sv;

    // We don't need the examples in the generated code, so we can drop them here.
    auto example_index = rule.find('@');
    VERIFY(example_index.has_value());

    auto condition = rule.substring_view(0, *example_index).trim_whitespace();

    // Our implementation does not generate rules for the "other" category. We simply return "other"
    // for values that do not match any rules. This will need to be revisited if this VERIFY fails.
    if (condition.is_empty()) {
        VERIFY(category == other_category);
        return;
    }

    auto& relation_list = rules.ensure(category);

    // The grammar for a condition (i.e. a chain of relations) is:
    //
    //     condition     = and_condition ('or' and_condition)*
    //     and_condition = relation ('and' relation)*
    //
    // This affords some simplicity in that disjunctions are never embedded within a conjunction.
    condition.for_each_split_view(disjunction_keyword, false, [&](auto disjunction) {
        Vector<Relation> conjunctions;

        disjunction.for_each_split_view(conjunction_keyword, false, [&](auto relation) {
            conjunctions.append(parse_relation(relation));
        });

        relation_list.relations.append(move(conjunctions));
    });
}

static ErrorOr<void> parse_plural_rules(String core_supplemental_path, StringView file_name, UnicodeLocaleData& locale_data)
{
    static constexpr auto form_prefix = "plurals-type-"sv;
    static constexpr auto rule_prefix = "pluralRule-count-"sv;

    LexicalPath plurals_path(move(core_supplemental_path));
    plurals_path = plurals_path.append(file_name);

    auto plurals = TRY(read_json_file(plurals_path.string()));
    auto const& supplemental_object = plurals.as_object().get("supplemental"sv);

    supplemental_object.as_object().for_each_member([&](auto const& key, auto const& plurals_object) {
        if (!key.starts_with(form_prefix))
            return;

        auto form = key.substring_view(form_prefix.length());

        plurals_object.as_object().for_each_member([&](auto const& loc, auto const& rules) {
            auto locale = locale_data.locales.get(loc);
            if (!locale.has_value())
                return;

            rules.as_object().for_each_member([&](auto const& key, auto const& condition) {
                VERIFY(key.starts_with(rule_prefix));

                auto category = key.substring_view(rule_prefix.length());
                parse_condition(category, condition.as_string(), locale->rules_for_form(form));

                if (!locale_data.categories.contains_slow(category))
                    locale_data.categories.append(category);
            });
        });
    });

    return {};
}

static ErrorOr<void> parse_all_locales(String core_path, String locale_names_path, UnicodeLocaleData& locale_data)
{
    auto identity_iterator = TRY(path_to_dir_iterator(move(locale_names_path)));

    LexicalPath core_supplemental_path(move(core_path));
    core_supplemental_path = core_supplemental_path.append("supplemental"sv);
    VERIFY(Core::File::is_directory(core_supplemental_path.string()));

    auto remove_variants_from_path = [&](String path) -> ErrorOr<String> {
        auto parsed_locale = TRY(CanonicalLanguageID<StringIndexType>::parse(locale_data.unique_strings, LexicalPath::basename(path)));

        StringBuilder builder;
        builder.append(locale_data.unique_strings.get(parsed_locale.language));
        if (auto script = locale_data.unique_strings.get(parsed_locale.script); !script.is_empty())
            builder.appendff("-{}", script);
        if (auto region = locale_data.unique_strings.get(parsed_locale.region); !region.is_empty())
            builder.appendff("-{}", region);

        return builder.build();
    };

    while (identity_iterator.has_next()) {
        auto locale_path = TRY(next_path_from_dir_iterator(identity_iterator));
        auto language = TRY(remove_variants_from_path(locale_path));

        locale_data.locales.ensure(language);
    }

    TRY(parse_plural_rules(core_supplemental_path.string(), "plurals.json"sv, locale_data));
    TRY(parse_plural_rules(core_supplemental_path.string(), "ordinals.json"sv, locale_data));
    return {};
}

static ErrorOr<void> generate_unicode_locale_header(Core::Stream::BufferedFile& file, UnicodeLocaleData& locale_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#include <AK/Types.h>

#pragma once

namespace Unicode {
)~~~");

    generate_enum(generator, format_identifier, "PluralCategory"sv, {}, locale_data.categories);

    generator.append(R"~~~(
}
)~~~");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}

static ErrorOr<void> generate_unicode_locale_implementation(Core::Stream::BufferedFile& file, UnicodeLocaleData& locale_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    auto locales = locale_data.locales.keys();
    quick_sort(locales);

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/BinarySearch.h>
#include <AK/StringView.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/PluralRules.h>
#include <LibUnicode/UnicodeLocale.h>
#include <LibUnicode/UnicodePluralRules.h>
#include <math.h>

namespace Unicode {

using PluralCategoryFunction = PluralCategory(*)(PluralOperands);

static PluralCategory default_category(PluralOperands)
{
    return PluralCategory::Other;
}

)~~~");

    auto append_string_conversions = [&](StringView enum_title, StringView enum_snake, auto const& values) {
        HashValueMap<String> hashes;
        hashes.ensure_capacity(values.size());

        for (auto const& value : values)
            hashes.set(value.hash(), format_identifier(enum_title, value));

        generate_value_from_string(generator, "{}_from_string"sv, enum_title, enum_snake, move(hashes));
        generate_value_to_string(generator, "{}_to_string"sv, enum_title, enum_snake, format_identifier, values);
    };

    auto append_rules = [&](auto form, auto const& locale, auto const& rules) {
        if (rules.is_empty())
            return;

        generator.set("method"sv, Locale::generated_method_name(form, locale));
        HashTable<String> generated_variables;

        generator.append(R"~~~(
static PluralCategory @method@([[maybe_unused]] PluralOperands ops)
{)~~~");

        for (auto [category, condition] : rules) {
            condition.generate_precomputed_variables(generator, generated_variables);

            generator.append(R"~~~(
    if ()~~~");

            generator.set("category"sv, format_identifier({}, category));
            condition.generate_condition(generator);

            generator.append(R"~~~()
        return PluralCategory::@category@;)~~~");
        }

        generator.append(R"~~~(
    return PluralCategory::Other;
}
)~~~");
    };

    auto append_lookup_table = [&](auto form) {
        generator.set("form"sv, form);
        generator.set("size"sv, String::number(locales.size()));

        generator.append(R"~~~(
static constexpr Array<PluralCategoryFunction, @size@> s_@form@_functions { {)~~~");

        for (auto const& locale : locales) {
            auto& rules = locale_data.locales.find(locale)->value;

            if (rules.rules_for_form(form).is_empty()) {
                generator.append(R"~~~(
    default_category,)~~~");
            } else {
                generator.set("method"sv, Locale::generated_method_name(form, locale));
                generator.append(R"~~~(
    @method@,)~~~");
            }
        }

        generator.append(R"~~~(
} };
)~~~");
    };

    append_string_conversions("PluralCategory"sv, "plural_category"sv, locale_data.categories);

    for (auto [locale, rules] : locale_data.locales) {
        append_rules("cardinal"sv, locale, rules.cardinal_rules);
        append_rules("ordinal"sv, locale, rules.ordinal_rules);
    }

    append_lookup_table("cardinal"sv);
    append_lookup_table("ordinal"sv);

    generator.append(R"~~~(
PluralCategory determine_plural_category(StringView locale, PluralForm form, PluralOperands operands)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return PluralCategory::Other;

    auto locale_index = to_underlying(*locale_value) - 1; // Subtract 1 because 0 == Locale::None.
    PluralCategoryFunction decider { nullptr };

    switch (form) {
    case PluralForm::Cardinal:
        decider = s_cardinal_functions[locale_index];
        break;
    case PluralForm::Ordinal:
        decider = s_ordinal_functions[locale_index];
        break;
    }

    return decider(move(operands));
}

}
)~~~");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView core_path;
    StringView locale_names_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode locale header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode locale implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(core_path, "Path to cldr-core directory", "core-path", 'r', "core-path");
    args_parser.add_option(locale_names_path, "Path to cldr-localenames directory", "locale-names-path", 'l', "locale-names-path");
    args_parser.parse(arguments);

    auto generated_header_file = TRY(open_file(generated_header_path, Core::Stream::OpenMode::Write));
    auto generated_implementation_file = TRY(open_file(generated_implementation_path, Core::Stream::OpenMode::Write));

    UnicodeLocaleData locale_data;
    TRY(parse_all_locales(core_path, locale_names_path, locale_data));

    TRY(generate_unicode_locale_header(*generated_header_file, locale_data));
    TRY(generate_unicode_locale_implementation(*generated_implementation_file, locale_data));

    return 0;
}
