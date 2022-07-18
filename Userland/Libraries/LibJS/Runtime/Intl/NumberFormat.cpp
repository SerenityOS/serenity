/*
 * Copyright (c) 2021-2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/Utf8View.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/NumberFormat.h>
#include <LibJS/Runtime/Intl/NumberFormatFunction.h>
#include <LibJS/Runtime/Intl/PluralRules.h>
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
    case RoundingType::MorePrecision:
        return "morePrecision"sv;
    case RoundingType::LessPrecision:
        return "lessPrecision"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

StringView NumberFormatBase::rounding_mode_string() const
{
    switch (m_rounding_mode) {
    case RoundingMode::Ceil:
        return "ceil"sv;
    case RoundingMode::Expand:
        return "expand"sv;
    case RoundingMode::Floor:
        return "floor"sv;
    case RoundingMode::HalfCeil:
        return "halfCeil"sv;
    case RoundingMode::HalfEven:
        return "halfEven"sv;
    case RoundingMode::HalfExpand:
        return "halfExpand"sv;
    case RoundingMode::HalfFloor:
        return "halfFloor"sv;
    case RoundingMode::HalfTrunc:
        return "halfTrunc"sv;
    case RoundingMode::Trunc:
        return "trunc"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void NumberFormatBase::set_rounding_mode(StringView rounding_mode)
{
    if (rounding_mode == "ceil"sv)
        m_rounding_mode = RoundingMode::Ceil;
    else if (rounding_mode == "expand"sv)
        m_rounding_mode = RoundingMode::Expand;
    else if (rounding_mode == "floor"sv)
        m_rounding_mode = RoundingMode::Floor;
    else if (rounding_mode == "halfCeil"sv)
        m_rounding_mode = RoundingMode::HalfCeil;
    else if (rounding_mode == "halfEven"sv)
        m_rounding_mode = RoundingMode::HalfEven;
    else if (rounding_mode == "halfExpand"sv)
        m_rounding_mode = RoundingMode::HalfExpand;
    else if (rounding_mode == "halfFloor"sv)
        m_rounding_mode = RoundingMode::HalfFloor;
    else if (rounding_mode == "halfTrunc"sv)
        m_rounding_mode = RoundingMode::HalfTrunc;
    else if (rounding_mode == "trunc"sv)
        m_rounding_mode = RoundingMode::Trunc;
    else
        VERIFY_NOT_REACHED();
}

StringView NumberFormatBase::trailing_zero_display_string() const
{
    switch (m_trailing_zero_display) {
    case TrailingZeroDisplay::Auto:
        return "auto"sv;
    case TrailingZeroDisplay::StripIfInteger:
        return "stripIfInteger"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void NumberFormatBase::set_trailing_zero_display(StringView trailing_zero_display)
{
    if (trailing_zero_display == "auto"sv)
        m_trailing_zero_display = TrailingZeroDisplay::Auto;
    else if (trailing_zero_display == "stripIfInteger"sv)
        m_trailing_zero_display = TrailingZeroDisplay::StripIfInteger;
    else
        VERIFY_NOT_REACHED();
}

Value NumberFormat::use_grouping_to_value(GlobalObject& global_object) const
{
    auto& vm = global_object.vm();

    switch (m_use_grouping) {
    case UseGrouping::Always:
        return js_string(vm, "always"sv);
    case UseGrouping::Auto:
        return js_string(vm, "auto"sv);
    case UseGrouping::Min2:
        return js_string(vm, "min2"sv);
    case UseGrouping::False:
        return Value(false);
    default:
        VERIFY_NOT_REACHED();
    }
}

void NumberFormat::set_use_grouping(StringOrBoolean const& use_grouping)
{
    use_grouping.visit(
        [this](StringView grouping) {
            if (grouping == "always"sv)
                m_use_grouping = UseGrouping::Always;
            else if (grouping == "auto"sv)
                m_use_grouping = UseGrouping::Auto;
            else if (grouping == "min2"sv)
                m_use_grouping = UseGrouping::Min2;
            else
                VERIFY_NOT_REACHED();
        },
        [this](bool grouping) {
            VERIFY(!grouping);
            m_use_grouping = UseGrouping::False;
        });
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
    else if (sign_display == "negative"sv)
        m_sign_display = SignDisplay::Negative;
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
    case SignDisplay::Negative:
        return "negative"sv;
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

static Value subtract(GlobalObject& global_object, Value lhs, Value rhs)
{
    if (lhs.is_number())
        return Value(lhs.as_double() - rhs.as_double());
    return js_bigint(global_object.vm(), lhs.as_bigint().big_integer().minus(rhs.as_bigint().big_integer()));
}

static Value multiply(GlobalObject& global_object, Value lhs, Checked<i32> rhs)
{
    if (lhs.is_number())
        return Value(lhs.as_double() * rhs.value());

    auto rhs_bigint = Crypto::SignedBigInteger::create_from(rhs.value());
    return js_bigint(global_object.vm(), lhs.as_bigint().big_integer().multiplied_by(rhs_bigint));
}

static Value divide(GlobalObject& global_object, Value lhs, Checked<i32> rhs)
{
    if (lhs.is_number())
        return Value(lhs.as_double() / rhs.value());

    auto rhs_bigint = Crypto::SignedBigInteger::create_from(rhs.value());
    return js_bigint(global_object.vm(), lhs.as_bigint().big_integer().divided_by(rhs_bigint).quotient);
}

static Value divide(GlobalObject& global_object, Value lhs, Value rhs)
{
    if (lhs.is_number())
        return Value(lhs.as_double() / rhs.as_double());
    return js_bigint(global_object.vm(), lhs.as_bigint().big_integer().divided_by(rhs.as_bigint().big_integer()).quotient);
}

static Crypto::SignedBigInteger bigint_power(Checked<i32> base, Checked<i32> exponent)
{
    VERIFY(exponent >= 0);

    auto base_bigint = Crypto::SignedBigInteger::create_from(base.value());
    auto result = Crypto::SignedBigInteger::create_from(1);

    for (i32 i = 0; i < exponent; ++i)
        result = result.multiplied_by(base_bigint);

    return result;
}

static ALWAYS_INLINE Value multiply_by_power(GlobalObject& global_object, Value number, Checked<i32> exponent)
{
    if (number.is_number())
        return Value(number.as_double() * pow(10, exponent.value()));

    if (exponent < 0) {
        auto exponent_bigint = bigint_power(10, -exponent.value());
        return js_bigint(global_object.vm(), number.as_bigint().big_integer().divided_by(exponent_bigint).quotient);
    }

    auto exponent_bigint = bigint_power(10, exponent);
    return js_bigint(global_object.vm(), number.as_bigint().big_integer().multiplied_by(exponent_bigint));
}

static ALWAYS_INLINE Value divide_by_power(GlobalObject& global_object, Value number, Checked<i32> exponent)
{
    if (number.is_number()) {
        if (exponent < 0)
            return Value(number.as_double() * pow(10, -exponent.value()));
        return Value(number.as_double() / pow(10, exponent.value()));
    }

    if (exponent < 0) {
        auto exponent_bigint = bigint_power(10, -exponent.value());
        return js_bigint(global_object.vm(), number.as_bigint().big_integer().multiplied_by(exponent_bigint));
    }

    auto exponent_bigint = bigint_power(10, exponent);
    return js_bigint(global_object.vm(), number.as_bigint().big_integer().divided_by(exponent_bigint).quotient);
}

static ALWAYS_INLINE bool is_equal(Value lhs, Value rhs)
{
    if (lhs.is_number()) {
        static constexpr double epsilon = 5e-14;
        return fabs(lhs.as_double() - rhs.as_double()) < epsilon;
    }
    return lhs.as_bigint().big_integer() == rhs.as_bigint().big_integer();
}

static ALWAYS_INLINE bool is_zero(Value number)
{
    if (number.is_number())
        return number.as_double() == 0.0;
    return number.as_bigint().big_integer().is_zero();
}

static bool modulo_is_zero(Value lhs, Checked<i32> rhs)
{
    if (lhs.is_number()) {
        auto mod = modulo(lhs.as_double(), rhs.value());
        return is_equal(Value(mod), Value(0));
    }

    auto rhs_bigint = Crypto::SignedBigInteger::create_from(rhs.value());
    return modulo(lhs.as_bigint().big_integer(), rhs_bigint).is_zero();
}

static ALWAYS_INLINE bool is_greater_than_zero(Value number)
{
    if (number.is_number())
        return number.as_double() > 0;
    return number.as_bigint().big_integer() > "0"_bigint;
}

static ALWAYS_INLINE bool is_less_than_zero(Value number)
{
    if (number.is_number())
        return number.as_double() < 0;
    return number.as_bigint().big_integer() < "0"_bigint;
}

static ALWAYS_INLINE bool is_less_than(Value lhs, Value rhs)
{
    if (lhs.is_number())
        return !is_equal(lhs, rhs) && (lhs.as_double() < rhs.as_double());
    return lhs.as_bigint().big_integer() < rhs.as_bigint().big_integer();
}

static ALWAYS_INLINE String number_to_string(Value number)
{
    if (number.is_number())
        return number.to_string_without_side_effects();
    return number.as_bigint().big_integer().to_base(10);
}

// 15.5.1 CurrencyDigits ( currency ), https://tc39.es/ecma402/#sec-currencydigits
int currency_digits(StringView currency)
{
    // 1. If the ISO 4217 currency and funds code list contains currency as an alphabetic code, return the minor
    //    unit value corresponding to the currency from the list; otherwise, return 2.
    if (auto currency_code = Unicode::get_currency_code(currency); currency_code.has_value())
        return currency_code->minor_unit.value_or(2);
    return 2;
}

// 15.5.3 FormatNumericToString ( intlObject, x ), https://tc39.es/ecma402/#sec-formatnumberstring
// 1.1.5 FormatNumericToString ( intlObject, x ), https://tc39.es/proposal-intl-numberformat-v3/out/numberformat/proposed.html#sec-formatnumberstring
FormatResult format_numeric_to_string(GlobalObject& global_object, NumberFormatBase const& intl_object, Value number)
{
    bool is_negative = false;

    // 1. If x is negative-zero, then
    if (number.is_negative_zero()) {
        // a. Let isNegative be true.
        is_negative = true;

        // b. Let x be the mathematical value 0.
        number = Value(0);
    }

    // 2. Assert: x is a mathematical value.
    VERIFY(number.is_number() || number.is_bigint());

    // 3. If x < 0, let isNegative be true; else let isNegative be false.
    // FIXME: Spec issue: this step would override step 1a, see https://github.com/tc39/proposal-intl-numberformat-v3/issues/67
    is_negative |= is_less_than_zero(number);

    // 4. If isNegative, then
    if (is_negative) {
        // a. Let x be -x.
        number = multiply(global_object, number, -1);
    }

    // 5. Let unsignedRoundingMode be GetUnsignedRoundingMode(intlObject.[[RoundingMode]], isNegative).
    // FIXME: Spec issue: Intl.PluralRules does not have [[RoundingMode]], see https://github.com/tc39/proposal-intl-numberformat-v3/issues/103
    Optional<NumberFormat::UnsignedRoundingMode> unsigned_rounding_mode;
    if (intl_object.rounding_mode() != NumberFormat::RoundingMode::Invalid)
        unsigned_rounding_mode = get_unsigned_rounding_mode(intl_object.rounding_mode(), is_negative);

    RawFormatResult result {};

    switch (intl_object.rounding_type()) {
    // 6. If intlObject.[[RoundingType]] is significantDigits, then
    case NumberFormatBase::RoundingType::SignificantDigits:
        // a. Let result be ToRawPrecision(x, intlObject.[[MinimumSignificantDigits]], intlObject.[[MaximumSignificantDigits]], unsignedRoundingMode).
        result = to_raw_precision(global_object, number, intl_object.min_significant_digits(), intl_object.max_significant_digits(), unsigned_rounding_mode);
        break;

    // 7. Else if intlObject.[[RoundingType]] is fractionDigits, then
    case NumberFormatBase::RoundingType::FractionDigits:
        // a. Let result be ToRawFixed(x, intlObject.[[MinimumFractionDigits]], intlObject.[[MaximumFractionDigits]], intlObject.[[RoundingIncrement]], unsignedRoundingMode).
        result = to_raw_fixed(global_object, number, intl_object.min_fraction_digits(), intl_object.max_fraction_digits(), intl_object.rounding_increment(), unsigned_rounding_mode);
        break;

    // 8. Else,
    case NumberFormatBase::RoundingType::MorePrecision:
    case NumberFormatBase::RoundingType::LessPrecision: {
        // a. Let sResult be ToRawPrecision(x, intlObject.[[MinimumSignificantDigits]], intlObject.[[MaximumSignificantDigits]], unsignedRoundingMode).
        auto significant_result = to_raw_precision(global_object, number, intl_object.min_significant_digits(), intl_object.max_significant_digits(), unsigned_rounding_mode);

        // b. Let fResult be ToRawFixed(x, intlObject.[[MinimumFractionDigits]], intlObject.[[MaximumFractionDigits]], intlObject.[[RoundingIncrement]], unsignedRoundingMode).
        auto fraction_result = to_raw_fixed(global_object, number, intl_object.min_fraction_digits(), intl_object.max_fraction_digits(), intl_object.rounding_increment(), unsigned_rounding_mode);

        // c. If intlObj.[[RoundingType]] is morePrecision, then
        if (intl_object.rounding_type() == NumberFormatBase::RoundingType::MorePrecision) {
            // i. If sResult.[[RoundingMagnitude]] ≤ fResult.[[RoundingMagnitude]], then
            if (significant_result.rounding_magnitude <= fraction_result.rounding_magnitude) {
                // 1. Let result be sResult.
                result = move(significant_result);
            }
            // ii. Else,
            else {
                // 2. Let result be fResult.
                result = move(fraction_result);
            }
        }
        // d. Else,
        else {
            // i. Assert: intlObj.[[RoundingType]] is lessPrecision.
            VERIFY(intl_object.rounding_type() == NumberFormatBase::RoundingType::LessPrecision);

            // ii. If sResult.[[RoundingMagnitude]] ≤ fResult.[[RoundingMagnitude]], then
            if (significant_result.rounding_magnitude <= fraction_result.rounding_magnitude) {
                // 1. Let result be fResult.
                result = move(fraction_result);
            }
            // iii. Else,
            else {
                // 1. Let result be sResult.
                result = move(significant_result);
            }
        }

        break;
    }

    default:
        VERIFY_NOT_REACHED();
    }

    // 9. Let x be result.[[RoundedNumber]].
    number = result.rounded_number;

    // 10. Let string be result.[[FormattedString]].
    auto string = move(result.formatted_string);

    // 11. If intlObject.[[TrailingZeroDisplay]] is "stripIfInteger" and x modulo 1 = 0, then
    if ((intl_object.trailing_zero_display() == NumberFormat::TrailingZeroDisplay::StripIfInteger) && modulo_is_zero(number, 1)) {
        // a. If string contains ".", then
        if (auto index = string.find('.'); index.has_value()) {
            // i. Set string to the substring of string from index 0 to the index of ".".
            string = string.substring(0, *index);
        }
    }

    // 12. Let int be result.[[IntegerDigitsCount]].
    int digits = result.digits;

    // 13. Let minInteger be intlObject.[[MinimumIntegerDigits]].
    int min_integer = intl_object.min_integer_digits();

    // 14. If int < minInteger, then
    if (digits < min_integer) {
        // a. Let forwardZeros be the String consisting of minInteger–int occurrences of the character "0".
        auto forward_zeros = String::repeated('0', min_integer - digits);

        // b. Set string to the string-concatenation of forwardZeros and string.
        string = String::formatted("{}{}", forward_zeros, string);
    }

    // 15. If isNegative and x is 0, then
    if (is_negative && is_zero(number)) {
        // a. Let x be -0.
        number = Value(-0.0);
    }
    // 16. Else if isNegative, then
    else if (is_negative) {
        // b. Let x be -x.
        number = multiply(global_object, number, -1);
    }

    // 17. Return the Record { [[RoundedNumber]]: x, [[FormattedString]]: string }.
    return { move(string), number };
}

// 15.5.4 PartitionNumberPattern ( numberFormat, x ), https://tc39.es/ecma402/#sec-partitionnumberpattern
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
    auto pattern = get_number_format_pattern(global_object, number_format, number, found_pattern);
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

static Vector<StringView> separate_integer_into_groups(Unicode::NumberGroupings const& grouping_sizes, StringView integer, NumberFormat::UseGrouping use_grouping)
{
    Utf8View utf8_integer { integer };
    if (utf8_integer.length() <= grouping_sizes.primary_grouping_size)
        return { integer };

    size_t index = utf8_integer.length() - grouping_sizes.primary_grouping_size;

    switch (use_grouping) {
    case NumberFormat::UseGrouping::Min2:
        if (utf8_integer.length() < 5)
            return { integer };
        break;

    case NumberFormat::UseGrouping::Auto:
        if (index < grouping_sizes.minimum_grouping_digits)
            return { integer };
        break;

    case NumberFormat::UseGrouping::Always:
        break;

    default:
        VERIFY_NOT_REACHED();
    }

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

// 15.5.5 PartitionNotationSubPattern ( numberFormat, x, n, exponent ), https://tc39.es/ecma402/#sec-partitionnotationsubpattern
// 1.1.7 PartitionNotationSubPattern ( numberFormat, x, n, exponent ), https://tc39.es/proposal-intl-numberformat-v3/out/numberformat/proposed.html#sec-partitionnotationsubpattern
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
                // 1. If the numberFormat.[[NumberingSystem]] matches one of the values in the "Numbering System" column of Table 12 below, then
                //     a. Let digits be a List whose 10 String valued elements are the UTF-16 string representations of the 10 digits specified in the "Digits" column of the matching row in Table 12.
                //     b. Replace each digit in n with the value of digits[digit].
                // 2. Else use an implementation dependent algorithm to map n to the appropriate representation of n in the given numbering system.
                formatted_string = Unicode::replace_digits_for_number_system(number_format.numbering_system(), formatted_string);

                // 3. Let decimalSepIndex be StringIndexOf(n, ".", 0).
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

                // 6. If the numberFormat.[[UseGrouping]] is false, then
                if (number_format.use_grouping() == NumberFormat::UseGrouping::False) {
                    // a. Append a new Record { [[Type]]: "integer", [[Value]]: integer } as the last element of result.
                    result.append({ "integer"sv, integer });
                }
                // 7. Else,
                else {
                    // a. Let groupSepSymbol be the implementation-, locale-, and numbering system-dependent (ILND) String representing the grouping separator.
                    auto group_sep_symbol = Unicode::get_number_system_symbol(number_format.data_locale(), number_format.numbering_system(), Unicode::NumericSymbol::Group).value_or(","sv);

                    // b. Let groups be a List whose elements are, in left to right order, the substrings defined by ILND set of locations within the integer, which may depend on the value of numberFormat.[[UseGrouping]].
                    auto groups = separate_integer_into_groups(*grouping_sizes, integer, number_format.use_grouping());

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

                // 2. Let exponentResult be ToRawFixed(exponent, 0, 0, 1, undefined).
                auto exponent_result = to_raw_fixed(global_object, Value(exponent), 0, 0, 1, {});

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

// 15.5.6 FormatNumeric ( numberFormat, x ), https://tc39.es/ecma402/#sec-formatnumber
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

// 15.5.7 FormatNumericToParts ( numberFormat, x ), https://tc39.es/ecma402/#sec-formatnumbertoparts
Array* format_numeric_to_parts(GlobalObject& global_object, NumberFormat& number_format, Value number)
{
    auto& vm = global_object.vm();

    // 1. Let parts be ? PartitionNumberPattern(numberFormat, x).
    // Note: Our implementation of PartitionNumberPattern does not throw.
    auto parts = partition_number_pattern(global_object, number_format, number);

    // 2. Let result be ! ArrayCreate(0).
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

enum class PreferredResult {
    LessThanNumber,
    GreaterThanNumber,
};

// ToRawPrecisionFn, https://tc39.es/proposal-intl-numberformat-v3/out/numberformat/proposed.html#eqn-ToRawPrecisionFn
static auto to_raw_precision_function(GlobalObject& global_object, Value number, int precision, PreferredResult mode)
{
    struct {
        Value number;
        int exponent { 0 };
        Value rounded;
    } result {};

    result.exponent = log10floor(number);

    if (number.is_number()) {
        result.number = divide_by_power(global_object, number, result.exponent - precision + 1);

        switch (mode) {
        case PreferredResult::LessThanNumber:
            result.number = Value(floor(result.number.as_double()));
            break;
        case PreferredResult::GreaterThanNumber:
            result.number = Value(ceil(result.number.as_double()));
            break;
        }
    } else {
        // NOTE: In order to round the BigInt to the proper precision, this computation is initially off by a
        //       factor of 10. This lets us inspect the ones digit and then round up if needed.
        result.number = divide_by_power(global_object, number, result.exponent - precision);

        // FIXME: Can we do this without string conversion?
        auto digits = result.number.as_bigint().big_integer().to_base(10);
        auto digit = digits.substring_view(digits.length() - 1);

        result.number = divide(global_object, result.number, 10);

        if (mode == PreferredResult::GreaterThanNumber && digit.to_uint().value() != 0)
            result.number = js_bigint(global_object.vm(), result.number.as_bigint().big_integer().plus("1"_bigint));
    }

    result.rounded = multiply_by_power(global_object, result.number, result.exponent - precision + 1);
    return result;
}

// 15.5.8 ToRawPrecision ( x, minPrecision, maxPrecision ), https://tc39.es/ecma402/#sec-torawprecision
// 1.1.10 ToRawPrecision ( x, minPrecision, maxPrecision, unsignedRoundingMode ), https://tc39.es/proposal-intl-numberformat-v3/out/numberformat/proposed.html#sec-torawprecision
RawFormatResult to_raw_precision(GlobalObject& global_object, Value number, int min_precision, int max_precision, Optional<NumberFormat::UnsignedRoundingMode> const& unsigned_rounding_mode)
{
    RawFormatResult result {};

    // 1. Let p be maxPrecision.
    int precision = max_precision;
    int exponent = 0;

    // 2. If x = 0, then
    if (is_zero(number)) {
        // a. Let m be the String consisting of p occurrences of the character "0".
        result.formatted_string = String::repeated('0', precision);

        // b. Let e be 0.
        exponent = 0;

        // c. Let xFinal be 0.
        result.rounded_number = Value(0);
    }
    // 3. Else,
    else {
        // FIXME: The result of these steps isn't entirely accurate for large values of 'p' (which
        //        defaults to 21, resulting in numbers on the order of 10^21). Either AK::format or
        //        our Number::toString AO (double_to_string in Value.cpp) will need to be improved
        //        to produce more accurate results.

        // a. Let n1 and e1 each be an integer and r1 a mathematical value, with r1 = ToRawPrecisionFn(n1, e1, p), such that r1 ≤ x and r1 is maximized.
        auto [number1, exponent1, rounded1] = to_raw_precision_function(global_object, number, precision, PreferredResult::LessThanNumber);

        // b. Let n2 and e2 each be an integer and r2 a mathematical value, with r2 = ToRawPrecisionFn(n2, e2, p), such that r2 ≥ x and r2 is minimized.
        auto [number2, exponent2, rounded2] = to_raw_precision_function(global_object, number, precision, PreferredResult::GreaterThanNumber);

        // c. Let r be ApplyUnsignedRoundingMode(x, r1, r2, unsignedRoundingMode).
        auto rounded = apply_unsigned_rounding_mode(global_object, number, rounded1, rounded2, unsigned_rounding_mode);

        Value n;

        // d. If r is r1, then
        if (is_equal(rounded, rounded1)) {
            // i. Let n be n1.
            n = number1;

            // ii. Let e be e1.
            exponent = exponent1;

            // iii. Let xFinal be r1.
            result.rounded_number = rounded1;
        }
        // e. Else,
        else {
            // i. Let n be n2.
            n = number2;

            // ii. Let e be e2.
            exponent = exponent2;

            // iii. Let xFinal be r2.
            result.rounded_number = rounded2;
        }

        // f. Let m be the String consisting of the digits of the decimal representation of n (in order, with no leading zeroes).
        result.formatted_string = number_to_string(n);
    }

    // 4. If e ≥ p–1, then
    if (exponent >= (precision - 1)) {
        // a. Let m be the string-concatenation of m and e–p+1 occurrences of the character "0".
        result.formatted_string = String::formatted(
            "{}{}",
            result.formatted_string,
            String::repeated('0', exponent - precision + 1));

        // b. Let int be e+1.
        result.digits = exponent + 1;
    }
    // 5. Else if e ≥ 0, then
    else if (exponent >= 0) {
        // a. Let m be the string-concatenation of the first e+1 characters of m, the character ".", and the remaining p–(e+1) characters of m.
        result.formatted_string = String::formatted(
            "{}.{}",
            result.formatted_string.substring_view(0, exponent + 1),
            result.formatted_string.substring_view(exponent + 1));

        // b. Let int be e+1.
        result.digits = exponent + 1;
    }
    // 6. Else,
    else {
        // a. Assert: e < 0.
        // b. Let m be the string-concatenation of "0.", –(e+1) occurrences of the character "0", and m.
        result.formatted_string = String::formatted(
            "0.{}{}",
            String::repeated('0', -1 * (exponent + 1)),
            result.formatted_string);

        // c. Let int be 1.
        result.digits = 1;
    }

    // 7. If m contains the character ".", and maxPrecision > minPrecision, then
    if (result.formatted_string.contains('.') && (max_precision > min_precision)) {
        // a. Let cut be maxPrecision – minPrecision.
        int cut = max_precision - min_precision;

        // Steps 8b-8c are implemented by cut_trailing_zeroes.
        result.formatted_string = cut_trailing_zeroes(result.formatted_string, cut);
    }

    // 8. Return the Record { [[FormattedString]]: m, [[RoundedNumber]]: xFinal, [[IntegerDigitsCount]]: int, [[RoundingMagnitude]]: e–p+1 }.
    result.rounding_magnitude = exponent - precision + 1;
    return result;
}

// ToRawFixedFn, https://tc39.es/proposal-intl-numberformat-v3/out/numberformat/proposed.html#eqn-ToRawFixedFn
static auto to_raw_fixed_function(GlobalObject& global_object, Value number, int fraction, int rounding_increment, PreferredResult mode)
{
    // FIXME: Handle NumberFormat V3's [[RoundingIncrement]] option.
    (void)rounding_increment;

    struct {
        Value number;
        Value rounded;
    } result {};

    if (number.is_number()) {
        result.number = multiply_by_power(global_object, number, fraction);

        switch (mode) {
        case PreferredResult::LessThanNumber:
            result.number = Value(floor(result.number.as_double()));
            break;
        case PreferredResult::GreaterThanNumber:
            result.number = Value(ceil(result.number.as_double()));
            break;
        }
    } else {
        // NOTE: In order to round the BigInt to the proper precision, this computation is initially off by a
        //       factor of 10. This lets us inspect the ones digit and then round up if needed.
        result.number = multiply_by_power(global_object, number, fraction - 1);

        // FIXME: Can we do this without string conversion?
        auto digits = result.number.as_bigint().big_integer().to_base(10);
        auto digit = digits.substring_view(digits.length() - 1);

        result.number = multiply(global_object, result.number, 10);

        if (mode == PreferredResult::GreaterThanNumber && digit.to_uint().value() != 0)
            result.number = js_bigint(global_object.vm(), result.number.as_bigint().big_integer().plus("1"_bigint));
    }

    result.rounded = divide_by_power(global_object, result.number, fraction);
    return result;
}

// 15.5.9 ToRawFixed ( x, minInteger, minFraction, maxFraction ), https://tc39.es/ecma402/#sec-torawfixed
// 1.1.11 ToRawFixed ( x, minFraction, maxFraction, roundingIncrement, unsignedRoundingMode ), https://tc39.es/proposal-intl-numberformat-v3/out/numberformat/proposed.html#sec-torawfixed
RawFormatResult to_raw_fixed(GlobalObject& global_object, Value number, int min_fraction, int max_fraction, int rounding_increment, Optional<NumberFormat::UnsignedRoundingMode> const& unsigned_rounding_mode)
{
    RawFormatResult result {};

    // 1. Let f be maxFraction.
    int fraction = max_fraction;

    // 2. Let n1 be an integer and r1 a mathematical value, with r1 = ToRawFixedFn(n1, f), such that n1 modulo roundingIncrement = 0, r1 ≤ x, and r1 is maximized.
    auto [number1, rounded1] = to_raw_fixed_function(global_object, number, fraction, rounding_increment, PreferredResult::LessThanNumber);

    // 3. Let n2 be an integer and r2 a mathematical value, with r2 = ToRawFixedFn(n2, f), such that n2 modulo roundingIncrement = 0, r2 ≥ x, and r2 is minimized.
    auto [number2, rounded2] = to_raw_fixed_function(global_object, number, fraction, rounding_increment, PreferredResult::GreaterThanNumber);

    // 4. Let r be ApplyUnsignedRoundingMode(x, r1, r2, unsignedRoundingMode).
    auto rounded = apply_unsigned_rounding_mode(global_object, number, rounded1, rounded2, unsigned_rounding_mode);

    Value n;

    // 5. If r is r1, then
    if (is_equal(rounded, rounded1)) {
        // a. Let n be n1.
        n = number1;

        // b. Let xFinal be r1.
        result.rounded_number = rounded1;
    }
    // 6. Else,
    else {
        // a. Let n be n2.
        n = number2;

        // b. Let xFinal be r2.
        result.rounded_number = rounded2;
    }

    // 7. If n = 0, let m be "0". Otherwise, let m be the String consisting of the digits of the decimal representation of n (in order, with no leading zeroes).
    result.formatted_string = is_zero(n) ? String("0"sv) : number_to_string(n);

    // 8. If f ≠ 0, then
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
    // 9. Else, let int be the number of characters in m.
    else {
        result.digits = result.formatted_string.length();
    }

    // 10. Let cut be maxFraction – minFraction.
    int cut = max_fraction - min_fraction;

    // Steps 11-12 are implemented by cut_trailing_zeroes.
    result.formatted_string = cut_trailing_zeroes(result.formatted_string, cut);

    // 13. Return the Record { [[FormattedString]]: m, [[RoundedNumber]]: xFinal, [[IntegerDigitsCount]]: int, [[RoundingMagnitude]]: –f }.
    result.rounding_magnitude = -fraction;
    return result;
}

// 15.5.11 GetNumberFormatPattern ( numberFormat, x ), https://tc39.es/ecma402/#sec-getnumberformatpattern
// 1.1.14 GetNumberFormatPattern ( numberFormat, x ), https://tc39.es/proposal-intl-numberformat-v3/out/numberformat/proposed.html#sec-getnumberformatpattern
Optional<Variant<StringView, String>> get_number_format_pattern(GlobalObject& global_object, NumberFormat& number_format, Value number, Unicode::NumberFormat& found_pattern)
{
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
        auto plurality = resolve_plural(global_object, number_format, Unicode::PluralForm::Cardinal, number);

        if (auto it = formats.find_if([&](auto& p) { return p.plurality == plurality; }); it != formats.end())
            patterns = move(*it);

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
            auto plurality = resolve_plural(global_object, number_format, Unicode::PluralForm::Cardinal, number);

            if (auto it = formats.find_if([&](auto& p) { return p.plurality == plurality; }); it != formats.end()) {
                patterns = move(*it);
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
        if (is_positive_zero || is_greater_than_zero(number) || is_nan) {
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
        if (is_positive_zero || is_greater_than_zero(number) || is_nan) {
            // i. Let pattern be patterns.[[positivePattern]].
            pattern = patterns->positive_format;
        }
        // b. Else,
        else {
            // i. Let pattern be patterns.[[negativePattern]].
            pattern = patterns->negative_format;
        }
        break;

    // 15. Else if signDisplay is "exceptZero", then
    case NumberFormat::SignDisplay::ExceptZero:
        // a. If x is NaN, or if x is finite and ℝ(x) is 0, then
        if (is_positive_zero || is_negative_zero || is_nan) {
            // i. Let pattern be patterns.[[zeroPattern]].
            pattern = patterns->zero_format;
        }
        // b. Else if ℝ(x) > 0, then
        else if (is_greater_than_zero(number)) {
            // i. Let pattern be patterns.[[positivePattern]].
            pattern = patterns->positive_format;
        }
        // c. Else,
        else {
            // i. Let pattern be patterns.[[negativePattern]].
            pattern = patterns->negative_format;
        }
        break;

    // 16. Else,
    case NumberFormat::SignDisplay::Negative:
        // a. Assert: signDisplay is "negative".
        // b. If x is 0 or x is -0 or x > 0 or x is NaN, then
        if (is_positive_zero || is_negative_zero || is_greater_than_zero(number) || is_nan) {
            // i. Let pattern be patterns.[[zeroPattern]].
            pattern = patterns->zero_format;
        }
        // c. Else,
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

// 15.5.12 GetNotationSubPattern ( numberFormat, exponent ), https://tc39.es/ecma402/#sec-getnotationsubpattern
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

// 15.5.13 ComputeExponent ( numberFormat, x ), https://tc39.es/ecma402/#sec-computeexponent
int compute_exponent(GlobalObject& global_object, NumberFormat& number_format, Value number)
{
    // 1. If x = 0, then
    if (is_zero(number)) {
        // a. Return 0.
        return 0;
    }

    // 2. If x < 0, then
    if (is_less_than_zero(number)) {
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

// 15.5.14 ComputeExponentForMagnitude ( numberFormat, magnitude ), https://tc39.es/ecma402/#sec-computeexponentformagnitude
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

// 1.1.19 GetUnsignedRoundingMode ( roundingMode, isNegative ), https://tc39.es/proposal-intl-numberformat-v3/out/numberformat/proposed.html#sec-getunsignedroundingmode
NumberFormat::UnsignedRoundingMode get_unsigned_rounding_mode(NumberFormat::RoundingMode rounding_mode, bool is_negative)
{
    // 1. If isNegative is true, return the specification type in the third column of Table 2 where the first column is roundingMode and the second column is "negative".
    // 2. Else, return the specification type in the third column of Table 2 where the first column is roundingMode and the second column is "positive".

    // Table 2: Conversion from rounding mode to unsigned rounding mode, https://tc39.es/proposal-intl-numberformat-v3/out/numberformat/proposed.html#table-intl-unsigned-rounding-modes
    switch (rounding_mode) {
    case NumberFormat::RoundingMode::Ceil:
        return is_negative ? NumberFormat::UnsignedRoundingMode::Zero : NumberFormat::UnsignedRoundingMode::Infinity;
    case NumberFormat::RoundingMode::Floor:
        return is_negative ? NumberFormat::UnsignedRoundingMode::Infinity : NumberFormat::UnsignedRoundingMode::Zero;
    case NumberFormat::RoundingMode::Expand:
        return NumberFormat::UnsignedRoundingMode::Infinity;
    case NumberFormat::RoundingMode::Trunc:
        return NumberFormat::UnsignedRoundingMode::Zero;
    case NumberFormat::RoundingMode::HalfCeil:
        return is_negative ? NumberFormat::UnsignedRoundingMode::HalfZero : NumberFormat::UnsignedRoundingMode::HalfInfinity;
    case NumberFormat::RoundingMode::HalfFloor:
        return is_negative ? NumberFormat::UnsignedRoundingMode::HalfInfinity : NumberFormat::UnsignedRoundingMode::HalfZero;
    case NumberFormat::RoundingMode::HalfExpand:
        return NumberFormat::UnsignedRoundingMode::HalfInfinity;
    case NumberFormat::RoundingMode::HalfTrunc:
        return NumberFormat::UnsignedRoundingMode::HalfZero;
    case NumberFormat::RoundingMode::HalfEven:
        return NumberFormat::UnsignedRoundingMode::HalfEven;
    default:
        VERIFY_NOT_REACHED();
    };
}

// 1.1.20 ApplyUnsignedRoundingMode ( x, r1, r2, unsignedRoundingMode ), https://tc39.es/proposal-intl-numberformat-v3/out/numberformat/proposed.html#sec-applyunsignedroundingmode
Value apply_unsigned_rounding_mode(GlobalObject& global_object, Value x, Value r1, Value r2, Optional<NumberFormat::UnsignedRoundingMode> const& unsigned_rounding_mode)
{
    // 1. If x is equal to r1, return r1.
    if (is_equal(x, r1))
        return r1;

    // FIXME: We skip this assertion due floating point inaccuracies. For example, entering "1.2345"
    //        in the JS REPL results in "1.234499999999999", and may cause this assertion to fail.
    //
    //        This should be resolved when the "Intl mathematical value" is implemented to support
    //        arbitrarily precise decimals.
    //        https://tc39.es/proposal-intl-numberformat-v3/out/numberformat/proposed.html#intl-mathematical-value
    // 2. Assert: r1 < x < r2.

    // 3. Assert: unsignedRoundingMode is not undefined.
    VERIFY(unsigned_rounding_mode.has_value());

    // 4. If unsignedRoundingMode is zero, return r1.
    if (unsigned_rounding_mode == NumberFormat::UnsignedRoundingMode::Zero)
        return r1;

    // 5. If unsignedRoundingMode is infinity, return r2.
    if (unsigned_rounding_mode == NumberFormat::UnsignedRoundingMode::Infinity)
        return r2;

    // 6. Let d1 be x – r1.
    auto d1 = subtract(global_object, x, r1);

    // 7. Let d2 be r2 – x.
    auto d2 = subtract(global_object, r2, x);

    // 8. If d1 < d2, return r1.
    if (is_less_than(d1, d2))
        return r1;

    // 9. If d2 < d1, return r2.
    if (is_less_than(d2, d1))
        return r2;

    // 10. Assert: d1 is equal to d2.
    VERIFY(is_equal(d1, d2));

    // 11. If unsignedRoundingMode is half-zero, return r1.
    if (unsigned_rounding_mode == NumberFormat::UnsignedRoundingMode::HalfZero)
        return r1;

    // 12. If unsignedRoundingMode is half-infinity, return r2.
    if (unsigned_rounding_mode == NumberFormat::UnsignedRoundingMode::HalfInfinity)
        return r2;

    // 13. Assert: unsignedRoundingMode is half-even.
    VERIFY(unsigned_rounding_mode == NumberFormat::UnsignedRoundingMode::HalfEven);

    // 14. Let cardinality be (r1 / (r2 – r1)) modulo 2.
    auto cardinality = subtract(global_object, r2, r1);
    cardinality = divide(global_object, r1, cardinality);

    // 15. If cardinality is 0, return r1.
    if (modulo_is_zero(cardinality, 2))
        return r1;

    // 16. Return r2.
    return r2;
}

}
