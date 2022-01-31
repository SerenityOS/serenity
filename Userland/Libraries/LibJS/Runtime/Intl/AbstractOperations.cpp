/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AllOf.h>
#include <AK/AnyOf.h>
#include <AK/CharacterTypes.h>
#include <AK/Find.h>
#include <AK/Function.h>
#include <AK/QuickSort.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/Locale.h>
#include <LibUnicode/Locale.h>

namespace JS::Intl {

// 6.2.2 IsStructurallyValidLanguageTag ( locale ), https://tc39.es/ecma402/#sec-isstructurallyvalidlanguagetag
Optional<Unicode::LocaleID> is_structurally_valid_language_tag(StringView locale)
{
    auto contains_duplicate_variant = [](auto& variants) {
        if (variants.is_empty())
            return false;

        quick_sort(variants);

        for (size_t i = 0; i < variants.size() - 1; ++i) {
            if (variants[i].equals_ignoring_case(variants[i + 1]))
                return true;
        }

        return false;
    };

    // IsStructurallyValidLanguageTag returns true if all of the following conditions hold, false otherwise:

    // locale can be generated from the EBNF grammar for unicode_locale_id in Unicode Technical Standard #35 LDML § 3.2 Unicode Locale Identifier;
    auto locale_id = Unicode::parse_unicode_locale_id(locale);
    if (!locale_id.has_value())
        return {};

    // locale does not use any of the backwards compatibility syntax described in Unicode Technical Standard #35 LDML § 3.3 BCP 47 Conformance;
    // https://unicode.org/reports/tr35/#BCP_47_Conformance
    if (locale.contains('_') || locale_id->language_id.is_root || !locale_id->language_id.language.has_value())
        return {};

    // the unicode_language_id within locale contains no duplicate unicode_variant_subtag subtags; and
    if (contains_duplicate_variant(locale_id->language_id.variants))
        return {};

    // if locale contains an extensions* component, that component
    Vector<char> unique_keys;
    for (auto& extension : locale_id->extensions) {
        // does not contain any other_extensions components with duplicate [alphanum-[tTuUxX]] subtags,
        // contains at most one unicode_locale_extensions component,
        // contains at most one transformed_extensions component, and
        char key = extension.visit(
            [](Unicode::LocaleExtension const&) { return 'u'; },
            [](Unicode::TransformedExtension const&) { return 't'; },
            [](Unicode::OtherExtension const& ext) { return static_cast<char>(to_ascii_lowercase(ext.key)); });

        if (unique_keys.contains_slow(key))
            return {};
        unique_keys.append(key);

        // if a transformed_extensions component that contains a tlang component is present, then
        // the tlang component contains no duplicate unicode_variant_subtag subtags.
        if (auto* transformed = extension.get_pointer<Unicode::TransformedExtension>()) {
            auto& language = transformed->language;
            if (language.has_value() && contains_duplicate_variant(language->variants))
                return {};
        }
    }

    return locale_id;
}

// 6.2.3 CanonicalizeUnicodeLocaleId ( locale ), https://tc39.es/ecma402/#sec-canonicalizeunicodelocaleid
String canonicalize_unicode_locale_id(Unicode::LocaleID& locale)
{
    // Note: This implementation differs from the spec in how Step 3 is implemented. The spec assumes
    // the input to this method is a string, and is written such that operations are performed on parts
    // of that string. LibUnicode gives us the parsed locale in a structure, so we can mutate that
    // structure directly. From a footnote in the spec:
    //
    // The third step of this algorithm ensures that a Unicode locale extension sequence in the
    // returned language tag contains:
    //     * only the first instance of any attribute duplicated in the input, and
    //     * only the first keyword for a given key in the input.
    for (auto& extension : locale.extensions) {
        if (!extension.has<Unicode::LocaleExtension>())
            continue;

        auto& locale_extension = extension.get<Unicode::LocaleExtension>();

        auto attributes = move(locale_extension.attributes);
        for (auto& attribute : attributes) {
            if (!locale_extension.attributes.contains_slow(attribute))
                locale_extension.attributes.append(move(attribute));
        }

        auto keywords = move(locale_extension.keywords);
        for (auto& keyword : keywords) {
            if (!any_of(locale_extension.keywords, [&](auto const& k) { return k.key == keyword.key; }))
                locale_extension.keywords.append(move(keyword));
        }

        break;
    }

    // 1. Let localeId be the string locale after performing the algorithm to transform it to canonical syntax per Unicode Technical Standard #35 LDML § 3.2.1 Canonical Unicode Locale Identifiers.
    // 2. Let localeId be the string localeId after performing the algorithm to transform it to canonical form.
    auto locale_id = Unicode::canonicalize_unicode_locale_id(locale);
    VERIFY(locale_id.has_value());

    // 4. Return localeId.
    return locale_id.release_value();
}

// 6.3.1 IsWellFormedCurrencyCode ( currency ), https://tc39.es/ecma402/#sec-canonicalcodefordisplaynames
bool is_well_formed_currency_code(StringView currency)
{
    // 1. Let normalized be the result of mapping currency to upper case as described in 6.1.
    // 2. If the number of elements in normalized is not 3, return false.
    if (currency.length() != 3)
        return false;

    // 3. If normalized contains any character that is not in the range "A" to "Z" (U+0041 to U+005A), return false.
    if (!all_of(currency, is_ascii_alpha))
        return false;

    // 4. Return true.
    return true;
}

// 6.5.1 IsWellFormedUnitIdentifier ( unitIdentifier ), https://tc39.es/ecma402/#sec-iswellformedunitidentifier
bool is_well_formed_unit_identifier(StringView unit_identifier)
{
    // 6.5.2 IsSanctionedSimpleUnitIdentifier ( unitIdentifier ), https://tc39.es/ecma402/#sec-iswellformedunitidentifier
    constexpr auto is_sanctioned_simple_unit_identifier = [](StringView unit_identifier) {
        // 1. If unitIdentifier is listed in Table 2 below, return true.
        // 2. Else, return false.
        static constexpr auto sanctioned_units = sanctioned_simple_unit_identifiers();
        return find(sanctioned_units.begin(), sanctioned_units.end(), unit_identifier) != sanctioned_units.end();
    };

    // 1. If the result of IsSanctionedSimpleUnitIdentifier(unitIdentifier) is true, then
    if (is_sanctioned_simple_unit_identifier(unit_identifier)) {
        // a. Return true.
        return true;
    }

    auto indices = unit_identifier.find_all("-per-"sv);

    // 2. If the substring "-per-" does not occur exactly once in unitIdentifier, then
    if (indices.size() != 1) {
        // a. Return false.
        return false;
    }

    // 3. Let numerator be the substring of unitIdentifier from the beginning to just before "-per-".
    auto numerator = unit_identifier.substring_view(0, indices[0]);

    // 4. If the result of IsSanctionedSimpleUnitIdentifier(numerator) is false, then
    if (!is_sanctioned_simple_unit_identifier(numerator)) {
        // a. Return false.
        return false;
    }

    // 5. Let denominator be the substring of unitIdentifier from just after "-per-" to the end.
    auto denominator = unit_identifier.substring_view(indices[0] + 5);

    // 6. If the result of IsSanctionedSimpleUnitIdentifier(denominator) is false, then
    if (!is_sanctioned_simple_unit_identifier(denominator)) {
        // a. Return false.
        return false;
    }

    // 7. Return true.
    return true;
}

// 9.2.1 CanonicalizeLocaleList ( locales ), https://tc39.es/ecma402/#sec-canonicalizelocalelist
ThrowCompletionOr<Vector<String>> canonicalize_locale_list(GlobalObject& global_object, Value locales)
{
    auto& vm = global_object.vm();

    // 1. If locales is undefined, then
    if (locales.is_undefined()) {
        // a. Return a new empty List.
        return Vector<String> {};
    }

    // 2. Let seen be a new empty List.
    Vector<String> seen;

    Object* object = nullptr;
    // 3. If Type(locales) is String or Type(locales) is Object and locales has an [[InitializedLocale]] internal slot, then
    if (locales.is_string() || (locales.is_object() && is<Locale>(locales.as_object()))) {
        // a. Let O be CreateArrayFromList(« locales »).
        object = Array::create_from(global_object, { locales });
    }
    // 4. Else,
    else {
        // a. Let O be ? ToObject(locales).
        object = TRY(locales.to_object(global_object));
    }

    // 5. Let len be ? ToLength(? Get(O, "length")).
    auto length_value = TRY(object->get(vm.names.length));
    auto length = TRY(length_value.to_length(global_object));

    // 6. Let k be 0.
    // 7. Repeat, while k < len,
    for (size_t k = 0; k < length; ++k) {
        // a. Let Pk be ToString(k).
        auto property_key = PropertyKey { k };

        // b. Let kPresent be ? HasProperty(O, Pk).
        auto key_present = TRY(object->has_property(property_key));

        // c. If kPresent is true, then
        if (key_present) {
            // i. Let kValue be ? Get(O, Pk).
            auto key_value = TRY(object->get(property_key));

            // ii. If Type(kValue) is not String or Object, throw a TypeError exception.
            if (!key_value.is_string() && !key_value.is_object())
                return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOrString, key_value.to_string_without_side_effects());

            String tag;

            // iii. If Type(kValue) is Object and kValue has an [[InitializedLocale]] internal slot, then
            if (key_value.is_object() && is<Locale>(key_value.as_object())) {
                // 1. Let tag be kValue.[[Locale]].
                tag = static_cast<Locale const&>(key_value.as_object()).locale();
            }
            // iv. Else,
            else {
                // 1. Let tag be ? ToString(kValue).
                tag = TRY(key_value.to_string(global_object));
            }

            // v. If IsStructurallyValidLanguageTag(tag) is false, throw a RangeError exception.
            auto locale_id = is_structurally_valid_language_tag(tag);
            if (!locale_id.has_value())
                return vm.throw_completion<RangeError>(global_object, ErrorType::IntlInvalidLanguageTag, tag);

            // vi. Let canonicalizedTag be CanonicalizeUnicodeLocaleId(tag).
            auto canonicalized_tag = JS::Intl::canonicalize_unicode_locale_id(*locale_id);

            // vii. If canonicalizedTag is not an element of seen, append canonicalizedTag as the last element of seen.
            if (!seen.contains_slow(canonicalized_tag))
                seen.append(move(canonicalized_tag));
        }

        // d. Increase k by 1.
    }

