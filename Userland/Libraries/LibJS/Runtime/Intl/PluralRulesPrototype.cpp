/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/MarkedVector.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/PluralRulesPrototype.h>
#include <LibUnicode/PluralRules.h>

namespace JS::Intl {

// 16.3 Properties of the Intl.PluralRules Prototype Object, https://tc39.es/ecma402/#sec-properties-of-intl-pluralrules-prototype-object
PluralRulesPrototype::PluralRulesPrototype(Realm& realm)
    : PrototypeObject(*realm.global_object().object_prototype())
{
}

void PluralRulesPrototype::initialize(Realm& realm)
{
    Object::initialize(realm);

    auto& vm = this->vm();

    // 16.3.2 Intl.PluralRules.prototype [ @@toStringTag ], https://tc39.es/ecma402/#sec-intl.pluralrules.prototype-tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.PluralRules"sv), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.select, select, 1, attr);
    define_native_function(vm.names.selectRange, select_range, 2, attr);
    define_native_function(vm.names.resolvedOptions, resolved_options, 0, attr);
}

// 16.3.3 Intl.PluralRules.prototype.select ( value ), https://tc39.es/ecma402/#sec-intl.pluralrules.prototype.select
JS_DEFINE_NATIVE_FUNCTION(PluralRulesPrototype::select)
{
    // 1. Let pr be the this value.
    // 2. Perform ? RequireInternalSlot(pr, [[InitializedPluralRules]]).
    auto* plural_rules = TRY(typed_this_object(global_object));

    // 3. Let n be ? ToNumber(value).
    auto number = TRY(vm.argument(0).to_number(global_object));

    // 4. Return ! ResolvePlural(pr, n).
    auto plurality = resolve_plural(*plural_rules, number);
    return js_string(vm, Unicode::plural_category_to_string(plurality));
}

// 1.4.4 Intl.PluralRules.prototype.selectRange ( start, end ), https://tc39.es/proposal-intl-numberformat-v3/out/pluralrules/proposed.html#sec-intl.pluralrules.prototype.selectrange
JS_DEFINE_NATIVE_FUNCTION(PluralRulesPrototype::select_range)
{
    auto start = vm.argument(0);
    auto end = vm.argument(1);

    // 1. Let pr be the this value.
    // 2. Perform ? RequireInternalSlot(pr, [[InitializedPluralRules]]).
    auto* plural_rules = TRY(typed_this_object(global_object));

    // 3. If start is undefined or end is undefined, throw a TypeError exception.
    if (start.is_undefined())
        return vm.throw_completion<TypeError>(global_object, ErrorType::IsUndefined, "start"sv);
    if (end.is_undefined())
        return vm.throw_completion<TypeError>(global_object, ErrorType::IsUndefined, "end"sv);

    // 4. Let x be ? ToNumber(start).
    auto x = TRY(start.to_number(global_object));

    // 5. Let y be ? ToNumber(end).
    auto y = TRY(end.to_number(global_object));

    // 6. Return ? ResolvePluralRange(pr, x, y).
    auto plurality = TRY(resolve_plural_range(global_object, *plural_rules, x, y));
    return js_string(vm, Unicode::plural_category_to_string(plurality));
}

// 16.3.4 Intl.PluralRules.prototype.resolvedOptions ( ), https://tc39.es/ecma402/#sec-intl.pluralrules.prototype.resolvedoptions
// 1.4.5 Intl.PluralRules.prototype.resolvedOptions ( ), https://tc39.es/proposal-intl-numberformat-v3/out/pluralrules/proposed.html#sec-intl.pluralrules.prototype.resolvedoptions
JS_DEFINE_NATIVE_FUNCTION(PluralRulesPrototype::resolved_options)
{
    // 1. Let pr be the this value.
    // 2. Perform ? RequireInternalSlot(pr, [[InitializedPluralRules]]).
    auto* plural_rules = TRY(typed_this_object(global_object));

    // 3. Let options be OrdinaryObjectCreate(%Object.prototype%).
    auto* options = Object::create(global_object, global_object.object_prototype());

    // 4. For each row of Table 13, except the header row, in table order, do
    //     a. Let p be the Property value of the current row.
    //     b. Let v be the value of pr's internal slot whose name is the Internal Slot value of the current row.
    //     c. If v is not undefined, then
    //         i. Perform ! CreateDataPropertyOrThrow(options, p, v).
    MUST(options->create_data_property_or_throw(vm.names.locale, js_string(vm, plural_rules->locale())));
    MUST(options->create_data_property_or_throw(vm.names.type, js_string(vm, plural_rules->type_string())));
    MUST(options->create_data_property_or_throw(vm.names.minimumIntegerDigits, Value(plural_rules->min_integer_digits())));
    if (plural_rules->has_min_fraction_digits())
        MUST(options->create_data_property_or_throw(vm.names.minimumFractionDigits, Value(plural_rules->min_fraction_digits())));
    if (plural_rules->has_max_fraction_digits())
        MUST(options->create_data_property_or_throw(vm.names.maximumFractionDigits, Value(plural_rules->max_fraction_digits())));
    if (plural_rules->has_min_significant_digits())
        MUST(options->create_data_property_or_throw(vm.names.minimumSignificantDigits, Value(plural_rules->min_significant_digits())));
    if (plural_rules->has_max_significant_digits())
        MUST(options->create_data_property_or_throw(vm.names.maximumSignificantDigits, Value(plural_rules->max_significant_digits())));

    // 5. Let pluralCategories be a List of Strings containing all possible results of PluralRuleSelect for the selected locale pr.[[Locale]].
    auto available_categories = Unicode::available_plural_categories(plural_rules->locale(), plural_rules->type());

    auto* plural_categories = Array::create_from<Unicode::PluralCategory>(global_object, available_categories, [&](auto category) {
        return js_string(vm, Unicode::plural_category_to_string(category));
    });

    // 6. Perform ! CreateDataProperty(options, "pluralCategories", CreateArrayFromList(pluralCategories)).
    MUST(options->create_data_property_or_throw(vm.names.pluralCategories, plural_categories));

    switch (plural_rules->rounding_type()) {
    // 7. If pr.[[RoundingType]] is morePrecision, then
    case NumberFormatBase::RoundingType::MorePrecision:
        // a. Perform ! CreateDataPropertyOrThrow(options, "roundingPriority", "morePrecision").
        MUST(options->create_data_property_or_throw(vm.names.roundingPriority, js_string(vm, "morePrecision"sv)));
        break;
    // 8. Else if pr.[[RoundingType]] is lessPrecision, then
    case NumberFormatBase::RoundingType::LessPrecision:
        // a. Perform ! CreateDataPropertyOrThrow(options, "roundingPriority", "lessPrecision").
        MUST(options->create_data_property_or_throw(vm.names.roundingPriority, js_string(vm, "lessPrecision"sv)));
        break;
    // 9. Else,
    default:
        // a. Perform ! CreateDataPropertyOrThrow(options, "roundingPriority", "auto").
        MUST(options->create_data_property_or_throw(vm.names.roundingPriority, js_string(vm, "auto"sv)));
        break;
    }

    // 10. Return options.
    return options;
}

}
