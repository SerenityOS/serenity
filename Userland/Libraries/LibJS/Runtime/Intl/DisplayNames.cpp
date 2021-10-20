/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/DisplayNames.h>
#include <LibUnicode/Locale.h>

namespace JS::Intl {

// 12 DisplayNames Objects, https://tc39.es/ecma402/#intl-displaynames-objects
DisplayNames::DisplayNames(Object& prototype)
    : Object(prototype)
{
}

void DisplayNames::set_style(StringView style)
{
    if (style == "narrow"sv) {
        m_style = Style::Narrow;
    } else if (style == "short"sv) {
        m_style = Style::Short;
    } else if (style == "long"sv) {
        m_style = Style::Long;
    } else {
        VERIFY_NOT_REACHED();
    }
}

StringView DisplayNames::style_string() const
{
    switch (m_style) {
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

void DisplayNames::set_type(StringView type)
{
    if (type == "language"sv) {
        m_type = Type::Language;
    } else if (type == "region"sv) {
        m_type = Type::Region;
    } else if (type == "script"sv) {
        m_type = Type::Script;
    } else if (type == "currency"sv) {
        m_type = Type::Currency;
    } else {
        VERIFY_NOT_REACHED();
    }
}

StringView DisplayNames::type_string() const
{
    switch (m_type) {
    case Type::Language:
        return "language"sv;
    case Type::Region:
        return "region"sv;
    case Type::Script:
        return "script"sv;
    case Type::Currency:
        return "currency"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void DisplayNames::set_fallback(StringView fallback)
{
    if (fallback == "none"sv) {
        m_fallback = Fallback::None;
    } else if (fallback == "code"sv) {
        m_fallback = Fallback::Code;
    } else {
        VERIFY_NOT_REACHED();
    }
}

StringView DisplayNames::fallback_string() const
{
    switch (m_fallback) {
    case Fallback::None:
        return "none"sv;
    case Fallback::Code:
        return "code"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

// 12.1.1 CanonicalCodeForDisplayNames ( type, code ), https://tc39.es/ecma402/#sec-canonicalcodefordisplaynames
ThrowCompletionOr<Value> canonical_code_for_display_names(GlobalObject& global_object, DisplayNames::Type type, StringView code)
{
    auto& vm = global_object.vm();

    // 1. If type is "language", then
    if (type == DisplayNames::Type::Language) {
        // a. If code does not match the unicode_language_id production, throw a RangeError exception.
        if (!Unicode::parse_unicode_language_id(code).has_value())
            return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, code, "language"sv);

        // b. If IsStructurallyValidLanguageTag(code) is false, throw a RangeError exception.
        auto locale_id = is_structurally_valid_language_tag(code);
        if (!locale_id.has_value())
            return vm.throw_completion<RangeError>(global_object, ErrorType::IntlInvalidLanguageTag, code);

        // c. Set code to CanonicalizeUnicodeLocaleId(code).
        // d. Return code.
        auto canonicalized_tag = JS::Intl::canonicalize_unicode_locale_id(*locale_id);
        return js_string(vm, move(canonicalized_tag));
    }

    // 2. If type is "region", then
    if (type == DisplayNames::Type::Region) {
        // a. If code does not match the unicode_region_subtag production, throw a RangeError exception.
        if (!Unicode::is_unicode_region_subtag(code))
            return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, code, "region"sv);

        // b. Let code be the result of mapping code to upper case as described in 6.1.
        // c. Return code.
        return js_string(vm, code.to_uppercase_string());
    }

    // 3. If type is "script", then
    if (type == DisplayNames::Type::Script) {
        // a. If code does not match the unicode_script_subtag production, throw a RangeError exception.
        if (!Unicode::is_unicode_script_subtag(code))
            return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, code, "script"sv);

        // b. Let code be the result of mapping the first character in code to upper case, and mapping the second, third, and fourth character in code to lower case, as described in 6.1.
        // c. Return code.
        return js_string(vm, code.to_titlecase_string());
    }

    // 4. Assert: type is "currency".
    VERIFY(type == DisplayNames::Type::Currency);

    // 5. If ! IsWellFormedCurrencyCode(code) is false, throw a RangeError exception.
    if (!is_well_formed_currency_code(code))
        return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, code, "currency"sv);

    // 6. Let code be the result of mapping code to upper case as described in 6.1.
    // 7. Return code.
    return js_string(vm, code.to_uppercase_string());
}

}
