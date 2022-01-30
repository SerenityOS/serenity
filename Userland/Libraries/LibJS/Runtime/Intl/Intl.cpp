/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/CollatorConstructor.h>
#include <LibJS/Runtime/Intl/DateTimeFormatConstructor.h>
#include <LibJS/Runtime/Intl/DisplayNamesConstructor.h>
#include <LibJS/Runtime/Intl/Intl.h>
#include <LibJS/Runtime/Intl/ListFormatConstructor.h>
#include <LibJS/Runtime/Intl/LocaleConstructor.h>
#include <LibJS/Runtime/Intl/NumberFormatConstructor.h>
#include <LibJS/Runtime/Intl/PluralRulesConstructor.h>
#include <LibJS/Runtime/Intl/RelativeTimeFormatConstructor.h>
#include <LibJS/Runtime/Intl/SegmenterConstructor.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibUnicode/CurrencyCode.h>
#include <LibUnicode/DateTimeFormat.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/NumberFormat.h>

namespace JS::Intl {

// 8 The Intl Object, https://tc39.es/ecma402/#intl-object
Intl::Intl(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void Intl::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 8.1.1 Intl[ @@toStringTag ], https://tc39.es/ecma402/#sec-Intl-toStringTag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl"), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_direct_property(vm.names.Collator, global_object.intl_collator_constructor(), attr);
    define_direct_property(vm.names.DateTimeFormat, global_object.intl_date_time_format_constructor(), attr);
    define_direct_property(vm.names.DisplayNames, global_object.intl_display_names_constructor(), attr);
    define_direct_property(vm.names.ListFormat, global_object.intl_list_format_constructor(), attr);
    define_direct_property(vm.names.Locale, global_object.intl_locale_constructor(), attr);
    define_direct_property(vm.names.NumberFormat, global_object.intl_number_format_constructor(), attr);
    define_direct_property(vm.names.PluralRules, global_object.intl_plural_rules_constructor(), attr);
    define_direct_property(vm.names.RelativeTimeFormat, global_object.intl_relative_time_format_constructor(), attr);
    define_direct_property(vm.names.Segmenter, global_object.intl_segmenter_constructor(), attr);

    define_native_function(vm.names.getCanonicalLocales, get_canonical_locales, 1, attr);
    define_native_function(vm.names.supportedValuesOf, supported_values_of, 1, attr);
}

// 8.3.1 Intl.getCanonicalLocales ( locales ), https://tc39.es/ecma402/#sec-intl.getcanonicallocales
JS_DEFINE_NATIVE_FUNCTION(Intl::get_canonical_locales)
{
    auto locales = vm.argument(0);

    // 1. Let ll be ? CanonicalizeLocaleList(locales).
    auto locale_list = TRY(canonicalize_locale_list(global_object, locales));

    MarkedValueList marked_locale_list { vm.heap() };
    marked_locale_list.ensure_capacity(locale_list.size());
    for (auto& locale : locale_list)
        marked_locale_list.append(js_string(vm, move(locale)));

    // 2. Return CreateArrayFromList(ll).
    return Array::create_from(global_object, marked_locale_list);
}

// 1.4.4 AvailableTimeZones (), https://tc39.es/proposal-intl-enumeration/#sec-availablecurrencies
static Vector<StringView> available_time_zones()
{
    // 1. Let names be a List of all supported Zone and Link names in the IANA Time Zone Database.
    auto names = TimeZone::all_time_zones();

    // 2. Let result be a new empty List.
    Vector<StringView> result;

    // 3. For each element name of names, do
    for (auto name : names) {
        // a. Assert: ! IsValidTimeZoneName( name ) is true.
        // b. Let canonical be ! CanonicalizeTimeZoneName( name ).
        auto canonical = TimeZone::canonicalize_time_zone(name).value();

        // c. If result does not contain an element equal to canonical, then
        if (!result.contains_slow(canonical)) {
            // i. Append canonical to the end of result.
            result.append(canonical);
        }
    }

    // 4. Sort result in order as if an Array of the same values had been sorted using %Array.prototype.sort% using undefined as comparefn.
    quick_sort(result);

    // 5. Return result.
    return result;
}

// 2.2.2 Intl.supportedValuesOf ( key ), https://tc39.es/proposal-intl-enumeration/#sec-intl.supportedvaluesof
JS_DEFINE_NATIVE_FUNCTION(Intl::supported_values_of)
{
    // 1. Let key be ? ToString(key).
    auto key = TRY(vm.argument(0).to_string(global_object));

    Span<StringView const> list;

    // 2. If key is "calendar", then
    if (key == "calendar"sv) {
        // a. Let list be ! AvailableCalendars( ).
        list = Unicode::get_available_calendars();
    }
    // 3. Else if key is "collation", then
    else if (key == "collation"sv) {
        // a. Let list be ! AvailableCollations( ).
        // NOTE: We don't yet parse any collation data, but "default" is allowed.
        static constexpr auto collations = AK::Array { "default"sv };
        list = collations.span();
    }
    // 4. Else if key is "currency", then
    else if (key == "currency"sv) {
        // a. Let list be ! AvailableCurrencies( ).
        list = Unicode::get_available_currencies();
    }
    // 5. Else if key is "numberingSystem", then
    else if (key == "numberingSystem"sv) {
        // a. Let list be ! AvailableNumberingSystems( ).
        list = Unicode::get_available_number_systems();
    }
    // 6. Else if key is "timeZone", then
    else if (key == "timeZone"sv) {
        // a. Let list be ! AvailableTimeZones( ).
        static auto time_zones = available_time_zones();
        list = time_zones.span();
    }
    // 7. Else if key is "unit", then
    else if (key == "unit"sv) {
        // a. Let list be ! AvailableUnits( ).
        static auto units = sanctioned_simple_unit_identifiers();
        list = units.span();
    }
    // 8. Else,
    else {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::IntlInvalidKey, key);
    }

    // 9. Return ! CreateArrayFromList( list ).
    return Array::create_from<StringView>(global_object, list, [&](auto value) { return js_string(vm, value); });
}

}
