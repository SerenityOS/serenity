/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <LibWeb/DOM/Node.h>

namespace Web::DOM {

class ParentNode : public Node {
public:
    template<typename F>
    void for_each_child(F) const;
    template<typename F>
    void for_each_child(F);

    RefPtr<Element> first_element_child();
    RefPtr<Element> last_element_child();
    u32 child_element_count() const;

    ExceptionOr<RefPtr<Element>> query_selector(StringView);
    ExceptionOr<NonnullRefPtr<NodeList>> query_selector_all(StringView);

    NonnullRefPtr<HTMLCollection> children();

    NonnullRefPtr<HTMLCollection> get_elements_by_tag_name(FlyString const&);
    NonnullRefPtr<HTMLCollection> get_elements_by_tag_name_ns(FlyString const&, FlyString const&);

    ExceptionOr<void> prepend(Vector<Variant<NonnullRefPtr<Node>, String>> const& nodes);
    ExceptionOr<void> append(Vector<Variant<NonnullRefPtr<Node>, String>> const& nodes);
    ExceptionOr<void> replace_children(Vector<Variant<NonnullRefPtr<Node>, String>> const& nodes);

protected:
    ParentNode(Document& document, NodeType type)
        : Node(document, type)
    {
    }
};

template<typename Callback>
inline void ParentNode::for_each_child(Callback callback) const
{
    for (auto* node = first_child(); node; node = node->next_sibling())
        callback(*node);
}

template<typename Callback>
inline void ParentNode::for_each_child(Callback callback)
{
    for (auto* node = first_child(); node; node = node->next_sibling())
        callback(*node);
}

}
