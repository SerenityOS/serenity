/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/Locale.h>
#include <LibJS/Runtime/Intl/LocaleConstructor.h>
#include <LibUnicode/Locale.h>

namespace JS::Intl {

struct LocaleAndKeys {
    String locale;
    Optional<String> ca;
    Optional<String> co;
    Optional<String> hc;
    Optional<String> kf;
    Optional<String> kn;
    Optional<String> nu;
};

// Note: This is not an AO in the spec. This just serves to abstract very similar steps in ApplyOptionsToTag and the Intl.Locale constructor.
static ThrowCompletionOr<Optional<String>> get_string_option(GlobalObject& global_object, Object const& options, PropertyKey const& property, Function<bool(StringView)> validator, Span<StringView const> values = {})
{
    auto& vm = global_object.vm();

    auto option = TRY(get_option(global_object, options, property, Value::Type::String, values, Empty {}));
    if (option.is_undefined())
        return Optional<String> {};

    if (validator && !validator(option.as_string().string()))
        return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, option, property);

    return option.as_string().string();
}

// 14.1.1 ApplyOptionsToTag ( tag, options ), https://tc39.es/ecma402/#sec-apply-options-to-tag
static ThrowCompletionOr<String> apply_options_to_tag(GlobalObject& global_object, StringView tag, Object const& options)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(tag) is String.
    // 2. Assert: Type(options) is Object.

    // 3. If IsStructurallyValidLanguageTag(tag) is false, throw a RangeError exception.
    auto locale_id = is_structurally_valid_language_tag(tag);
    if (!locale_id.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::IntlInvalidLanguageTag, tag);

    // 4. Let language be ? GetOption(options, "language", "string", undefined, undefined).
    // 5. If language is not undefined, then
    //     a. If language does not match the unicode_language_subtag production, throw a RangeError exception.
    auto language = TRY(get_string_option(global_object, options, vm.names.language, Unicode::is_unicode_language_subtag));

    // 6. Let script be ? GetOption(options, "script", "string", undefined, undefined).
    // 7. If script is not undefined, then
    //     a. If script does not match the unicode_script_subtag production, throw a RangeError exception.
    auto script = TRY(get_string_option(global_object, options, vm.names.script, Unicode::is_unicode_script_subtag));

    // 8. Let region be ? GetOption(options, "region", "string", undefined, undefined).
    // 9. If region is not undefined, then
    //     a. If region does not match the unicode_region_subtag production, throw a RangeError exception.
    auto region = TRY(get_string_option(global_object, options, vm.names.region, Unicode::is_unicode_region_subtag));

    // 10. Set tag to CanonicalizeUnicodeLocaleId(tag).
    auto canonicalized_tag = JS::Intl::canonicalize_unicode_locale_id(*locale_id);

    // 11. Assert: tag matches the unicode_locale_id production.
    locale_id = Unicode::parse_unicode_locale_id(canonicalized_tag);
    VERIFY(locale_id.has_value());

    // 12. Let languageId be the substring of tag corresponding to the unicode_language_id production.
    auto& language_id = locale_id->language_id;

    // 13. If language is not undefined, then
    if (language.has_value()) {
        // a. Set languageId to languageId with the substring corresponding to the unicode_language_subtag production replaced by the string language.
        language_id.language = language.release_value();
    }

    // 14. If script is not undefined, then
    if (script.has_value()) {
        // a. If languageId does not contain a unicode_script_subtag production, then
        //     i. Set languageId to the string-concatenation of the unicode_language_subtag production of languageId, "-", script, and the rest of languageId.
        // b. Else,
        //     i. Set languageId to languageId with the substring corresponding to the unicode_script_subtag production replaced by the string script.
        language_id.script = script.release_value();
    }

    // 15. If region is not undefined, then
    if (region.has_value()) {
        // a. If languageId does not contain a unicode_region_subtag production, then
        //     i. Set languageId to the string-concatenation of the unicode_language_subtag production of languageId, the substring corresponding to "-"` and the `unicode_script_subtag` production if present, `"-", region, and the rest of languageId.
        // b. Else,
        //     i. Set languageId to languageId with the substring corresponding to the unicode_region_subtag production replaced by the string region.
        language_id.region = region.release_value();
    }

    // 16. Set tag to tag with the substring corresponding to the unicode_language_id production replaced by the string languageId.
    // 17. Return CanonicalizeUnicodeLocaleId(tag).
    return JS::Intl::canonicalize_unicode_locale_id(*locale_id);
}

