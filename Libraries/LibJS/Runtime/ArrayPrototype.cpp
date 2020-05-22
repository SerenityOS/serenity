/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Function.h>
#include <AK/StringBuilder.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayPrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/ObjectPrototype.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

ArrayPrototype::ArrayPrototype()
    : Object(interpreter().global_object().object_prototype())
{
    u8 attr = Attribute::Writable | Attribute::Configurable;

    put_native_function("filter", filter, 1, attr);
    put_native_function("forEach", for_each, 1, attr);
    put_native_function("map", map, 1, attr);
    put_native_function("pop", pop, 0, attr);
    put_native_function("push", push, 1, attr);
    put_native_function("shift", shift, 0, attr);
    put_native_function("toString", to_string, 0, attr);
    put_native_function("unshift", unshift, 1, attr);
    put_native_function("join", join, 1, attr);
    put_native_function("concat", concat, 1, attr);
    put_native_function("slice", slice, 2, attr);
    put_native_function("indexOf", index_of, 1, attr);
    put_native_function("reverse", reverse, 0, attr);
    put_native_function("lastIndexOf", last_index_of, 1, attr);
    put_native_function("includes", includes, 1, attr);
    put_native_function("find", find, 1, attr);
    put_native_function("findIndex", find_index, 1, attr);
    put_native_function("some", some, 1, attr);
    put_native_function("every", every, 1, attr);
    put("length", Value(0), Attribute::Configurable);
}

ArrayPrototype::~ArrayPrototype()
{
}

static Function* callback_from_args(Interpreter& interpreter, const String& name)
{
    if (interpreter.argument_count() < 1) {
        interpreter.throw_exception<TypeError>(String::format("Array.prototype.%s() requires at least one argument", name.characters()));
        return nullptr;
    }
    auto callback = interpreter.argument(0);
    if (!callback.is_function()) {
        interpreter.throw_exception<TypeError>(String::format("%s is not a function", callback.to_string_without_side_effects().characters()));
        return nullptr;
    }
    return &callback.as_function();
}

static size_t get_length(Interpreter& interpreter, Object& object)
{
    auto length_property = object.get("length");
    if (interpreter.exception())
        return 0;
    return length_property.to_size_t(interpreter);
}

static void for_each_item(Interpreter& interpreter, const String& name, AK::Function<IterationDecision(size_t index, Value value, Value callback_result)> callback, bool skip_empty = true)
{
    auto* this_object = interpreter.this_value().to_object(interpreter);
    if (!this_object)
        return;

    auto initial_length = get_length(interpreter, *this_object);
    if (interpreter.exception())
        return;

    auto* callback_function = callback_from_args(interpreter, name);
    if (!callback_function)
        return;

    auto this_value = interpreter.argument(1);

    for (size_t i = 0; i < initial_length; ++i) {
        auto value = this_object->get_by_index(i);
        if (value.is_empty()) {
            if (skip_empty)
                continue;
            value = js_undefined();
        }

        MarkedValueList arguments(interpreter.heap());
        arguments.append(value);
        arguments.append(Value((i32)i));
        arguments.append(this_object);

        auto callback_result = interpreter.call(*callback_function, this_value, move(arguments));
        if (interpreter.exception())
            return;

        if (callback(i, value, callback_result) == IterationDecision::Break)
            break;
    }
}

Value ArrayPrototype::filter(Interpreter& interpreter)
{
    auto* new_array = Array::create(interpreter.global_object());
    for_each_item(interpreter, "filter", [&](auto, auto value, auto callback_result) {
        if (callback_result.to_boolean())
            new_array->elements().append(value);
        return IterationDecision::Continue;
    });
    return Value(new_array);
}

Value ArrayPrototype::for_each(Interpreter& interpreter)
{
    for_each_item(interpreter, "forEach", [](auto, auto, auto) {
        return IterationDecision::Continue;
    });
    return js_undefined();
}

Value ArrayPrototype::map(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter);
    if (!this_object)
        return {};
    auto initial_length = get_length(interpreter, *this_object);
    if (interpreter.exception())
        return {};
    auto* new_array = Array::create(interpreter.global_object());
    new_array->elements().resize(initial_length);
    for_each_item(interpreter, "map", [&](auto index, auto, auto callback_result) {
        new_array->elements()[index] = callback_result;
        return IterationDecision::Continue;
    });
    return Value(new_array);
}

