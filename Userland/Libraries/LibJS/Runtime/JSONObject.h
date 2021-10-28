/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class JSONObject final : public Object {
    JS_OBJECT(JSONObject, Object);

public:
    explicit JSONObject(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~JSONObject() override;

    // The base implementation of stringify is exposed because it is used by
    // test-js to communicate between the JS tests and the C++ test runner.
    static ThrowCompletionOr<String> stringify_impl(GlobalObject&, Value value, Value replacer, Value space);

    static Value parse_json_value(GlobalObject&, const JsonValue&);

private:
    struct StringifyState {
        FunctionObject* replacer_function { nullptr };
        HashTable<Object*> seen_objects;
        String indent { String::empty() };
        String gap;
        Optional<Vector<String>> property_list;
    };

    // Stringify helpers
    static ThrowCompletionOr<String> serialize_json_property(GlobalObject&, StringifyState&, const PropertyKey& key, Object* holder);
    static ThrowCompletionOr<String> serialize_json_object(GlobalObject&, StringifyState&, Object&);
    static ThrowCompletionOr<String> serialize_json_array(GlobalObject&, StringifyState&, Object&);
    static String quote_json_string(String);

    // Parse helpers
    static Object* parse_json_object(GlobalObject&, const JsonObject&);
    static Array* parse_json_array(GlobalObject&, const JsonArray&);
    static ThrowCompletionOr<Value> internalize_json_property(GlobalObject&, Object* holder, PropertyKey const& name, FunctionObject& reviver);

    JS_DECLARE_NATIVE_FUNCTION(stringify);
    JS_DECLARE_NATIVE_FUNCTION(parse);
};

}
