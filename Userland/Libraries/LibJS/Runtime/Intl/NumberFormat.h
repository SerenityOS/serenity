/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Object.h>
#include <LibUnicode/NumberFormat.h>

namespace JS::Intl {

class NumberFormat final : public Object {
    JS_OBJECT(NumberFormat, Object);

public:
    enum class Style {
        Invalid,
        Decimal,
        Percent,
        Currency,
        Unit,
    };

    enum class CurrencyDisplay {
        Code,
        Symbol,
        NarrowSymbol,
        Name,
    };

    enum class CurrencySign {
        Standard,
        Accounting,
    };

    enum class UnitDisplay {
        Short,
        Narrow,
        Long,
    };

    enum class RoundingType {
        Invalid,
        SignificantDigits,
        FractionDigits,
        CompactRounding,
    };

    enum class Notation {
        Invalid,
        Standard,
        Scientific,
        Engineering,
        Compact,
    };

    enum class CompactDisplay {
        Short,
        Long,
    };

    enum class SignDisplay {
        Invalid,
        Auto,
        Never,
        Always,
        ExceptZero,
    };

    static constexpr auto relevant_extension_keys()
    {
        // 15.3.3 Internal slots, https://tc39.es/ecma402/#sec-intl.numberformat-internal-slots
        // The value of the [[RelevantExtensionKeys]] internal slot is « "nu" ».
        return AK::Array { "nu"sv };
    }

    NumberFormat(Object& prototype);
    virtual ~NumberFormat() override = default;

    String const& locale() const { return m_locale; }
    void set_locale(String locale) { m_locale = move(locale); }

    String const& data_locale() const { return m_data_locale; }
    void set_data_locale(String data_locale) { m_data_locale = move(data_locale); }

    String const& numbering_system() const { return m_numbering_system; }
    void set_numbering_system(String numbering_system) { m_numbering_system = move(numbering_system); }

    Style style() const { return m_style; }
    StringView style_string() const;
    void set_style(StringView style);

    bool has_currency() const { return m_currency.has_value(); }
    String const& currency() const { return m_currency.value(); }
    void set_currency(String currency) { m_currency = move(currency); }

    bool has_currency_display() const { return m_currency_display.has_value(); }
    CurrencyDisplay currency_display() const { return *m_currency_display; }
    StringView currency_display_string() const;
    void set_currency_display(StringView currency_display);
    StringView resolve_currency_display();

    bool has_currency_sign() const { return m_currency_sign.has_value(); }
    CurrencySign currency_sign() const { return *m_currency_sign; }
    StringView currency_sign_string() const;
    void set_currency_sign(StringView set_currency_sign);

    bool has_unit() const { return m_unit.has_value(); }
    String const& unit() const { return m_unit.value(); }
    void set_unit(String unit) { m_unit = move(unit); }

    bool has_unit_display() const { return m_unit_display.has_value(); }
    UnitDisplay unit_display() const { return *m_unit_display; }
    StringView unit_display_string() const;
    void set_unit_display(StringView unit_display);

    int min_integer_digits() const { return m_min_integer_digits; }
    void set_min_integer_digits(int min_integer_digits) { m_min_integer_digits = min_integer_digits; }

    bool has_min_fraction_digits() const { return m_min_fraction_digits.has_value(); }
    int min_fraction_digits() const { return *m_min_fraction_digits; }
    void set_min_fraction_digits(int min_fraction_digits) { m_min_fraction_digits = min_fraction_digits; }

    bool has_max_fraction_digits() const { return m_max_fraction_digits.has_value(); }
    int max_fraction_digits() const { return *m_max_fraction_digits; }
    void set_max_fraction_digits(int max_fraction_digits) { m_max_fraction_digits = max_fraction_digits; }

    bool has_min_significant_digits() const { return m_min_significant_digits.has_value(); }
    int min_significant_digits() const { return *m_min_significant_digits; }
    void set_min_significant_digits(int min_significant_digits) { m_min_significant_digits = min_significant_digits; }

    bool has_max_significant_digits() const { return m_max_significant_digits.has_value(); }
    int max_significant_digits() const { return *m_max_significant_digits; }
    void set_max_significant_digits(int max_significant_digits) { m_max_significant_digits = max_significant_digits; }

    bool use_grouping() const { return m_use_grouping; }
    void set_use_grouping(bool use_grouping) { m_use_grouping = use_grouping; }

    RoundingType rounding_type() const { return m_rounding_type; }
    StringView rounding_type_string() const;
    void set_rounding_type(RoundingType rounding_type) { m_rounding_type = rounding_type; }

    Notation notation() const { return m_notation; }
    StringView notation_string() const;
    void set_notation(StringView notation);