    return seen;
}

// 9.2.2 BestAvailableLocale ( availableLocales, locale ), https://tc39.es/ecma402/#sec-bestavailablelocale
Optional<String> best_available_locale(StringView locale)
{
    // 1. Let candidate be locale.
    StringView candidate = locale;

    // 2. Repeat,
    while (true) {
        // a. If availableLocales contains an element equal to candidate, return candidate.
        if (Unicode::is_locale_available(candidate))
            return candidate;

        // b. Let pos be the character index of the last occurrence of "-" (U+002D) within candidate. If that character does not occur, return undefined.
        auto pos = candidate.find_last('-');
        if (!pos.has_value())
            return {};

        // c. If pos ≥ 2 and the character "-" occurs at index pos-2 of candidate, decrease pos by 2.
        if ((*pos >= 2) && (candidate[*pos - 2] == '-'))
            pos = *pos - 2;

        // d. Let candidate be the substring of candidate from position 0, inclusive, to position pos, exclusive.
        candidate = candidate.substring_view(0, *pos);
    }
}

struct MatcherResult {
    String locale;
    Vector<Unicode::Extension> extensions {};
};

// 9.2.3 LookupMatcher ( availableLocales, requestedLocales ), https://tc39.es/ecma402/#sec-lookupmatcher
static MatcherResult lookup_matcher(Vector<String> const& requested_locales)
{
    // 1. Let result be a new Record.
    MatcherResult result {};

    // 2. For each element locale of requestedLocales, do
    for (auto const& locale : requested_locales) {
        auto locale_id = Unicode::parse_unicode_locale_id(locale);
        VERIFY(locale_id.has_value());

        // a. Let noExtensionsLocale be the String value that is locale with any Unicode locale extension sequences removed.
        auto extensions = locale_id->remove_extension_type<Unicode::LocaleExtension>();
        auto no_extensions_locale = locale_id->to_string();

        // b. Let availableLocale be BestAvailableLocale(availableLocales, noExtensionsLocale).
        auto available_locale = best_available_locale(no_extensions_locale);

        // c. If availableLocale is not undefined, then
        if (available_locale.has_value()) {
            // i. Set result.[[locale]] to availableLocale.
            result.locale = available_locale.release_value();

            // ii. If locale and noExtensionsLocale are not the same String value, then
            if (locale != no_extensions_locale) {
                // 1. Let extension be the String value consisting of the substring of the Unicode locale extension sequence within locale.
                // 2. Set result.[[extension]] to extension.
                result.extensions.extend(move(extensions));
            }

            // iii. Return result.
            return result;
        }
    }

    // 3. Let defLocale be DefaultLocale().
    // 4. Set result.[[locale]] to defLocale.
    result.locale = Unicode::default_locale();

    // 5. Return result.
    return result;
}

