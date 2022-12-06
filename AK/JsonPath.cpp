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

JsonValue JsonPath::resolve(JsonValue const& top_root) const
{
    auto root = top_root;
    for (auto const& element : *this) {
        switch (element.kind()) {
        case JsonPathElement::Kind::Key:
            root = JsonValue { root.as_object().get(element.key()) };
            break;
        case JsonPathElement::Kind::Index:
            root = JsonValue { root.as_array().at(element.index()) };
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }
    return root;
}

DeprecatedString JsonPath::to_deprecated_string() const
{
    StringBuilder builder;
    builder.append("{ ."sv);
    for (auto const& el : *this) {
        builder.append("sv > "sv);
        builder.append(el.to_deprecated_string());
    }
    builder.append("sv }"sv);
    return builder.to_deprecated_string();
}

}
