/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/PluralRulesPrototype.h>
#include <LibJS/Runtime/MarkedValueList.h>

namespace JS::Intl {

// 16.4 Properties of the Intl.PluralRules Prototype Object, https://tc39.es/ecma402/#sec-properties-of-intl-pluralrules-prototype-object
PluralRulesPrototype::PluralRulesPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void PluralRulesPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 16.4.2 Intl.PluralRules.prototype [ @@toStringTag ], https://tc39.es/ecma402/#sec-intl.pluralrules.prototype-tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.PluralRules"sv), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.resolvedOptions, resolved_options, 0, attr);
}

// 16.4.4 Intl.PluralRules.prototype.resolvedOptions ( ), https://tc39.es/ecma402/#sec-intl.pluralrules.prototype.resolvedoptions
JS_DEFINE_NATIVE_FUNCTION(PluralRulesPrototype::resolved_options)
{
    // 1. Let pr be the this value.
    // 2. Perform ? RequireInternalSlot(pr, [[InitializedPluralRules]]).
    auto* plural_rules = TRY(typed_this_object(global_object));

    // 3. Let options be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* options = Object::create(global_object, global_object.object_prototype());

    // 4. For each row of Table 14, except the header row, in table order, do
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
    // FIXME: Implement this when the data is available in LibUnicode.
    MarkedValueList plural_categories { vm.heap() };

    // 6. Perform ! CreateDataProperty(options, "pluralCategories", CreateArrayFromList(pluralCategories)).
    MUST(options->create_data_property_or_throw(vm.names.pluralCategories, Array::create_from(global_object, plural_categories)));

    // 7. Return options.
    return options;
}

}
