/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BigIntObject.h>
#include <LibJS/Runtime/BooleanObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/JSONObject.h>
#include <LibJS/Runtime/NumberObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/StringObject.h>

namespace JS {

JSONObject::JSONObject(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void JSONObject::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.stringify, stringify, 3, attr);
    define_native_function(vm.names.parse, parse, 2, attr);

    // 25.5.3 JSON [ @@toStringTag ], https://tc39.es/ecma262/#sec-json-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), "JSON"), Attribute::Configurable);
}

JSONObject::~JSONObject()
{
}

// 25.5.2 JSON.stringify ( value [ , replacer [ , space ] ] ), https://tc39.es/ecma262/#sec-json.stringify
String JSONObject::stringify_impl(GlobalObject& global_object, Value value, Value replacer, Value space)
{
    auto& vm = global_object.vm();
    StringifyState state;

    if (replacer.is_object()) {
        if (replacer.as_object().is_function()) {
            state.replacer_function = &replacer.as_function();
        } else {
            auto is_array = TRY_OR_DISCARD(replacer.is_array(global_object));
            if (is_array) {
                auto& replacer_object = replacer.as_object();
                auto replacer_length = TRY_OR_DISCARD(length_of_array_like(global_object, replacer_object));
                Vector<String> list;
                for (size_t i = 0; i < replacer_length; ++i) {
                    auto replacer_value = replacer_object.get(i);
                    if (vm.exception())
                        return {};
                    String item;
                    if (replacer_value.is_string()) {
                        item = replacer_value.as_string().string();
                    } else if (replacer_value.is_number()) {
                        item = replacer_value.to_string(global_object);
                    } else if (replacer_value.is_object()) {
                        auto& value_object = replacer_value.as_object();
                        if (is<StringObject>(value_object) || is<NumberObject>(value_object)) {
                            item = replacer_value.to_string(global_object);
                            if (vm.exception())
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
    }

    if (space.is_object()) {
        auto& space_object = space.as_object();
        if (is<NumberObject>(space_object)) {
            space = space.to_number(global_object);
            if (vm.exception())
                return {};
        } else if (is<StringObject>(space_object)) {
            space = space.to_primitive_string(global_object);
            if (vm.exception())
                return {};
        }
    }

    if (space.is_number()) {
        auto space_mv = space.to_integer_or_infinity(global_object);
        space_mv = min(10, space_mv);
        state.gap = space_mv < 1 ? String::empty() : String::repeated(' ', space_mv);
    } else if (space.is_string()) {
        auto string = space.as_string().string();
        if (string.length() <= 10)
            state.gap = string;
        else
            state.gap = string.substring(0, 10);
    } else {
        state.gap = String::empty();
    }

    auto* wrapper = Object::create(global_object, global_object.object_prototype());
    wrapper->create_data_property_or_throw(String::empty(), value);
    auto result = serialize_json_property(global_object, state, String::empty(), wrapper);
    if (vm.exception())
        return {};

    return result;
}

// 25.5.2 JSON.stringify ( value [ , replacer [ , space ] ] ), https://tc39.es/ecma262/#sec-json.stringify
JS_DEFINE_NATIVE_FUNCTION(JSONObject::stringify)
{
    if (!vm.argument_count())
        return js_undefined();

    auto value = vm.argument(0);
    auto replacer = vm.argument(1);
    auto space = vm.argument(2);

    auto string = stringify_impl(global_object, value, replacer, space);
    if (string.is_null())
        return js_undefined();

    return js_string(vm, string);
}

// 25.5.2.1 SerializeJSONProperty ( state, key, holder ), https://tc39.es/ecma262/#sec-serializejsonproperty
String JSONObject::serialize_json_property(GlobalObject& global_object, StringifyState& state, const PropertyName& key, Object* holder)
{
    auto& vm = global_object.vm();
    auto value = holder->get(key);
    if (vm.exception())
        return {};
    if (value.is_object() || value.is_bigint()) {
        auto* value_object = value.to_object(global_object);
        if (vm.exception())
            return {};
        auto to_json = value_object->get(vm.names.toJSON);
        if (vm.exception())
            return {};
        if (to_json.is_function())
            value = TRY_OR_DISCARD(vm.call(to_json.as_function(), value, js_string(vm, key.to_string())));
    }

    if (state.replacer_function)
        value = TRY_OR_DISCARD(vm.call(*state.replacer_function, holder, js_string(vm, key.to_string()), value));

    if (value.is_object()) {
        auto& value_object = value.as_object();
        if (is<NumberObject>(value_object)) {
            value = value.to_number(global_object);
            if (vm.exception())
                return {};
        } else if (is<StringObject>(value_object)) {
            value = value.to_primitive_string(global_object);
            if (vm.exception())
                return {};
        } else if (is<BooleanObject>(value_object)) {
            value = static_cast<BooleanObject&>(value_object).value_of();
        } else if (is<BigIntObject>(value_object)) {
            value = static_cast<BigIntObject&>(value_object).value_of();
        }
    }

    if (value.is_null())
        return "null";
    if (value.is_boolean())
        return value.as_bool() ? "true" : "false";
    if (value.is_string())
        return quote_json_string(value.as_string().string());
    if (value.is_number()) {
        if (value.is_finite_number())
            return value.to_string(global_object);
        return "null";
    }
    if (value.is_bigint()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::JsonBigInt);
        return {};
    }
    if (value.is_object() && !value.is_function()) {
        auto is_array = TRY_OR_DISCARD(value.is_array(global_object));
        if (is_array) {
            auto result = serialize_json_array(global_object, state, static_cast<Array&>(value.as_object()));
            if (vm.exception())
                return {};
            return result;
        }
        auto result = serialize_json_object(global_object, state, value.as_object());
        if (vm.exception())
            return {};
        return result;
    }
    return {};
}

// 25.5.2.4 SerializeJSONObject ( state, value ), https://tc39.es/ecma262/#sec-serializejsonobject
String JSONObject::serialize_json_object(GlobalObject& global_object, StringifyState& state, Object& object)
{
    auto& vm = global_object.vm();
    if (state.seen_objects.contains(&object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::JsonCircular);
        return {};
    }

    state.seen_objects.set(&object);
    String previous_indent = state.indent;
    state.indent = String::formatted("{}{}", state.indent, state.gap);
    Vector<String> property_strings;

    auto process_property = [&](const PropertyName& key) {
        if (key.is_symbol())
            return;
        auto serialized_property_string = serialize_json_property(global_object, state, key, &object);
        if (vm.exception())
            return;
        if (!serialized_property_string.is_null()) {
            property_strings.append(String::formatted(
                "{}:{}{}",
                quote_json_string(key.to_string()),
                state.gap.is_empty() ? "" : " ",
                serialized_property_string));
        }
    };

    if (state.property_list.has_value()) {
        auto property_list = state.property_list.value();
        for (auto& property : property_list) {
            process_property(property);
            if (vm.exception())
                return {};
        }
    } else {
        auto property_list = object.enumerable_own_property_names(PropertyKind::Key);
        if (vm.exception())
            return {};
        for (auto& property : property_list) {
            process_property(property.as_string().string());
            if (vm.exception())
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
            auto separator = String::formatted(",\n{}", state.indent);
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

// 25.5.2.5 SerializeJSONArray ( state, value ), https://tc39.es/ecma262/#sec-serializejsonarray
String JSONObject::serialize_json_array(GlobalObject& global_object, StringifyState& state, Object& object)
{
    auto& vm = global_object.vm();
    if (state.seen_objects.contains(&object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::JsonCircular);
        return {};
    }

    state.seen_objects.set(&object);
    String previous_indent = state.indent;
    state.indent = String::formatted("{}{}", state.indent, state.gap);
    Vector<String> property_strings;

    auto length = TRY_OR_DISCARD(length_of_array_like(global_object, object));

    // Optimization
    property_strings.ensure_capacity(length);

    for (size_t i = 0; i < length; ++i) {
        if (vm.exception())
            return {};
        auto serialized_property_string = serialize_json_property(global_object, state, i, &object);
        if (vm.exception())
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
            auto separator = String::formatted(",\n{}", state.indent);
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

// 25.5.2.2 QuoteJSONString ( value ), https://tc39.es/ecma262/#sec-quotejsonstring
String JSONObject::quote_json_string(String string)
{
    // FIXME: Handle UTF16
    StringBuilder builder;
    builder.append('"');
    auto utf_view = Utf8View(string);
    for (auto code_point : utf_view) {
        switch (code_point) {
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
            if (code_point < 0x20) {
                builder.appendff("\\u{:04x}", code_point);
            } else {
                builder.append_code_point(code_point);
            }
        }
    }
    builder.append('"');
    return builder.to_string();
}

// 25.5.1 JSON.parse ( text [ , reviver ] ), https://tc39.es/ecma262/#sec-json.parse
JS_DEFINE_NATIVE_FUNCTION(JSONObject::parse)
{
    auto string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto reviver = vm.argument(1);

    auto json = JsonValue::from_string(string);
    if (!json.has_value()) {
        vm.throw_exception<SyntaxError>(global_object, ErrorType::JsonMalformed);
        return {};
    }
    Value unfiltered = parse_json_value(global_object, json.value());
    if (reviver.is_function()) {
        auto* root = Object::create(global_object, global_object.object_prototype());
        auto root_name = String::empty();
        root->create_data_property_or_throw(root_name, unfiltered);
        auto result = internalize_json_property(global_object, root, root_name, reviver.as_function());
        if (vm.exception())
            return {};
        return result;
    }
    return unfiltered;
}

Value JSONObject::parse_json_value(GlobalObject& global_object, const JsonValue& value)
{
    if (value.is_object())
        return Value(parse_json_object(global_object, value.as_object()));
    if (value.is_array())
        return Value(parse_json_array(global_object, value.as_array()));
    if (value.is_null())
        return js_null();
    if (value.is_double())
        return Value(value.as_double());
    if (value.is_number())
        return Value(value.to_i32(0));
    if (value.is_string())
        return js_string(global_object.heap(), value.to_string());
    if (value.is_bool())
        return Value(static_cast<bool>(value.as_bool()));
    VERIFY_NOT_REACHED();
}

Object* JSONObject::parse_json_object(GlobalObject& global_object, const JsonObject& json_object)
{
    auto* object = Object::create(global_object, global_object.object_prototype());
    json_object.for_each_member([&](auto& key, auto& value) {
        object->define_direct_property(key, parse_json_value(global_object, value), JS::default_attributes);
    });
    return object;
}

Array* JSONObject::parse_json_array(GlobalObject& global_object, const JsonArray& json_array)
{
    auto* array = Array::create(global_object, 0);
    size_t index = 0;
    json_array.for_each([&](auto& value) {
        array->define_direct_property(index++, parse_json_value(global_object, value), JS::default_attributes);
    });
    return array;
}

// 25.5.1.1 InternalizeJSONProperty ( holder, name, reviver ), https://tc39.es/ecma262/#sec-internalizejsonproperty
Value JSONObject::internalize_json_property(GlobalObject& global_object, Object* holder, PropertyName const& name, FunctionObject& reviver)
{
    auto& vm = global_object.vm();
    auto value = holder->get(name);
    if (vm.exception())
        return {};
    if (value.is_object()) {
        auto is_array = TRY_OR_DISCARD(value.is_array(global_object));

        auto& value_object = value.as_object();
        auto process_property = [&](const PropertyName& key) -> ThrowCompletionOr<void> {
            auto element = internalize_json_property(global_object, &value_object, key, reviver);
            if (auto* exception = vm.exception())
                return throw_completion(exception->value());
            if (element.is_undefined()) {
                TRY(value_object.internal_delete(key));
            } else {
                value_object.create_data_property(key, element);
                if (auto* exception = vm.exception())
                    return throw_completion(exception->value());
            }
            return {};
        };

        if (is_array) {
            auto length = TRY_OR_DISCARD(length_of_array_like(global_object, value_object));
            for (size_t i = 0; i < length; ++i)
                TRY_OR_DISCARD(process_property(i));
        } else {
            auto property_list = value_object.enumerable_own_property_names(Object::PropertyKind::Key);
            if (vm.exception())
                return {};
            for (auto& property_name : property_list)
                TRY_OR_DISCARD(process_property(property_name.as_string().string()));
        }
    }

    return TRY_OR_DISCARD(vm.call(reviver, Value(holder), js_string(vm, name.to_string()), value));
}

}
