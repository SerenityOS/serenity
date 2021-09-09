/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::DOM {

Comment::Comment(Document& document, const String& data)
    : CharacterData(document, NodeType::COMMENT_NODE, data)
{
}

Comment::~Comment()
{
}

// https://dom.spec.whatwg.org/#dom-comment-comment
NonnullRefPtr<Comment> Comment::create_with_global_object(Bindings::WindowObject& window, String const& data)
{
    return make_ref_counted<Comment>(window.impl().associated_document(), data);
}

}
