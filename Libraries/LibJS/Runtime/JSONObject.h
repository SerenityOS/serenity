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
    static String stringify_impl(GlobalObject&, Value value, Value replacer, Value space);

private:
    struct StringifyState {
        Function* replacer_function { nullptr };
        HashTable<Object*> seen_objects;
        String indent { String::empty() };
        String gap;
        Optional<Vector<String>> property_list;
    };

    // Stringify helpers
    static String serialize_json_property(GlobalObject&, StringifyState&, const PropertyName& key, Object* holder);
    static String serialize_json_object(GlobalObject&, StringifyState&, Object&);
    static String serialize_json_array(GlobalObject&, StringifyState&, Object&);
    static String quote_json_string(String);

    // Parse helpers
    static Object* parse_json_object(GlobalObject&, const JsonObject&);
    static Array* parse_json_array(GlobalObject&, const JsonArray&);
    static Value parse_json_value(GlobalObject&, const JsonValue&);
    static Value internalize_json_property(GlobalObject&, Object* holder, const PropertyName& name, Function& reviver);

    JS_DECLARE_NATIVE_FUNCTION(stringify);
    JS_DECLARE_NATIVE_FUNCTION(parse);
};

}