// 9.2.4 BestFitMatcher ( availableLocales, requestedLocales ), https://tc39.es/ecma402/#sec-bestfitmatcher
static MatcherResult best_fit_matcher(Vector<String> const& requested_locales)
{
    // The algorithm is implementation dependent, but should produce results that a typical user of the requested locales would
    // perceive as at least as good as those produced by the LookupMatcher abstract operation.
    return lookup_matcher(requested_locales);
}

// 9.2.6 InsertUnicodeExtensionAndCanonicalize ( locale, extension ), https://tc39.es/ecma402/#sec-insert-unicode-extension-and-canonicalize
String insert_unicode_extension_and_canonicalize(Unicode::LocaleID locale, Unicode::LocaleExtension extension)
{
    // Note: This implementation differs from the spec in how the extension is inserted. The spec assumes
    // the input to this method is a string, and is written such that operations are performed on parts
    // of that string. LibUnicode gives us the parsed locale in a structure, so we can mutate that
    // structure directly.
    locale.extensions.append(move(extension));
    return JS::Intl::canonicalize_unicode_locale_id(locale);
}

template<typename T>
static auto& find_key_in_value(T& value, StringView key)
{
    if (key == "ca"sv)
        return value.ca;
    if (key == "co"sv)
        return value.co;
    if (key == "hc"sv)
        return value.hc;
    if (key == "kf"sv)
        return value.kf;
    if (key == "kn"sv)
        return value.kn;
    if (key == "nu"sv)
        return value.nu;

    // If you hit this point, you must add any missing keys from [[RelevantExtensionKeys]] to LocaleOptions and LocaleResult.
    VERIFY_NOT_REACHED();
}

