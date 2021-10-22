/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/Locale.h>
#include <LibJS/Runtime/Intl/LocalePrototype.h>
#include <LibUnicode/Locale.h>

namespace JS::Intl {

// 14.3 Properties of the Intl.Locale Prototype Object, https://tc39.es/ecma402/#sec-properties-of-intl-locale-prototype-object
LocalePrototype::LocalePrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void LocalePrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.maximize, maximize, 0, attr);
    define_native_function(vm.names.minimize, minimize, 0, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);

    // 14.3.2 Intl.Locale.prototype[ @@toStringTag ], https://tc39.es/ecma402/#sec-Intl.Locale.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.Locale"), Attribute::Configurable);

    define_native_accessor(vm.names.baseName, base_name, {}, Attribute::Configurable);
    define_native_accessor(vm.names.calendar, calendar, {}, Attribute::Configurable);
    define_native_accessor(vm.names.caseFirst, case_first, {}, Attribute::Configurable);
    define_native_accessor(vm.names.collation, collation, {}, Attribute::Configurable);
    define_native_accessor(vm.names.hourCycle, hour_cycle, {}, Attribute::Configurable);
    define_native_accessor(vm.names.numberingSystem, numbering_system, {}, Attribute::Configurable);
    define_native_accessor(vm.names.numeric, numeric, {}, Attribute::Configurable);
    define_native_accessor(vm.names.language, language, {}, Attribute::Configurable);
    define_native_accessor(vm.names.script, script, {}, Attribute::Configurable);
    define_native_accessor(vm.names.region, region, {}, Attribute::Configurable);
}

// 14.3.3 Intl.Locale.prototype.maximize ( ), https://tc39.es/ecma402/#sec-Intl.Locale.prototype.maximize
JS_DEFINE_NATIVE_FUNCTION(LocalePrototype::maximize)
{
    // 1. Let loc be the this value.
    // 2. Perform ? RequireInternalSlot(loc, [[InitializedLocale]]).
    auto* locale_object = TRY(typed_this_object(global_object));

    auto locale = Unicode::parse_unicode_locale_id(locale_object->locale());
    VERIFY(locale.has_value());

    // 3. Let maximal be the result of the Add Likely Subtags algorithm applied to loc.[[Locale]]. If an error is signaled, set maximal to loc.[[Locale]].
    if (auto maximal = Unicode::add_likely_subtags(locale->language_id); maximal.has_value())
        locale->language_id = maximal.release_value();

    // 4. Return ! Construct(%Locale%, maximal).
    return Locale::create(global_object, *locale);
}

// 14.3.4 Intl.Locale.prototype.minimize ( ), https://tc39.es/ecma402/#sec-Intl.Locale.prototype.minimize
JS_DEFINE_NATIVE_FUNCTION(LocalePrototype::minimize)
{
    // 1. Let loc be the this value.
    // 2. Perform ? RequireInternalSlot(loc, [[InitializedLocale]]).
    auto* locale_object = TRY(typed_this_object(global_object));

    auto locale = Unicode::parse_unicode_locale_id(locale_object->locale());
    VERIFY(locale.has_value());

    // 3. Let minimal be the result of the Remove Likely Subtags algorithm applied to loc.[[Locale]]. If an error is signaled, set minimal to loc.[[Locale]].
    if (auto minimal = Unicode::remove_likely_subtags(locale->language_id); minimal.has_value())
        locale->language_id = minimal.release_value();

    // 4. Return ! Construct(%Locale%, minimal).
    return Locale::create(global_object, *locale);
}

// 14.3.5 Intl.Locale.prototype.toString ( ), https://tc39.es/ecma402/#sec-Intl.Locale.prototype.toString
JS_DEFINE_NATIVE_FUNCTION(LocalePrototype::to_string)
{
    // 1. Let loc be the this value.
    // 2. Perform ? RequireInternalSlot(loc, [[InitializedLocale]]).
    auto* locale_object = TRY(typed_this_object(global_object));

    // 3. Return loc.[[Locale]].
    return js_string(vm, locale_object->locale());
}

// 14.3.6 get Intl.Locale.prototype.baseName, https://tc39.es/ecma402/#sec-Intl.Locale.prototype.baseName
JS_DEFINE_NATIVE_FUNCTION(LocalePrototype::base_name)
{
    // 1. Let loc be the this value.
    // 2. Perform ? RequireInternalSlot(loc, [[InitializedLocale]]).
    auto* locale_object = TRY(typed_this_object(global_object));

    // 3. Let locale be loc.[[Locale]].
    auto locale = Unicode::parse_unicode_locale_id(locale_object->locale());
    VERIFY(locale.has_value());

    // 4. Return the substring of locale corresponding to the unicode_language_id production.
    return js_string(vm, locale->language_id.to_string());
}

