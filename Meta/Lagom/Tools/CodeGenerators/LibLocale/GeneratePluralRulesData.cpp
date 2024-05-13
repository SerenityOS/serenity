/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "../LibUnicode/GeneratorUtil.h" // FIXME: Move this somewhere common.
#include <AK/ByteString.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <AK/Variant.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Directory.h>
#include <LibFileSystem/FileSystem.h>
#include <LibLocale/PluralRules.h>

static ByteString format_identifier(StringView owner, ByteString identifier)
{
    identifier = identifier.replace("-"sv, "_"sv, ReplaceMode::All);

    if (all_of(identifier, is_ascii_digit))
        return ByteString::formatted("{}_{}", owner[0], identifier);
    if (is_ascii_lower_alpha(identifier[0]))
        return ByteString::formatted("{:c}{}", to_ascii_uppercase(identifier[0]), identifier.substring_view(1));
    return identifier;
}

struct Relation {
    using Range = Array<u32, 2>;
    using Comparator = Variant<u32, Range>;

    enum class Type {
        Equality,
        Inequality,
    };

    ByteString const& modulus_variable_name() const
    {
        VERIFY(modulus.has_value());

        if (!cached_modulus_variable_name.has_value())
            cached_modulus_variable_name = ByteString::formatted("mod_{}_{}", symbol, *modulus);

        return *cached_modulus_variable_name;
    }

    ByteString const& exponential_variable_name() const
    {
        if (!cached_exponential_variable_name.has_value())
            cached_exponential_variable_name = ByteString::formatted("exp_{}", symbol);

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
                generator.append(ByteString::formatted("ops.{}", Locale::PluralOperands::symbol_to_variable_name(symbol)));
        };

        auto append_value = [&](u32 value) {
            append_variable_name();
            generator.append(" == "sv);
            generator.append(ByteString::number(value));
        };

