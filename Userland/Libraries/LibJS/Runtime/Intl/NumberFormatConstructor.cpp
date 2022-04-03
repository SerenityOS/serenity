/*
 * Copyright (c) 2021-2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/NumberFormatConstructor.h>
#include <LibUnicode/Locale.h>

namespace JS::Intl {

// 15.1 The Intl.NumberFormat Constructor, https://tc39.es/ecma402/#sec-intl-numberformat-constructor
NumberFormatConstructor::NumberFormatConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.NumberFormat.as_string(), *global_object.function_prototype())
{
}

void NumberFormatConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 15.2.1 Intl.NumberFormat.prototype, https://tc39.es/ecma402/#sec-intl.numberformat.prototype
    define_direct_property(vm.names.prototype, global_object.intl_number_format_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.supportedLocalesOf, supported_locales_of, 1, attr);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

// 15.1.1 Intl.NumberFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.numberformat
ThrowCompletionOr<Value> NumberFormatConstructor::call()
{
    // 1. If NewTarget is undefined, let newTarget be the active function object, else let newTarget be NewTarget.
    return TRY(construct(*this));
}

// 15.1.1 Intl.NumberFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.numberformat
ThrowCompletionOr<Object*> NumberFormatConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 2. Let numberFormat be ? OrdinaryCreateFromConstructor(newTarget, "%NumberFormat.prototype%", « [[InitializedNumberFormat]], [[Locale]], [[DataLocale]], [[NumberingSystem]], [[Style]], [[Unit]], [[UnitDisplay]], [[Currency]], [[CurrencyDisplay]], [[CurrencySign]], [[MinimumIntegerDigits]], [[MinimumFractionDigits]], [[MaximumFractionDigits]], [[MinimumSignificantDigits]], [[MaximumSignificantDigits]], [[RoundingType]], [[Notation]], [[CompactDisplay]], [[UseGrouping]], [[SignDisplay]], [[BoundFormat]] »).
    auto* number_format = TRY(ordinary_create_from_constructor<NumberFormat>(global_object, new_target, &GlobalObject::intl_number_format_prototype));

    // 3. Perform ? InitializeNumberFormat(numberFormat, locales, options).
    TRY(initialize_number_format(global_object, *number_format, locales, options));

    // 4. If the implementation supports the normative optional constructor mode of 4.3 Note 1, then
    //     a. Let this be the this value.
    //     b. Return ? ChainNumberFormat(numberFormat, NewTarget, this).

    // 5. Return numberFormat.
    return number_format;
}

// 15.2.2 Intl.NumberFormat.supportedLocalesOf ( locales [ , options ] ), https://tc39.es/ecma402/#sec-intl.numberformat.supportedlocalesof
JS_DEFINE_NATIVE_FUNCTION(NumberFormatConstructor::supported_locales_of)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let availableLocales be %NumberFormat%.[[AvailableLocales]].

    // 2. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(global_object, locales));

    // 3. Return ? SupportedLocales(availableLocales, requestedLocales, options).
    return TRY(supported_locales(global_object, requested_locales, options));
}

// 15.1.2 InitializeNumberFormat ( numberFormat, locales, options ), https://tc39.es/ecma402/#sec-initializenumberformat
ThrowCompletionOr<NumberFormat*> initialize_number_format(GlobalObject& global_object, NumberFormat& number_format, Value locales_value, Value options_value)
{
    auto& vm = global_object.vm();

    // 1. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(global_object, locales_value));

    // 2. Set options to ? CoerceOptionsToObject(options).
    auto* options = TRY(coerce_options_to_object(global_object, options_value));

    // 3. Let opt be a new Record.
    LocaleOptions opt {};

    // 4. Let matcher be ? GetOption(options, "localeMatcher", "string", « "lookup", "best fit" », "best fit").
    auto matcher = TRY(get_option(global_object, *options, vm.names.localeMatcher, Value::Type::String, { "lookup"sv, "best fit"sv }, "best fit"sv));

    // 5. Set opt.[[localeMatcher]] to matcher.
    opt.locale_matcher = matcher;

    // 6. Let numberingSystem be ? GetOption(options, "numberingSystem", "string", undefined, undefined).
    auto numbering_system = TRY(get_option(global_object, *options, vm.names.numberingSystem, Value::Type::String, {}, Empty {}));

    // 7. If numberingSystem is not undefined, then
    if (!numbering_system.is_undefined()) {
        // a. If numberingSystem does not match the Unicode Locale Identifier type nonterminal, throw a RangeError exception.
        if (!Unicode::is_type_identifier(numbering_system.as_string().string()))
            return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, numbering_system, "numberingSystem"sv);

        // 8. Set opt.[[nu]] to numberingSystem.
        opt.nu = numbering_system.as_string().string();
    }

    // 9. Let localeData be %NumberFormat%.[[LocaleData]].
    // 10. Let r be ResolveLocale(%NumberFormat%.[[AvailableLocales]], requestedLocales, opt, %NumberFormat%.[[RelevantExtensionKeys]], localeData).
    auto result = resolve_locale(requested_locales, opt, NumberFormat::relevant_extension_keys());

    // 11. Set numberFormat.[[Locale]] to r.[[locale]].
    number_format.set_locale(move(result.locale));

    // 12. Set numberFormat.[[DataLocale]] to r.[[dataLocale]].
    number_format.set_data_locale(move(result.data_locale));

    // 13. Set numberFormat.[[NumberingSystem]] to r.[[nu]].
    if (result.nu.has_value())
        number_format.set_numbering_system(result.nu.release_value());

    // 14. Perform ? SetNumberFormatUnitOptions(numberFormat, options).
    TRY(set_number_format_unit_options(global_object, number_format, *options));

    // 15. Let style be numberFormat.[[Style]].
    auto style = number_format.style();

    int default_min_fraction_digits = 0;
    int default_max_fraction_digits = 0;

    // 16. If style is "currency", then
    if (style == NumberFormat::Style::Currency) {
        // a. Let currency be numberFormat.[[Currency]].
        auto const& currency = number_format.currency();

        // b. Let cDigits be CurrencyDigits(currency).
        int digits = currency_digits(currency);

        // c. Let mnfdDefault be cDigits.
        default_min_fraction_digits = digits;

        // d. Let mxfdDefault be cDigits.
        default_max_fraction_digits = digits;
    }
    // 17. Else,
    else {
        // a. Let mnfdDefault be 0.
        default_min_fraction_digits = 0;

        // b. If style is "percent", then
        //     i. Let mxfdDefault be 0.
        // c. Else,
        //     i. Let mxfdDefault be 3.
        default_max_fraction_digits = style == NumberFormat::Style::Percent ? 0 : 3;
    }

    // 18. Let notation be ? GetOption(options, "notation", "string", « "standard", "scientific", "engineering", "compact" », "standard").
    auto notation = TRY(get_option(global_object, *options, vm.names.notation, Value::Type::String, { "standard"sv, "scientific"sv, "engineering"sv, "compact"sv }, "standard"sv));

    // 19. Set numberFormat.[[Notation]] to notation.
    number_format.set_notation(notation.as_string().string());

    // 20. Perform ? SetNumberFormatDigitOptions(numberFormat, options, mnfdDefault, mxfdDefault, notation).
    TRY(set_number_format_digit_options(global_object, number_format, *options, default_min_fraction_digits, default_max_fraction_digits, number_format.notation()));

    // 21. Let compactDisplay be ? GetOption(options, "compactDisplay", "string", « "short", "long" », "short").
    auto compact_display = TRY(get_option(global_object, *options, vm.names.compactDisplay, Value::Type::String, { "short"sv, "long"sv }, "short"sv));

    // 22. If notation is "compact", then
    if (number_format.notation() == NumberFormat::Notation::Compact) {
        // a. Set numberFormat.[[CompactDisplay]] to compactDisplay.
        number_format.set_compact_display(compact_display.as_string().string());
    }

    // 23. Let useGrouping be ? GetOption(options, "useGrouping", "boolean", undefined, true).
    auto use_grouping = TRY(get_option(global_object, *options, vm.names.useGrouping, Value::Type::Boolean, {}, true));

    // 24. Set numberFormat.[[UseGrouping]] to useGrouping.
    number_format.set_use_grouping(use_grouping.as_bool());

    // 25. Let signDisplay be ? GetOption(options, "signDisplay", "string", « "auto", "never", "always", "exceptZero" », "auto").
    auto sign_display = TRY(get_option(global_object, *options, vm.names.signDisplay, Value::Type::String, { "auto"sv, "never"sv, "always"sv, "exceptZero"sv }, "auto"sv));

    // 26. Set numberFormat.[[SignDisplay]] to signDisplay.
    number_format.set_sign_display(sign_display.as_string().string());

    // 27. Return numberFormat.
    return &number_format;
}

// 15.1.3 SetNumberFormatDigitOptions ( intlObj, options, mnfdDefault, mxfdDefault, notation ), https://tc39.es/ecma402/#sec-setnfdigitoptions
ThrowCompletionOr<void> set_number_format_digit_options(GlobalObject& global_object, NumberFormatBase& intl_object, Object const& options, int default_min_fraction_digits, int default_max_fraction_digits, NumberFormat::Notation notation)
{
    auto& vm = global_object.vm();

    // 1. Let mnid be ? GetNumberOption(options, "minimumIntegerDigits,", 1, 21, 1).
    auto min_integer_digits = TRY(get_number_option(global_object, options, vm.names.minimumIntegerDigits, 1, 21, 1));

    // 2. Let mnfd be ? Get(options, "minimumFractionDigits").
    auto min_fraction_digits = TRY(options.get(vm.names.minimumFractionDigits));

    // 3. Let mxfd be ? Get(options, "maximumFractionDigits").
    auto max_fraction_digits = TRY(options.get(vm.names.maximumFractionDigits));

    // 4. Let mnsd be ? Get(options, "minimumSignificantDigits").
    auto min_significant_digits = TRY(options.get(vm.names.minimumSignificantDigits));

    // 5. Let mxsd be ? Get(options, "maximumSignificantDigits").
    auto max_significant_digits = TRY(options.get(vm.names.maximumSignificantDigits));

    // 6. Set intlObj.[[MinimumIntegerDigits]] to mnid.
    intl_object.set_min_integer_digits(*min_integer_digits);

    // 7. If mnsd is not undefined or mxsd is not undefined, then
    //     a. Let hasSd be true.
    // 8. Else,
    //     a. Let hasSd be false.
    bool has_significant_digits = !min_significant_digits.is_undefined() || !max_significant_digits.is_undefined();

    // 9. If mnfd is not undefined or mxfd is not undefined, then
    //     a. Let hasFd be true.
    // 10. Else,
    //     a. Let hasFd be false.
    bool has_fraction_digits = !min_fraction_digits.is_undefined() || !max_fraction_digits.is_undefined();

    // 11. Let needSd be hasSd.
    bool need_significant_digits = has_significant_digits;

    // 12. If hasSd is true, or hasFd is false and notation is "compact", then
    //     a. Let needFd be false.
    // 13. Else,
    //     a. Let needFd be true.
    bool need_fraction_digits = !has_significant_digits && (has_fraction_digits || (notation != NumberFormat::Notation::Compact));

    // 14. If needSd is true, then
    if (need_significant_digits) {
        // a. Assert: hasSd is true.
        VERIFY(has_significant_digits);

        // b. Set mnsd to ? DefaultNumberOption(mnsd, 1, 21, 1).
        auto min_digits = TRY(default_number_option(global_object, min_significant_digits, 1, 21, 1));

        // c. Set mxsd to ? DefaultNumberOption(mxsd, mnsd, 21, 21).
        auto max_digits = TRY(default_number_option(global_object, max_significant_digits, *min_digits, 21, 21));

        // d. Set intlObj.[[MinimumSignificantDigits]] to mnsd.
        intl_object.set_min_significant_digits(*min_digits);

        // e. Set intlObj.[[MaximumSignificantDigits]] to mxsd.
        intl_object.set_max_significant_digits(*max_digits);
    }

    // 15. If needFd is true, then
    if (need_fraction_digits) {
        // a. If hasFd is true, then
        if (has_fraction_digits) {
            // i. Set mnfd to ? DefaultNumberOption(mnfd, 0, 20, undefined).
            auto min_digits = TRY(default_number_option(global_object, min_fraction_digits, 0, 20, {}));

            // ii. Set mxfd to ? DefaultNumberOption(mxfd, 0, 20, undefined).
            auto max_digits = TRY(default_number_option(global_object, max_fraction_digits, 0, 20, {}));

            // iii. If mnfd is undefined, set mnfd to min(mnfdDefault, mxfd).
            if (!min_digits.has_value())
                min_digits = min(default_min_fraction_digits, *max_digits);
            // iv. Else if mxfd is undefined, set mxfd to max(mxfdDefault, mnfd).
            else if (!max_digits.has_value())
                max_digits = max(default_max_fraction_digits, *min_digits);
            // v. Else if mnfd is greater than mxfd, throw a RangeError exception.
            else if (*min_digits > *max_digits)
                return vm.throw_completion<RangeError>(global_object, ErrorType::IntlMinimumExceedsMaximum, *min_digits, *max_digits);

            // vi. Set intlObj.[[MinimumFractionDigits]] to mnfd.
            intl_object.set_min_fraction_digits(*min_digits);

            // vii. Set intlObj.[[MaximumFractionDigits]] to mxfd.
            intl_object.set_max_fraction_digits(*max_digits);
        }
        // b. Else,
        else {
            // i. Set intlObj.[[MinimumFractionDigits]] to mnfdDefault.
            intl_object.set_min_fraction_digits(default_min_fraction_digits);

            // ii. Set intlObj.[[MaximumFractionDigits]] to mxfdDefault.
            intl_object.set_max_fraction_digits(default_max_fraction_digits);
        }
    }

    // 16. If needSd is false and needFd is false, then
    if (!need_significant_digits && !need_fraction_digits) {
        // a. Set intlObj.[[RoundingType]] to compactRounding.
        intl_object.set_rounding_type(NumberFormatBase::RoundingType::CompactRounding);
    }
    // 17. Else if hasSd is true, then
    else if (has_significant_digits) {
        // a. Set intlObj.[[RoundingType]] to significantDigits.
        intl_object.set_rounding_type(NumberFormatBase::RoundingType::SignificantDigits);
    }
    // 18. Else,
    else {
        // a. Set intlObj.[[RoundingType]] to fractionDigits.
        intl_object.set_rounding_type(NumberFormatBase::RoundingType::FractionDigits);
    }

    return {};
}

// 15.1.4 SetNumberFormatUnitOptions ( intlObj, options ), https://tc39.es/ecma402/#sec-setnumberformatunitoptions
ThrowCompletionOr<void> set_number_format_unit_options(GlobalObject& global_object, NumberFormat& intl_object, Object const& options)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(intlObj) is Object.
    // 2. Assert: Type(options) is Object.

    // 3. Let style be ? GetOption(options, "style", "string", « "decimal", "percent", "currency", "unit" », "decimal").
    auto style = TRY(get_option(global_object, options, vm.names.style, Value::Type::String, { "decimal"sv, "percent"sv, "currency"sv, "unit"sv }, "decimal"sv));

    // 4. Set intlObj.[[Style]] to style.
    intl_object.set_style(style.as_string().string());

    // 5. Let currency be ? GetOption(options, "currency", "string", undefined, undefined).
    auto currency = TRY(get_option(global_object, options, vm.names.currency, Value::Type::String, {}, Empty {}));

    // 6. If currency is undefined, then
    if (currency.is_undefined()) {
        // a. If style is "currency", throw a TypeError exception.
        if (intl_object.style() == NumberFormat::Style::Currency)
            return vm.throw_completion<TypeError>(global_object, ErrorType::IntlOptionUndefined, "currency"sv, "style"sv, style);
    }
    // 7. Else,
    //     a. If ! IsWellFormedCurrencyCode(currency) is false, throw a RangeError exception.
    else if (!is_well_formed_currency_code(currency.as_string().string()))
        return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, currency, "currency"sv);

    // 8. Let currencyDisplay be ? GetOption(options, "currencyDisplay", "string", « "code", "symbol", "narrowSymbol", "name" », "symbol").
    auto currency_display = TRY(get_option(global_object, options, vm.names.currencyDisplay, Value::Type::String, { "code"sv, "symbol"sv, "narrowSymbol"sv, "name"sv }, "symbol"sv));

    // 9. Let currencySign be ? GetOption(options, "currencySign", "string", « "standard", "accounting" », "standard").
    auto currency_sign = TRY(get_option(global_object, options, vm.names.currencySign, Value::Type::String, { "standard"sv, "accounting"sv }, "standard"sv));

    // 10. Let unit be ? GetOption(options, "unit", "string", undefined, undefined).
    auto unit = TRY(get_option(global_object, options, vm.names.unit, Value::Type::String, {}, Empty {}));

    // 11. If unit is undefined, then
    if (unit.is_undefined()) {
        // a. If style is "unit", throw a TypeError exception.
        if (intl_object.style() == NumberFormat::Style::Unit)
            return vm.throw_completion<TypeError>(global_object, ErrorType::IntlOptionUndefined, "unit"sv, "style"sv, style);
    }
    // 12. Else,
    //     a. If ! IsWellFormedUnitIdentifier(unit) is false, throw a RangeError exception.
    else if (!is_well_formed_unit_identifier(unit.as_string().string()))
        return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, unit, "unit"sv);

    // 13. Let unitDisplay be ? GetOption(options, "unitDisplay", "string", « "short", "narrow", "long" », "short").
    auto unit_display = TRY(get_option(global_object, options, vm.names.unitDisplay, Value::Type::String, { "short"sv, "narrow"sv, "long"sv }, "short"sv));

    // 14. If style is "currency", then
    if (intl_object.style() == NumberFormat::Style::Currency) {
        // a. Set intlObj.[[Currency]] to the ASCII-uppercase of currency.
        intl_object.set_currency(currency.as_string().string().to_uppercase());

        // c. Set intlObj.[[CurrencyDisplay]] to currencyDisplay.
        intl_object.set_currency_display(currency_display.as_string().string());

        // d. Set intlObj.[[CurrencySign]] to currencySign.
        intl_object.set_currency_sign(currency_sign.as_string().string());
    }

    // 15. If style is "unit", then
    if (intl_object.style() == NumberFormat::Style::Unit) {
        // a. Set intlObj.[[Unit]] to unit.
        intl_object.set_unit(unit.as_string().string());

        // b. Set intlObj.[[UnitDisplay]] to unitDisplay.
        intl_object.set_unit_display(unit_display.as_string().string());
    }

    return {};
}

}
