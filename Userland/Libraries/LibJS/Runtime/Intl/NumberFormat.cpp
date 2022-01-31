/*
 * Copyright (c) 2021-2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/NumberFormat.h>
#include <LibJS/Runtime/Intl/NumberFormatFunction.h>
#include <LibUnicode/CurrencyCode.h>
#include <math.h>
#include <stdlib.h>

namespace JS::Intl {

NumberFormatBase::NumberFormatBase(Object& prototype)
    : Object(prototype)
{
}

// 15 NumberFormat Objects, https://tc39.es/ecma402/#numberformat-objects
NumberFormat::NumberFormat(Object& prototype)
    : NumberFormatBase(prototype)
{
}

void NumberFormat::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    if (m_bound_format)
        visitor.visit(m_bound_format);
}

void NumberFormat::set_style(StringView style)
{
    if (style == "decimal"sv)
        m_style = Style::Decimal;
    else if (style == "percent"sv)
        m_style = Style::Percent;
    else if (style == "currency"sv)
        m_style = Style::Currency;
    else if (style == "unit"sv)
        m_style = Style::Unit;
    else
        VERIFY_NOT_REACHED();
}

StringView NumberFormat::style_string() const
{
    switch (m_style) {
    case Style::Decimal:
        return "decimal"sv;
    case Style::Percent:
        return "percent"sv;
    case Style::Currency:
        return "currency"sv;
    case Style::Unit:
        return "unit"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void NumberFormat::set_currency_display(StringView currency_display)
{
    m_resolved_currency_display.clear();

    if (currency_display == "code"sv)
        m_currency_display = CurrencyDisplay::Code;
    else if (currency_display == "symbol"sv)
        m_currency_display = CurrencyDisplay::Symbol;
    else if (currency_display == "narrowSymbol"sv)
        m_currency_display = CurrencyDisplay::NarrowSymbol;
    else if (currency_display == "name"sv)
        m_currency_display = CurrencyDisplay::Name;
    else
        VERIFY_NOT_REACHED();
}

StringView NumberFormat::resolve_currency_display()
{
    if (m_resolved_currency_display.has_value())
        return *m_resolved_currency_display;

    switch (currency_display()) {
    case NumberFormat::CurrencyDisplay::Code:
        m_resolved_currency_display = currency();
        break;
    case NumberFormat::CurrencyDisplay::Symbol:
        m_resolved_currency_display = Unicode::get_locale_short_currency_mapping(data_locale(), currency());
        break;
    case NumberFormat::CurrencyDisplay::NarrowSymbol:
        m_resolved_currency_display = Unicode::get_locale_narrow_currency_mapping(data_locale(), currency());
        break;
    case NumberFormat::CurrencyDisplay::Name:
        m_resolved_currency_display = Unicode::get_locale_numeric_currency_mapping(data_locale(), currency());
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    if (!m_resolved_currency_display.has_value())
        m_resolved_currency_display = currency();

    return *m_resolved_currency_display;
}

StringView NumberFormat::currency_display_string() const
{
    VERIFY(m_currency_display.has_value());

    switch (*m_currency_display) {
    case CurrencyDisplay::Code:
        return "code"sv;
    case CurrencyDisplay::Symbol:
        return "symbol"sv;
    case CurrencyDisplay::NarrowSymbol:
        return "narrowSymbol"sv;
    case CurrencyDisplay::Name:
        return "name"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void NumberFormat::set_currency_sign(StringView currency_sign)
{
    if (currency_sign == "standard"sv)
        m_currency_sign = CurrencySign::Standard;
    else if (currency_sign == "accounting"sv)
        m_currency_sign = CurrencySign::Accounting;
    else
        VERIFY_NOT_REACHED();
}

StringView NumberFormat::currency_sign_string() const
{
    VERIFY(m_currency_sign.has_value());

    switch (*m_currency_sign) {
    case CurrencySign::Standard:
        return "standard"sv;
    case CurrencySign::Accounting:
        return "accounting"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

StringView NumberFormatBase::rounding_type_string() const
{
    switch (m_rounding_type) {
    case RoundingType::SignificantDigits:
        return "significantDigits"sv;
    case RoundingType::FractionDigits:
        return "fractionDigits"sv;
    case RoundingType::CompactRounding:
        return "compactRounding"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void NumberFormat::set_notation(StringView notation)
{
    if (notation == "standard"sv)
        m_notation = Notation::Standard;
    else if (notation == "scientific"sv)
        m_notation = Notation::Scientific;
    else if (notation == "engineering"sv)
        m_notation = Notation::Engineering;
    else if (notation == "compact"sv)
        m_notation = Notation::Compact;
    else
        VERIFY_NOT_REACHED();
}

StringView NumberFormat::notation_string() const
{
    switch (m_notation) {
    case Notation::Standard:
        return "standard"sv;
    case Notation::Scientific:
        return "scientific"sv;
    case Notation::Engineering:
        return "engineering"sv;
    case Notation::Compact:
        return "compact"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void NumberFormat::set_compact_display(StringView compact_display)
{
    if (compact_display == "short"sv)
        m_compact_display = CompactDisplay::Short;
    else if (compact_display == "long"sv)
        m_compact_display = CompactDisplay::Long;
    else
        VERIFY_NOT_REACHED();
}

StringView NumberFormat::compact_display_string() const
{
    VERIFY(m_compact_display.has_value());

    switch (*m_compact_display) {
    case CompactDisplay::Short:
        return "short"sv;
    case CompactDisplay::Long:
        return "long"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void NumberFormat::set_sign_display(StringView sign_display)
{
    if (sign_display == "auto"sv)
        m_sign_display = SignDisplay::Auto;
    else if (sign_display == "never"sv)
        m_sign_display = SignDisplay::Never;
    else if (sign_display == "always"sv)
        m_sign_display = SignDisplay::Always;
    else if (sign_display == "exceptZero"sv)
        m_sign_display = SignDisplay::ExceptZero;
    else
        VERIFY_NOT_REACHED();
}

StringView NumberFormat::sign_display_string() const
{
    switch (m_sign_display) {
    case SignDisplay::Auto:
        return "auto"sv;
    case SignDisplay::Never:
        return "never"sv;
    case SignDisplay::Always:
        return "always"sv;
    case SignDisplay::ExceptZero:
        return "exceptZero"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

static ALWAYS_INLINE int log10floor(Value number)
{
    if (number.is_number())
        return static_cast<int>(floor(log10(number.as_double())));

    // FIXME: Can we do this without string conversion?
    auto as_string = number.as_bigint().big_integer().to_base(10);
    return as_string.length() - 1;
}

static Value multiply(GlobalObject& global_object, Value lhs, i64 rhs)
{
    if (lhs.is_number())
        return Value(lhs.as_double() * rhs);

    auto rhs_bigint = Crypto::SignedBigInteger::create_from(rhs);
    return js_bigint(global_object.vm(), lhs.as_bigint().big_integer().multiplied_by(rhs_bigint));
}

static Value divide(GlobalObject& global_object, Value lhs, i64 rhs)
{
    if (lhs.is_number())
        return Value(lhs.as_double() / rhs);

    auto rhs_bigint = Crypto::SignedBigInteger::create_from(rhs);
    return js_bigint(global_object.vm(), lhs.as_bigint().big_integer().divided_by(rhs_bigint).quotient);
}

static ALWAYS_INLINE Value multiply_by_power(GlobalObject& global_object, Value number, i64 exponent)
{
    if (exponent < 0)
        return divide(global_object, number, pow(10, -exponent));
    return multiply(global_object, number, pow(10, exponent));
}

static ALWAYS_INLINE Value divide_by_power(GlobalObject& global_object, Value number, i64 exponent)
{
    if (exponent < 0)
        return multiply(global_object, number, pow(10, -exponent));
    return divide(global_object, number, pow(10, exponent));
}

static ALWAYS_INLINE Value rounded(Value number)
{
    if (number.is_number())
        return Value(round(number.as_double()));
    return number;
}

static ALWAYS_INLINE bool is_zero(Value number)
{
    if (number.is_number())
        return number.as_double() == 0.0;
    return number.as_bigint().big_integer() == Crypto::SignedBigInteger::create_from(0);
}

static ALWAYS_INLINE bool is_greater_than(Value number, i64 rhs)
{
    if (number.is_number())
        return number.as_double() > rhs;
    return number.as_bigint().big_integer() > Crypto::SignedBigInteger::create_from(rhs);
}

static ALWAYS_INLINE bool is_greater_than_or_equal(Value number, i64 rhs)
{
    if (number.is_number())
        return number.as_double() >= rhs;
    return number.as_bigint().big_integer() >= Crypto::SignedBigInteger::create_from(rhs);
}

static ALWAYS_INLINE bool is_less_than(Value number, i64 rhs)
{
    if (number.is_number())
        return number.as_double() < rhs;
    return number.as_bigint().big_integer() < Crypto::SignedBigInteger::create_from(rhs);
}

static ALWAYS_INLINE String number_to_string(Value number)
{
    if (number.is_number())
        return number.to_string_without_side_effects();
    return number.as_bigint().big_integer().to_base(10);
}

// 15.1.1 SetNumberFormatDigitOptions ( intlObj, options, mnfdDefault, mxfdDefault, notation ), https://tc39.es/ecma402/#sec-setnfdigitoptions
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

// 15.1.3 CurrencyDigits ( currency ), https://tc39.es/ecma402/#sec-currencydigits
int currency_digits(StringView currency)
{
    // 1. If the ISO 4217 currency and funds code list contains currency as an alphabetic code, return the minor
    //    unit value corresponding to the currency from the list; otherwise, return 2.
    if (auto currency_code = Unicode::get_currency_code(currency); currency_code.has_value())
        return currency_code->minor_unit.value_or(2);
    return 2;
}

// 15.1.5 FormatNumericToString ( intlObject, x ), https://tc39.es/ecma402/#sec-formatnumberstring
FormatResult format_numeric_to_string(GlobalObject& global_object, NumberFormatBase& intl_object, Value number)
{
    // 1. If x < 0 or x is -0𝔽, let isNegative be true; else let isNegative be false.
    bool is_negative = is_less_than(number, 0) || number.is_negative_zero();

    // 2. If isNegative, then
    if (is_negative) {
        // a. Let x be -x.
        number = multiply(global_object, number, -1);
    }

    RawFormatResult result {};

    switch (intl_object.rounding_type()) {
    // 3. If intlObject.[[RoundingType]] is significantDigits, then
    case NumberFormatBase::RoundingType::SignificantDigits:
        // a. Let result be ToRawPrecision(x, intlObject.[[MinimumSignificantDigits]], intlObject.[[MaximumSignificantDigits]]).
        result = to_raw_precision(global_object, number, intl_object.min_significant_digits(), intl_object.max_significant_digits());
        break;

    // 4. Else if intlObject.[[RoundingType]] is fractionDigits, then
    case NumberFormatBase::RoundingType::FractionDigits:
        // a. Let result be ToRawFixed(x, intlObject.[[MinimumFractionDigits]], intlObject.[[MaximumFractionDigits]]).
        result = to_raw_fixed(global_object, number, intl_object.min_fraction_digits(), intl_object.max_fraction_digits());
        break;

    // 5. Else,
    case NumberFormatBase::RoundingType::CompactRounding:
        // a. Assert: intlObject.[[RoundingType]] is compactRounding.
        // b. Let result be ToRawPrecision(x, 1, 2).
        result = to_raw_precision(global_object, number, 1, 2);

        // c. If result.[[IntegerDigitsCount]] > 1, then
        if (result.digits > 1) {
            // i. Let result be ToRawFixed(x, 0, 0).
            result = to_raw_fixed(global_object, number, 0, 0);
        }

        break;

    default:
        VERIFY_NOT_REACHED();
    }

    // 6. Let x be result.[[RoundedNumber]].
    number = result.rounded_number;

    // 7. Let string be result.[[FormattedString]].
    auto string = move(result.formatted_string);

    // 8. Let int be result.[[IntegerDigitsCount]].
    int digits = result.digits;

    // 9. Let minInteger be intlObject.[[MinimumIntegerDigits]].
    int min_integer = intl_object.min_integer_digits();

    // 10. If int < minInteger, then
    if (digits < min_integer) {
        // a. Let forwardZeros be the String consisting of minInteger–int occurrences of the character "0".
        auto forward_zeros = String::repeated('0', min_integer - digits);

        // b. Set string to the string-concatenation of forwardZeros and string.
        string = String::formatted("{}{}", forward_zeros, string);
    }

    // 11. If isNegative, then
    if (is_negative) {
        // a. Let x be -x.
        number = multiply(global_object, number, -1);
    }

    // 12. Return the Record { [[RoundedNumber]]: x, [[FormattedString]]: string }.
    return { move(string), number };
}

// 15.1.6 PartitionNumberPattern ( numberFormat, x ), https://tc39.es/ecma402/#sec-partitionnumberpattern
Vector<PatternPartition> partition_number_pattern(GlobalObject& global_object, NumberFormat& number_format, Value number)
{
    // 1. Let exponent be 0.
    int exponent = 0;

    String formatted_string;

    // 2. If x is NaN, then
    if (number.is_nan()) {
        // a. Let n be an implementation- and locale-dependent (ILD) String value indicating the NaN value.
        formatted_string = Unicode::get_number_system_symbol(number_format.data_locale(), number_format.numbering_system(), Unicode::NumericSymbol::NaN).value_or("NaN"sv);
    }
    // 3. Else if x is +∞, then
    else if (number.is_positive_infinity()) {
        // a. Let n be an ILD String value indicating positive infinity.
        formatted_string = Unicode::get_number_system_symbol(number_format.data_locale(), number_format.numbering_system(), Unicode::NumericSymbol::Infinity).value_or("infinity"sv);
    }
    // 4. Else if x is -∞, then
    else if (number.is_negative_infinity()) {
        // a. Let n be an ILD String value indicating negative infinity.
        // NOTE: The CLDR does not contain unique strings for negative infinity. The negative sign will
        //       be inserted by the pattern returned from GetNumberFormatPattern.
        formatted_string = Unicode::get_number_system_symbol(number_format.data_locale(), number_format.numbering_system(), Unicode::NumericSymbol::Infinity).value_or("infinity"sv);
    }
    // 5. Else,
    else {
        // a. If numberFormat.[[Style]] is "percent", let x be 100 × x.
        if (number_format.style() == NumberFormat::Style::Percent)
            number = multiply(global_object, number, 100);

        // b. Let exponent be ComputeExponent(numberFormat, x).
        exponent = compute_exponent(global_object, number_format, number);

        // c. Let x be x × 10^(-exponent).
        number = multiply_by_power(global_object, number, -exponent);

        // d. Let formatNumberResult be FormatNumericToString(numberFormat, x).
        auto format_number_result = format_numeric_to_string(global_object, number_format, number);

        // e. Let n be formatNumberResult.[[FormattedString]].
        formatted_string = move(format_number_result.formatted_string);

        // f. Let x be formatNumberResult.[[RoundedNumber]].
        number = format_number_result.rounded_number;
    }

    Unicode::NumberFormat found_pattern {};

    // 6. Let pattern be GetNumberFormatPattern(numberFormat, x).
    auto pattern = get_number_format_pattern(number_format, number, found_pattern);
    if (!pattern.has_value())
        return {};

    // 7. Let result be a new empty List.
    Vector<PatternPartition> result;

    // 8. Let patternParts be PartitionPattern(pattern).
    auto pattern_parts = pattern->visit([](auto const& p) { return partition_pattern(p); });

    // 9. For each Record { [[Type]], [[Value]] } patternPart of patternParts, do
    for (auto& pattern_part : pattern_parts) {
        // a. Let p be patternPart.[[Type]].
        auto part = pattern_part.type;

        // b. If p is "literal", then
        if (part == "literal"sv) {
            // i. Append a new Record { [[Type]]: "literal", [[Value]]: patternPart.[[Value]] } as the last element of result.
            result.append({ "literal"sv, move(pattern_part.value) });
        }

        // c. Else if p is equal to "number", then
        else if (part == "number"sv) {
            // i. Let notationSubParts be PartitionNotationSubPattern(numberFormat, x, n, exponent).
            auto notation_sub_parts = partition_notation_sub_pattern(global_object, number_format, number, formatted_string, exponent);
            // ii. Append all elements of notationSubParts to result.
            result.extend(move(notation_sub_parts));
        }

        // d. Else if p is equal to "plusSign", then
        else if (part == "plusSign"sv) {
            // i. Let plusSignSymbol be the ILND String representing the plus sign.
            auto plus_sign_symbol = Unicode::get_number_system_symbol(number_format.data_locale(), number_format.numbering_system(), Unicode::NumericSymbol::PlusSign).value_or("+"sv);
            // ii. Append a new Record { [[Type]]: "plusSign", [[Value]]: plusSignSymbol } as the last element of result.
            result.append({ "plusSign"sv, plus_sign_symbol });
        }

        // e. Else if p is equal to "minusSign", then
        else if (part == "minusSign"sv) {
            // i. Let minusSignSymbol be the ILND String representing the minus sign.
            auto minus_sign_symbol = Unicode::get_number_system_symbol(number_format.data_locale(), number_format.numbering_system(), Unicode::NumericSymbol::MinusSign).value_or("-"sv);
            // ii. Append a new Record { [[Type]]: "minusSign", [[Value]]: minusSignSymbol } as the last element of result.
            result.append({ "minusSign"sv, minus_sign_symbol });
        }

        // f. Else if p is equal to "percentSign" and numberFormat.[[Style]] is "percent", then
        else if ((part == "percentSign"sv) && (number_format.style() == NumberFormat::Style::Percent)) {
            // i. Let percentSignSymbol be the ILND String representing the percent sign.
            auto percent_sign_symbol = Unicode::get_number_system_symbol(number_format.data_locale(), number_format.numbering_system(), Unicode::NumericSymbol::PercentSign).value_or("%"sv);
            // ii. Append a new Record { [[Type]]: "percentSign", [[Value]]: percentSignSymbol } as the last element of result.
            result.append({ "percentSign"sv, percent_sign_symbol });
        }

        // g. Else if p is equal to "unitPrefix" and numberFormat.[[Style]] is "unit", then
        // h. Else if p is equal to "unitSuffix" and numberFormat.[[Style]] is "unit", then
        else if ((part.starts_with("unitIdentifier:"sv)) && (number_format.style() == NumberFormat::Style::Unit)) {
            // Note: Our implementation combines "unitPrefix" and "unitSuffix" into one field, "unitIdentifier".

            auto identifier_index = part.substring_view("unitIdentifier:"sv.length()).to_uint();
            VERIFY(identifier_index.has_value());

            // i. Let unit be numberFormat.[[Unit]].
            // ii. Let unitDisplay be numberFormat.[[UnitDisplay]].
            // iii. Let mu be an ILD String value representing unit before x in unitDisplay form, which may depend on x in languages having different plural forms.
            auto unit_identifier = found_pattern.identifiers[*identifier_index];

            // iv. Append a new Record { [[Type]]: "unit", [[Value]]: mu } as the last element of result.
            result.append({ "unit"sv, unit_identifier });
        }

        // i. Else if p is equal to "currencyCode" and numberFormat.[[Style]] is "currency", then
        // j. Else if p is equal to "currencyPrefix" and numberFormat.[[Style]] is "currency", then
        // k. Else if p is equal to "currencySuffix" and numberFormat.[[Style]] is "currency", then
        //
        // Note: Our implementation manipulates the format string to inject/remove spacing around the
        //       currency code during GetNumberFormatPattern so that we do not have to do currency
        //       display / plurality lookups more than once.
        else if ((part == "currency"sv) && (number_format.style() == NumberFormat::Style::Currency)) {
            result.append({ "currency"sv, number_format.resolve_currency_display() });
        }

        // l. Else,
        else {
            // i. Let unknown be an ILND String based on x and p.
            // ii. Append a new Record { [[Type]]: "unknown", [[Value]]: unknown } as the last element of result.

            // LibUnicode doesn't generate any "unknown" patterns.
            VERIFY_NOT_REACHED();
        }
    }

    // 10. Return result.
    return result;
}

static Vector<StringView> separate_integer_into_groups(Unicode::NumberGroupings const& grouping_sizes, StringView integer)
{
    Utf8View utf8_integer { integer };
    if (utf8_integer.length() <= grouping_sizes.primary_grouping_size)
        return { integer };

    size_t index = utf8_integer.length() - grouping_sizes.primary_grouping_size;
    if (index < grouping_sizes.minimum_grouping_digits)
        return { integer };

    Vector<StringView> groups;

    auto add_group = [&](size_t index, size_t length) {
        groups.prepend(utf8_integer.unicode_substring_view(index, length).as_string());
    };

    add_group(index, grouping_sizes.primary_grouping_size);

    while (index > grouping_sizes.secondary_grouping_size) {
        index -= grouping_sizes.secondary_grouping_size;
        add_group(index, grouping_sizes.secondary_grouping_size);
    }

    if (index > 0)
        add_group(0, index);

    return groups;
}

// 15.1.7 PartitionNotationSubPattern ( numberFormat, x, n, exponent ), https://tc39.es/ecma402/#sec-partitionnotationsubpattern
Vector<PatternPartition> partition_notation_sub_pattern(GlobalObject& global_object, NumberFormat& number_format, Value number, String formatted_string, int exponent)
{
    // 1. Let result be a new empty List.
    Vector<PatternPartition> result;

    auto grouping_sizes = Unicode::get_number_system_groupings(number_format.data_locale(), number_format.numbering_system());
    if (!grouping_sizes.has_value())
        return {};

    // 2. If x is NaN, then
    if (number.is_nan()) {
        // a. Append a new Record { [[Type]]: "nan", [[Value]]: n } as the last element of result.
        result.append({ "nan"sv, move(formatted_string) });
    }
    // 3. Else if x is a non-finite Number, then
    else if (number.is_number() && !number.is_finite_number()) {
        // a. Append a new Record { [[Type]]: "infinity", [[Value]]: n } as the last element of result.
        result.append({ "infinity"sv, move(formatted_string) });
    }
    // 4. Else,
    else {
        // a. Let notationSubPattern be GetNotationSubPattern(numberFormat, exponent).
        auto notation_sub_pattern = get_notation_sub_pattern(number_format, exponent);
        if (!notation_sub_pattern.has_value())
            return {};

        // b. Let patternParts be PartitionPattern(notationSubPattern).
        auto pattern_parts = partition_pattern(*notation_sub_pattern);

        // c. For each Record { [[Type]], [[Value]] } patternPart of patternParts, do
        for (auto& pattern_part : pattern_parts) {
            // i. Let p be patternPart.[[Type]].
            auto part = pattern_part.type;

            // ii. If p is "literal", then
            if (part == "literal"sv) {
                // 1. Append a new Record { [[Type]]: "literal", [[Value]]: patternPart.[[Value]] } as the last element of result.
                result.append({ "literal"sv, move(pattern_part.value) });
            }
            // iii. Else if p is equal to "number", then
            else if (part == "number"sv) {
                // 1. If the numberFormat.[[NumberingSystem]] matches one of the values in the "Numbering System" column of Table 10 below, then
                //     a. Let digits be a List whose 10 String valued elements are the UTF-16 string representations of the 10 digits specified in the "Digits" column of the matching row in Table 10.
                //     b. Replace each digit in n with the value of digits[digit].
                // 2. Else use an implementation dependent algorithm to map n to the appropriate representation of n in the given numbering system.
                formatted_string = Unicode::replace_digits_for_number_system(number_format.numbering_system(), formatted_string);

                // 3. Let decimalSepIndex be ! StringIndexOf(n, ".", 0).
                auto decimal_sep_index = formatted_string.find('.');

                StringView integer;
                Optional<StringView> fraction;

                // 4. If decimalSepIndex > 0, then
                if (decimal_sep_index.has_value() && (*decimal_sep_index > 0)) {
                    // a. Let integer be the substring of n from position 0, inclusive, to position decimalSepIndex, exclusive.
                    integer = formatted_string.substring_view(0, *decimal_sep_index);
                    // b. Let fraction be the substring of n from position decimalSepIndex, exclusive, to the end of n.
                    fraction = formatted_string.substring_view(*decimal_sep_index + 1);
                }
                // 5. Else,
                else {
                    // a. Let integer be n.
                    integer = formatted_string;
                    // b. Let fraction be undefined.
                }

                bool use_grouping = number_format.use_grouping();

                // FIXME: The spec doesn't indicate this, but grouping should be disabled for numbers less than 10,000 when the notation is compact.
                //        This is addressed in Intl.NumberFormat V3 with the "min2" [[UseGrouping]] option. However, test262 explicitly expects this
                //        behavior in the "de-DE" locale tests, because this is how ICU (and therefore V8, SpiderMoney, etc.) has always behaved.
                //
                //        So, in locales "de-*", we must have:
                //            Intl.NumberFormat("de", {notation: "compact"}).format(1234) === "1234"
                //            Intl.NumberFormat("de", {notation: "compact"}).format(12345) === "12.345"
                //            Intl.NumberFormat("de").format(1234) === "1.234"
                //            Intl.NumberFormat("de").format(12345) === "12.345"
                //
                //        See: https://github.com/tc39/proposal-intl-numberformat-v3/issues/3
                if (number_format.has_compact_format())
                    use_grouping = is_greater_than_or_equal(number, 10'000);

                // 6. If the numberFormat.[[UseGrouping]] is true, then
                if (use_grouping) {
                    // a. Let groupSepSymbol be the implementation-, locale-, and numbering system-dependent (ILND) String representing the grouping separator.
                    auto group_sep_symbol = Unicode::get_number_system_symbol(number_format.data_locale(), number_format.numbering_system(), Unicode::NumericSymbol::Group).value_or(","sv);

                    // b. Let groups be a List whose elements are, in left to right order, the substrings defined by ILND set of locations within the integer.
                    auto groups = separate_integer_into_groups(*grouping_sizes, integer);

                    // c. Assert: The number of elements in groups List is greater than 0.
                    VERIFY(!groups.is_empty());

                    // d. Repeat, while groups List is not empty,
                    while (!groups.is_empty()) {
                        // i. Remove the first element from groups and let integerGroup be the value of that element.
                        auto integer_group = groups.take_first();

                        // ii. Append a new Record { [[Type]]: "integer", [[Value]]: integerGroup } as the last element of result.
                        result.append({ "integer"sv, integer_group });

                        // iii. If groups List is not empty, then
                        if (!groups.is_empty()) {
                            // i. Append a new Record { [[Type]]: "group", [[Value]]: groupSepSymbol } as the last element of result.
                            result.append({ "group"sv, group_sep_symbol });
                        }
                    }
                }
                // 7. Else,
                else {
                    // a. Append a new Record { [[Type]]: "integer", [[Value]]: integer } as the last element of result.
                    result.append({ "integer"sv, integer });
                }

                // 8. If fraction is not undefined, then
                if (fraction.has_value()) {
                    // a. Let decimalSepSymbol be the ILND String representing the decimal separator.
                    auto decimal_sep_symbol = Unicode::get_number_system_symbol(number_format.data_locale(), number_format.numbering_system(), Unicode::NumericSymbol::Decimal).value_or("."sv);
                    // b. Append a new Record { [[Type]]: "decimal", [[Value]]: decimalSepSymbol } as the last element of result.
                    result.append({ "decimal"sv, decimal_sep_symbol });
                    // c. Append a new Record { [[Type]]: "fraction", [[Value]]: fraction } as the last element of result.
                    result.append({ "fraction"sv, fraction.release_value() });
                }
            }
            // iv. Else if p is equal to "compactSymbol", then
            // v. Else if p is equal to "compactName", then
            else if (part.starts_with("compactIdentifier:"sv)) {
                // Note: Our implementation combines "compactSymbol" and "compactName" into one field, "compactIdentifier".

                auto identifier_index = part.substring_view("compactIdentifier:"sv.length()).to_uint();
                VERIFY(identifier_index.has_value());

                // 1. Let compactSymbol be an ILD string representing exponent in short form, which may depend on x in languages having different plural forms. The implementation must be able to provide this string, or else the pattern would not have a "{compactSymbol}" placeholder.
                auto compact_identifier = number_format.compact_format().identifiers[*identifier_index];

                // 2. Append a new Record { [[Type]]: "compact", [[Value]]: compactSymbol } as the last element of result.
                result.append({ "compact"sv, compact_identifier });
            }
            // vi. Else if p is equal to "scientificSeparator", then
            else if (part == "scientificSeparator"sv) {
                // 1. Let scientificSeparator be the ILND String representing the exponent separator.
                auto scientific_separator = Unicode::get_number_system_symbol(number_format.data_locale(), number_format.numbering_system(), Unicode::NumericSymbol::Exponential).value_or("E"sv);
                // 2. Append a new Record { [[Type]]: "exponentSeparator", [[Value]]: scientificSeparator } as the last element of result.
                result.append({ "exponentSeparator"sv, scientific_separator });
            }
            // vii. Else if p is equal to "scientificExponent", then
            else if (part == "scientificExponent"sv) {
                // 1. If exponent < 0, then
                if (exponent < 0) {
                    // a. Let minusSignSymbol be the ILND String representing the minus sign.
                    auto minus_sign_symbol = Unicode::get_number_system_symbol(number_format.data_locale(), number_format.numbering_system(), Unicode::NumericSymbol::MinusSign).value_or("-"sv);

                    // b. Append a new Record { [[Type]]: "exponentMinusSign", [[Value]]: minusSignSymbol } as the last element of result.
                    result.append({ "exponentMinusSign"sv, minus_sign_symbol });

                    // c. Let exponent be -exponent.
                    exponent *= -1;
                }

                // 2. Let exponentResult be ToRawFixed(exponent, 1, 0, 0).
                // Note: See the implementation of ToRawFixed for why we do not pass the 1.
                auto exponent_result = to_raw_fixed(global_object, Value(exponent), 0, 0);

                // FIXME: The spec does not say to do this, but all of major engines perform this replacement.
                //        Without this, formatting with non-Latin numbering systems will produce non-localized results.
                exponent_result.formatted_string = Unicode::replace_digits_for_number_system(number_format.numbering_system(), exponent_result.formatted_string);

                // 3. Append a new Record { [[Type]]: "exponentInteger", [[Value]]: exponentResult.[[FormattedString]] } as the last element of result.
                result.append({ "exponentInteger"sv, move(exponent_result.formatted_string) });
            }
            // viii. Else,
            else {
                // 1. Let unknown be an ILND String based on x and p.
                // 2. Append a new Record { [[Type]]: "unknown", [[Value]]: unknown } as the last element of result.

                // LibUnicode doesn't generate any "unknown" patterns.
                VERIFY_NOT_REACHED();
            }
        }
    }

    // 5. Return result.
    return result;
}

// 15.1.8 FormatNumeric ( numberFormat, x ), https://tc39.es/ecma402/#sec-formatnumber
String format_numeric(GlobalObject& global_object, NumberFormat& number_format, Value number)
{
    // 1. Let parts be ? PartitionNumberPattern(numberFormat, x).
    // Note: Our implementation of PartitionNumberPattern does not throw.
    auto parts = partition_number_pattern(global_object, number_format, number);

    // 2. Let result be the empty String.
    StringBuilder result;

    // 3. For each Record { [[Type]], [[Value]] } part in parts, do
    for (auto& part : parts) {
        // a. Set result to the string-concatenation of result and part.[[Value]].
        result.append(move(part.value));
    }

    // 4. Return result.
    return result.build();
}

// 15.1.9 FormatNumericToParts ( numberFormat, x ), https://tc39.es/ecma402/#sec-formatnumbertoparts
Array* format_numeric_to_parts(GlobalObject& global_object, NumberFormat& number_format, Value number)
{
    auto& vm = global_object.vm();

    // 1. Let parts be ? PartitionNumberPattern(numberFormat, x).
    // Note: Our implementation of PartitionNumberPattern does not throw.
    auto parts = partition_number_pattern(global_object, number_format, number);

    // 2. Let result be ArrayCreate(0).
    auto* result = MUST(Array::create(global_object, 0));

    // 3. Let n be 0.
    size_t n = 0;

    // 4. For each Record { [[Type]], [[Value]] } part in parts, do
    for (auto& part : parts) {
        // a. Let O be OrdinaryObjectCreate(%Object.prototype%).
        auto* object = Object::create(global_object, global_object.object_prototype());

        // b. Perform ! CreateDataPropertyOrThrow(O, "type", part.[[Type]]).
        MUST(object->create_data_property_or_throw(vm.names.type, js_string(vm, part.type)));

        // c. Perform ! CreateDataPropertyOrThrow(O, "value", part.[[Value]]).
        MUST(object->create_data_property_or_throw(vm.names.value, js_string(vm, move(part.value))));

        // d. Perform ! CreateDataPropertyOrThrow(result, ! ToString(n), O).
        MUST(result->create_data_property_or_throw(n, object));

        // e. Increment n by 1.
        ++n;
    }

    // 5. Return result.
    return result;
}

static String cut_trailing_zeroes(StringView string, int cut)
{
    // These steps are exactly the same between ToRawPrecision and ToRawFixed.

    // Repeat, while cut > 0 and the last character of m is "0",
    while ((cut > 0) && string.ends_with('0')) {
        // Remove the last character from m.
        string = string.substring_view(0, string.length() - 1);

        // Decrease cut by 1.
        --cut;
    }

    // If the last character of m is ".", then
    if (string.ends_with('.')) {
        // Remove the last character from m.
        string = string.substring_view(0, string.length() - 1);
    }

    return string.to_string();
}

// 15.1.10 ToRawPrecision ( x, minPrecision, maxPrecision ), https://tc39.es/ecma402/#sec-torawprecision
RawFormatResult to_raw_precision(GlobalObject& global_object, Value number, int min_precision, int max_precision)
{
    RawFormatResult result {};

    // 1. Set x to ℝ(x).

    // 2. Let p be maxPrecision.
    int precision = max_precision;
    int exponent = 0;

    // 3. If x = 0, then
    if (is_zero(number)) {
        // a. Let m be the String consisting of p occurrences of the character "0".
        result.formatted_string = String::repeated('0', precision);

        // b. Let e be 0.
        exponent = 0;

        // c. Let xFinal be 0.
        result.rounded_number = Value(0);
    }
    // 4. Else,
    else {
        // FIXME: The result of these steps isn't entirely accurate for large values of 'p' (which
        //        defaults to 21, resulting in numbers on the order of 10^21). Either AK::format or
        //        our Number::toString AO (double_to_string in Value.cpp) will need to be improved
        //        to produce more accurate results.

        // a. Let e and n be integers such that 10^(p–1) ≤ n < 10^p and for which n × 10^(e–p+1) – x is as close to zero as possible.
        //    If there are two such sets of e and n, pick the e and n for which n × 10^(e–p+1) is larger.
        exponent = log10floor(number);
        Value n;

        if (number.is_number()) {
            n = rounded(divide_by_power(global_object, number, exponent - precision + 1));
        } else {
            // NOTE: In order to round the BigInt to the proper precision, this computation is initially off by a
            //       factor of 10. This lets us inspect the ones digit and then round up if needed.
            n = divide_by_power(global_object, number, exponent - precision);

            // FIXME: Can we do this without string conversion?
            auto digits = n.as_bigint().big_integer().to_base(10);
            auto digit = digits.substring_view(digits.length() - 1);

            n = divide(global_object, n, 10);
            if (digit.to_uint().value() >= 5)
                n = js_bigint(global_object.vm(), n.as_bigint().big_integer().plus(Crypto::SignedBigInteger::create_from(1)));
        }

        // b. Let m be the String consisting of the digits of the decimal representation of n (in order, with no leading zeroes).
        result.formatted_string = number_to_string(n);

        // c. Let xFinal be n × 10^(e–p+1).
        result.rounded_number = multiply_by_power(global_object, n, exponent - precision + 1);
    }

    // 5. If e ≥ p–1, then
    if (exponent >= (precision - 1)) {
        // a. Let m be the string-concatenation of m and e–p+1 occurrences of the character "0".
        result.formatted_string = String::formatted(
            "{}{}",
            result.formatted_string,
            String::repeated('0', exponent - precision + 1));

        // b. Let int be e+1.
        result.digits = exponent + 1;
    }
    // 6. Else if e ≥ 0, then
    else if (exponent >= 0) {
        // a. Let m be the string-concatenation of the first e+1 characters of m, the character ".", and the remaining p–(e+1) characters of m.
        result.formatted_string = String::formatted(
            "{}.{}",
            result.formatted_string.substring_view(0, exponent + 1),
            result.formatted_string.substring_view(exponent + 1));

        // b. Let int be e+1.
        result.digits = exponent + 1;
    }
    // 7. Else,
    else {
        // a. Assert: e < 0.
        // b. Let m be the string-concatenation of the String value "0.", –(e+1) occurrences of the character "0", and m.
        result.formatted_string = String::formatted(
            "0.{}{}",
            String::repeated('0', -1 * (exponent + 1)),
            result.formatted_string);

        // c. Let int be 1.
        result.digits = 1;
    }

    // 8. If m contains the character ".", and maxPrecision > minPrecision, then
    if (result.formatted_string.contains('.') && (max_precision > min_precision)) {
        // a. Let cut be maxPrecision – minPrecision.
        int cut = max_precision - min_precision;

        // Steps 8b-8c are implemented by cut_trailing_zeroes.
        result.formatted_string = cut_trailing_zeroes(result.formatted_string, cut);
    }

    // 9. Return the Record { [[FormattedString]]: m, [[RoundedNumber]]: xFinal, [[IntegerDigitsCount]]: int }.
    return result;
}

// 15.1.11 ToRawFixed ( x, minInteger, minFraction, maxFraction ), https://tc39.es/ecma402/#sec-torawfixed
// NOTE: The spec has a mistake here. The minInteger parameter is unused and is not provided by FormatNumericToString.
RawFormatResult to_raw_fixed(GlobalObject& global_object, Value number, int min_fraction, int max_fraction)
{
    RawFormatResult result {};

    // 1. Set x to ℝ(x).

    // 2. Let f be maxFraction.
    int fraction = max_fraction;

    // 3. Let n be an integer for which the exact mathematical value of n / 10^f – x is as close to zero as possible. If there are two such n, pick the larger n.
    auto n = rounded(multiply_by_power(global_object, number, fraction));

    // 4. Let xFinal be n / 10^f.
    result.rounded_number = divide_by_power(global_object, n, fraction);

    // 5. If n = 0, let m be the String "0". Otherwise, let m be the String consisting of the digits of the decimal representation of n (in order, with no leading zeroes).
    result.formatted_string = is_zero(n) ? String("0"sv) : number_to_string(n);

    // 6. If f ≠ 0, then
    if (fraction != 0) {
        // a. Let k be the number of characters in m.
        auto decimals = result.formatted_string.length();

        // b. If k ≤ f, then
        if (decimals <= static_cast<size_t>(fraction)) {
            // i. Let z be the String value consisting of f+1–k occurrences of the character "0".
            auto zeroes = String::repeated('0', fraction + 1 - decimals);

            // ii. Let m be the string-concatenation of z and m.
            result.formatted_string = String::formatted("{}{}", zeroes, result.formatted_string);

            // iii. Let k be f+1.
            decimals = fraction + 1;
        }

        // c. Let a be the first k–f characters of m, and let b be the remaining f characters of m.
        auto a = result.formatted_string.substring_view(0, decimals - fraction);
        auto b = result.formatted_string.substring_view(decimals - fraction, fraction);

        // d. Let m be the string-concatenation of a, ".", and b.
        result.formatted_string = String::formatted("{}.{}", a, b);

        // e. Let int be the number of characters in a.
        result.digits = a.length();
    }
    // 7. Else, let int be the number of characters in m.
    else {
        result.digits = result.formatted_string.length();
    }

    // 8. Let cut be maxFraction – minFraction.
    int cut = max_fraction - min_fraction;

    // Steps 9-10 are implemented by cut_trailing_zeroes.
    result.formatted_string = cut_trailing_zeroes(result.formatted_string, cut);

    // 11. Return the Record { [[FormattedString]]: m, [[RoundedNumber]]: xFinal, [[IntegerDigitsCount]]: int }.
    return result;
}

// 15.1.13 SetNumberFormatUnitOptions ( intlObj, options ), https://tc39.es/ecma402/#sec-setnumberformatunitoptions
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
    //     a. If the result of IsWellFormedCurrencyCode(currency) is false, throw a RangeError exception.
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
    //     a. If the result of IsWellFormedUnitIdentifier(unit) is false, throw a RangeError exception.
    else if (!is_well_formed_unit_identifier(unit.as_string().string()))
        return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, unit, "unit"sv);

    // 13. Let unitDisplay be ? GetOption(options, "unitDisplay", "string", « "short", "narrow", "long" », "short").
    auto unit_display = TRY(get_option(global_object, options, vm.names.unitDisplay, Value::Type::String, { "short"sv, "narrow"sv, "long"sv }, "short"sv));

    // 14. If style is "currency", then
    if (intl_object.style() == NumberFormat::Style::Currency) {
        // a. Let currency be the result of converting currency to upper case as specified in 6.1.
        // b. Set intlObj.[[Currency]] to currency.
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

// 15.1.14 GetNumberFormatPattern ( numberFormat, x ), https://tc39.es/ecma402/#sec-getnumberformatpattern
Optional<Variant<StringView, String>> get_number_format_pattern(NumberFormat& number_format, Value number, Unicode::NumberFormat& found_pattern)
{
    auto as_number = [&]() {
        if (number.is_number())
            return number.as_double();

        // FIXME: This should be okay for now as our naive Unicode::select_pattern_with_plurality implementation
        //        checks against just a few specific small values. But revisit this if precision becomes a concern.
        return number.as_bigint().big_integer().to_double();
    };

    // 1. Let localeData be %NumberFormat%.[[LocaleData]].
    // 2. Let dataLocale be numberFormat.[[DataLocale]].
    // 3. Let dataLocaleData be localeData.[[<dataLocale>]].
    // 4. Let patterns be dataLocaleData.[[patterns]].
    // 5. Assert: patterns is a Record (see 15.3.3).
    Optional<Unicode::NumberFormat> patterns;

    // 6. Let style be numberFormat.[[Style]].
    switch (number_format.style()) {
    // 7. If style is "percent", then
    case NumberFormat::Style::Percent:
        // a. Let patterns be patterns.[[percent]].
        patterns = Unicode::get_standard_number_system_format(number_format.data_locale(), number_format.numbering_system(), Unicode::StandardNumberFormatType::Percent);
        break;

    // 8. Else if style is "unit", then
    case NumberFormat::Style::Unit: {
        // a. Let unit be numberFormat.[[Unit]].
        // b. Let unitDisplay be numberFormat.[[UnitDisplay]].
        // c. Let patterns be patterns.[[unit]].
        // d. If patterns doesn't have a field [[<unit>]], then
        //     i. Let unit be "fallback".
        // e. Let patterns be patterns.[[<unit>]].
        // f. Let patterns be patterns.[[<unitDisplay>]].
        auto formats = Unicode::get_unit_formats(number_format.data_locale(), number_format.unit(), number_format.unit_display());
        patterns = Unicode::select_pattern_with_plurality(formats, as_number());
        break;
    }

    // 9. Else if style is "currency", then
    case NumberFormat::Style::Currency:
        // a. Let currency be numberFormat.[[Currency]].
        // b. Let currencyDisplay be numberFormat.[[CurrencyDisplay]].
        // c. Let currencySign be numberFormat.[[CurrencySign]].
        // d. Let patterns be patterns.[[currency]].
        // e. If patterns doesn't have a field [[<currency>]], then
        //     i. Let currency be "fallback".
        // f. Let patterns be patterns.[[<currency>]].
        // g. Let patterns be patterns.[[<currencyDisplay>]].
        // h. Let patterns be patterns.[[<currencySign>]].

        // Handling of other [[CurrencyDisplay]] options will occur after [[SignDisplay]].
        if (number_format.currency_display() == NumberFormat::CurrencyDisplay::Name) {
            auto formats = Unicode::get_compact_number_system_formats(number_format.data_locale(), number_format.numbering_system(), Unicode::CompactNumberFormatType::CurrencyUnit);

            auto maybe_patterns = Unicode::select_pattern_with_plurality(formats, as_number());
            if (maybe_patterns.has_value()) {
                patterns = maybe_patterns.release_value();
                break;
            }
        }

        switch (number_format.currency_sign()) {
        case NumberFormat::CurrencySign::Standard:
            patterns = Unicode::get_standard_number_system_format(number_format.data_locale(), number_format.numbering_system(), Unicode::StandardNumberFormatType::Currency);
            break;
        case NumberFormat::CurrencySign::Accounting:
            patterns = Unicode::get_standard_number_system_format(number_format.data_locale(), number_format.numbering_system(), Unicode::StandardNumberFormatType::Accounting);
            break;
        }

        break;

    // 10. Else,
    case NumberFormat::Style::Decimal:
        // a. Assert: style is "decimal".
        // b. Let patterns be patterns.[[decimal]].
        patterns = Unicode::get_standard_number_system_format(number_format.data_locale(), number_format.numbering_system(), Unicode::StandardNumberFormatType::Decimal);
        break;

    default:
        VERIFY_NOT_REACHED();
    }

    if (!patterns.has_value())
        return {};

    StringView pattern;

    bool is_positive_zero = number.is_positive_zero() || (number.is_bigint() && is_zero(number));
    bool is_negative_zero = number.is_negative_zero();
    bool is_nan = number.is_nan();

    // 11. Let signDisplay be numberFormat.[[SignDisplay]].
    switch (number_format.sign_display()) {
    // 12. If signDisplay is "never", then
    case NumberFormat::SignDisplay::Never:
        // a. Let pattern be patterns.[[zeroPattern]].
        pattern = patterns->zero_format;
        break;

    // 13. Else if signDisplay is "auto", then
    case NumberFormat::SignDisplay::Auto:
        // a. If x is 0 or x > 0 or x is NaN, then
        if (is_positive_zero || is_greater_than(number, 0) || is_nan) {
            // i. Let pattern be patterns.[[zeroPattern]].
            pattern = patterns->zero_format;
        }
        // b. Else,
        else {
            // i. Let pattern be patterns.[[negativePattern]].
            pattern = patterns->negative_format;
        }
        break;

    // 14. Else if signDisplay is "always", then
    case NumberFormat::SignDisplay::Always:
        // a. If x is 0 or x > 0 or x is NaN, then
        if (is_positive_zero || is_greater_than(number, 0) || is_nan) {
            // i. Let pattern be patterns.[[positivePattern]].
            pattern = patterns->positive_format;
        }
        // b. Else,
        else {
            // i. Let pattern be patterns.[[negativePattern]].
            pattern = patterns->negative_format;
        }
        break;

    // 15. Else,
    case NumberFormat::SignDisplay::ExceptZero:
        // a. Assert: signDisplay is "exceptZero".
        // b. If x is 0 or x is -0 or x is NaN, then
        if (is_positive_zero || is_negative_zero || is_nan) {
            // i. Let pattern be patterns.[[zeroPattern]].
            pattern = patterns->zero_format;
        }
        // c. Else if x > 0, then
        else if (is_greater_than(number, 0)) {
            // i. Let pattern be patterns.[[positivePattern]].
            pattern = patterns->positive_format;
        }
        // d. Else,
        else {
            // i. Let pattern be patterns.[[negativePattern]].
            pattern = patterns->negative_format;
        }
        break;

    default:
        VERIFY_NOT_REACHED();
    }

    found_pattern = patterns.release_value();

    // Handling of steps 9b/9g: Depending on the currency display and the format pattern found above,
    // we might need to mutate the format pattern to inject a space between the currency display and
    // the currency number.
    if (number_format.style() == NumberFormat::Style::Currency) {
        auto modified_pattern = Unicode::augment_currency_format_pattern(number_format.resolve_currency_display(), pattern);
        if (modified_pattern.has_value())
            return modified_pattern.release_value();
    }

    // 16. Return pattern.
    return pattern;
}

// 15.1.15 GetNotationSubPattern ( numberFormat, exponent ), https://tc39.es/ecma402/#sec-getnotationsubpattern
Optional<StringView> get_notation_sub_pattern(NumberFormat& number_format, int exponent)
{
    // 1. Let localeData be %NumberFormat%.[[LocaleData]].
    // 2. Let dataLocale be numberFormat.[[DataLocale]].
    // 3. Let dataLocaleData be localeData.[[<dataLocale>]].
    // 4. Let notationSubPatterns be dataLocaleData.[[notationSubPatterns]].
    // 5. Assert: notationSubPatterns is a Record (see 15.3.3).

    // 6. Let notation be numberFormat.[[Notation]].
    auto notation = number_format.notation();

    // 7. If notation is "scientific" or notation is "engineering", then
    if ((notation == NumberFormat::Notation::Scientific) || (notation == NumberFormat::Notation::Engineering)) {
        // a. Return notationSubPatterns.[[scientific]].
        auto notation_sub_patterns = Unicode::get_standard_number_system_format(number_format.data_locale(), number_format.numbering_system(), Unicode::StandardNumberFormatType::Scientific);
        if (!notation_sub_patterns.has_value())
            return {};

        return notation_sub_patterns->zero_format;
    }
    // 8. Else if exponent is not 0, then
    else if (exponent != 0) {
        // a. Assert: notation is "compact".
        VERIFY(notation == NumberFormat::Notation::Compact);

        // b. Let compactDisplay be numberFormat.[[CompactDisplay]].
        // c. Let compactPatterns be notationSubPatterns.[[compact]].[[<compactDisplay>]].
        // d. Return compactPatterns.[[<exponent>]].
        if (number_format.has_compact_format())
            return number_format.compact_format().zero_format;
    }

    // 9. Else,
    //     a. Return "{number}".
    return "{number}"sv;
}

// 15.1.16 ComputeExponent ( numberFormat, x ), https://tc39.es/ecma402/#sec-computeexponent
int compute_exponent(GlobalObject& global_object, NumberFormat& number_format, Value number)
{
    // 1. If x = 0, then
    if (is_zero(number)) {
        // a. Return 0.
        return 0;
    }

    // 2. If x < 0, then
    if (is_less_than(number, 0)) {
        // a. Let x = -x.
        number = multiply(global_object, number, -1);
    }

    // 3. Let magnitude be the base 10 logarithm of x rounded down to the nearest integer.
    int magnitude = log10floor(number);

    // 4. Let exponent be ComputeExponentForMagnitude(numberFormat, magnitude).
    int exponent = compute_exponent_for_magnitude(number_format, magnitude);

    // 5. Let x be x × 10^(-exponent).
    number = multiply_by_power(global_object, number, -exponent);

    // 6. Let formatNumberResult be FormatNumericToString(numberFormat, x).
    auto format_number_result = format_numeric_to_string(global_object, number_format, number);

    // 7. If formatNumberResult.[[RoundedNumber]] = 0, then
    if (is_zero(format_number_result.rounded_number)) {
        // a. Return exponent.
        return exponent;
    }

    // 8. Let newMagnitude be the base 10 logarithm of formatNumberResult.[[RoundedNumber]] rounded down to the nearest integer.
    int new_magnitude = log10floor(format_number_result.rounded_number);

    // 9. If newMagnitude is magnitude – exponent, then
    if (new_magnitude == magnitude - exponent) {
        // a. Return exponent.
        return exponent;
    }

    // 10. Return ComputeExponentForMagnitude(numberFormat, magnitude + 1).
    return compute_exponent_for_magnitude(number_format, magnitude + 1);
}

// 15.1.17 ComputeExponentForMagnitude ( numberFormat, magnitude ), https://tc39.es/ecma402/#sec-computeexponentformagnitude
int compute_exponent_for_magnitude(NumberFormat& number_format, int magnitude)
{
    // 1. Let notation be numberFormat.[[Notation]].
    switch (number_format.notation()) {
    // 2. If notation is "standard", then
    case NumberFormat::Notation::Standard:
        // a. Return 0.
        return 0;

    // 3. Else if notation is "scientific", then
    case NumberFormat::Notation::Scientific:
        // a. Return magnitude.
        return magnitude;

    // 4. Else if notation is "engineering", then
    case NumberFormat::Notation::Engineering: {
        // a. Let thousands be the greatest integer that is not greater than magnitude / 3.
        double thousands = floor(static_cast<double>(magnitude) / 3.0);

        // b. Return thousands × 3.
        return static_cast<int>(thousands) * 3;
    }

    // 5. Else,
    case NumberFormat::Notation::Compact: {
        // a. Assert: notation is "compact".
        VERIFY(number_format.has_compact_display());

        // b. Let exponent be an implementation- and locale-dependent (ILD) integer by which to scale a number of the given magnitude in compact notation for the current locale.
        // c. Return exponent.
        Vector<Unicode::NumberFormat> format_rules;

        if (number_format.style() == NumberFormat::Style::Currency)
            format_rules = Unicode::get_compact_number_system_formats(number_format.data_locale(), number_format.numbering_system(), Unicode::CompactNumberFormatType::CurrencyShort);
        else if (number_format.compact_display() == NumberFormat::CompactDisplay::Long)
            format_rules = Unicode::get_compact_number_system_formats(number_format.data_locale(), number_format.numbering_system(), Unicode::CompactNumberFormatType::DecimalLong);
        else
            format_rules = Unicode::get_compact_number_system_formats(number_format.data_locale(), number_format.numbering_system(), Unicode::CompactNumberFormatType::DecimalShort);

        Unicode::NumberFormat const* best_number_format = nullptr;

        for (auto const& format_rule : format_rules) {
            if (format_rule.magnitude > magnitude)
                break;
            best_number_format = &format_rule;
        }

        if (best_number_format == nullptr)
            return 0;

        number_format.set_compact_format(*best_number_format);
        return best_number_format->exponent;
    }

    default:
        VERIFY_NOT_REACHED();
    }
}

}
