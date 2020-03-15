/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibJsonValidator/JsonSchemaNode.h>
#include <LibJsonValidator/Parser.h>
#include <stdio.h>

namespace JsonValidator {

static void print_indent(int indent)
{
    for (int i = 0; i < indent * 2; ++i)
        putchar(' ');
}

String to_string(InstanceType type)
{
    switch (type) {
    case InstanceType::Object:
        return "object";
    case InstanceType::Array:
        return "array";
    case InstanceType::String:
        return "string";
    case InstanceType::Number:
        return "number";
    case InstanceType::Boolean:
        return "boolean";
    }
    return "";
}

void JsonSchemaNode::dump(int indent, String additional) const
{
    print_indent(indent);
    printf("%s (%s%s%s)\n", m_id.characters(), class_name(), m_required ? " *" : "", additional.characters());
}

void ObjectNode::dump(int indent, String = "") const
{
    JsonSchemaNode::dump(indent);
    for (auto& property : properties()) {
        print_indent(indent + 1);
        printf("%s:\n", property.key.characters());
        property.value->dump(indent + 1);
    }
}

void ArrayNode::dump(int indent, String = "") const
{
    JsonSchemaNode::dump(indent, m_unique_items ? " with unique_items" : "");

    if (m_items.size())
        for (auto* item : m_items)
            item->dump(indent + 1);
}

void StringNode::dump(int indent, String = "") const
{
    JsonSchemaNode::dump(indent);
}

void NumberNode::dump(int indent, String = "") const
{
    JsonSchemaNode::dump(indent);
}

void BooleanNode::dump(int indent, String = "") const
{
    JsonSchemaNode::dump(indent);
}

bool validate_type(InstanceType type, const JsonValue& json)
{
    if (type == InstanceType::Array && json.is_array())
        return true;

    else if (type == InstanceType::Object && json.is_object())
        return true;

    else if (type == InstanceType::String && json.is_string())
        return true;

    else if (type == InstanceType::Number && json.is_number())
        return true;

    else if (type == InstanceType::Boolean)
        // boolean type matches always! validation checks for true/false of boolean value.
        return true;

    return false;
}

JsonValue JsonSchemaNode::validate(const JsonValue& json) const
{
#ifdef JSON_SCHEMA_DEBUG
    printf("Validating node: %s (%s)\n", m_id.characters(), class_name());
#endif

    // check if type is matching
    if (!validate_type(m_type, json)) {
        StringBuilder b;
        b.appendf("type validation failed: %s checking for: %s", json.to_string().characters(), to_string(m_type).characters());
        return JsonValue(b.build());
    }

    // check if required is matching
    if (m_required && json.is_null())
        return JsonValue("item is required, but is not persistent");

    return JsonValue(true);
}

JsonValue StringNode::validate(const JsonValue& json) const
{
    JsonValue res = JsonSchemaNode::validate(json);

    // FIXME: Implement checks for strings

    return res;
}

JsonValue NumberNode::validate(const JsonValue& json) const
{
    JsonValue res = JsonSchemaNode::validate(json);

    // FIXME: Implement checks for numbers

    return res;
}

JsonValue BooleanNode::validate(const JsonValue& json) const
{
    JsonValue res = JsonSchemaNode::validate(json);

    if (!res.is_bool())
        return res;

    return JsonValue((res.as_bool() & m_value) == true);
}

JsonValue merge_results(JsonArray values)
{

    JsonArray result;
    bool boolean_result { true };

    for (auto& item : values.values()) {
        if (item.is_bool())
            boolean_result &= item.as_bool();
        else
            result.append(item);
    }

    if (result.values().size()) {
        if (result.values().size() == 1)
            return JsonValue(result.at(0));
        else
            return result;
    }
    return JsonValue(boolean_result);
}

JsonValue ObjectNode::validate(const JsonValue& json) const
{
    JsonArray res;
    res.append(JsonSchemaNode::validate(json));

    if (!json.is_object()) {
        StringBuilder b;
        b.appendf("json is not object: %s", json.to_string().characters());
        return JsonValue(b.build());
    }

#ifdef JSON_SCHEMA_DEBUG
    printf("Validating %lu properties.\n", m_properties.size());
#endif

    Vector<String> json_property_keys;
    size_t members_count = 0;
    json.as_object().for_each_member([&](auto& key, auto&) {
        json_property_keys.append(key);
        ++members_count;
    });

    for (auto& property : m_properties) {
        if (property.value->identified_by_pattern()) {
            // get all property keys that match the pattern
            Vector<String> matched;
            for (auto& key : json_property_keys)
                if (property.value->match_against_pattern(key))
                    matched.append(key);

            Optional<size_t> n;
            for (auto& match : matched)
                if ((n = json_property_keys.find_first_index(match)).has_value())
                    json_property_keys.remove(n.value());

            StringBuilder keys_builder;
            keys_builder.join(matched, ", ");
#ifdef JSON_SCHEMA_DEBUG
            printf("%lu/%lu key(s) matched the pattern: %s\n", matched.size(), members_count, keys_builder.build().characters());
#endif
            if (matched.size()) {
                for (auto& match : matched) {
                    res.append(property.value->validate(json.as_object().get(match)));
                }
            }

        } else if (json.as_object().has(property.key)) {
#ifdef JSON_SCHEMA_DEBUG
            printf("Validating property %s.\n", property.key.characters());
#endif
            res.append(property.value->validate(json.as_object().get(property.key)));

            auto index = json_property_keys.find_first_index(property.key);
            if (index.has_value())
                json_property_keys.remove(index.value());
#ifdef JSON_SCHEMA_DEBUG
            else
                printf("error: could not remove key from vector\n");
#endif
        } else if (property.value->required()) {
            StringBuilder b;
            b.appendf("required value %s not found at %s", property.key.characters(), property.value->id().characters());
            return JsonValue(b.build());
        }
    }

    // when no additional properties allowed, json_property_keys must be empty
    if (!m_additional_properties && json_property_keys.size()) {
        StringBuilder props_builder;
        props_builder.append("found additional properties \"");
        props_builder.join(json_property_keys, ", ");
        props_builder.append("\", but not allowed due to additionalProperties");
        return JsonValue(props_builder.build());
    }

    return merge_results(res);
}

JsonValue ArrayNode::validate(const JsonValue& json) const
{
    JsonArray res;
    res.append(JsonSchemaNode::validate(json));

    if (!json.is_array())
        return true; // ignore non array types...

    auto& values = json.as_array().values();

    // validate min and max items
    if (values.size() < m_min_items)
        return JsonValue("minItems violation");
    if (m_max_items.has_value() && values.size() > m_max_items.value())
        return JsonValue("maxItems violation");

    // validate each json array element against the items spec
    HashMap<u32, bool> hashes;
    JsonSchemaNode* item { nullptr };
    for (size_t i = 0; i < values.size(); ++i) {
        auto& value = values[i];

        // check for duplicate hash of array item
        if (m_unique_items) {
            auto hash = value.to_string().impl()->hash();
            if (hashes.get(hash).has_value())
                return JsonValue("duplicate item found, but not allowed due to uniqueItems");
            else
                hashes.set(hash, true);
        }

        if (m_items_is_array) {
            if (m_items.size() > i) {
                auto& item = m_items.at(i);
                res.append(item->validate(value));
            } else {
                if (m_additional_items)
                    res.append(m_additional_items->validate(value));
            }
        } else {
            if (m_items.size()) {
                item = m_items.at(0);
                res.append(item->validate(value));
            }
        }
    }

    return merge_results(res);
}

}
