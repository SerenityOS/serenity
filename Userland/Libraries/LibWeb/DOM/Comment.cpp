/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Comment.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::DOM {

Comment::Comment(Document& document, DeprecatedString const& data)
    : CharacterData(document, NodeType::COMMENT_NODE, data)
{
}

// https://dom.spec.whatwg.org/#dom-comment-comment
JS::NonnullGCPtr<Comment> Comment::construct_impl(JS::Realm& realm, DeprecatedString const& data)
{
    auto& window = verify_cast<HTML::Window>(realm.global_object());
    return realm.heap().allocate<Comment>(realm, window.associated_document(), data).release_allocated_value_but_fixme_should_propagate_errors();
}

}
