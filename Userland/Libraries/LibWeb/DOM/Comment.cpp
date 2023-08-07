/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CommentPrototype.h>
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::DOM {

Comment::Comment(Document& document, DeprecatedString const& data)
    : CharacterData(document, NodeType::COMMENT_NODE, data)
{
}

// https://dom.spec.whatwg.org/#dom-comment-comment
WebIDL::ExceptionOr<JS::NonnullGCPtr<Comment>> Comment::construct_impl(JS::Realm& realm, DeprecatedString const& data)
{
    auto& window = verify_cast<HTML::Window>(realm.global_object());
    return MUST_OR_THROW_OOM(realm.heap().allocate<Comment>(realm, window.associated_document(), data));
}

void Comment::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CommentPrototype>(realm, "Comment"));
}

}