#define JS_ENUMERATE_LOCALE_KEYWORD_PROPERTIES \
    __JS_ENUMERATE(calendar)                   \
    __JS_ENUMERATE(case_first)                 \
    __JS_ENUMERATE(collation)                  \
    __JS_ENUMERATE(hour_cycle)                 \
    __JS_ENUMERATE(numbering_system)

// 14.3.7 get Intl.Locale.prototype.calendar, https://tc39.es/ecma402/#sec-Intl.Locale.prototype.calendar
// 14.3.8 get Intl.Locale.prototype.caseFirst, https://tc39.es/ecma402/#sec-Intl.Locale.prototype.caseFirst
// 14.3.9 get Intl.Locale.prototype.collation, https://tc39.es/ecma402/#sec-Intl.Locale.prototype.collation
// 14.3.10 get Intl.Locale.prototype.hourCycle, https://tc39.es/ecma402/#sec-Intl.Locale.prototype.hourCycle
// 14.3.12 get Intl.Locale.prototype.numberingSystem, https://tc39.es/ecma402/#sec-Intl.Locale.prototype.numberingSystem
#define __JS_ENUMERATE(keyword)                                      \
    JS_DEFINE_NATIVE_FUNCTION(LocalePrototype::keyword)              \
    {                                                                \
        auto* locale_object = TRY(typed_this_object(global_object)); \
        if (!locale_object->has_##keyword())                         \
            return js_undefined();                                   \
        return js_string(vm, locale_object->keyword());              \
    }
JS_ENUMERATE_LOCALE_KEYWORD_PROPERTIES
#undef __JS_ENUMERATE

// 14.3.11 get Intl.Locale.prototype.numeric, https://tc39.es/ecma402/#sec-Intl.Locale.prototype.numeric
JS_DEFINE_NATIVE_FUNCTION(LocalePrototype::numeric)
{
    // 1. Let loc be the this value.
    // 2. Perform ? RequireInternalSlot(loc, [[InitializedLocale]]).
    auto* locale_object = TRY(typed_this_object(global_object));

    // 3. Return loc.[[Numeric]].
    return Value(locale_object->numeric());
}

// 14.3.13 get Intl.Locale.prototype.language, https://tc39.es/ecma402/#sec-Intl.Locale.prototype.language
JS_DEFINE_NATIVE_FUNCTION(LocalePrototype::language)
{
    // 1. Let loc be the this value.
    // 2. Perform ? RequireInternalSlot(loc, [[InitializedLocale]]).
    auto* locale_object = TRY(typed_this_object(global_object));

    // 3. Let locale be loc.[[Locale]].
    auto locale = Unicode::parse_unicode_locale_id(locale_object->locale());

    // 4. Assert: locale matches the unicode_locale_id production.
    VERIFY(locale.has_value());

    // 5. Return the substring of locale corresponding to the unicode_language_subtag production of the unicode_language_id.
    return js_string(vm, *locale->language_id.language);
}

// 14.3.14 get Intl.Locale.prototype.script, https://tc39.es/ecma402/#sec-Intl.Locale.prototype.script
JS_DEFINE_NATIVE_FUNCTION(LocalePrototype::script)
{
    // 1. Let loc be the this value.
    // 2. Perform ? RequireInternalSlot(loc, [[InitializedLocale]]).
    auto* locale_object = TRY(typed_this_object(global_object));

    // 3. Let locale be loc.[[Locale]].
    auto locale = Unicode::parse_unicode_locale_id(locale_object->locale());

    // 4. Assert: locale matches the unicode_locale_id production.
    VERIFY(locale.has_value());

    // 5. If the unicode_language_id production of locale does not contain the ["-" unicode_script_subtag] sequence, return undefined.
    if (!locale->language_id.script.has_value())
        return js_undefined();

    // 6. Return the substring of locale corresponding to the unicode_script_subtag production of the unicode_language_id.
    return js_string(vm, *locale->language_id.script);
}

// 14.3.15 get Intl.Locale.prototype.region, https://tc39.es/ecma402/#sec-Intl.Locale.prototype.region
JS_DEFINE_NATIVE_FUNCTION(LocalePrototype::region)
{
    // 1. Let loc be the this value.
    // 2. Perform ? RequireInternalSlot(loc, [[InitializedLocale]]).
    auto* locale_object = TRY(typed_this_object(global_object));

    // 3. Let locale be loc.[[Locale]].
    auto locale = Unicode::parse_unicode_locale_id(locale_object->locale());

    // 4. Assert: locale matches the unicode_locale_id production.
    VERIFY(locale.has_value());

    // 5. If the unicode_language_id production of locale does not contain the ["-" unicode_region_subtag] sequence, return undefined.
    if (!locale->language_id.region.has_value())
        return js_undefined();

    // 6. Return the substring of locale corresponding to the unicode_region_subtag production of the unicode_language_id.
    return js_string(vm, *locale->language_id.region);
}

}
