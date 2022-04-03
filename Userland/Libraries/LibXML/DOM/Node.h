/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibXML/FundamentalTypes.h>

namespace XML {

struct Attribute {
    Name name;
    String value;
};

struct Node {
    struct Text {
        StringBuilder builder;
    };
    struct Comment {
        String text;
    };
    struct Element {
        Name name;
        HashMap<Name, String> attributes;
        NonnullOwnPtrVector<Node> children;
    };

    bool operator==(Node const&) const;

    Variant<Text, Comment, Element> content;
    Node* parent { nullptr };
};
}