    bool has_compact_display() const { return m_compact_display.has_value(); }
    CompactDisplay compact_display() const { return *m_compact_display; }
    StringView compact_display_string() const;
    void set_compact_display(StringView compact_display);

    SignDisplay sign_display() const { return m_sign_display; }
    StringView sign_display_string() const;
    void set_sign_display(StringView sign_display);

    NativeFunction* bound_format() const { return m_bound_format; }
    void set_bound_format(NativeFunction* bound_format) { m_bound_format = bound_format; }

    bool has_compact_format() const { return m_compact_format.has_value(); }
    void set_compact_format(Unicode::NumberFormat compact_format) { m_compact_format = compact_format; }
    Unicode::NumberFormat compact_format() const { return *m_compact_format; }

private:
    virtual void visit_edges(Visitor&) override;

    String m_locale;                                        // [[Locale]]
    String m_data_locale;                                   // [[DataLocale]]
    String m_numbering_system;                              // [[NumberingSystem]]
    Style m_style { Style::Invalid };                       // [[Style]]
    Optional<String> m_currency {};                         // [[Currency]]
    Optional<CurrencyDisplay> m_currency_display {};        // [[CurrencyDisplay]]
    Optional<CurrencySign> m_currency_sign {};              // [[CurrencySign]]
    Optional<String> m_unit {};                             // [[Unit]]
    Optional<UnitDisplay> m_unit_display {};                // [[UnitDisplay]]
    int m_min_integer_digits { 0 };                         // [[MinimumIntegerDigits]]
    Optional<int> m_min_fraction_digits {};                 // [[MinimumFractionDigits]]
    Optional<int> m_max_fraction_digits {};                 // [[MaximumFractionDigits]]
    Optional<int> m_min_significant_digits {};              // [[MinimumSignificantDigits]]
    Optional<int> m_max_significant_digits {};              // [[MaximumSignificantDigits]]
    bool m_use_grouping { false };                          // [[UseGrouping]]
    RoundingType m_rounding_type { RoundingType::Invalid }; // [[RoundingType]]
    Notation m_notation { Notation::Invalid };              // [[Notation]]
    Optional<CompactDisplay> m_compact_display {};          // [[CompactDisplay]]
    SignDisplay m_sign_display { SignDisplay::Invalid };    // [[SignDisplay]]
    NativeFunction* m_bound_format { nullptr };             // [[BoundFormat]]

    // Non-standard. Stores the resolved currency display string based on [[Locale]], [[Currency]], and [[CurrencyDisplay]].
    Optional<StringView> m_resolved_currency_display;

    // Non-standard. Stores the resolved compact number format based on [[Locale]], [[Notation], [[Style]], and [[CompactDisplay]].
    Optional<Unicode::NumberFormat> m_compact_format;
};

struct FormatResult {
    String formatted_string;       // [[FormattedString]]
    double rounded_number { 0.0 }; // [[RoundedNumber]]
};

struct RawFormatResult : public FormatResult {
    int digits { 0 }; // [[IntegerDigitsCount]]
};

ThrowCompletionOr<void> set_number_format_digit_options(GlobalObject& global_object, NumberFormat& intl_object, Object const& options, int default_min_fraction_digits, int default_max_fraction_digits, NumberFormat::Notation notation);
ThrowCompletionOr<NumberFormat*> initialize_number_format(GlobalObject& global_object, NumberFormat& number_format, Value locales_value, Value options_value);
int currency_digits(StringView currency);
FormatResult format_numeric_to_string(NumberFormat& number_format, double number);
Vector<PatternPartition> partition_number_pattern(NumberFormat& number_format, double number);
Vector<PatternPartition> partition_notation_sub_pattern(NumberFormat& number_format, double number, String formatted_string, int exponent);
String format_numeric(NumberFormat& number_format, double number);
Array* format_numeric_to_parts(GlobalObject& global_object, NumberFormat& number_format, double number);
RawFormatResult to_raw_precision(double number, int min_precision, int max_precision);
RawFormatResult to_raw_fixed(double number, int min_fraction, int max_fraction);
ThrowCompletionOr<void> set_number_format_unit_options(GlobalObject& global_object, NumberFormat& intl_object, Object const& options);
Optional<Variant<StringView, String>> get_number_format_pattern(NumberFormat& number_format, double number, Unicode::NumberFormat& found_pattern);
Optional<StringView> get_notation_sub_pattern(NumberFormat& number_format, int exponent);
int compute_exponent(NumberFormat& number_format, double number);
int compute_exponent_for_magniude(NumberFormat& number_format, int magnitude);

}
