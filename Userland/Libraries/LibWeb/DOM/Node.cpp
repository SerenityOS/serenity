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
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/ProcessingInstruction.h>
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
    for_each_in_inclusive_subtree_of_type<Element>([&](auto& element) {
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

// https://dom.spec.whatwg.org/#concept-node-ensure-pre-insertion-validity
ExceptionOr<void> Node::ensure_pre_insertion_validity(NonnullRefPtr<Node> node, RefPtr<Node> child) const
{
    if (!is<Document>(this) && !is<DocumentFragment>(this) && !is<Element>(this))
        return DOM::HierarchyRequestError::create("Can only insert into a document, document fragment or element");

    if (node->is_host_including_inclusive_ancestor_of(*this))
        return DOM::HierarchyRequestError::create("New node is an ancestor of this node");

    if (child && child->parent() != this)
        return DOM::NotFoundError::create("This node is not the parent of the given child");

    // FIXME: All the following "Invalid node type for insertion" messages could be more descriptive.

    if (!is<DocumentFragment>(*node) && !is<DocumentType>(*node) && !is<Element>(*node) && !is<Text>(*node) && !is<Comment>(*node) && !is<ProcessingInstruction>(*node))
        return DOM::HierarchyRequestError::create("Invalid node type for insertion");

    if ((is<Text>(*node) && is<Document>(this)) || (is<DocumentType>(*node) && !is<Document>(this)))
        return DOM::HierarchyRequestError::create("Invalid node type for insertion");

    if (is<Document>(this)) {
        if (is<DocumentFragment>(*node)) {
            auto node_element_child_count = node->element_child_count();
            if ((node_element_child_count > 1 || node->has_child_of_type<Text>())
                || (node_element_child_count == 1 && (has_child_of_type<Element>() || is<DocumentType>(child.ptr()) /* FIXME: or child is non-null and a doctype is following child. */))) {
                return DOM::HierarchyRequestError::create("Invalid node type for insertion");
            }
        } else if (is<Element>(*node)) {
            if (has_child_of_type<Element>() || is<DocumentType>(child.ptr()) /* FIXME: or child is non-null and a doctype is following child. */)
                return DOM::HierarchyRequestError::create("Invalid node type for insertion");
        } else if (is<DocumentType>(*node)) {
            if (has_child_of_type<DocumentType>() /* FIXME: or child is non-null and an element is preceding child */ || (!child && has_child_of_type<Element>()))
                return DOM::HierarchyRequestError::create("Invalid node type for insertion");
        }
    }

    return {};
}

// https://dom.spec.whatwg.org/#concept-node-insert
void Node::insert_before(NonnullRefPtr<Node> node, RefPtr<Node> child, bool suppress_observers)
{
    NonnullRefPtrVector<Node> nodes;
    if (is<DocumentFragment>(*node))
        nodes = downcast<DocumentFragment>(*node).child_nodes();
    else
        nodes.append(node);

    auto count = nodes.size();
    if (count == 0)
        return;

    if (is<DocumentFragment>(*node)) {
        node->remove_all_children(true);
        // FIXME: Queue a tree mutation record for node with « », nodes, null, and null.
    }

    if (child) {
        // FIXME: For each live range whose start node is parent and start offset is greater than child’s index, increase its start offset by count.
        // FIXME: For each live range whose end node is parent and end offset is greater than child’s index, increase its end offset by count.
    }

    // FIXME: Let previousSibling be child’s previous sibling or parent’s last child if child is null. (Currently unused so not included)

    for (auto& node_to_insert : nodes) { // FIXME: In tree order
        document().adopt_node(node_to_insert);

        if (!child)
            TreeNode<Node>::append_child(node);
        else
            TreeNode<Node>::insert_before(node, child);

        // FIXME: If parent is a shadow host and node is a slottable, then assign a slot for node.
        // FIXME: If parent’s root is a shadow root, and parent is a slot whose assigned nodes is the empty list, then run signal a slot change for parent.
        // FIXME: Run assign slottables for a tree with node’s root.

        // FIXME: This should be shadow-including.
        node_to_insert.for_each_in_inclusive_subtree([&](Node& inclusive_descendant) {
            inclusive_descendant.inserted();
            if (inclusive_descendant.is_connected()) {
                // FIXME: If inclusiveDescendant is custom, then enqueue a custom element callback reaction with inclusiveDescendant,
                //        callback name "connectedCallback", and an empty argument list.

                // FIXME: Otherwise, try to upgrade inclusiveDescendant.
            }

            return IterationDecision::Continue;
        });
    }

    if (!suppress_observers) {
        // FIXME: queue a tree mutation record for parent with nodes, « », previousSibling, and child.
    }

    children_changed();
}

// https://dom.spec.whatwg.org/#concept-node-pre-insert
NonnullRefPtr<Node> Node::pre_insert(NonnullRefPtr<Node> node, RefPtr<Node> child)
{
    auto validity_result = ensure_pre_insertion_validity(node, child);
    if (validity_result.is_exception()) {
        dbgln("Node::pre_insert: ensure_pre_insertion_validity failed: {}. (FIXME: throw as exception, see issue #6075)", validity_result.exception().message());
        return node;
    }

    auto reference_child = child;
    if (reference_child == node)
        reference_child = node->next_sibling();

    insert_before(node, reference_child);
    return node;
}

// https://dom.spec.whatwg.org/#concept-node-pre-remove
NonnullRefPtr<Node> Node::pre_remove(NonnullRefPtr<Node> child)
{
    if (child->parent() != this) {
        dbgln("Node::pre_remove: Child doesn't belong to this node. (FIXME: throw NotFoundError DOMException, see issue #6075)");
        return child;
    }

    child->remove();

    return child;
}

// https://dom.spec.whatwg.org/#concept-node-append
NonnullRefPtr<Node> Node::append_child(NonnullRefPtr<Node> node)
{
    return pre_insert(node, nullptr);
}

// https://dom.spec.whatwg.org/#concept-node-remove
void Node::remove(bool suppress_observers)
{
    auto* parent = TreeNode<Node>::parent();
    VERIFY(parent);

    // FIXME: Let index be node’s index. (Currently unused so not included)

    // FIXME: For each live range whose start node is an inclusive descendant of node, set its start to (parent, index).
    // FIXME: For each live range whose end node is an inclusive descendant of node, set its end to (parent, index).
    // FIXME: For each live range whose start node is parent and start offset is greater than index, decrease its start offset by 1.
    // FIXME: For each live range whose end node is parent and end offset is greater than index, decrease its end offset by 1.

    // FIXME: For each NodeIterator object iterator whose root’s node document is node’s node document, run the NodeIterator pre-removing steps given node and iterator.

    // FIXME: Let oldPreviousSibling be node’s previous sibling. (Currently unused so not included)
    // FIXME: Let oldNextSibling be node’s next sibling. (Currently unused so not included)

    parent->remove_child(*this);

    // FIXME: If node is assigned, then run assign slottables for node’s assigned slot.

    // FIXME: If parent’s root is a shadow root, and parent is a slot whose assigned nodes is the empty list, then run signal a slot change for parent.

    // FIXME: If node has an inclusive descendant that is a slot, then:
    //          Run assign slottables for a tree with parent’s root.
    //          Run assign slottables for a tree with node.

    removed_from(parent);

    // FIXME: Let isParentConnected be parent’s connected. (Currently unused so not included)

    // FIXME: If node is custom and isParentConnected is true, then enqueue a custom element callback reaction with node,
    //        callback name "disconnectedCallback", and an empty argument list.

    // FIXME: This should be shadow-including.
    for_each_in_subtree([&](Node& descendant) {
        descendant.removed_from(nullptr);

        // FIXME: If descendant is custom and isParentConnected is true, then enqueue a custom element callback reaction with descendant,
        //        callback name "disconnectedCallback", and an empty argument list.

        return IterationDecision::Continue;
    });

    if (!suppress_observers) {
        // FIXME: queue a tree mutation record for parent with « », « node », oldPreviousSibling, and oldNextSibling.
    }

    parent->children_changed();
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
        for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
            //dbgln("{}", ancestor->node_name());
            ancestor->m_child_needs_style_update = true;
        }
        document().schedule_style_update();
    }
}

void Node::inserted()
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

void Node::remove_all_children(bool suppress_observers)
{
    while (RefPtr<Node> child = first_child())
        child->remove(suppress_observers);
}

// https://dom.spec.whatwg.org/#concept-tree-host-including-inclusive-ancestor
bool Node::is_host_including_inclusive_ancestor_of(const Node& other) const
{
    return is_inclusive_ancestor_of(other) || (is<DocumentFragment>(other.root()) && downcast<DocumentFragment>(other.root())->host() && is_inclusive_ancestor_of(*downcast<DocumentFragment>(other.root())->host().ptr()));
}

size_t Node::element_child_count() const
{
    size_t count = 0;
    for (auto* child = first_child(); child; child = child->next_sibling()) {
        if (is<Element>(child))
            ++count;
    }
    return count;
}

}
