/*
 * Copyright (c) 2021-2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/DisplayNames.h>
#include <LibJS/Runtime/Intl/DisplayNamesPrototype.h>
#include <LibUnicode/Locale.h>

namespace JS::Intl {

// 12.4 Properties of the Intl.DisplayNames Prototype Object, https://tc39.es/ecma402/#sec-properties-of-intl-displaynames-prototype-object
DisplayNamesPrototype::DisplayNamesPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void DisplayNamesPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 12.4.2 Intl.DisplayNames.prototype[ @@toStringTag ], https://tc39.es/ecma402/#sec-Intl.DisplayNames.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.DisplayNames"), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.of, of, 1, attr);
    define_native_function(vm.names.resolvedOptions, resolved_options, 0, attr);
}

// 12.4.3 Intl.DisplayNames.prototype.of ( code ), https://tc39.es/ecma402/#sec-Intl.DisplayNames.prototype.of
JS_DEFINE_NATIVE_FUNCTION(DisplayNamesPrototype::of)
{
    auto code = vm.argument(0);

    // 1. Let displayNames be this value.
    // 2. Perform ? RequireInternalSlot(displayNames, [[InitializedDisplayNames]]).
    auto* display_names = TRY(typed_this_object(global_object));

    // 3. Let code be ? ToString(code).
    auto code_string = TRY(code.to_string(global_object));
    code = js_string(vm, move(code_string));

    // 4. Let code be ? CanonicalCodeForDisplayNames(displayNames.[[Type]], code).
    code = TRY(canonical_code_for_display_names(global_object, display_names->type(), code.as_string().string()));

    // 5. Let fields be displayNames.[[Fields]].
    // 6. If fields has a field [[<code>]], return fields.[[<code>]].
    Optional<StringView> result;
    Optional<String> formatted_result;

    switch (display_names->type()) {
    case DisplayNames::Type::Language:
        if (display_names->language_display() == DisplayNames::LanguageDisplay::Dialect) {
            result = Unicode::get_locale_language_mapping(display_names->locale(), code.as_string().string());
            if (result.has_value())
                break;
        }

        if (auto locale = is_structurally_valid_language_tag(code.as_string().string()); locale.has_value())
            formatted_result = Unicode::format_locale_for_display(display_names->locale(), locale.release_value());
        break;
    case DisplayNames::Type::Region:
        result = Unicode::get_locale_territory_mapping(display_names->locale(), code.as_string().string());
        break;
    case DisplayNames::Type::Script:
        result = Unicode::get_locale_script_mapping(display_names->locale(), code.as_string().string());
        break;
    case DisplayNames::Type::Currency:
        switch (display_names->style()) {
        case Unicode::Style::Long:
            result = Unicode::get_locale_long_currency_mapping(display_names->locale(), code.as_string().string());
            break;
        case Unicode::Style::Short:
            result = Unicode::get_locale_short_currency_mapping(display_names->locale(), code.as_string().string());
            break;
        case Unicode::Style::Narrow:
            result = Unicode::get_locale_narrow_currency_mapping(display_names->locale(), code.as_string().string());
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        break;
    case DisplayNames::Type::Calendar:
        result = Unicode::get_locale_calendar_mapping(display_names->locale(), code.as_string().string());
        break;
    case DisplayNames::Type::DateTimeField:
        switch (display_names->style()) {
        case Unicode::Style::Long:
            result = Unicode::get_locale_long_date_field_mapping(display_names->locale(), code.as_string().string());
            break;
        case Unicode::Style::Short:
            result = Unicode::get_locale_short_date_field_mapping(display_names->locale(), code.as_string().string());
            break;
        case Unicode::Style::Narrow:
            result = Unicode::get_locale_narrow_date_field_mapping(display_names->locale(), code.as_string().string());
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    if (result.has_value())
        return js_string(vm, result.release_value());
    if (formatted_result.has_value())
        return js_string(vm, formatted_result.release_value());

    // 7. If displayNames.[[Fallback]] is "code", return code.
    if (display_names->fallback() == DisplayNames::Fallback::Code)
        return code;

    // 8. Return undefined.
    return js_undefined();
}

// 12.4.4 Intl.DisplayNames.prototype.resolvedOptions ( ), https://tc39.es/ecma402/#sec-Intl.DisplayNames.prototype.resolvedOptions
JS_DEFINE_NATIVE_FUNCTION(DisplayNamesPrototype::resolved_options)
{
    // 1. Let displayNames be this value.
    // 2. Perform ? RequireInternalSlot(displayNames, [[InitializedDisplayNames]]).
    auto* display_names = TRY(typed_this_object(global_object));

    // 3. Let options be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* options = Object::create(global_object, global_object.object_prototype());

    // 4. For each row of Table 8, except the header row, in table order, do
    //     a. Let p be the Property value of the current row.
    //     b. Let v be the value of displayNames's internal slot whose name is the Internal Slot value of the current row.
    //     c. Assert: v is not undefined.
    //     d. Perform ! CreateDataPropertyOrThrow(options, p, v).
    MUST(options->create_data_property_or_throw(vm.names.locale, js_string(vm, display_names->locale())));
    MUST(options->create_data_property_or_throw(vm.names.style, js_string(vm, display_names->style_string())));
    MUST(options->create_data_property_or_throw(vm.names.type, js_string(vm, display_names->type_string())));
    MUST(options->create_data_property_or_throw(vm.names.fallback, js_string(vm, display_names->fallback_string())));

    // NOTE: Step 4c indicates languageDisplay must not be undefined, but it is only set when the type option is language.
    if (display_names->has_language_display())
        MUST(options->create_data_property_or_throw(vm.names.languageDisplay, js_string(vm, display_names->language_display_string())));

    // 5. Return options.
    return options;
}

}
