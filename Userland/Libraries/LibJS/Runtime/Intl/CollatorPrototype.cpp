/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/Collator.h>
#include <LibJS/Runtime/Intl/CollatorPrototype.h>

namespace JS::Intl {

// 10.3 Properties of the Intl.Collator Prototype Object, https://tc39.es/ecma402/#sec-properties-of-the-intl-collator-prototype-object
CollatorPrototype::CollatorPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void CollatorPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 10.3.2 Intl.Collator.prototype [ @@toStringTag ], https://tc39.es/ecma402/#sec-intl.collator.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.Collator"), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.resolvedOptions, resolved_options, 0, attr);
}

// 10.3.4 Intl.Collator.prototype.resolvedOptions ( ), https://tc39.es/ecma402/#sec-intl.collator.prototype.resolvedoptions
JS_DEFINE_NATIVE_FUNCTION(CollatorPrototype::resolved_options)
{
    // 1. Let collator be the this value.
    // 2. Perform ? RequireInternalSlot(collator, [[InitializedCollator]]).
    auto* collator = TRY(typed_this_object(global_object));

    // 3. Let options be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* options = Object::create(global_object, global_object.object_prototype());

    // 4. For each row of Table 3, except the header row, in table order, do
    //     a. Let p be the Property value of the current row.
    //     b. Let v be the value of collator's internal slot whose name is the Internal Slot value of the current row.
    //     c. If the current row has an Extension Key value, then
    //         i. Let extensionKey be the Extension Key value of the current row.
    //         ii. If %Collator%.[[RelevantExtensionKeys]] does not contain extensionKey, then
    //             1. Let v be undefined.
    //     d. If v is not undefined, then
    //         i. Perform ! CreateDataPropertyOrThrow(options, p, v).
    MUST(options->create_data_property_or_throw(vm.names.locale, js_string(vm, collator->locale())));
    MUST(options->create_data_property_or_throw(vm.names.usage, js_string(vm, collator->usage_string())));
    MUST(options->create_data_property_or_throw(vm.names.sensitivity, js_string(vm, collator->sensitivity_string())));
    MUST(options->create_data_property_or_throw(vm.names.ignorePunctuation, Value(collator->ignore_punctuation())));
    MUST(options->create_data_property_or_throw(vm.names.collation, js_string(vm, collator->collation())));
    MUST(options->create_data_property_or_throw(vm.names.numeric, Value(collator->numeric())));
    MUST(options->create_data_property_or_throw(vm.names.caseFirst, js_string(vm, collator->case_first_string())));

    // 5. Return options.
    return options;
}

}
