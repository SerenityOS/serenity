/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibXML/FundamentalTypes.h>

namespace XML {

struct Attribute {
    Name name;
    DeprecatedString value;
};

struct Node {
    struct Text {
        StringBuilder builder;
    };
    struct Comment {
        DeprecatedString text;
    };
    struct Element {
        Name name;
        HashMap<Name, DeprecatedString> attributes;
        Vector<NonnullOwnPtr<Node>> children;
    };

    bool operator==(Node const&) const;

    Variant<Text, Comment, Element> content;
    Node* parent { nullptr };

    bool is_text() const { return content.has<Text>(); }
    Text const& as_text() const { return content.get<Text>(); }

    bool is_comment() const { return content.has<Comment>(); }
    Comment const& as_comment() const { return content.get<Comment>(); }

    bool is_element() const { return content.has<Element>(); }
    Element const& as_element() const { return content.get<Element>(); }
};
}
