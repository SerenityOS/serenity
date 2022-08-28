/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Comment.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::DOM {

Comment::Comment(Document& document, String const& data)
    : CharacterData(document, NodeType::COMMENT_NODE, data)
{
}

// https://dom.spec.whatwg.org/#dom-comment-comment
JS::NonnullGCPtr<Comment> Comment::create_with_global_object(HTML::Window& window, String const& data)
{
    return *window.heap().allocate<Comment>(window.realm(), window.associated_document(), data);
}

}