        auto append_range = [&](auto const& range) {
            // This check avoids generating "0 <= unsigned_value", which is always true.
            if (range[0] != 0 || Locale::PluralOperands::symbol_requires_floating_point_modulus(symbol)) {
                generator.append(ByteString::formatted("{} <= ", range[0]));
                append_variable_name();
                generator.append(" && "sv);
            }

            append_variable_name();
            generator.append(ByteString::formatted(" <= {}", range[1]));
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

    void generate_precomputed_variables(SourceGenerator& generator, HashTable<ByteString>& generated_variables) const
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
        generator.set("operand"sv, Locale::PluralOperands::symbol_to_variable_name(symbol));
        generator.set("modulus"sv, ByteString::number(*modulus));

        if (Locale::PluralOperands::symbol_requires_floating_point_modulus(symbol)) {
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
    mutable Optional<ByteString> cached_modulus_variable_name;
    mutable Optional<ByteString> cached_exponential_variable_name;
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

    void generate_precomputed_variables(SourceGenerator& generator, HashTable<ByteString>& generated_variables) const
    {
        for (auto const& conjunctions : relations) {
            for (auto const& relation : conjunctions)
                relation.generate_precomputed_variables(generator, generated_variables);
        }
    }

    Vector<Vector<Relation>> relations;
};

struct Range {
    ByteString start;
    ByteString end;
    ByteString category;
};

using Conditions = HashMap<ByteString, Condition>;
using Ranges = Vector<Range>;

struct LocaleData {
    static ByteString generated_method_name(StringView form, StringView locale)
    {
        return ByteString::formatted("{}_plurality_{}", form, format_identifier({}, locale));
    }

    Conditions& rules_for_form(StringView form)
    {
        if (form == "cardinal")
            return cardinal_rules;
        if (form == "ordinal")
            return ordinal_rules;
        VERIFY_NOT_REACHED();
    }

    Conditions cardinal_rules;
    Conditions ordinal_rules;
    Ranges plural_ranges;
};

struct CLDR {
    UniqueStringStorage unique_strings;

    HashMap<ByteString, LocaleData> locales;
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

        auto modulus = lhs.substring_view(*index + modulus_operator.length()).to_number<unsigned>();
        VERIFY(modulus.has_value());

        parsed.symbol = symbol[0];
        parsed.modulus = move(modulus);
    } else {
        VERIFY(lhs.length() == 1);
        parsed.symbol = lhs[0];
    }

    rhs.for_each_split_view(set_operator, SplitBehavior::Nothing, [&](auto set) {
        if (auto index = set.find(range_operator); index.has_value()) {
            auto range_begin = set.substring_view(0, *index).template to_number<unsigned>();
            VERIFY(range_begin.has_value());

            auto range_end = set.substring_view(*index + range_operator.length()).template to_number<unsigned>();
            VERIFY(range_end.has_value());

            parsed.comparators.empend(Array { *range_begin, *range_end });
        } else {
            auto value = set.template to_number<unsigned>();
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
static void parse_condition(StringView category, StringView rule, Conditions& rules)
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
    condition.for_each_split_view(disjunction_keyword, SplitBehavior::Nothing, [&](auto disjunction) {
        Vector<Relation> conjunctions;

        disjunction.for_each_split_view(conjunction_keyword, SplitBehavior::Nothing, [&](auto relation) {
            conjunctions.append(parse_relation(relation));
        });

        relation_list.relations.append(move(conjunctions));
    });
}

static ErrorOr<void> parse_plural_rules(ByteString core_supplemental_path, StringView file_name, CLDR& cldr)
{
    static constexpr auto form_prefix = "plurals-type-"sv;
    static constexpr auto rule_prefix = "pluralRule-count-"sv;

    LexicalPath plurals_path(move(core_supplemental_path));
    plurals_path = plurals_path.append(file_name);

    auto plurals = TRY(read_json_file(plurals_path.string()));
    auto const& supplemental_object = plurals.as_object().get_object("supplemental"sv).value();

    supplemental_object.for_each_member([&](auto const& key, auto const& plurals_object) {
        if (!key.starts_with(form_prefix))
            return;

        auto form = key.substring_view(form_prefix.length());

        plurals_object.as_object().for_each_member([&](auto const& loc, auto const& rules) {
            auto locale = cldr.locales.get(loc);
            if (!locale.has_value())
                return;

            rules.as_object().for_each_member([&](auto const& key, auto const& condition) {
                VERIFY(key.starts_with(rule_prefix));

                auto category = key.substring_view(rule_prefix.length());
                parse_condition(category, condition.as_string(), locale->rules_for_form(form));
            });
        });
    });

    return {};
}

// https://unicode.org/reports/tr35/tr35-numbers.html#Plural_Ranges
static ErrorOr<void> parse_plural_ranges(ByteString core_supplemental_path, CLDR& cldr)
{
    static constexpr auto start_segment = "-start-"sv;
    static constexpr auto end_segment = "-end-"sv;

    LexicalPath plural_ranges_path(move(core_supplemental_path));
    plural_ranges_path = plural_ranges_path.append("pluralRanges.json"sv);

    auto plural_ranges = TRY(read_json_file(plural_ranges_path.string()));
    auto const& supplemental_object = plural_ranges.as_object().get_object("supplemental"sv).value();
    auto const& plurals_object = supplemental_object.get_object("plurals"sv).value();

    plurals_object.for_each_member([&](auto const& loc, auto const& ranges_object) {
        auto locale = cldr.locales.get(loc);
        if (!locale.has_value())
            return;

        ranges_object.as_object().for_each_member([&](auto const& range, auto const& category) {
            auto start_index = range.find(start_segment);
            VERIFY(start_index.has_value());

            auto end_index = range.find(end_segment);
            VERIFY(end_index.has_value());

            *start_index += start_segment.length();

            auto start = range.substring(*start_index, *end_index - *start_index);
            auto end = range.substring(*end_index + end_segment.length());

            locale->plural_ranges.empend(move(start), move(end), category.as_string());
        });
    });

    return {};
}

static ErrorOr<void> parse_all_locales(ByteString core_path, ByteString locale_names_path, CLDR& cldr)
{
    LexicalPath core_supplemental_path(move(core_path));
    core_supplemental_path = core_supplemental_path.append("supplemental"sv);
    VERIFY(FileSystem::is_directory(core_supplemental_path.string()));

    auto remove_variants_from_path = [&](ByteString path) -> ErrorOr<ByteString> {
        auto parsed_locale = TRY(CanonicalLanguageID::parse(cldr.unique_strings, LexicalPath::basename(path)));

        StringBuilder builder;
        builder.append(cldr.unique_strings.get(parsed_locale.language));
        if (auto script = cldr.unique_strings.get(parsed_locale.script); !script.is_empty())
            builder.appendff("-{}", script);
        if (auto region = cldr.unique_strings.get(parsed_locale.region); !region.is_empty())
            builder.appendff("-{}", region);

        return builder.to_byte_string();
    };

    TRY(Core::Directory::for_each_entry(TRY(String::formatted("{}/main", locale_names_path)), Core::DirIterator::SkipParentAndBaseDir, [&](auto& entry, auto& directory) -> ErrorOr<IterationDecision> {
        auto locale_path = LexicalPath::join(directory.path().string(), entry.name).string();
        auto language = TRY(remove_variants_from_path(locale_path));

        cldr.locales.ensure(language);
        return IterationDecision::Continue;
    }));

    TRY(parse_plural_rules(core_supplemental_path.string(), "plurals.json"sv, cldr));
    TRY(parse_plural_rules(core_supplemental_path.string(), "ordinals.json"sv, cldr));
    TRY(parse_plural_ranges(core_supplemental_path.string(), cldr));
    return {};
}

static ErrorOr<void> generate_unicode_locale_header(Core::InputBufferedFile& file, CLDR&)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

#include <AK/Types.h>

namespace Locale {
)~~~");

    generator.append(R"~~~(
}
)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

static ErrorOr<void> generate_unicode_locale_implementation(Core::InputBufferedFile& file, CLDR& cldr)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    auto locales = cldr.locales.keys();
    quick_sort(locales);

    generator.append(R"~~~(
#include <AK/Array.h>
#include <LibLocale/Locale.h>
#include <LibLocale/LocaleData.h>
#include <LibLocale/PluralRules.h>
#include <LibLocale/PluralRulesData.h>
#include <math.h>

namespace Locale {

using PluralCategoryFunction = PluralCategory(*)(PluralOperands);
using PluralRangeFunction = PluralCategory(*)(PluralCategory, PluralCategory);

static PluralCategory default_category(PluralOperands)
{
    return PluralCategory::Other;
}

static PluralCategory default_range(PluralCategory, PluralCategory end)
{
    return end;
}

)~~~");

    auto append_rules = [&](auto form, auto const& locale, auto const& rules) {
        if (rules.is_empty())
            return;

        generator.set("method"sv, LocaleData::generated_method_name(form, locale));
        HashTable<ByteString> generated_variables;

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

    auto append_ranges = [&](auto const& locale, auto const& ranges) {
        if (ranges.is_empty())
            return;

        generator.set("method"sv, LocaleData::generated_method_name("range"sv, locale));

        generator.append(R"~~~(
static PluralCategory @method@(PluralCategory start, PluralCategory end)
{)~~~");

        for (auto const& range : ranges) {
            generator.set("start"sv, format_identifier({}, range.start));
            generator.set("end"sv, format_identifier({}, range.end));
            generator.set("category"sv, format_identifier({}, range.category));

            generator.append(R"~~~(
    if (start == PluralCategory::@start@ && end == PluralCategory::@end@)
        return PluralCategory::@category@;)~~~");
        }

        generator.append(R"~~~(
    return end;
}
)~~~");
    };

    auto append_lookup_table = [&](auto type, auto form, auto default_, auto data_for_locale) {
        generator.set("type"sv, type);
        generator.set("form"sv, form);
        generator.set("default"sv, default_);
        generator.set("size"sv, ByteString::number(locales.size()));

        generator.append(R"~~~(
static constexpr Array<@type@, @size@> s_@form@_functions { {)~~~");

        for (auto const& locale : locales) {
            auto& rules = data_for_locale(cldr.locales.find(locale)->value, form);

            if (rules.is_empty()) {
                generator.append(R"~~~(
    @default@,)~~~");
            } else {
                generator.set("method"sv, LocaleData::generated_method_name(form, locale));
                generator.append(R"~~~(
    @method@,)~~~");
            }
        }

        generator.append(R"~~~(
} };
)~~~");
    };

    auto append_categories = [&](auto const& name, auto const& rules) {
        generator.set("name", name);
        generator.set("size", ByteString::number(rules.size() + 1));

        generator.append(R"~~~(
static constexpr Array<PluralCategory, @size@> @name@ { { PluralCategory::Other)~~~");

        for (auto [category, condition] : rules) {
            generator.set("category"sv, format_identifier({}, category));
            generator.append(", PluralCategory::@category@"sv);
        }

        generator.append("} };");
    };

    for (auto const& [locale, rules] : cldr.locales) {
        append_rules("cardinal"sv, locale, rules.cardinal_rules);
        append_rules("ordinal"sv, locale, rules.ordinal_rules);
        append_ranges(locale, rules.plural_ranges);
    }

    append_lookup_table("PluralCategoryFunction"sv, "cardinal"sv, "default_category"sv, [](auto& rules, auto form) -> Conditions& { return rules.rules_for_form(form); });
    append_lookup_table("PluralCategoryFunction"sv, "ordinal"sv, "default_category"sv, [](auto& rules, auto form) -> Conditions& { return rules.rules_for_form(form); });
    append_lookup_table("PluralRangeFunction"sv, "range"sv, "default_range"sv, [](auto& rules, auto) -> Ranges& { return rules.plural_ranges; });

    generate_mapping(generator, locales, "PluralCategory"sv, "s_cardinal_categories"sv, "s_cardinal_categories_{}"sv, format_identifier,
        [&](auto const& name, auto const& locale) {
            auto& rules = cldr.locales.find(locale)->value;
            append_categories(name, rules.rules_for_form("cardinal"sv));
        });

    generate_mapping(generator, locales, "PluralCategory"sv, "s_ordinal_categories"sv, "s_ordinal_categories_{}"sv, format_identifier,
        [&](auto const& name, auto const& locale) {
            auto& rules = cldr.locales.find(locale)->value;
            append_categories(name, rules.rules_for_form("ordinal"sv));
        });

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

ReadonlySpan<PluralCategory> available_plural_categories(StringView locale, PluralForm form)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return {};

    auto locale_index = to_underlying(*locale_value) - 1; // Subtract 1 because 0 == Locale::None.

    switch (form) {
    case PluralForm::Cardinal:
        return s_cardinal_categories[locale_index];
    case PluralForm::Ordinal:
        return s_ordinal_categories[locale_index];
    }

    VERIFY_NOT_REACHED();
}

PluralCategory determine_plural_range(StringView locale, PluralCategory start, PluralCategory end)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return PluralCategory::Other;

    auto locale_index = to_underlying(*locale_value) - 1; // Subtract 1 because 0 == Locale::None.

    PluralRangeFunction decider = s_range_functions[locale_index];
    return decider(start, end);
}

}
)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
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

    auto generated_header_file = TRY(open_file(generated_header_path, Core::File::OpenMode::Write));
    auto generated_implementation_file = TRY(open_file(generated_implementation_path, Core::File::OpenMode::Write));

    CLDR cldr;
    TRY(parse_all_locales(core_path, locale_names_path, cldr));

    TRY(generate_unicode_locale_header(*generated_header_file, cldr));
    TRY(generate_unicode_locale_implementation(*generated_implementation_file, cldr));

    return 0;
}
