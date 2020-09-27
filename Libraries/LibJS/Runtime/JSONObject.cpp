/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/StringBuilder.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/JSONObject.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

JSONObject::JSONObject(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void JSONObject::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function("stringify", stringify, 3, attr);
    define_native_function("parse", parse, 2, attr);

    define_property(global_object.vm().well_known_symbol_to_string_tag(), js_string(global_object.heap(), "JSON"), Attribute::Configurable);
}

JSONObject::~JSONObject()
{
}

String JSONObject::stringify_impl(Interpreter& interpreter, GlobalObject& global_object, Value value, Value replacer, Value space)
{

    StringifyState state;

    if (replacer.is_object()) {
        if (replacer.as_object().is_function()) {
            state.replacer_function = &replacer.as_function();
        } else if (replacer.is_array()) {
            auto& replacer_object = replacer.as_object();
            auto replacer_length = length_of_array_like(interpreter, replacer);
            if (interpreter.exception())
                return {};
            Vector<String> list;
            for (size_t i = 0; i < replacer_length; ++i) {
                auto replacer_value = replacer_object.get(i);
                if (interpreter.exception())
                    return {};
                String item;
                if (replacer_value.is_string() || replacer_value.is_number()) {
                    item = replacer_value.to_string(interpreter);
                    if (interpreter.exception())
                        return {};
                } else if (replacer_value.is_object()) {
                    auto& value_object = replacer_value.as_object();
                    if (value_object.is_string_object() || value_object.is_number_object()) {
                        item = value_object.value_of().to_string(interpreter);
                        if (interpreter.exception())
                            return {};
                    }
                }
                if (!item.is_null() && !list.contains_slow(item)) {
                    list.append(item);
                }
            }
            state.property_list = list;
        }
    }

    if (space.is_object()) {
        auto& space_obj = space.as_object();
        if (space_obj.is_string_object() || space_obj.is_number_object())
            space = space_obj.value_of();
    }

    if (space.is_number()) {
        StringBuilder gap_builder;
        auto gap_size = min(10, space.as_i32());
        for (auto i = 0; i < gap_size; ++i)
            gap_builder.append(' ');
        state.gap = gap_builder.to_string();
    } else if (space.is_string()) {
        auto string = space.as_string().string();
        if (string.length() <= 10) {
            state.gap = string;
        } else {
            state.gap = string.substring(0, 10);
        }
    } else {
        state.gap = String::empty();
    }

    auto* wrapper = Object::create_empty(global_object);
    wrapper->define_property(String::empty(), value);
    if (interpreter.exception())
        return {};
    auto result = serialize_json_property(interpreter, state, String::empty(), wrapper);
    if (interpreter.exception())
        return {};
    if (result.is_null())
        return {};

    return result;
}

JS_DEFINE_NATIVE_FUNCTION(JSONObject::stringify)
{
    if (!interpreter.argument_count())
        return js_undefined();

    auto value = interpreter.argument(0);
    auto replacer = interpreter.argument(1);
    auto space = interpreter.argument(2);

    auto string = stringify_impl(interpreter, global_object, value, replacer, space);
    if (string.is_null())
        return js_undefined();

    return js_string(interpreter, string);
}

String JSONObject::serialize_json_property(Interpreter& interpreter, StringifyState& state, const PropertyName& key, Object* holder)
{
    auto value = holder->get(key);
    if (interpreter.exception())
        return {};
    if (value.is_object()) {
        auto to_json = value.as_object().get("toJSON");
        if (interpreter.exception())
            return {};
        if (to_json.is_function()) {
            value = interpreter.call(to_json.as_function(), value, js_string(interpreter, key.to_string()));
            if (interpreter.exception())
                return {};
        }
    }

    if (state.replacer_function) {
        value = interpreter.call(*state.replacer_function, holder, js_string(interpreter, key.to_string()), value);
        if (interpreter.exception())
            return {};
    }

    if (value.is_object()) {
        auto& value_object = value.as_object();
        if (value_object.is_number_object() || value_object.is_boolean_object() || value_object.is_string_object() || value_object.is_bigint_object())
            value = value_object.value_of();
    }

    if (value.is_null())
        return "null";
    if (value.is_boolean())
        return value.as_bool() ? "true" : "false";
    if (value.is_string())
        return quote_json_string(value.as_string().string());
    if (value.is_number()) {
        if (value.is_finite_number())
            return value.to_string(interpreter);
        return "null";
    }
    if (value.is_object() && !value.is_function()) {
        if (value.is_array())
            return serialize_json_array(interpreter, state, static_cast<Array&>(value.as_object()));
        return serialize_json_object(interpreter, state, value.as_object());
    }
    if (value.is_bigint())
        interpreter.vm().throw_exception<TypeError>(interpreter.global_object(), ErrorType::JsonBigInt);
    return {};
}