// 14.1.2 ApplyUnicodeExtensionToTag ( tag, options, relevantExtensionKeys ), https://tc39.es/ecma402/#sec-apply-unicode-extension-to-tag
static LocaleAndKeys apply_unicode_extension_to_tag(StringView tag, LocaleAndKeys options, Span<StringView const> relevant_extension_keys)
{
    // 1. Assert: Type(tag) is String.
    // 2. Assert: tag matches the unicode_locale_id production.
    auto locale_id = Unicode::parse_unicode_locale_id(tag);
    VERIFY(locale_id.has_value());

    Vector<String> attributes;
    Vector<Unicode::Keyword> keywords;

    // 3. If tag contains a substring that is a Unicode locale extension sequence, then
    for (auto& extension : locale_id->extensions) {
        if (!extension.has<Unicode::LocaleExtension>())
            continue;

        // a. Let extension be the String value consisting of the substring of the Unicode locale extension sequence within tag.
        // b. Let components be ! UnicodeExtensionComponents(extension).
        auto& components = extension.get<Unicode::LocaleExtension>();
        // c. Let attributes be components.[[Attributes]].
        attributes = move(components.attributes);
        // d. Let keywords be components.[[Keywords]].
        keywords = move(components.keywords);

        break;
    }
    // 4. Else,
    //     a. Let attributes be a new empty List.
    //     b. Let keywords be a new empty List.

    auto field_from_key = [](LocaleAndKeys& value, StringView key) -> Optional<String>& {
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
        VERIFY_NOT_REACHED();
    };

    // 5. Let result be a new Record.
    LocaleAndKeys result {};

    // 6. For each element key of relevantExtensionKeys, do
    for (auto const& key : relevant_extension_keys) {
        // a. Let value be undefined.
        Optional<String> value {};

        Unicode::Keyword* entry = nullptr;
        // b. If keywords contains an element whose [[Key]] is the same as key, then
        if (auto it = keywords.find_if([&](auto const& k) { return key == k.key; }); it != keywords.end()) {
            // i. Let entry be the element of keywords whose [[Key]] is the same as key.
            entry = &(*it);

            // ii. Let value be entry.[[Value]].
            value = entry->value;
        }
        // c. Else,
        //     i. Let entry be empty.

        // d. Assert: options has a field [[<key>]].
        // e. Let optionsValue be options.[[<key>]].
        auto options_value = field_from_key(options, key);

        // f. If optionsValue is not undefined, then
        if (options_value.has_value()) {
            // i. Assert: Type(optionsValue) is String.
            // ii. Let value be optionsValue.
            value = options_value.release_value();

            // iii. If entry is not empty, then
            if (entry != nullptr) {
                // 1. Set entry.[[Value]] to value.
                entry->value = *value;
            }
            // iv. Else,
            else {
                // 1. Append the Record { [[Key]]: key, [[Value]]: value } to keywords.
                keywords.append({ key, *value });
            }
        }

        // g. Set result.[[<key>]] to value.
        field_from_key(result, key) = move(value);
    }

    // 7. Let locale be the String value that is tag with any Unicode locale extension sequences removed.
    locale_id->remove_extension_type<Unicode::LocaleExtension>();
    auto locale = locale_id->to_string();

    // 8. Let newExtension be a Unicode BCP 47 U Extension based on attributes and keywords.
    Unicode::LocaleExtension new_extension { move(attributes), move(keywords) };

    // 9. If newExtension is not the empty String, then
    if (!new_extension.attributes.is_empty() || !new_extension.keywords.is_empty()) {
        // a. Let locale be ! InsertUnicodeExtensionAndCanonicalize(locale, newExtension).
        locale = insert_unicode_extension_and_canonicalize(locale_id.release_value(), move(new_extension));
    }

    // 10. Set result.[[locale]] to locale.
    result.locale = move(locale);

    // 11. Return result.
    return result;
}

