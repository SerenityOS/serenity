/*
 * Copyright (c) 2020, The SerenityOS developers.
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
#include <AK/JsonPath.h>
#include <AK/JsonValue.h>

namespace AK {

JsonPathElement JsonPathElement::any_array_element { Kind::AnyIndex };
JsonPathElement JsonPathElement::any_object_element { Kind::AnyKey };

JsonValue JsonPath::resolve(const JsonValue& top_root) const
{
    auto root = top_root;
    for (auto& element : *this) {
        switch (element.kind()) {
        case JsonPathElement::Kind::Key:
            root = JsonValue { root.as_object().get(element.key()) };
            break;
        case JsonPathElement::Kind::Index:
            root = JsonValue { root.as_array().at(element.index()) };
            break;
        default:
            ASSERT_NOT_REACHED();
        }
    }
    return root;
}

String JsonPath::to_string() const
{
    StringBuilder builder;
    builder.append("{ .");
    for (auto& el : *this) {
        builder.append(" > ");
        builder.append(el.to_string());
    }
    builder.append(" }");
    return builder.to_string();
}

}