// 9.2.7 ResolveLocale ( availableLocales, requestedLocales, options, relevantExtensionKeys, localeData ), https://tc39.es/ecma402/#sec-resolvelocale
LocaleResult resolve_locale(Vector<String> const& requested_locales, LocaleOptions const& options, Span<StringView const> relevant_extension_keys)
{
    // 1. Let matcher be options.[[localeMatcher]].
    auto const& matcher = options.locale_matcher;
    MatcherResult matcher_result;

    // 2. If matcher is "lookup", then
    if (matcher.is_string() && (matcher.as_string().string() == "lookup"sv)) {
        // a. Let r be LookupMatcher(availableLocales, requestedLocales).
        matcher_result = lookup_matcher(requested_locales);
    }
    // 3. Else,
    else {
        // a. Let r be BestFitMatcher(availableLocales, requestedLocales).
        matcher_result = best_fit_matcher(requested_locales);
    }

    // 4. Let foundLocale be r.[[locale]].
    auto found_locale = move(matcher_result.locale);

    // 5. Let result be a new Record.
    LocaleResult result {};

    // 6. Set result.[[dataLocale]] to foundLocale.
    result.data_locale = found_locale;

    // 7. If r has an [[extension]] field, then
    Vector<Unicode::Keyword> keywords;
    for (auto& extension : matcher_result.extensions) {
        if (!extension.has<Unicode::LocaleExtension>())
            continue;

        // a. Let components be ! UnicodeExtensionComponents(r.[[extension]]).
        auto& components = extension.get<Unicode::LocaleExtension>();
        // b. Let keywords be components.[[Keywords]].
        keywords = move(components.keywords);

        break;
    }

    // 8. Let supportedExtension be "-u".
    Unicode::LocaleExtension supported_extension {};

    // 9. For each element key of relevantExtensionKeys, do
    for (auto const& key : relevant_extension_keys) {
        // a. Let foundLocaleData be localeData.[[<foundLocale>]].
        // b. Assert: Type(foundLocaleData) is Record.
        // c. Let keyLocaleData be foundLocaleData.[[<key>]].
        // d. Assert: Type(keyLocaleData) is List.
        auto key_locale_data = Unicode::get_locale_key_mapping_list(found_locale, key);

        // e. Let value be keyLocaleData[0].
        // f. Assert: Type(value) is either String or Null.
        Optional<String> value;
        if (!key_locale_data.is_empty())
            value = key_locale_data[0];

        // g. Let supportedExtensionAddition be "".
        Optional<Unicode::Keyword> supported_extension_addition {};

        // h. If r has an [[extension]] field, then
        for (auto& entry : keywords) {
            // i. If keywords contains an element whose [[Key]] is the same as key, then
            if (entry.key != key)
                continue;

            // 1. Let entry be the element of keywords whose [[Key]] is the same as key.
            // 2. Let requestedValue be entry.[[Value]].
            auto requested_value = entry.value;

            // 3. If requestedValue is not the empty String, then
            if (!requested_value.is_empty()) {
                // a. If keyLocaleData contains requestedValue, then
                if (key_locale_data.contains_slow(requested_value)) {
                    // i. Let value be requestedValue.
                    value = move(requested_value);

                    // ii. Let supportedExtensionAddition be the string-concatenation of "-", key, "-", and value.
                    supported_extension_addition = Unicode::Keyword { key, move(entry.value) };
                }
            }
            // 4. Else if keyLocaleData contains "true", then
            else if (key_locale_data.contains_slow("true"sv)) {
                // a. Let value be "true".
                value = "true"sv;

                // b. Let supportedExtensionAddition be the string-concatenation of "-" and key.
                supported_extension_addition = Unicode::Keyword { key, {} };
            }

            break;
        }

        // i. If options has a field [[<key>]], then
        // i. Let optionsValue be options.[[<key>]].
        // ii. Assert: Type(optionsValue) is either String, Undefined, or Null.
        auto options_value = find_key_in_value(options, key);

        // iii. If Type(optionsValue) is String, then
        if (options_value.has_value()) {
            // 1. Let optionsValue be the string optionsValue after performing the algorithm steps to transform Unicode extension values to canonical syntax per Unicode Technical Standard #35 LDML § 3.2.1 Canonical Unicode Locale Identifiers, treating key as ukey and optionsValue as uvalue productions.
            // 2. Let optionsValue be the string optionsValue after performing the algorithm steps to replace Unicode extension values with their canonical form per Unicode Technical Standard #35 LDML § 3.2.1 Canonical Unicode Locale Identifiers, treating key as ukey and optionsValue as uvalue productions.
            Unicode::canonicalize_unicode_extension_values(key, *options_value, true);

            // 3. If optionsValue is the empty String, then
            if (options_value->is_empty()) {
                // a. Let optionsValue be "true".
                options_value = "true"sv;
            }
        }

        // iv. If keyLocaleData contains optionsValue, then
        if (options_value.has_value() && key_locale_data.contains_slow(*options_value)) {
            // 1. If SameValue(optionsValue, value) is false, then
            if (options_value != value) {
                // a. Let value be optionsValue.
                value = move(options_value);

                // b. Let supportedExtensionAddition be "".
                supported_extension_addition.clear();
            }
        }

        // j. Set result.[[<key>]] to value.
        find_key_in_value(result, key) = move(value);

        // k. Append supportedExtensionAddition to supportedExtension.
        if (supported_extension_addition.has_value())
            supported_extension.keywords.append(supported_extension_addition.release_value());
    }

    // 10. If the number of elements in supportedExtension is greater than 2, then
    if (!supported_extension.keywords.is_empty()) {
        auto locale_id = Unicode::parse_unicode_locale_id(found_locale);
        VERIFY(locale_id.has_value());

        // a. Let foundLocale be InsertUnicodeExtensionAndCanonicalize(foundLocale, supportedExtension).
        found_locale = insert_unicode_extension_and_canonicalize(locale_id.release_value(), move(supported_extension));
    }

    // 11. Set result.[[locale]] to foundLocale.
    result.locale = move(found_locale);

    // 12. Return result.
    return result;
}

