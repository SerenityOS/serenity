/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibUnicode/UnicodeSymbols.h>

#if ENABLE_UNICODE_DATA
#    if defined(__serenity__)
#        include <LibDl/dlfcn.h>
#        include <LibDl/dlfcn_integration.h>
#    else
#        include <dlfcn.h>
#    endif
#else
#    include <AK/Function.h>
#    include <LibUnicode/DateTimeFormat.h>
#    include <LibUnicode/Locale.h>
#endif

namespace Unicode::Detail {

#if !ENABLE_UNICODE_DATA

template<typename T>
struct FunctionStub;

template<typename ReturnType, typename... ParameterTypes>
struct FunctionStub<Function<ReturnType(ParameterTypes...)>> {
    static constexpr auto make_stub()
    {
        if constexpr (IsVoid<ReturnType>)
            return [](ParameterTypes...) {};
        else
            return [](ParameterTypes...) -> ReturnType { return {}; };
    }
};

#endif

// This loader supports 3 modes:
//
// 1. When the Unicode data generators are enabled, and the target is Serenity, the symbols are
//    dynamically loaded from the shared library containing them.
//
// 2. When the Unicode data generators are enabled, and the target is Lagom, the symbols are
//    dynamically loaded from the main program.
//
// 3. When the Unicode data generators are disabled, the symbols are stubbed out to empty lambdas.
//    This allows callers to remain agnostic as to whether the generators are enabled.
Symbols const& Symbols::ensure_loaded()
{
    static Symbols symbols {};

    static bool initialized = false;
    if (initialized)
        return symbols;

#if ENABLE_UNICODE_DATA
#    if defined(__serenity__)
    static void* libunicodedata = MUST(__dlopen("libunicodedata.so.serenity", RTLD_NOW));

    auto load_symbol = [&]<typename T>(T& dest, char const* name) {
        dest = reinterpret_cast<T>(MUST(__dlsym(libunicodedata, name)));
    };
#    else
    static void* libunicodedata = dlopen(nullptr, RTLD_NOW);
    VERIFY(libunicodedata);

    auto load_symbol = [&]<typename T>(T& dest, char const* name) {
        dest = reinterpret_cast<T>(dlsym(libunicodedata, name));
        VERIFY(dest);
    };
#    endif
#else
    auto load_symbol = []<typename T>(T& dest, char const*) {
        dest = +FunctionStub<Function<RemovePointer<T>>>::make_stub();
    };
#endif

    load_symbol(symbols.code_point_display_name, "unicode_code_point_display_name");
    load_symbol(symbols.canonical_combining_class, "unicode_canonical_combining_class");
    load_symbol(symbols.simple_uppercase_mapping, "unicode_simple_uppercase_mapping");
    load_symbol(symbols.simple_lowercase_mapping, "unicode_simple_lowercase_mapping");
    load_symbol(symbols.special_case_mapping, "unicode_special_case_mapping");
    load_symbol(symbols.general_category_from_string, "unicode_general_category_from_string");
    load_symbol(symbols.code_point_has_general_category, "unicode_code_point_has_general_category");
    load_symbol(symbols.property_from_string, "unicode_property_from_string");
    load_symbol(symbols.code_point_has_property, "unicode_code_point_has_property");
    load_symbol(symbols.script_from_string, "unicode_script_from_string");
    load_symbol(symbols.code_point_has_script, "unicode_code_point_has_script");
    load_symbol(symbols.code_point_has_script_extension, "unicode_code_point_has_script_extension");

    load_symbol(symbols.locale_from_string, "unicode_locale_from_string");
    load_symbol(symbols.get_locale_language_mapping, "unicode_get_locale_language_mapping");
    load_symbol(symbols.get_locale_territory_mapping, "unicode_get_locale_territory_mapping");
    load_symbol(symbols.get_locale_script_tag_mapping, "unicode_get_locale_script_tag_mapping");
    load_symbol(symbols.get_locale_long_currency_mapping, "unicode_get_locale_long_currency_mapping");
    load_symbol(symbols.get_locale_short_currency_mapping, "unicode_get_locale_short_currency_mapping");
    load_symbol(symbols.get_locale_narrow_currency_mapping, "unicode_get_locale_narrow_currency_mapping");
    load_symbol(symbols.get_locale_numeric_currency_mapping, "unicode_get_locale_numeric_currency_mapping");
    load_symbol(symbols.get_locale_key_mapping, "unicode_get_locale_key_mapping");
    load_symbol(symbols.get_locale_list_pattern_mapping, "unicode_get_locale_list_pattern_mapping");
    load_symbol(symbols.resolve_language_alias, "unicode_resolve_language_alias");
    load_symbol(symbols.resolve_territory_alias, "unicode_resolve_territory_alias");
    load_symbol(symbols.resolve_script_tag_alias, "unicode_resolve_script_tag_alias");
    load_symbol(symbols.resolve_variant_alias, "unicode_resolve_variant_alias");
    load_symbol(symbols.resolve_subdivision_alias, "unicode_resolve_subdivision_alias");
    load_symbol(symbols.resolve_complex_language_aliases, "unicode_resolve_complex_language_aliases");
    load_symbol(symbols.add_likely_subtags, "unicode_add_likely_subtags");
    load_symbol(symbols.resolve_most_likely_territory, "unicode_resolve_most_likely_territory");

    load_symbol(symbols.get_regional_hour_cycles, "unicode_get_regional_hour_cycles");
    load_symbol(symbols.get_calendar_date_format, "unicode_get_calendar_date_format");
    load_symbol(symbols.get_calendar_time_format, "unicode_get_calendar_time_format");
    load_symbol(symbols.get_calendar_date_time_format, "unicode_get_calendar_date_time_format");
    load_symbol(symbols.get_calendar_available_formats, "unicode_get_calendar_available_formats");
    load_symbol(symbols.get_calendar_default_range_format, "unicode_get_calendar_default_range_format");
    load_symbol(symbols.get_calendar_range_formats, "unicode_get_calendar_range_formats");
    load_symbol(symbols.get_calendar_range12_formats, "unicode_get_calendar_range12_formats");
    load_symbol(symbols.get_calendar_era_symbol, "unicode_get_calendar_era_symbol");
    load_symbol(symbols.get_calendar_month_symbol, "unicode_get_calendar_month_symbol");
    load_symbol(symbols.get_calendar_weekday_symbol, "unicode_get_calendar_weekday_symbol");
    load_symbol(symbols.get_calendar_day_period_symbol, "unicode_get_calendar_day_period_symbol");
    load_symbol(symbols.get_calendar_day_period_symbol_for_hour, "unicode_get_calendar_day_period_symbol_for_hour");
    load_symbol(symbols.get_time_zone_name, "unicode_get_time_zone_name");

    initialized = true;
    return symbols;
}

}
