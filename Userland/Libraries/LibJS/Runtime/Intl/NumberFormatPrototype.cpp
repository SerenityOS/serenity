/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/NumberFormat.h>
#include <LibJS/Runtime/Intl/NumberFormatFunction.h>
#include <LibJS/Runtime/Intl/NumberFormatPrototype.h>

namespace JS::Intl {

// 15.4 Properties of the Intl.NumberFormat Prototype Object, https://tc39.es/ecma402/#sec-properties-of-intl-numberformat-prototype-object
NumberFormatPrototype::NumberFormatPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void NumberFormatPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 15.4.2 Intl.NumberFormat.prototype [ @@toStringTag ], https://tc39.es/ecma402/#sec-intl.numberformat.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.NumberFormat"), Attribute::Configurable);

    define_native_accessor(vm.names.format, format, nullptr, Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.formatToParts, format_to_parts, 1, attr);
    define_native_function(vm.names.resolvedOptions, resolved_options, 0, attr);
}

// 15.4.3 get Intl.NumberFormat.prototype.format, https://tc39.es/ecma402/#sec-intl.numberformat.prototype.format
JS_DEFINE_NATIVE_FUNCTION(NumberFormatPrototype::format)
{
    // 1. Let nf be the this value.
    // 2. If the implementation supports the normative optional constructor mode of 4.3 Note 1, then
    //     a. Set nf to ? UnwrapNumberFormat(nf).
    // 3. Perform ? RequireInternalSlot(nf, [[InitializedNumberFormat]]).
    auto* number_format = TRY(typed_this_object(global_object));

    // 4. If nf.[[BoundFormat]] is undefined, then
    if (!number_format->bound_format()) {
        // a. Let F be a new built-in function object as defined in Number Format Functions (15.1.4).
        // b. Set F.[[NumberFormat]] to nf.
        auto* bound_format = NumberFormatFunction::create(global_object, *number_format);

        // c. Set nf.[[BoundFormat]] to F.
        number_format->set_bound_format(bound_format);
    }

    // 5. Return nf.[[BoundFormat]].
    return number_format->bound_format();
}

// 15.4.4 Intl.NumberFormat.prototype.formatToParts ( value ), https://tc39.es/ecma402/#sec-intl.numberformat.prototype.formattoparts
JS_DEFINE_NATIVE_FUNCTION(NumberFormatPrototype::format_to_parts)
{
    auto value = vm.argument(0);

    // 1. Let nf be the this value.
    // 2. Perform ? RequireInternalSlot(nf, [[InitializedNumberFormat]]).
    auto* number_format = TRY(typed_this_object(global_object));

    // 3. Let x be ? ToNumeric(value).
    value = TRY(value.to_numeric(global_object));

    // FIXME: Support BigInt number formatting.
    if (value.is_bigint())
        return vm.throw_completion<InternalError>(global_object, ErrorType::NotImplemented, "BigInt number formatting");

    // 4. Return ? FormatNumericToParts(nf, x).
    // Note: Our implementation of FormatNumericToParts does not throw.
    return format_numeric_to_parts(global_object, *number_format, value.as_double());
}

// 15.4.5 Intl.NumberFormat.prototype.resolvedOptions ( ), https://tc39.es/ecma402/#sec-intl.numberformat.prototype.resolvedoptions
JS_DEFINE_NATIVE_FUNCTION(NumberFormatPrototype::resolved_options)
{
    // 1. Let nf be the this value.
    // 2. If the implementation supports the normative optional constructor mode of 4.3 Note 1, then
    //     a. Set nf to ? UnwrapNumberFormat(nf).
    // 3. Perform ? RequireInternalSlot(nf, [[InitializedNumberFormat]]).
    auto* number_format = TRY(typed_this_object(global_object));

    // 4. Let options be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* options = Object::create(global_object, global_object.object_prototype());

    // 5. For each row of Table 11, except the header row, in table order, do
    //     a. Let p be the Property value of the current row.
    //     b. Let v be the value of nf's internal slot whose name is the Internal Slot value of the current row.
    //     c. If v is not undefined, then
    //         i. Perform ! CreateDataPropertyOrThrow(options, p, v).
    MUST(options->create_data_property_or_throw(vm.names.locale, js_string(vm, number_format->locale())));
    MUST(options->create_data_property_or_throw(vm.names.numberingSystem, js_string(vm, number_format->numbering_system())));
    MUST(options->create_data_property_or_throw(vm.names.style, js_string(vm, number_format->style_string())));
    if (number_format->has_currency())
        MUST(options->create_data_property_or_throw(vm.names.currency, js_string(vm, number_format->currency())));
    if (number_format->has_currency_display())
        MUST(options->create_data_property_or_throw(vm.names.currencyDisplay, js_string(vm, number_format->currency_display_string())));
    if (number_format->has_currency_sign())
        MUST(options->create_data_property_or_throw(vm.names.currencySign, js_string(vm, number_format->currency_sign_string())));
    if (number_format->has_unit())
        MUST(options->create_data_property_or_throw(vm.names.unit, js_string(vm, number_format->unit())));
    if (number_format->has_unit_display())
        MUST(options->create_data_property_or_throw(vm.names.unitDisplay, js_string(vm, number_format->unit_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.minimumIntegerDigits, Value(number_format->min_integer_digits())));
    if (number_format->has_min_fraction_digits())
        MUST(options->create_data_property_or_throw(vm.names.minimumFractionDigits, Value(number_format->min_fraction_digits())));
    if (number_format->has_max_fraction_digits())
        MUST(options->create_data_property_or_throw(vm.names.maximumFractionDigits, Value(number_format->max_fraction_digits())));
    if (number_format->has_min_significant_digits())
        MUST(options->create_data_property_or_throw(vm.names.minimumSignificantDigits, Value(number_format->min_significant_digits())));
    if (number_format->has_max_significant_digits())
        MUST(options->create_data_property_or_throw(vm.names.maximumSignificantDigits, Value(number_format->max_significant_digits())));
    MUST(options->create_data_property_or_throw(vm.names.useGrouping, Value(number_format->use_grouping())));
    MUST(options->create_data_property_or_throw(vm.names.notation, js_string(vm, number_format->notation_string())));
    if (number_format->has_compact_display())
        MUST(options->create_data_property_or_throw(vm.names.compactDisplay, js_string(vm, number_format->compact_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.signDisplay, js_string(vm, number_format->sign_display_string())));

    // 5. Return options.
    return options;
}

}