// 9.2.8 LookupSupportedLocales ( availableLocales, requestedLocales ), https://tc39.es/ecma402/#sec-lookupsupportedlocales
Vector<String> lookup_supported_locales(Vector<String> const& requested_locales)
{
    // 1. Let subset be a new empty List.
    Vector<String> subset;

    // 2. For each element locale of requestedLocales, do
    for (auto const& locale : requested_locales) {
        auto locale_id = Unicode::parse_unicode_locale_id(locale);
        VERIFY(locale_id.has_value());

        // a. Let noExtensionsLocale be the String value that is locale with any Unicode locale extension sequences removed.
        locale_id->remove_extension_type<Unicode::LocaleExtension>();
        auto no_extensions_locale = locale_id->to_string();

        // b. Let availableLocale be BestAvailableLocale(availableLocales, noExtensionsLocale).
        auto available_locale = best_available_locale(no_extensions_locale);

        // c. If availableLocale is not undefined, append locale to the end of subset.
        if (available_locale.has_value())
            subset.append(locale);
    }

    // 3. Return subset.
    return subset;
}

// 9.2.9 BestFitSupportedLocales ( availableLocales, requestedLocales ), https://tc39.es/ecma402/#sec-bestfitsupportedlocales
Vector<String> best_fit_supported_locales(Vector<String> const& requested_locales)
{
    // The BestFitSupportedLocales abstract operation returns the subset of the provided BCP 47
    // language priority list requestedLocales for which availableLocales has a matching locale
    // when using the Best Fit Matcher algorithm. Locales appear in the same order in the returned
    // list as in requestedLocales. The steps taken are implementation dependent.

    // :yakbrain:
    return lookup_supported_locales(requested_locales);
}

