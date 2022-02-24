/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/FunctionObject.h>

namespace JS {

FunctionObject::FunctionObject(GlobalObject& global_object, Object* prototype)
    : Object(global_object, prototype)
{
}

FunctionObject::FunctionObject(Object& prototype)
    : Object(prototype)
{
}

FunctionObject::~FunctionObject()
{
}

// 10.2.9 SetFunctionName ( F, name [ , prefix ] ), https://tc39.es/ecma262/#sec-setfunctionname
void FunctionObject::set_function_name(Variant<PropertyKey, PrivateName> const& name_arg, Optional<StringView> const& prefix)
{
    auto& vm = this->vm();

    // 1. Assert: F is an extensible object that does not have a "name" own property.
    VERIFY(m_is_extensible);
    VERIFY(!storage_has(vm.names.name));

    String name;

    // 2. If Type(name) is Symbol, then
    if (auto const* property_key = name_arg.get_pointer<PropertyKey>(); property_key && property_key->is_symbol()) {
        // a. Let description be name's [[Description]] value.
        auto const& description = property_key->as_symbol()->raw_description();

        // b. If description is undefined, set name to the empty String.
        if (!description.has_value())
            name = String::empty();
        // c. Else, set name to the string-concatenation of "[", description, and "]".
        else
            name = String::formatted("[{}]", *description);
    }
    // 3. Else if name is a Private Name, then
    else if (auto const* private_name = name_arg.get_pointer<PrivateName>()) {
        // a. Set name to name.[[Description]].
        name = private_name->description;
    }
    // NOTE: This is necessary as we use a different parameter name.
    else {
        name = name_arg.get<PropertyKey>().to_string();
    }

    // 4. If F has an [[InitialName]] internal slot, then
    if (is<NativeFunction>(this)) {
        // a. Set F.[[InitialName]] to name.
        static_cast<NativeFunction&>(*this).set_initial_name({}, name);
    }

    // 5. If prefix is present, then
    if (prefix.has_value()) {
        // a. Set name to the string-concatenation of prefix, the code unit 0x0020 (SPACE), and name.
        name = String::formatted("{} {}", *prefix, name);

        // b. If F has an [[InitialName]] internal slot, then
        if (is<NativeFunction>(this)) {
            // i. Optionally, set F.[[InitialName]] to name.
            static_cast<NativeFunction&>(*this).set_initial_name({}, name);
        }
    }

    // 6. Return ! DefinePropertyOrThrow(F, "name", PropertyDescriptor { [[Value]]: name, [[Writable]]: false, [[Enumerable]]: false, [[Configurable]]: true }).
    MUST(define_property_or_throw(vm.names.name, PropertyDescriptor { .value = js_string(vm, move(name)), .writable = false, .enumerable = false, .configurable = true }));
}

// 10.2.10 SetFunctionLength ( F, length ), https://tc39.es/ecma262/#sec-setfunctionlength
void FunctionObject::set_function_length(double length)
{
    auto& vm = this->vm();

    // "length (a non-negative integer or +∞)"
    VERIFY(trunc(length) == length || __builtin_isinf_sign(length) == 1);

    // 1. Assert: F is an extensible object that does not have a "length" own property.
    VERIFY(m_is_extensible);
    VERIFY(!storage_has(vm.names.length));

    // 2. Return ! DefinePropertyOrThrow(F, "length", PropertyDescriptor { [[Value]]: 𝔽(length), [[Writable]]: false, [[Enumerable]]: false, [[Configurable]]: true }).
    MUST(define_property_or_throw(vm.names.length, PropertyDescriptor { .value = Value { length }, .writable = false, .enumerable = false, .configurable = true }));
}

}