String JSONObject::serialize_json_object(Interpreter& interpreter, StringifyState& state, Object& object)
{
    if (state.seen_objects.contains(&object)) {
        interpreter.vm().throw_exception<TypeError>(interpreter.global_object(), ErrorType::JsonCircular);
        return {};
    }

    state.seen_objects.set(&object);
    String previous_indent = state.indent;
    state.indent = String::format("%s%s", state.indent.characters(), state.gap.characters());
    Vector<String> property_strings;

    auto process_property = [&](const PropertyName& key) {
        auto serialized_property_string = serialize_json_property(interpreter, state, key, &object);
        if (interpreter.exception())
            return;
        if (!serialized_property_string.is_null()) {
            property_strings.append(String::format(
                "%s:%s%s",
                quote_json_string(key.to_string()).characters(),
                state.gap.is_empty() ? "" : " ",
                serialized_property_string.characters()));
        }
    };

    if (state.property_list.has_value()) {
        auto property_list = state.property_list.value();
        for (auto& property : property_list) {
            process_property(property);
            if (interpreter.exception())
                return {};
        }
    } else {
        for (auto& entry : object.indexed_properties()) {
            auto value_and_attributes = entry.value_and_attributes(&object);
            if (!value_and_attributes.attributes.is_enumerable())
                continue;
            process_property(entry.index());
            if (interpreter.exception())
                return {};
        }
        for (auto& [key, metadata] : object.shape().property_table_ordered()) {
            if (!metadata.attributes.is_enumerable())
                continue;
            process_property(key);
            if (interpreter.exception())
                return {};
        }
    }
    StringBuilder builder;
    if (property_strings.is_empty()) {
        builder.append("{}");
    } else {
        bool first = true;
        builder.append('{');
        if (state.gap.is_empty()) {
            for (auto& property_string : property_strings) {
                if (!first)
                    builder.append(',');
                first = false;
                builder.append(property_string);
            }
        } else {
            builder.append('\n');
            builder.append(state.indent);
            auto separator = String::format(",\n%s", state.indent.characters());
            for (auto& property_string : property_strings) {
                if (!first)
                    builder.append(separator);
                first = false;
                builder.append(property_string);
            }
            builder.append('\n');
            builder.append(previous_indent);
        }
        builder.append('}');
    }

    state.seen_objects.remove(&object);
    state.indent = previous_indent;
    return builder.to_string();
}

String JSONObject::serialize_json_array(Interpreter& interpreter, StringifyState& state, Object& object)
{
    if (state.seen_objects.contains(&object)) {
        interpreter.vm().throw_exception<TypeError>(interpreter.global_object(), ErrorType::JsonCircular);
        return {};
    }

    state.seen_objects.set(&object);
    String previous_indent = state.indent;
    state.indent = String::format("%s%s", state.indent.characters(), state.gap.characters());
    Vector<String> property_strings;

    auto length = length_of_array_like(interpreter, Value(&object));
    if (interpreter.exception())
        return {};
    for (size_t i = 0; i < length; ++i) {
        if (interpreter.exception())
            return {};
        auto serialized_property_string = serialize_json_property(interpreter, state, i, &object);
        if (interpreter.exception())
            return {};
        if (serialized_property_string.is_null()) {
            property_strings.append("null");
        } else {
            property_strings.append(serialized_property_string);
        }
    }

    StringBuilder builder;
    if (property_strings.is_empty()) {
        builder.append("[]");
    } else {
        if (state.gap.is_empty()) {
            builder.append('[');
            bool first = true;
            for (auto& property_string : property_strings) {
                if (!first)
                    builder.append(',');
                first = false;
                builder.append(property_string);
            }
            builder.append(']');
        } else {
            builder.append("[\n");
            builder.append(state.indent);
            auto separator = String::format(",\n%s", state.indent.characters());
            bool first = true;
            for (auto& property_string : property_strings) {
                if (!first)
                    builder.append(separator);
                first = false;
                builder.append(property_string);
            }
            builder.append('\n');
            builder.append(previous_indent);
            builder.append(']');
        }
    }

    state.seen_objects.remove(&object);
    state.indent = previous_indent;
    return builder.to_string();
}