Value ArrayPrototype::push(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter);
    if (!this_object)
        return {};
    if (this_object->is_array()) {
        auto* array = static_cast<Array*>(this_object);
        for (size_t i = 0; i < interpreter.argument_count(); ++i)
            array->elements().append(interpreter.argument(i));
        return Value(array->length());
    }
    auto length = get_length(interpreter, *this_object);
    if (interpreter.exception())
        return {};
    auto argument_count = interpreter.argument_count();
    auto new_length = length + argument_count;
    if (new_length > MAX_ARRAY_LIKE_INDEX)
        return interpreter.throw_exception<TypeError>("Maximum array size exceeded");
    for (size_t i = 0; i < argument_count; ++i)
        this_object->put_by_index(length + i, interpreter.argument(i));
    auto new_length_value = Value((i32)new_length);
    this_object->put("length", new_length_value);
    if (interpreter.exception())
        return {};
    return new_length_value;
}

Value ArrayPrototype::unshift(Interpreter& interpreter)
{
    auto* array = array_from(interpreter);
    if (!array)
        return {};
    for (size_t i = 0; i < interpreter.argument_count(); ++i)
        array->elements().insert(i, interpreter.argument(i));
    return Value(array->length());
}

Value ArrayPrototype::pop(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter);
    if (!this_object)
        return {};
    if (this_object->is_array()) {
        auto* array = static_cast<Array*>(this_object);
        if (array->elements().is_empty())
            return js_undefined();
        return array->elements().take_last().value_or(js_undefined());
    }
    auto length = get_length(interpreter, *this_object);
    if (length == 0) {
        this_object->put("length", Value(0));
        return js_undefined();
    }
    auto index = length - 1;
    auto element = this_object->get_by_index(index).value_or(js_undefined());
    if (interpreter.exception())
        return {};
    this_object->delete_property(PropertyName(index));
    this_object->put("length", Value((i32)index));
    if (interpreter.exception())
        return {};
    return element;
}

Value ArrayPrototype::shift(Interpreter& interpreter)
{
    auto* array = array_from(interpreter);
    if (!array)
        return {};
    if (array->elements().is_empty())
        return js_undefined();
    return array->elements().take_first().value_or(js_undefined());
}

Value ArrayPrototype::to_string(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter);
    if (!this_object)
        return {};
    auto join_function = this_object->get("join");
    if (interpreter.exception())
        return {};
    if (!join_function.is_function())
        return ObjectPrototype::to_string(interpreter);
    return interpreter.call(join_function.as_function(), this_object);
}

Value ArrayPrototype::join(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter);
    if (!this_object)
        return {};
    String separator = ",";
    if (interpreter.argument_count()) {
        separator = interpreter.argument(0).to_string(interpreter);
        if (interpreter.exception())
            return {};
    }
    auto length = get_length(interpreter, *this_object);
    if (interpreter.exception())
        return {};
    StringBuilder builder;
    for (size_t i = 0; i < length; ++i) {
        if (i > 0)
            builder.append(separator);
        auto value = this_object->get_by_index(i).value_or(js_undefined());
        if (interpreter.exception())
            return {};
        if (value.is_undefined() || value.is_null())
            continue;
        auto string = value.to_string(interpreter);
        if (interpreter.exception())
            return {};
        builder.append(string);
    }
    return js_string(interpreter, builder.to_string());
}

Value ArrayPrototype::concat(Interpreter& interpreter)
{
    auto* array = array_from(interpreter);
    if (!array)
        return {};

    auto* new_array = Array::create(interpreter.global_object());
    new_array->elements().append(array->elements());

    for (size_t i = 0; i < interpreter.argument_count(); ++i) {
        auto argument = interpreter.argument(i);
        if (argument.is_array()) {
            auto& argument_object = argument.as_object();
            new_array->elements().append(argument_object.elements());
        } else {
            new_array->elements().append(argument);
        }
    }

    return Value(new_array);
}

Value ArrayPrototype::slice(Interpreter& interpreter)
{
    auto* array = array_from(interpreter);
    if (!array)
        return {};

    auto* new_array = Array::create(interpreter.global_object());
    if (interpreter.argument_count() == 0) {
        new_array->elements().append(array->elements());
        return new_array;
    }

    ssize_t array_size = static_cast<ssize_t>(array->elements().size());
    auto start_slice = interpreter.argument(0).to_i32(interpreter);
    if (interpreter.exception())
        return {};
    auto end_slice = array_size;

    if (start_slice > array_size)
        return new_array;

    if (start_slice < 0)
        start_slice = end_slice + start_slice;

    if (interpreter.argument_count() >= 2) {
        end_slice = interpreter.argument(1).to_i32(interpreter);
        if (interpreter.exception())
            return {};
        if (end_slice < 0)
            end_slice = array_size + end_slice;
        else if (end_slice > array_size)
            end_slice = array_size;
    }

    size_t array_capacity = start_slice + array_size - end_slice;
    new_array->elements().ensure_capacity(array_capacity);
    for (ssize_t i = start_slice; i < end_slice; ++i) {
        new_array->elements().append(array->elements().at(i));
    }

    return new_array;
}

