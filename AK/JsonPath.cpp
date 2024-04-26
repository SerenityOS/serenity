/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/JsonPath.h>
#include <AK/JsonValue.h>

namespace AK {

JsonPathElement JsonPathElement::any_array_element { Kind::AnyIndex };
JsonPathElement JsonPathElement::any_object_element { Kind::AnyKey };

ErrorOr<JsonValue> JsonPath::try_resolve(JsonValue const& top_root) const
{
    auto root = top_root;
    for (auto const& element : *this) {
        switch (element.kind()) {
        case JsonPathElement::Kind::Key: {
            if (!root.is_object())
                return Error::from_string_literal("Element is not an object");
            auto& object = root.as_object();
            auto entry = object.get(element.key());
            if (!entry.has_value())
                return Error::from_string_literal("Element not found");
            root = JsonValue { entry.value() };
            break;
        }
        case JsonPathElement::Kind::Index: {
            if (!root.is_array())
                return Error::from_string_literal("Element is not an array");
            auto& array = root.as_array();
            if (element.index() >= array.size())
                return Error::from_string_literal("Element not found");
            root = JsonValue { array.at(element.index()) };
            break;
        }
        default:
            VERIFY_NOT_REACHED();
        }
    }
    return root;
}

ByteString JsonPath::to_byte_string() const
{
    StringBuilder builder;
    builder.append("{ ."sv);
    for (auto const& el : *this) {
        builder.append("sv > "sv);
        builder.append(el.to_byte_string());
    }
    builder.append("sv }"sv);
    return builder.to_byte_string();
}

}
