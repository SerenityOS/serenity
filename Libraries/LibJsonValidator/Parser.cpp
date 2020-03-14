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

namespace JsonValidator {

Parser::Parser()
{
}

Parser::~Parser()
{
}

JsonValue Parser::run(const int /*fd*/)
{
    ASSERT_NOT_REACHED();
    return JsonValue();
}

JsonValue Parser::run(const String& /*content*/)
{
    ASSERT_NOT_REACHED();
    return JsonValue();
}

JsonValue Parser::run(const JsonValue& json)
{
    if (!json.is_object()) {
        add_parser_error("root json instance not of type object");
    }

    auto json_object = json.as_object();

    if (!json_object.has("$schema")) {
        add_parser_error("no json schema provided");
    }

    // FIXME: Here, we should load the file given in $schema, and check the $id in the root. This will provide the actual schema version used, that could be located anywhere.
    if (json_object.get("$schema").as_string_or("") != "https://json-schema.org/draft/2019-09/schema") {
        add_parser_error("unknown json schema provided, currently, only \"https://json-schema.org/draft/2019-09/schema\" is allowed for $schema.");
    }

    m_root_node = get_typed_node(json_object);

    if (m_parser_errors.size()) {
        JsonArray vals;
        for (auto& e : m_parser_errors) {
            vals.append(e);
        }
        return vals;
    }
    return JsonValue(true);
}

void Parser::add_parser_error(String error)
{
    m_parser_errors.append(error);
}

JsonSchemaNode* Parser::get_typed_node(const JsonObject& json_object)
{
    JsonSchemaNode* node { nullptr };
    JsonValue id = json_object.get("$id");
    JsonValue type = json_object.get("type");

    if (type.is_string()) {
        if (type.as_string() == "object") {
            auto properties = json_object.get_or("properties", JsonObject());
            if (!properties.is_object()) {
                add_parser_error("properties element is not of type 'object'");
            }

            node = new ObjectNode(id.as_string_or(""));

            properties.as_object().for_each_member([&](auto& key, auto& json_value) {
                if (!json_value.is_object()) {
                    add_parser_error("properties element is not of type object");
                }
                JsonSchemaNode* child_node = get_typed_node(json_value.as_object());
                if (child_node)
                    static_cast<ObjectNode*>(node)->append_property(key, child_node);
            });
        } else if (type.as_string() == "array") {
            if (!json_object.has("items")) {
                add_parser_error("no items in array type!");
            }

            auto items = json_object.get_or("items", JsonObject());
            if (!items.is_object()) {
                add_parser_error("items element is not of type 'object'");
            }

            node = new ArrayNode(id.as_string_or(""));
            JsonObject items_object = items.as_object();
            JsonSchemaNode* child_node = get_typed_node(items_object);

            if (child_node)
                static_cast<ArrayNode*>(node)->set_items(move(child_node));

        } else if (type.as_string() == "string") {
            node = new StringNode(id.as_string_or(""));
        } else {
            add_parser_error("type not supported!");
        }

    } else if (type.is_array()) {
        add_parser_error("multiple types for element not supported.");
    }

    return node;
}

}
