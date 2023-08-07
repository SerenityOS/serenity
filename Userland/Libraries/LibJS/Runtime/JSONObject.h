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
    virtual void initialize(Realm&) override;
    virtual ~JSONObject() override = default;

    // The base implementation of stringify is exposed because it is used by
    // test-js to communicate between the JS tests and the C++ test runner.
    static ThrowCompletionOr<DeprecatedString> stringify_impl(VM&, Value value, Value replacer, Value space);

    static Value parse_json_value(VM&, JsonValue const&);

private:
    explicit JSONObject(Realm&);

    struct StringifyState {
        GCPtr<FunctionObject> replacer_function;
        HashTable<GCPtr<Object>> seen_objects;
        DeprecatedString indent { DeprecatedString::empty() };
        DeprecatedString gap;
        Optional<Vector<DeprecatedString>> property_list;
    };

    // Stringify helpers
    static ThrowCompletionOr<DeprecatedString> serialize_json_property(VM&, StringifyState&, PropertyKey const& key, Object* holder);
    static ThrowCompletionOr<DeprecatedString> serialize_json_object(VM&, StringifyState&, Object&);
    static ThrowCompletionOr<DeprecatedString> serialize_json_array(VM&, StringifyState&, Object&);
    static DeprecatedString quote_json_string(DeprecatedString);

    // Parse helpers
    static Object* parse_json_object(VM&, JsonObject const&);
    static Array* parse_json_array(VM&, JsonArray const&);
    static ThrowCompletionOr<Value> internalize_json_property(VM&, Object* holder, PropertyKey const& name, FunctionObject& reviver);

    JS_DECLARE_NATIVE_FUNCTION(stringify);
    JS_DECLARE_NATIVE_FUNCTION(parse);
};

}