Value ArrayPrototype::index_of(Interpreter& interpreter)
{
    auto* array = array_from(interpreter);
    if (!array)
        return {};

    i32 array_size = static_cast<i32>(array->elements().size());
    if (array_size == 0)
        return Value(-1);

    i32 from_index = 0;
    if (interpreter.argument_count() >= 2) {
        from_index = interpreter.argument(1).to_i32(interpreter);
        if (interpreter.exception())
            return {};
        if (from_index >= array_size)
            return Value(-1);
        auto negative_min_index = ((array_size - 1) * -1);
        if (from_index < negative_min_index)
            from_index = 0;
        else if (from_index < 0)
            from_index = array_size + from_index;
    }

    auto search_element = interpreter.argument(0);
    for (i32 i = from_index; i < array_size; ++i) {
        auto& element = array->elements().at(i);
        if (strict_eq(interpreter, element, search_element))
            return Value(i);
    }

    return Value(-1);
}

Value ArrayPrototype::reverse(Interpreter& interpreter)
{
    auto* array = array_from(interpreter);
    if (!array)
        return {};

    if (array->elements().size() == 0)
        return array;

    Vector<Value> array_reverse;
    array_reverse.ensure_capacity(array->elements().size());

    for (ssize_t i = array->elements().size() - 1; i >= 0; --i)
        array_reverse.append(array->elements().at(i));

    array->elements() = move(array_reverse);

    return array;
}

Value ArrayPrototype::last_index_of(Interpreter& interpreter)
{
    auto* array = array_from(interpreter);
    if (!array)
        return {};

    i32 array_size = static_cast<i32>(array->elements().size());
    if (array_size == 0)
        return Value(-1);

    i32 from_index = array_size - 1;
    if (interpreter.argument_count() >= 2) {
        from_index = interpreter.argument(1).to_i32(interpreter);
        if (interpreter.exception())
            return {};
        if (from_index >= 0)
            from_index = min(from_index, array_size - 1);
        else
            from_index = array_size + from_index;
    }

    auto search_element = interpreter.argument(0);
    for (i32 i = from_index; i >= 0; --i) {
        auto& element = array->elements().at(i);
        if (strict_eq(interpreter, element, search_element))
            return Value(i);
    }

    return Value(-1);
}

Value ArrayPrototype::includes(Interpreter& interpreter)
{
    auto* array = array_from(interpreter);
    if (!array)
        return {};

    i32 array_size = array->elements().size();
    if (array_size == 0)
        return Value(false);

    i32 from_index = 0;
    if (interpreter.argument_count() >= 2) {
        from_index = interpreter.argument(1).to_i32(interpreter);
        if (interpreter.exception())
            return {};
        if (from_index >= array_size)
            return Value(false);
        auto negative_min_index = ((array_size - 1) * -1);
        if (from_index < negative_min_index)
            from_index = 0;
        else if (from_index < 0)
            from_index = array_size + from_index;
    }

    auto value_to_find = interpreter.argument(0);
    for (i32 i = from_index; i < array_size; ++i) {
        auto& element = array->elements().at(i);
        if (same_value_zero(interpreter, element, value_to_find))
            return Value(true);
    }

    return Value(false);
}

Value ArrayPrototype::find(Interpreter& interpreter)
{
    auto result = js_undefined();
    for_each_item(
        interpreter, "find", [&](auto, auto value, auto callback_result) {
            if (callback_result.to_boolean()) {
                result = value;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        },
        false);
    return result;
}

Value ArrayPrototype::find_index(Interpreter& interpreter)
{
    auto result_index = -1;
    for_each_item(
        interpreter, "findIndex", [&](auto index, auto, auto callback_result) {
            if (callback_result.to_boolean()) {
                result_index = index;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        },
        false);
    return Value(result_index);
}

Value ArrayPrototype::some(Interpreter& interpreter)
{
    auto result = false;
    for_each_item(interpreter, "some", [&](auto, auto, auto callback_result) {
        if (callback_result.to_boolean()) {
            result = true;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return Value(result);
}

Value ArrayPrototype::every(Interpreter& interpreter)
{
    auto result = true;
    for_each_item(interpreter, "every", [&](auto, auto, auto callback_result) {
        if (!callback_result.to_boolean()) {
            result = false;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return Value(result);
}

}