// 9.2.10 SupportedLocales ( availableLocales, requestedLocales, options ), https://tc39.es/ecma402/#sec-supportedlocales
ThrowCompletionOr<Array*> supported_locales(GlobalObject& global_object, Vector<String> const& requested_locales, Value options)
{
    auto& vm = global_object.vm();

    // 1. Set options to ? CoerceOptionsToObject(options).
    auto* options_object = TRY(coerce_options_to_object(global_object, options));

    // 2. Let matcher be ? GetOption(options, "localeMatcher", "string", « "lookup", "best fit" », "best fit").
    auto matcher = TRY(get_option(global_object, *options_object, vm.names.localeMatcher, Value::Type::String, { "lookup"sv, "best fit"sv }, "best fit"sv));

    Vector<String> supported_locales;

    // 3. If matcher is "best fit", then
    if (matcher.as_string().string() == "best fit"sv) {
        // a. Let supportedLocales be BestFitSupportedLocales(availableLocales, requestedLocales).
        supported_locales = best_fit_supported_locales(requested_locales);
    }
    // 4. Else,
    else {
        // a. Let supportedLocales be LookupSupportedLocales(availableLocales, requestedLocales).
        supported_locales = lookup_supported_locales(requested_locales);
    }

    // 5. Return CreateArrayFromList(supportedLocales).
    return Array::create_from<String>(global_object, supported_locales, [&vm](auto& locale) { return js_string(vm, locale); });
}

// 9.2.12 CoerceOptionsToObject ( options ), https://tc39.es/ecma402/#sec-coerceoptionstoobject
ThrowCompletionOr<Object*> coerce_options_to_object(GlobalObject& global_object, Value options)
{
    // 1. If options is undefined, then
    if (options.is_undefined()) {
        // a. Return ! OrdinaryObjectCreate(null).
        return Object::create(global_object, nullptr);
    }

    // 2. Return ? ToObject(options).
    return TRY(options.to_object(global_object));
}

// 9.2.13 GetOption ( options, property, type, values, fallback ), https://tc39.es/ecma402/#sec-getoption
ThrowCompletionOr<Value> get_option(GlobalObject& global_object, Object const& options, PropertyKey const& property, Value::Type type, Span<StringView const> values, Fallback fallback)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(options) is Object.

    // 2. Let value be ? Get(options, property).
    auto value = TRY(options.get(property));

    // 3. If value is undefined, return fallback.
    if (value.is_undefined()) {
        return fallback.visit(
            [](Empty) { return js_undefined(); },
            [](bool f) { return Value(f); },
            [&vm](StringView f) { return Value(js_string(vm, f)); });
    }

    // 4. Assert: type is "boolean" or "string".
    VERIFY((type == Value::Type::Boolean) || (type == Value::Type::String));

    // 5. If type is "boolean", then
    if (type == Value::Type::Boolean) {
        // a. Set value to ! ToBoolean(value).
        value = Value(value.to_boolean());
    }
    // 6. If type is "string", then
    else {
        // a. Set value to ? ToString(value).
        value = TRY(value.to_primitive_string(global_object));
    }

    // 7. If values is not undefined and values does not contain an element equal to value, throw a RangeError exception.
    if (!values.is_empty()) {
        // Note: Every location in the spec that invokes GetOption with type=boolean also has values=undefined.
        VERIFY(value.is_string());
        if (!values.contains_slow(value.as_string().string()))
            return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, value.to_string_without_side_effects(), property.as_string());
    }

    // 8. Return value.
    return value;
}

