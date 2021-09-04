/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Comment.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::DOM {

Comment::Comment(Document& document, String const& data)
    : CharacterData(document, NodeType::COMMENT_NODE, data)
{
}

Comment::~Comment()
{
}

}
