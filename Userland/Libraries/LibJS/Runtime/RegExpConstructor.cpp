/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/RegExpConstructor.h>
#include <LibJS/Runtime/RegExpObject.h>

namespace JS {

RegExpConstructor::RegExpConstructor(Realm& realm)
    : NativeFunction(realm.vm().names.RegExp.as_string(), *realm.intrinsics().function_prototype())
{
}

void RegExpConstructor::initialize(Realm& realm)
{
    auto& vm = this->vm();
    NativeFunction::initialize(realm);

    // 22.2.4.1 RegExp.prototype, https://tc39.es/ecma262/#sec-regexp.prototype
    define_direct_property(vm.names.prototype, realm.intrinsics().regexp_prototype(), 0);

    define_native_accessor(realm, *vm.well_known_symbol_species(), symbol_species_getter, {}, Attribute::Configurable);

    define_direct_property(vm.names.length, Value(2), Attribute::Configurable);
}

// 22.2.3.1 RegExp ( pattern, flags ), https://tc39.es/ecma262/#sec-regexp-pattern-flags
ThrowCompletionOr<Value> RegExpConstructor::call()
{
    auto& vm = this->vm();

    auto pattern = vm.argument(0);
    auto flags = vm.argument(1);

    // 1. Let patternIsRegExp be ? IsRegExp(pattern).
    bool pattern_is_regexp = TRY(pattern.is_regexp(vm));

    // 2. If NewTarget is undefined, then
    // a. Let newTarget be the active function object.
    auto& new_target = *this;

    // b. If patternIsRegExp is true and flags is undefined, then
    if (pattern_is_regexp && flags.is_undefined()) {
        // i. Let patternConstructor be ? Get(pattern, "constructor").
        auto pattern_constructor = TRY(pattern.as_object().get(vm.names.constructor));

        // ii. If SameValue(newTarget, patternConstructor) is true, return pattern.
        if (same_value(&new_target, pattern_constructor))
            return pattern;
    }

    return TRY(construct(new_target));
}

// 22.2.3.1 RegExp ( pattern, flags ), https://tc39.es/ecma262/#sec-regexp-pattern-flags
ThrowCompletionOr<Object*> RegExpConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();

    auto pattern = vm.argument(0);
    auto flags = vm.argument(1);

    // 1. Let patternIsRegExp be ? IsRegExp(pattern).
    bool pattern_is_regexp = TRY(pattern.is_regexp(vm));

    // NOTE: Step 2 is handled in call() above.
    // 3. Else, let newTarget be NewTarget.

    Value pattern_value;
    Value flags_value;

    // 4. If pattern is an Object and pattern has a [[RegExpMatcher]] internal slot, then
    if (pattern.is_object() && is<RegExpObject>(pattern.as_object())) {
        // a. Let P be pattern.[[OriginalSource]].
        auto& regexp_pattern = static_cast<RegExpObject&>(pattern.as_object());
        pattern_value = js_string(vm, regexp_pattern.pattern());

        // b. If flags is undefined, let F be pattern.[[OriginalFlags]].
        if (flags.is_undefined())
            flags_value = js_string(vm, regexp_pattern.flags());
        // c. Else, let F be flags.
        else
            flags_value = flags;
    }
    // 5. Else if patternIsRegExp is true, then
    else if (pattern_is_regexp) {
        // a. Let P be ? Get(pattern, "source").
        pattern_value = TRY(pattern.as_object().get(vm.names.source));

        // b. If flags is undefined, then
        if (flags.is_undefined()) {
            // i. Let F be ? Get(pattern, "flags").
            flags_value = TRY(pattern.as_object().get(vm.names.flags));
        }
        // c. Else, let F be flags.
        else {
            flags_value = flags;
        }
    }
    // 6. Else,
    else {
        // a. Let P be pattern.
        pattern_value = pattern;

        // b. Let F be flags.
        flags_value = flags;
    }

    // 7. Let O be ? RegExpAlloc(newTarget).
    auto regexp_object = TRY(regexp_alloc(vm, new_target));

    // 8. Return ? RegExpInitialize(O, P, F).
    return TRY(regexp_object->regexp_initialize(vm, pattern_value, flags_value)).ptr();
}

// 22.2.4.2 get RegExp [ @@species ], https://tc39.es/ecma262/#sec-get-regexp-@@species
JS_DEFINE_NATIVE_FUNCTION(RegExpConstructor::symbol_species_getter)
{
    // 1. Return the this value.
    return vm.this_value();
}

}
