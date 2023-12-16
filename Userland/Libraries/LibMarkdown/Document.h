/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <LibMarkdown/Block.h>
#include <LibMarkdown/ContainerBlock.h>

namespace Markdown {

class Document final {
public:
    Document(OwnPtr<ContainerBlock> container)
        : m_container(move(container))
    {
    }
    ByteString render_to_html(StringView extra_head_contents = ""sv) const;
    ByteString render_to_inline_html() const;
    ErrorOr<String> render_for_terminal(size_t view_width = 0) const;

    /*
     * Walk recursively through the document tree. Returning `RecursionDecision::Recurse` from
     * `Visitor::visit` proceeds with the next element of the pre-order walk, usually a child element.
     * Returning `RecursionDecision::Continue` skips the subtree, and usually proceeds with the next
     * sibling. Returning `RecursionDecision::Break` breaks the recursion, with no further calls to
     * any of the `Visitor::visit` methods.
     *
     * Note that `walk()` will only return `RecursionDecision::Continue` or `RecursionDecision::Break`.
     */
    RecursionDecision walk(Visitor&) const;

    static OwnPtr<Document> parse(StringView);

private:
    OwnPtr<ContainerBlock> m_container;
};

}