String JSONObject::quote_json_string(String string)
{
    // FIXME: Handle UTF16
    StringBuilder builder;
    builder.append('"');
    for (auto& ch : string) {
        switch (ch) {
        case '\b':
            builder.append("\\b");
            break;
        case '\t':
            builder.append("\\t");
            break;
        case '\n':
            builder.append("\\n");
            break;
        case '\f':
            builder.append("\\f");
            break;
        case '\r':
            builder.append("\\r");
            break;
        case '"':
            builder.append("\\\"");
            break;
        case '\\':
            builder.append("\\\\");
            break;
        default:
            if (ch < 0x20) {
                builder.append("\\u%#08x", ch);
            } else {
                builder.append(ch);
            }
        }
    }
    builder.append('"');
    return builder.to_string();
}

JS_DEFINE_NATIVE_FUNCTION(JSONObject::parse)
{
    if (!interpreter.argument_count())
        return js_undefined();
    auto string = interpreter.argument(0).to_string(interpreter);
    if (interpreter.exception())
        return {};
    auto reviver = interpreter.argument(1);

    auto json = JsonValue::from_string(string);
    if (!json.has_value()) {
        interpreter.vm().throw_exception<SyntaxError>(global_object, ErrorType::JsonMalformed);
        return {};
    }
    Value result = parse_json_value(interpreter, global_object, json.value());
    if (reviver.is_function()) {
        auto* holder_object = Object::create_empty(global_object);
        holder_object->define_property(String::empty(), result);
        if (interpreter.exception())
            return {};
        return internalize_json_property(interpreter, holder_object, String::empty(), reviver.as_function());
    }
    return result;
}

Value JSONObject::parse_json_value(Interpreter& interpreter, GlobalObject& global_object, const JsonValue& value)
{
    if (value.is_object())
        return Value(parse_json_object(interpreter, global_object, value.as_object()));
    if (value.is_array())
        return Value(parse_json_array(interpreter, global_object, value.as_array()));
    if (value.is_null())
        return js_null();
#if !defined(KERNEL)
    if (value.is_double())
        return Value(value.as_double());
#endif
    if (value.is_number())
        return Value(value.to_i32(0));
    if (value.is_string())
        return js_string(interpreter, value.to_string());
    if (value.is_bool())
        return Value(static_cast<bool>(value.as_bool()));
    ASSERT_NOT_REACHED();
}

Object* JSONObject::parse_json_object(Interpreter& interpreter, GlobalObject& global_object, const JsonObject& json_object)
{
    auto* object = Object::create_empty(global_object);
    json_object.for_each_member([&](auto& key, auto& value) {
        object->define_property(key, parse_json_value(interpreter, global_object, value));
    });
    return object;
}

Array* JSONObject::parse_json_array(Interpreter& interpreter, GlobalObject& global_object, const JsonArray& json_array)
{
    auto* array = Array::create(global_object);
    size_t index = 0;
    json_array.for_each([&](auto& value) {
        array->define_property(index++, parse_json_value(interpreter, global_object, value));
    });
    return array;
}

Value JSONObject::internalize_json_property(Interpreter& interpreter, Object* holder, const PropertyName& name, Function& reviver)
{
    auto value = holder->get(name);
    if (interpreter.exception())
        return {};
    if (value.is_object()) {
        auto& value_object = value.as_object();

        auto process_property = [&](const PropertyName& key) {
            auto element = internalize_json_property(interpreter, &value_object, key, reviver);
            if (interpreter.exception())
                return;
            if (element.is_undefined()) {
                value_object.delete_property(key);
            } else {
                value_object.define_property(key, element, default_attributes, false);
            }
        };

        if (value_object.is_array()) {
            auto length = length_of_array_like(interpreter, value);
            for (size_t i = 0; i < length; ++i) {
                process_property(i);
                if (interpreter.exception())
                    return {};
            }
        } else {
            for (auto& entry : value_object.indexed_properties()) {
                auto value_and_attributes = entry.value_and_attributes(&value_object);
                if (!value_and_attributes.attributes.is_enumerable())
                    continue;
                process_property(entry.index());
                if (interpreter.exception())
                    return {};
            }
            for (auto& [key, metadata] : value_object.shape().property_table_ordered()) {
                if (!metadata.attributes.is_enumerable())
                    continue;
                process_property(key);
                if (interpreter.exception())
                    return {};
            }
        }
    }

    return interpreter.call(reviver, Value(holder), js_string(interpreter, name.to_string()), value);
}

}
