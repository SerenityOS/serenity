/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/StringBuilder.h>
#include <LibJS/AST.h>
#include <LibJS/Runtime/Function.h>
#include <LibWeb/Bindings/EventWrapper.h>
#include <LibWeb/Bindings/NodeWrapper.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::DOM {

Node::Node(Document& document, NodeType type)
    : EventTarget(static_cast<Bindings::ScriptExecutionContext&>(document))
    , m_document(&document)
    , m_type(type)
{
    if (!is_document())
        m_document->ref_from_node({});
}

Node::~Node()
{
    VERIFY(m_deletion_has_begun);
    if (layout_node() && layout_node()->parent())
        layout_node()->parent()->remove_child(*layout_node());

    if (!is_document())
        m_document->unref_from_node({});
}

const HTML::HTMLAnchorElement* Node::enclosing_link_element() const
{
    for (auto* node = this; node; node = node->parent()) {
        if (is<HTML::HTMLAnchorElement>(*node) && downcast<HTML::HTMLAnchorElement>(*node).has_attribute(HTML::AttributeNames::href))
            return downcast<HTML::HTMLAnchorElement>(node);
    }
    return nullptr;
}

const HTML::HTMLElement* Node::enclosing_html_element() const
{
    return first_ancestor_of_type<HTML::HTMLElement>();
}

const HTML::HTMLElement* Node::enclosing_html_element_with_attribute(const FlyString& attribute) const
{
    for (auto* node = this; node; node = node->parent()) {
        if (is<HTML::HTMLElement>(*node) && downcast<HTML::HTMLElement>(*node).has_attribute(attribute))
            return downcast<HTML::HTMLElement>(node);
    }
    return nullptr;
}

String Node::text_content() const
{
    StringBuilder builder;
    for (auto* child = first_child(); child; child = child->next_sibling()) {
        builder.append(child->text_content());
    }
    return builder.to_string();
}

void Node::set_text_content(const String& content)
{
    if (is_text()) {
        downcast<Text>(this)->set_data(content);
    } else {
        remove_all_children();
        append_child(document().create_text_node(content));
    }

    set_needs_style_update(true);
    document().invalidate_layout();
}

RefPtr<Layout::Node> Node::create_layout_node()
{
    return nullptr;
}

void Node::invalidate_style()
{
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        element.set_needs_style_update(true);
        return IterationDecision::Continue;
    });
    document().schedule_style_update();
}

bool Node::is_link() const
{
    return enclosing_link_element();
}

bool Node::dispatch_event(NonnullRefPtr<Event> event)
{
    return EventDispatcher::dispatch(*this, event);
}

String Node::child_text_content() const
{
    if (!is<ParentNode>(*this))
        return String::empty();

    StringBuilder builder;
    downcast<ParentNode>(*this).for_each_child([&](auto& child) {
        if (is<Text>(child))
            builder.append(downcast<Text>(child).text_content());
    });
    return builder.build();
}

Node* Node::root()
{
    Node* root = this;
    while (root->parent())
        root = root->parent();
    return root;
}

Node* Node::shadow_including_root()
{
    auto node_root = root();
    if (is<ShadowRoot>(node_root))
        return downcast<ShadowRoot>(node_root)->host()->shadow_including_root();
    return node_root;
}

bool Node::is_connected() const
{
    return shadow_including_root() && shadow_including_root()->is_document();
}

Element* Node::parent_element()
{
    if (!parent() || !is<Element>(parent()))
        return nullptr;
    return downcast<Element>(parent());
}

const Element* Node::parent_element() const
{
    if (!parent() || !is<Element>(parent()))
        return nullptr;
    return downcast<Element>(parent());
}

RefPtr<Node> Node::append_child(NonnullRefPtr<Node> node, bool notify)
{
    if (&node->document() != &document())
        document().adopt_node(node);
    TreeNode<Node>::append_child(node, notify);
    return node;
}

RefPtr<Node> Node::remove_child(NonnullRefPtr<Node> node)
{
    TreeNode<Node>::remove_child(node);
    return node;
}

RefPtr<Node> Node::insert_before(NonnullRefPtr<Node> node, RefPtr<Node> child, bool notify)
{
    if (!child)
        return append_child(move(node), notify);
    if (child->parent_node() != this) {
        dbgln("FIXME: Trying to insert_before() a bogus child");
        return nullptr;
    }
    if (&node->document() != &document())
        document().adopt_node(node);
    TreeNode<Node>::insert_before(node, child, notify);
    return node;
}

void Node::set_document(Badge<Document>, Document& document)
{
    if (m_document == &document)
        return;
    document.ref_from_node({});
    m_document->unref_from_node({});
    m_document = &document;
}

bool Node::is_editable() const
{
    return parent() && parent()->is_editable();
}

JS::Object* Node::create_wrapper(JS::GlobalObject& global_object)
{
    return wrap(global_object, *this);
}

void Node::removed_last_ref()
{
    if (is<Document>(*this)) {
        downcast<Document>(*this).removed_last_ref();
        return;
    }
    m_deletion_has_begun = true;
    delete this;
}

void Node::set_layout_node(Badge<Layout::Node>, Layout::Node* layout_node) const
{
    if (layout_node)
        m_layout_node = layout_node->make_weak_ptr();
    else
        m_layout_node = nullptr;
}

EventTarget* Node::get_parent(const Event&)
{
    // FIXME: returns the node’s assigned slot, if node is assigned, and node’s parent otherwise.
    return parent();
}

void Node::set_needs_style_update(bool value)
{
    if (m_needs_style_update == value)
        return;
    m_needs_style_update = value;

    if (m_needs_style_update) {
        for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent())
            ancestor->m_child_needs_style_update = true;
        document().schedule_style_update();
    }
}

void Node::inserted_into(Node&)
{
    set_needs_style_update(true);
}

ParentNode* Node::parent_or_shadow_host()
{
    if (is<ShadowRoot>(*this))
        return downcast<ShadowRoot>(*this).host();
    return downcast<ParentNode>(parent());
}

NonnullRefPtrVector<Node> Node::child_nodes() const
{
    NonnullRefPtrVector<Node> nodes;

    for_each_child([&](auto& child) {
        nodes.append(child);
    });

    return nodes;
}

}