// 14.1 The Intl.Locale Constructor, https://tc39.es/ecma402/#sec-intl-locale-constructor
LocaleConstructor::LocaleConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Locale.as_string(), *global_object.function_prototype())
{
}

void LocaleConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 14.2.1 Intl.Locale.prototype, https://tc39.es/ecma402/#sec-Intl.Locale.prototype
    define_direct_property(vm.names.prototype, global_object.intl_locale_prototype(), 0);
    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

// 14.1.3 Intl.Locale ( tag [ , options ] ), https://tc39.es/ecma402/#sec-Intl.Locale
ThrowCompletionOr<Value> LocaleConstructor::call()
{
    // 1. If NewTarget is undefined, throw a TypeError exception.
    return vm().throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Intl.Locale");
}

// 14.1.3 Intl.Locale ( tag [ , options ] ), https://tc39.es/ecma402/#sec-Intl.Locale
ThrowCompletionOr<Object*> LocaleConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto tag_value = vm.argument(0);
    auto options_value = vm.argument(1);

    // 2. Let relevantExtensionKeys be %Locale%.[[RelevantExtensionKeys]].
    auto relevant_extension_keys = Locale::relevant_extension_keys();

    // 3. Let internalSlotsList be « [[InitializedLocale]], [[Locale]], [[Calendar]], [[Collation]], [[HourCycle]], [[NumberingSystem]] ».
    // 4. If relevantExtensionKeys contains "kf", then
    //     a. Append [[CaseFirst]] as the last element of internalSlotsList.
    // 5. If relevantExtensionKeys contains "kn", then
    //     a. Append [[Numeric]] as the last element of internalSlotsList.

    // 6. Let locale be ? OrdinaryCreateFromConstructor(NewTarget, "%Locale.prototype%", internalSlotsList).
    auto* locale = TRY(ordinary_create_from_constructor<Locale>(global_object, new_target, &GlobalObject::intl_locale_prototype));

    String tag;

    // 7. If Type(tag) is not String or Object, throw a TypeError exception.
    if (!tag_value.is_string() && !tag_value.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOrString, "tag"sv);

    // 8. If Type(tag) is Object and tag has an [[InitializedLocale]] internal slot, then
    if (tag_value.is_object() && is<Locale>(tag_value.as_object())) {
        // a. Let tag be tag.[[Locale]].
        auto const& tag_object = static_cast<Locale const&>(tag_value.as_object());
        tag = tag_object.locale();
    }
    // 9. Else,
    else {
        // a. Let tag be ? ToString(tag).
        tag = TRY(tag_value.to_string(global_object));
    }

    // 10. Set options to ? CoerceOptionsToObject(options).
    auto* options = TRY(coerce_options_to_object(global_object, options_value));

    // 11. Set tag to ? ApplyOptionsToTag(tag, options).
    tag = TRY(apply_options_to_tag(global_object, tag, *options));

    // 12. Let opt be a new Record.
    LocaleAndKeys opt {};

    // 13. Let calendar be ? GetOption(options, "calendar", "string", undefined, undefined).
    // 14. If calendar is not undefined, then
    //     a. If calendar does not match the Unicode Locale Identifier type nonterminal, throw a RangeError exception.
    // 15. Set opt.[[ca]] to calendar.
    opt.ca = TRY(get_string_option(global_object, *options, vm.names.calendar, Unicode::is_type_identifier));

    // 16. Let collation be ? GetOption(options, "collation", "string", undefined, undefined).
    // 17. If collation is not undefined, then
    //     a. If collation does not match the Unicode Locale Identifier type nonterminal, throw a RangeError exception.
    // 18. Set opt.[[co]] to collation.
    opt.co = TRY(get_string_option(global_object, *options, vm.names.collation, Unicode::is_type_identifier));

    // 19. Let hc be ? GetOption(options, "hourCycle", "string", « "h11", "h12", "h23", "h24" », undefined).
    // 20. Set opt.[[hc]] to hc.
    opt.hc = TRY(get_string_option(global_object, *options, vm.names.hourCycle, nullptr, AK::Array { "h11"sv, "h12"sv, "h23"sv, "h24"sv }));

    // 21. Let kf be ? GetOption(options, "caseFirst", "string", « "upper", "lower", "false" », undefined).
    // 22. Set opt.[[kf]] to kf.
    opt.kf = TRY(get_string_option(global_object, *options, vm.names.caseFirst, nullptr, AK::Array { "upper"sv, "lower"sv, "false"sv }));

    // 23. Let kn be ? GetOption(options, "numeric", "boolean", undefined, undefined).
    auto kn = TRY(get_option(global_object, *options, vm.names.numeric, Value::Type::Boolean, {}, Empty {}));

    // 24. If kn is not undefined, set kn to ! ToString(kn).
    // 25. Set opt.[[kn]] to kn.
    if (!kn.is_undefined())
        opt.kn = TRY(kn.to_string(global_object));

    // 26. Let numberingSystem be ? GetOption(options, "numberingSystem", "string", undefined, undefined).
    // 27. If numberingSystem is not undefined, then
    //     a. If numberingSystem does not match the Unicode Locale Identifier type nonterminal, throw a RangeError exception.
    // 28. Set opt.[[nu]] to numberingSystem.
    opt.nu = TRY(get_string_option(global_object, *options, vm.names.numberingSystem, Unicode::is_type_identifier));

    // 29. Let r be ! ApplyUnicodeExtensionToTag(tag, opt, relevantExtensionKeys).
    auto result = apply_unicode_extension_to_tag(tag, move(opt), relevant_extension_keys);

    // 30. Set locale.[[Locale]] to r.[[locale]].
    locale->set_locale(move(result.locale));
    // 31. Set locale.[[Calendar]] to r.[[ca]].
    if (result.ca.has_value())
        locale->set_calendar(result.ca.release_value());
    // 32. Set locale.[[Collation]] to r.[[co]].
    if (result.co.has_value())
        locale->set_collation(result.co.release_value());
    // 33. Set locale.[[HourCycle]] to r.[[hc]].
    if (result.hc.has_value())
        locale->set_hour_cycle(result.hc.release_value());

    // 34. If relevantExtensionKeys contains "kf", then
    if (relevant_extension_keys.span().contains_slow("kf"sv)) {
        // a. Set locale.[[CaseFirst]] to r.[[kf]].
        if (result.kf.has_value())
            locale->set_case_first(result.kf.release_value());
    }

    // 35. If relevantExtensionKeys contains "kn", then
    if (relevant_extension_keys.span().contains_slow("kn"sv)) {
        // a. If ! SameValue(r.[[kn]], "true") is true or r.[[kn]] is the empty String, then
        if (result.kn.has_value() && (same_value(js_string(vm, *result.kn), js_string(vm, "true")) || result.kn->is_empty())) {
            // i. Set locale.[[Numeric]] to true.
            locale->set_numeric(true);
        }
        // b. Else,
        else {
            // i. Set locale.[[Numeric]] to false.
            locale->set_numeric(false);
        }
    }

    // 36. Set locale.[[NumberingSystem]] to r.[[nu]].
    if (result.nu.has_value())
        locale->set_numbering_system(result.nu.release_value());

    // 37. Return locale.
    return locale;
}

}
