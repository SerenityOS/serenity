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
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/ScriptFunction.h>
#include <LibWeb/Bindings/EventWrapper.h>
#include <LibWeb/Bindings/EventWrapperFactory.h>
#include <LibWeb/Bindings/NodeWrapper.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/Layout/LayoutBlock.h>
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/Layout/LayoutInline.h>
#include <LibWeb/Layout/LayoutNode.h>
#include <LibWeb/Layout/LayoutText.h>

//#define EVENT_DEBUG

namespace Web {

Node::Node(Document& document, NodeType type)
    : m_document(&document)
    , m_type(type)
{
}

Node::~Node()
{
    if (layout_node() && layout_node()->parent())
        layout_node()->parent()->remove_child(*layout_node());
}

const HTMLAnchorElement* Node::enclosing_link_element() const
{
    for (auto* node = this; node; node = node->parent()) {
        if (is<HTMLAnchorElement>(*node) && downcast<HTMLAnchorElement>(*node).has_attribute(HTML::AttributeNames::href))
            return downcast<HTMLAnchorElement>(node);
    }
    return nullptr;
}

const HTMLElement* Node::enclosing_html_element() const
{
    return first_ancestor_of_type<HTMLElement>();
}

String Node::text_content() const
{
    Vector<String> strings;
    StringBuilder builder;
    for (auto* child = first_child(); child; child = child->next_sibling()) {
        auto text = child->text_content();
        if (!text.is_empty()) {
            builder.append(child->text_content());
            builder.append(' ');
        }
    }
    if (builder.length() > 1)
        builder.trim(1);
    return builder.to_string();
}

const Element* Node::next_element_sibling() const
{
    for (auto* node = next_sibling(); node; node = node->next_sibling()) {
        if (node->is_element())
            return static_cast<const Element*>(node);
    }
    return nullptr;
}

const Element* Node::previous_element_sibling() const
{
    for (auto* node = previous_sibling(); node; node = node->previous_sibling()) {
        if (node->is_element())
            return static_cast<const Element*>(node);
    }
    return nullptr;
}

RefPtr<LayoutNode> Node::create_layout_node(const StyleProperties*)
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

void Node::dispatch_event(NonnullRefPtr<Event> event)
{
    for (auto& listener : listeners()) {
        if (listener.event_name == event->type()) {
            auto& function = const_cast<EventListener&>(*listener.listener).function();
#ifdef EVENT_DEBUG
            static_cast<const JS::ScriptFunction&>(function).body().dump(0);
#endif
            auto& global_object = function.global_object();
            auto* this_value = wrap(global_object, *this);
#ifdef EVENT_DEBUG
            dbg() << "calling event listener with this=" << this_value;
#endif
            auto* event_wrapper = wrap(global_object, *event);
            JS::MarkedValueList arguments(global_object.heap());
            arguments.append(event_wrapper);
            document().interpreter().call(function, this_value, move(arguments));
        }
    }

    // FIXME: This is a hack. We should follow the real rules of event bubbling.
    if (parent())
        parent()->dispatch_event(move(event));
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

const Node* Node::root() const
{
    const Node* root = this;
    while (root->parent())
        root = root->parent();
    return root;
}

bool Node::is_connected() const
{
    return root() && root()->is_document();
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
    TreeNode<Node>::append_child(node, notify);
    return node;
}

RefPtr<Node> Node::insert_before(NonnullRefPtr<Node> node, RefPtr<Node> child, bool notify)
{
    if (!child)
        return append_child(move(node), notify);
    if (child->parent_node() != this) {
        dbg() << "FIXME: Trying to insert_before() a bogus child";
        return nullptr;
    }
    TreeNode<Node>::insert_before(node, child, notify);
    return node;
}

void Node::set_document(Badge<Document>, Document& document)
{
    m_document = &document;
}

}
