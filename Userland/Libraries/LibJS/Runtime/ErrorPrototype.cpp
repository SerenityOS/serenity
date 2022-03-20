/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/AST.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ErrorPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

ErrorPrototype::ErrorPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void ErrorPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_direct_property(vm.names.name, js_string(vm, "Error"), attr);
    define_direct_property(vm.names.message, js_string(vm, ""), attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    // Non standard property "stack"
    // Every other engine seems to have this in some way or another, and the spec
    // proposal for this is only Stage 1
    define_native_accessor(vm.names.stack, stack_getter, stack_setter, attr);
}

// 20.5.3.4 Error.prototype.toString ( ), https://tc39.es/ecma262/#sec-error.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(ErrorPrototype::to_string)
{
    // 1. Let O be the this value.
    // 2. If Type(O) is not Object, throw a TypeError exception.
    auto* this_object = TRY(PrototypeObject::this_object(global_object));

    // 3. Let name be ? Get(O, "name").
    auto name_property = TRY(this_object->get(vm.names.name));

    // 4. If name is undefined, set name to "Error"; otherwise set name to ? ToString(name).
    auto name = name_property.is_undefined()
        ? String { "Error"sv }
        : TRY(name_property.to_string(global_object));

    // 5. Let msg be ? Get(O, "message").
    auto message_property = TRY(this_object->get(vm.names.message));

    // 6. If msg is undefined, set msg to the empty String; otherwise set msg to ? ToString(msg).
    auto message = message_property.is_undefined()
        ? String::empty()
        : TRY(message_property.to_string(global_object));

    // 7. If name is the empty String, return msg.
    if (name.is_empty())
        return js_string(vm, message);

    // 8. If msg is the empty String, return name.
    if (message.is_empty())
        return js_string(vm, name);

    // 9. Return the string-concatenation of name, the code unit 0x003A (COLON), the code unit 0x0020 (SPACE), and msg.
    return js_string(vm, String::formatted("{}: {}", name, message));
}

// B.1.1 get Error.prototype.stack ( ), https://tc39.es/proposal-error-stacks/#sec-get-error.prototype-stack
JS_DEFINE_NATIVE_FUNCTION(ErrorPrototype::stack_getter)
{
    // 1. Let E be the this value.
    // 2. If ! Type(E) is not Object, throw a TypeError exception.
    auto* this_object = TRY(PrototypeObject::this_object(global_object));

    // 3. If E does not have an [[ErrorData]] internal slot, return undefined.
    if (!is<Error>(this_object))
        return js_undefined();

    auto& error = static_cast<Error&>(*this_object);

    // 4. Return ? GetStackString(error).
    // NOTE: These steps are not implemented based on the proposal, but to roughly follow behavior of other browsers.

    String name = "Error";
    auto name_property = TRY(error.get(vm.names.name));
    if (!name_property.is_undefined())
        name = TRY(name_property.to_string(global_object));

    String message = "";
    auto message_property = TRY(error.get(vm.names.message));
    if (!message_property.is_undefined())
        message = TRY(message_property.to_string(global_object));

    String header = name;
    if (!message.is_empty())
        header = String::formatted("{}: {}", name, message);

    return js_string(vm, String::formatted("{}\n{}", header, error.stack_string()));
}

// B.1.2 set Error.prototype.stack ( value ), https://tc39.es/proposal-error-stacks/#sec-set-error.prototype-stack
JS_DEFINE_NATIVE_FUNCTION(ErrorPrototype::stack_setter)
{
    // 1. Let E be the this value.
    auto this_value = vm.this_value(global_object);

    // 2. If ! Type(E) is not Object, throw a TypeError exception.
    if (!this_value.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, this_value.to_string_without_side_effects());

    auto& this_object = this_value.as_object();

    // 3. Let numberOfArgs be the number of arguments passed to this function call.
    // 4. If numberOfArgs is 0, throw a TypeError exception.
    if (vm.argument_count() == 0)
        return vm.throw_completion<TypeError>(global_object, ErrorType::BadArgCountOne, "set stack");

    // 5. Return ? CreateDataPropertyOrThrow(E, "stack", value);
    return TRY(this_object.create_data_property_or_throw(vm.names.stack, vm.argument(0)));
}

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    PrototypeName::PrototypeName(GlobalObject& global_object)                            \
        : PrototypeObject(*global_object.error_prototype())                              \
    {                                                                                    \
    }                                                                                    \
                                                                                         \
    void PrototypeName::initialize(GlobalObject& global_object)                          \
    {                                                                                    \
        auto& vm = this->vm();                                                           \
        Object::initialize(global_object);                                               \
        u8 attr = Attribute::Writable | Attribute::Configurable;                         \
        define_direct_property(vm.names.name, js_string(vm, #ClassName), attr);          \
        define_direct_property(vm.names.message, js_string(vm, ""), attr);               \
    }

JS_ENUMERATE_NATIVE_ERRORS
#undef __JS_ENUMERATE

}