// 9.2.14 DefaultNumberOption ( value, minimum, maximum, fallback ), https://tc39.es/ecma402/#sec-defaultnumberoption
ThrowCompletionOr<Optional<int>> default_number_option(GlobalObject& global_object, Value value, int minimum, int maximum, Optional<int> fallback)
{
    auto& vm = global_object.vm();

    // 1. If value is undefined, return fallback.
    if (value.is_undefined())
        return fallback;

    // 2. Set value to ? ToNumber(value).
    value = TRY(value.to_number(global_object));

    // 3. If value is NaN or less than minimum or greater than maximum, throw a RangeError exception.
    if (value.is_nan() || (value.as_double() < minimum) || (value.as_double() > maximum))
        return vm.throw_completion<RangeError>(global_object, ErrorType::IntlNumberIsNaNOrOutOfRange, value, minimum, maximum);

    // 4. Return floor(value).
    return floor(value.as_double());
}

// 9.2.15 GetNumberOption ( options, property, minimum, maximum, fallback ), https://tc39.es/ecma402/#sec-getnumberoption
ThrowCompletionOr<Optional<int>> get_number_option(GlobalObject& global_object, Object const& options, PropertyKey const& property, int minimum, int maximum, Optional<int> fallback)
{
    // 1. Assert: Type(options) is Object.

    // 2. Let value be ? Get(options, property).
    auto value = TRY(options.get(property));

    // 3. Return ? DefaultNumberOption(value, minimum, maximum, fallback).
    return default_number_option(global_object, value, minimum, maximum, move(fallback));
}

// 9.2.16 PartitionPattern ( pattern ), https://tc39.es/ecma402/#sec-partitionpattern
Vector<PatternPartition> partition_pattern(StringView pattern)
{
    // 1. Let result be a new empty List.
    Vector<PatternPartition> result;

    // 2. Let beginIndex be ! StringIndexOf(pattern, "{", 0).
    auto begin_index = pattern.find('{', 0);

    // 3. Let endIndex be 0.
    size_t end_index = 0;

    // 4. Let nextIndex be 0.
    size_t next_index = 0;

    // 5. Let length be the number of code units in pattern.
    // 6. Repeat, while beginIndex is an integer index into pattern,
    while (begin_index.has_value()) {
        // a. Set endIndex to ! StringIndexOf(pattern, "}", beginIndex).
        end_index = pattern.find('}', *begin_index).value();

        // b. Assert: endIndex is greater than beginIndex.
        VERIFY(end_index > *begin_index);

        // c. If beginIndex is greater than nextIndex, then
        if (*begin_index > next_index) {
            // i. Let literal be a substring of pattern from position nextIndex, inclusive, to position beginIndex, exclusive.
            auto literal = pattern.substring_view(next_index, *begin_index - next_index);

            // ii. Append a new Record { [[Type]]: "literal", [[Value]]: literal } as the last element of the list result.
            result.append({ "literal"sv, literal });
        }

        // d. Let p be the substring of pattern from position beginIndex, exclusive, to position endIndex, exclusive.
        auto partition = pattern.substring_view(*begin_index + 1, end_index - *begin_index - 1);

        // e. Append a new Record { [[Type]]: p, [[Value]]: undefined } as the last element of the list result.
        result.append({ partition, {} });

        // f. Set nextIndex to endIndex + 1.
        next_index = end_index + 1;

        // g. Set beginIndex to ! StringIndexOf(pattern, "{", nextIndex).
        begin_index = pattern.find('{', next_index);
    }

    // 7. If nextIndex is less than length, then
    if (next_index < pattern.length()) {
        // a. Let literal be the substring of pattern from position nextIndex, inclusive, to position length, exclusive.
        auto literal = pattern.substring_view(next_index);

        // b. Append a new Record { [[Type]]: "literal", [[Value]]: literal } as the last element of the list result.
        result.append({ "literal"sv, literal });
    }

    // 8. Return result.
    return result;
}

}
