/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IDAllocator.h>
#include <AK/StringBuilder.h>
#include <LibJS/AST.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Bindings/NodePrototype.h>
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/IDLEventListener.h>
#include <LibWeb/DOM/LiveNodeList.h>
#include <LibWeb/DOM/MutationType.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/NodeIterator.h>
#include <LibWeb/DOM/ProcessingInstruction.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/StaticNodeList.h>
#include <LibWeb/HTML/BrowsingContextContainer.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::DOM {

static IDAllocator s_node_id_allocator;
static HashMap<i32, Node*> s_node_directory;

static i32 allocate_node_id(Node* node)
{
    i32 id = s_node_id_allocator.allocate();
    s_node_directory.set(id, node);
    return id;
}

static void deallocate_node_id(i32 node_id)
{
    if (!s_node_directory.remove(node_id))
        VERIFY_NOT_REACHED();
    s_node_id_allocator.deallocate(node_id);
}

Node* Node::from_id(i32 node_id)
{
    return s_node_directory.get(node_id).value_or(nullptr);
}

Node::Node(JS::Realm& realm, Document& document, NodeType type)
    : EventTarget(realm)
    , m_document(&document)
    , m_type(type)
    , m_id(allocate_node_id(this))
{
}

Node::Node(Document& document, NodeType type)
    : Node(document.realm(), document, type)
{
}

Node::~Node()
{
    if (layout_node() && layout_node()->parent())
        layout_node()->parent()->remove_child(*layout_node());

    deallocate_node_id(m_id);
}

void Node::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_document.ptr());
    visitor.visit(m_parent.ptr());
    visitor.visit(m_first_child.ptr());
    visitor.visit(m_last_child.ptr());
    visitor.visit(m_next_sibling.ptr());
    visitor.visit(m_previous_sibling.ptr());
}

// https://dom.spec.whatwg.org/#dom-node-baseuri
String Node::base_uri() const
{
    // Return this’s node document’s document base URL, serialized.
    return document().base_url().to_string();
}

const HTML::HTMLAnchorElement* Node::enclosing_link_element() const
{
    for (auto* node = this; node; node = node->parent()) {
        if (!is<HTML::HTMLAnchorElement>(*node))
            continue;
        auto const& anchor_element = static_cast<HTML::HTMLAnchorElement const&>(*node);
        if (anchor_element.has_attribute(HTML::AttributeNames::href))
            return &anchor_element;
    }
    return nullptr;
}

const HTML::HTMLElement* Node::enclosing_html_element() const
{
    return first_ancestor_of_type<HTML::HTMLElement>();
}

const HTML::HTMLElement* Node::enclosing_html_element_with_attribute(FlyString const& attribute) const
{
    for (auto* node = this; node; node = node->parent()) {
        if (is<HTML::HTMLElement>(*node) && verify_cast<HTML::HTMLElement>(*node).has_attribute(attribute))
            return verify_cast<HTML::HTMLElement>(node);
    }
    return nullptr;
}

// https://dom.spec.whatwg.org/#concept-descendant-text-content
String Node::descendant_text_content() const
{
    StringBuilder builder;
    for_each_in_subtree_of_type<Text>([&](auto& text_node) {
        builder.append(text_node.data());
        return IterationDecision::Continue;
    });
    return builder.to_string();
}

// https://dom.spec.whatwg.org/#dom-node-textcontent
String Node::text_content() const
{
    // The textContent getter steps are to return the following, switching on the interface this implements:
    // If DocumentFragment or Element, return the descendant text content of this.
    if (is<DocumentFragment>(this) || is<Element>(this))
        return descendant_text_content();
    else if (is<CharacterData>(this))
        // If CharacterData, return this’s data.
        return verify_cast<CharacterData>(this)->data();

    // FIXME: If this is an Attr node, return this's value.

    // Otherwise, return null
    return {};
}

// https://dom.spec.whatwg.org/#ref-for-dom-node-textcontent%E2%91%A0
void Node::set_text_content(String const& content)
{
    // The textContent setter steps are to, if the given value is null, act as if it was the empty string instead,
    // and then do as described below, switching on the interface this implements:

    // If DocumentFragment or Element, string replace all with the given value within this.
    if (is<DocumentFragment>(this) || is<Element>(this)) {
        string_replace_all(content);
    } else if (is<CharacterData>(this)) {
        // If CharacterData, replace data with node this, offset 0, count this’s length, and data the given value.
        auto* character_data_node = verify_cast<CharacterData>(this);
        character_data_node->set_data(content);

        // FIXME: CharacterData::set_data is not spec compliant. Make this match the spec when set_data becomes spec compliant.
        //        Do note that this will make this function able to throw an exception.
    } else {
        // FIXME: If this is an Attr node, set an existing attribute value with this and the given value.
        return;
    }

    // Otherwise, do nothing.

    set_needs_style_update(true);
}

// https://dom.spec.whatwg.org/#dom-node-nodevalue
String Node::node_value() const
{
    // The nodeValue getter steps are to return the following, switching on the interface this implements:

    // If Attr, return this’s value.
    if (is<Attribute>(this)) {
        return verify_cast<Attribute>(this)->value();
    }

    // If CharacterData, return this’s data.
    if (is<CharacterData>(this)) {
        return verify_cast<CharacterData>(this)->data();
    }

    // Otherwise, return null.
    return {};
}

// https://dom.spec.whatwg.org/#ref-for-dom-node-nodevalue%E2%91%A0
void Node::set_node_value(String const& value)
{
    // The nodeValue setter steps are to, if the given value is null, act as if it was the empty string instead,
    // and then do as described below, switching on the interface this implements:

    // If Attr, set an existing attribute value with this and the given value.
    if (is<Attribute>(this)) {
        verify_cast<Attribute>(this)->set_value(value);
    } else if (is<CharacterData>(this)) {
        // If CharacterData, replace data with node this, offset 0, count this’s length, and data the given value.
        verify_cast<CharacterData>(this)->set_data(value);
    }

    // Otherwise, do nothing.
}

void Node::invalidate_style()
{
    if (is_document()) {
        auto& document = static_cast<DOM::Document&>(*this);
        document.set_needs_full_style_update(true);
        document.schedule_style_update();
        return;
    }

    for_each_in_inclusive_subtree([&](Node& node) {
        node.m_needs_style_update = true;
        if (node.has_children())
            node.m_child_needs_style_update = true;
        if (auto* shadow_root = node.is_element() ? static_cast<DOM::Element&>(node).shadow_root() : nullptr) {
            node.m_child_needs_style_update = true;
            shadow_root->m_needs_style_update = true;
            if (shadow_root->has_children())
                shadow_root->m_child_needs_style_update = true;
        }
        return IterationDecision::Continue;
    });
    for (auto* ancestor = parent_or_shadow_host(); ancestor; ancestor = ancestor->parent_or_shadow_host())
        ancestor->m_child_needs_style_update = true;
    document().schedule_style_update();
}

bool Node::is_link() const
{
    return enclosing_link_element();
}

String Node::child_text_content() const
{
    if (!is<ParentNode>(*this))
        return String::empty();

    StringBuilder builder;
    verify_cast<ParentNode>(*this).for_each_child([&](auto& child) {
        if (is<Text>(child))
            builder.append(verify_cast<Text>(child).text_content());
    });
    return builder.build();
}

// https://dom.spec.whatwg.org/#concept-tree-root
Node& Node::root()
{
    // The root of an object is itself, if its parent is null, or else it is the root of its parent.
    // The root of a tree is any object participating in that tree whose parent is null.
    Node* root = this;
    while (root->parent())
        root = root->parent();
    return *root;
}

// https://dom.spec.whatwg.org/#concept-shadow-including-root
Node& Node::shadow_including_root()
{
    // The shadow-including root of an object is its root’s host’s shadow-including root,
    // if the object’s root is a shadow root; otherwise its root.
    auto& node_root = root();
    if (is<ShadowRoot>(node_root))
        return verify_cast<ShadowRoot>(node_root).host()->shadow_including_root();
    return node_root;
}

// https://dom.spec.whatwg.org/#connected
bool Node::is_connected() const
{
    // An element is connected if its shadow-including root is a document.
    return shadow_including_root().is_document();
}

Element* Node::parent_element()
{
    if (!parent() || !is<Element>(parent()))
        return nullptr;
    return verify_cast<Element>(parent());
}

Element const* Node::parent_element() const
{
    if (!parent() || !is<Element>(parent()))
        return nullptr;
    return verify_cast<Element>(parent());
}

// https://dom.spec.whatwg.org/#concept-node-ensure-pre-insertion-validity
ExceptionOr<void> Node::ensure_pre_insertion_validity(JS::NonnullGCPtr<Node> node, JS::GCPtr<Node> child) const
{
    // 1. If parent is not a Document, DocumentFragment, or Element node, then throw a "HierarchyRequestError" DOMException.
    if (!is<Document>(this) && !is<DocumentFragment>(this) && !is<Element>(this))
        return DOM::HierarchyRequestError::create("Can only insert into a document, document fragment or element");

    // 2. If node is a host-including inclusive ancestor of parent, then throw a "HierarchyRequestError" DOMException.
    if (node->is_host_including_inclusive_ancestor_of(*this))
        return DOM::HierarchyRequestError::create("New node is an ancestor of this node");

    // 3. If child is non-null and its parent is not parent, then throw a "NotFoundError" DOMException.
    if (child && child->parent() != this)
        return DOM::NotFoundError::create("This node is not the parent of the given child");

    // FIXME: All the following "Invalid node type for insertion" messages could be more descriptive.
    // 4. If node is not a DocumentFragment, DocumentType, Element, or CharacterData node, then throw a "HierarchyRequestError" DOMException.
    if (!is<DocumentFragment>(*node) && !is<DocumentType>(*node) && !is<Element>(*node) && !is<Text>(*node) && !is<Comment>(*node) && !is<ProcessingInstruction>(*node))
        return DOM::HierarchyRequestError::create("Invalid node type for insertion");

    // 5. If either node is a Text node and parent is a document, or node is a doctype and parent is not a document, then throw a "HierarchyRequestError" DOMException.
    if ((is<Text>(*node) && is<Document>(this)) || (is<DocumentType>(*node) && !is<Document>(this)))
        return DOM::HierarchyRequestError::create("Invalid node type for insertion");

    // 6. If parent is a document, and any of the statements below, switched on the interface node implements, are true, then throw a "HierarchyRequestError" DOMException.
    if (is<Document>(this)) {
        // DocumentFragment
        if (is<DocumentFragment>(*node)) {
            // If node has more than one element child or has a Text node child.
            // Otherwise, if node has one element child and either parent has an element child, child is a doctype, or child is non-null and a doctype is following child.
            auto node_element_child_count = verify_cast<DocumentFragment>(*node).child_element_count();
            if ((node_element_child_count > 1 || node->has_child_of_type<Text>())
                || (node_element_child_count == 1 && (has_child_of_type<Element>() || is<DocumentType>(child.ptr()) || (child && child->has_following_node_of_type_in_tree_order<DocumentType>())))) {
                return DOM::HierarchyRequestError::create("Invalid node type for insertion");
            }
        } else if (is<Element>(*node)) {
            // Element
            // If parent has an element child, child is a doctype, or child is non-null and a doctype is following child.
            if (has_child_of_type<Element>() || is<DocumentType>(child.ptr()) || (child && child->has_following_node_of_type_in_tree_order<DocumentType>()))
                return DOM::HierarchyRequestError::create("Invalid node type for insertion");
        } else if (is<DocumentType>(*node)) {
            // DocumentType
            // parent has a doctype child, child is non-null and an element is preceding child, or child is null and parent has an element child.
            if (has_child_of_type<DocumentType>() || (child && child->has_preceding_node_of_type_in_tree_order<Element>()) || (!child && has_child_of_type<Element>()))
                return DOM::HierarchyRequestError::create("Invalid node type for insertion");
        }
    }

    return {};
}

// https://dom.spec.whatwg.org/#concept-node-insert
void Node::insert_before(JS::NonnullGCPtr<Node> node, JS::GCPtr<Node> child, bool suppress_observers)
{
    // 1. Let nodes be node’s children, if node is a DocumentFragment node; otherwise « node ».
    Vector<JS::Handle<Node>> nodes;
    if (is<DocumentFragment>(*node))
        nodes = node->children_as_vector();
    else
        nodes.append(JS::make_handle(*node));

    // 2. Let count be nodes’s size.
    auto count = nodes.size();

    // 3. If count is 0, then return.
    if (count == 0)
        return;

    // 4. If node is a DocumentFragment node, then:
    if (is<DocumentFragment>(*node)) {
        // 1. Remove its children with the suppress observers flag set.
        node->remove_all_children(true);

        // 2. Queue a tree mutation record for node with « », nodes, null, and null.
        // NOTE: This step intentionally does not pay attention to the suppress observers flag.
        node->queue_tree_mutation_record(StaticNodeList::create(window(), {}), StaticNodeList::create(window(), nodes), nullptr, nullptr);
    }

    // 5. If child is non-null, then:
    if (child) {
        // 1. For each live range whose start node is parent and start offset is greater than child’s index, increase its start offset by count.
        for (auto& range : Range::live_ranges()) {
            if (range->start_container() == this && range->start_offset() > child->index())
                range->set_start(*range->start_container(), range->start_offset() + count);
        }

        // 2. For each live range whose end node is parent and end offset is greater than child’s index, increase its end offset by count.
        for (auto& range : Range::live_ranges()) {
            if (range->end_container() == this && range->end_offset() > child->index())
                range->set_end(*range->end_container(), range->end_offset() + count);
        }
    }

    // 6. Let previousSibling be child’s previous sibling or parent’s last child if child is null.
    JS::GCPtr<Node> previous_sibling;
    if (child)
        previous_sibling = child->previous_sibling();
    else
        previous_sibling = last_child();

    // 7. For each node in nodes, in tree order:
    // FIXME: In tree order
    for (auto& node_to_insert : nodes) {
        // 1. Adopt node into parent’s node document.
        document().adopt_node(*node_to_insert);

        // 2. If child is null, then append node to parent’s children.
        if (!child)
            append_child_impl(*node_to_insert);
        // 3. Otherwise, insert node into parent’s children before child’s index.
        else
            insert_before_impl(*node_to_insert, child);

        // FIXME: 4. If parent is a shadow host and node is a slottable, then assign a slot for node.
        // FIXME: 5. If parent’s root is a shadow root, and parent is a slot whose assigned nodes is the empty list, then run signal a slot change for parent.
        // FIXME: 6. Run assign slottables for a tree with node’s root.

        // FIXME: This should be shadow-including.
        // 7. For each shadow-including inclusive descendant inclusiveDescendant of node, in shadow-including tree order:
        node_to_insert->for_each_in_inclusive_subtree([&](Node& inclusive_descendant) {
            // 1. Run the insertion steps with inclusiveDescendant.
            inclusive_descendant.inserted();

            // 2. If inclusiveDescendant is connected, then:
            if (inclusive_descendant.is_connected()) {
                // FIXME: 1. If inclusiveDescendant is custom, then enqueue a custom element callback reaction with inclusiveDescendant, callback name "connectedCallback", and an empty argument list.

                // FIXME: 2. Otherwise, try to upgrade inclusiveDescendant.
                // NOTE: If this successfully upgrades inclusiveDescendant, its connectedCallback will be enqueued automatically during the upgrade an element algorithm.
            }

            return IterationDecision::Continue;
        });
    }

    // 8. If suppress observers flag is unset, then queue a tree mutation record for parent with nodes, « », previousSibling, and child.
    if (!suppress_observers)
        queue_tree_mutation_record(StaticNodeList::create(window(), move(nodes)), StaticNodeList::create(window(), {}), previous_sibling.ptr(), child.ptr());

    // 9. Run the children changed steps for parent.
    children_changed();

    document().invalidate_style();
}

// https://dom.spec.whatwg.org/#concept-node-pre-insert
ExceptionOr<JS::NonnullGCPtr<Node>> Node::pre_insert(JS::NonnullGCPtr<Node> node, JS::GCPtr<Node> child)
{
    // 1. Ensure pre-insertion validity of node into parent before child.
    TRY(ensure_pre_insertion_validity(node, child));

    // 2. Let referenceChild be child.
    auto reference_child = child;

    // 3. If referenceChild is node, then set referenceChild to node’s next sibling.
    if (reference_child == node)
        reference_child = node->next_sibling();

    // 4. Insert node into parent before referenceChild.
    insert_before(node, reference_child);

    // 5. Return node.
    return node;
}

// https://dom.spec.whatwg.org/#dom-node-removechild
ExceptionOr<JS::NonnullGCPtr<Node>> Node::remove_child(JS::NonnullGCPtr<Node> child)
{
    // The removeChild(child) method steps are to return the result of pre-removing child from this.
    return pre_remove(child);
}

// https://dom.spec.whatwg.org/#concept-node-pre-remove
ExceptionOr<JS::NonnullGCPtr<Node>> Node::pre_remove(JS::NonnullGCPtr<Node> child)
{
    // 1. If child’s parent is not parent, then throw a "NotFoundError" DOMException.
    if (child->parent() != this)
        return DOM::NotFoundError::create("Child does not belong to this node");

    // 2. Remove child.
    child->remove();

    // 3. Return child.
    return child;
}

// https://dom.spec.whatwg.org/#concept-node-append
ExceptionOr<JS::NonnullGCPtr<Node>> Node::append_child(JS::NonnullGCPtr<Node> node)
{
    // To append a node to a parent, pre-insert node into parent before null.
    return pre_insert(node, nullptr);
}

// https://dom.spec.whatwg.org/#concept-node-remove
void Node::remove(bool suppress_observers)
{
    // 1. Let parent be node’s parent
    auto* parent = this->parent();

    // 2. Assert: parent is non-null.
    VERIFY(parent);

    // 3. Let index be node’s index.
    auto index = this->index();

    // 4. For each live range whose start node is an inclusive descendant of node, set its start to (parent, index).
    for (auto& range : Range::live_ranges()) {
        if (range->start_container()->is_inclusive_descendant_of(*this))
            range->set_start(*parent, index);
    }

    // 5. For each live range whose end node is an inclusive descendant of node, set its end to (parent, index).
    for (auto& range : Range::live_ranges()) {
        if (range->end_container()->is_inclusive_descendant_of(*this))
            range->set_end(*parent, index);
    }

    // 6. For each live range whose start node is parent and start offset is greater than index, decrease its start offset by 1.
    for (auto& range : Range::live_ranges()) {
        if (range->start_container() == parent && range->start_offset() > index)
            range->set_start(*range->start_container(), range->start_offset() - 1);
    }

    // 7. For each live range whose end node is parent and end offset is greater than index, decrease its end offset by 1.
    for (auto& range : Range::live_ranges()) {
        if (range->end_container() == parent && range->end_offset() > index)
            range->set_end(*range->end_container(), range->end_offset() - 1);
    }

    // 8. For each NodeIterator object iterator whose root’s node document is node’s node document, run the NodeIterator pre-removing steps given node and iterator.
    document().for_each_node_iterator([&](NodeIterator& node_iterator) {
        node_iterator.run_pre_removing_steps(*this);
    });

    // 9. Let oldPreviousSibling be node’s previous sibling.
    JS::GCPtr<Node> old_previous_sibling = previous_sibling();

    // 10. Let oldNextSibling be node’s next sibling.
    JS::GCPtr<Node> old_next_sibling = next_sibling();

    // 11. Remove node from its parent’s children.
    parent->remove_child_impl(*this);

    // FIXME: 12. If node is assigned, then run assign slottables for node’s assigned slot.

    // FIXME: 13. If parent’s root is a shadow root, and parent is a slot whose assigned nodes is the empty list, then run signal a slot change for parent.

    // FIXME: 14. If node has an inclusive descendant that is a slot, then:
    //     1. Run assign slottables for a tree with parent’s root.
    //     2. Run assign slottables for a tree with node.

    // 15. Run the removing steps with node and parent.
    removed_from(parent);

    // FIXME: 16. Let isParentConnected be parent’s connected. (Currently unused so not included)

    // FIXME: 17. If node is custom and isParentConnected is true, then enqueue a custom element callback reaction with node,
    //        callback name "disconnectedCallback", and an empty argument list.
    // NOTE: It is intentional for now that custom elements do not get parent passed. This might change in the future if there is a need.

    // FIXME: This should be shadow-including.
    // 18. For each shadow-including descendant descendant of node, in shadow-including tree order, then:
    for_each_in_subtree([&](Node& descendant) {
        // 1. Run the removing steps with descendant
        descendant.removed_from(nullptr);

        //  FIXME: 2. If descendant is custom and isParentConnected is true, then enqueue a custom element callback reaction with descendant,
        //        callback name "disconnectedCallback", and an empty argument list.

        return IterationDecision::Continue;
    });

    // 19. For each inclusive ancestor inclusiveAncestor of parent, and then for each registered of inclusiveAncestor’s registered observer list,
    //     if registered’s options["subtree"] is true, then append a new transient registered observer
    //     whose observer is registered’s observer, options is registered’s options, and source is registered to node’s registered observer list.
    for (auto* inclusive_ancestor = parent; inclusive_ancestor; inclusive_ancestor = inclusive_ancestor->parent()) {
        for (auto& registered : inclusive_ancestor->m_registered_observer_list) {
            if (registered.options.subtree) {
                auto transient_observer = TransientRegisteredObserver::create(registered.observer, registered.options, registered);
                m_registered_observer_list.append(move(transient_observer));
            }
        }
    }

    // 20. If suppress observers flag is unset, then queue a tree mutation record for parent with « », « node », oldPreviousSibling, and oldNextSibling.
    if (!suppress_observers) {
        Vector<JS::Handle<Node>> removed_nodes;
        removed_nodes.append(JS::make_handle(*this));
        parent->queue_tree_mutation_record(StaticNodeList::create(window(), {}), StaticNodeList::create(window(), move(removed_nodes)), old_previous_sibling.ptr(), old_next_sibling.ptr());
    }

    // 21. Run the children changed steps for parent.
    parent->children_changed();

    document().invalidate_style();
}

// https://dom.spec.whatwg.org/#concept-node-replace
ExceptionOr<JS::NonnullGCPtr<Node>> Node::replace_child(JS::NonnullGCPtr<Node> node, JS::NonnullGCPtr<Node> child)
{
    // If parent is not a Document, DocumentFragment, or Element node, then throw a "HierarchyRequestError" DOMException.
    if (!is<Document>(this) && !is<DocumentFragment>(this) && !is<Element>(this))
        return DOM::HierarchyRequestError::create("Can only insert into a document, document fragment or element");

    // 2. If node is a host-including inclusive ancestor of parent, then throw a "HierarchyRequestError" DOMException.
    if (node->is_host_including_inclusive_ancestor_of(*this))
        return DOM::HierarchyRequestError::create("New node is an ancestor of this node");

    // 3. If child’s parent is not parent, then throw a "NotFoundError" DOMException.
    if (child->parent() != this)
        return DOM::NotFoundError::create("This node is not the parent of the given child");

    // FIXME: All the following "Invalid node type for insertion" messages could be more descriptive.

    // 4. If node is not a DocumentFragment, DocumentType, Element, or CharacterData node, then throw a "HierarchyRequestError" DOMException.
    if (!is<DocumentFragment>(*node) && !is<DocumentType>(*node) && !is<Element>(*node) && !is<Text>(*node) && !is<Comment>(*node) && !is<ProcessingInstruction>(*node))
        return DOM::HierarchyRequestError::create("Invalid node type for insertion");

    // 5. If either node is a Text node and parent is a document, or node is a doctype and parent is not a document, then throw a "HierarchyRequestError" DOMException.
    if ((is<Text>(*node) && is<Document>(this)) || (is<DocumentType>(*node) && !is<Document>(this)))
        return DOM::HierarchyRequestError::create("Invalid node type for insertion");

    // If parent is a document, and any of the statements below, switched on the interface node implements, are true, then throw a "HierarchyRequestError" DOMException.
    if (is<Document>(this)) {
        // DocumentFragment
        if (is<DocumentFragment>(*node)) {
            // If node has more than one element child or has a Text node child.
            // Otherwise, if node has one element child and either parent has an element child that is not child or a doctype is following child.
            auto node_element_child_count = verify_cast<DocumentFragment>(*node).child_element_count();
            if ((node_element_child_count > 1 || node->has_child_of_type<Text>())
                || (node_element_child_count == 1 && (first_child_of_type<Element>() != child || child->has_following_node_of_type_in_tree_order<DocumentType>()))) {
                return DOM::HierarchyRequestError::create("Invalid node type for insertion");
            }
        } else if (is<Element>(*node)) {
            // Element
            // parent has an element child that is not child or a doctype is following child.
            if (first_child_of_type<Element>() != child || child->has_following_node_of_type_in_tree_order<DocumentType>())
                return DOM::HierarchyRequestError::create("Invalid node type for insertion");
        } else if (is<DocumentType>(*node)) {
            // DocumentType
            // parent has a doctype child that is not child, or an element is preceding child.
            if (first_child_of_type<DocumentType>() != node || child->has_preceding_node_of_type_in_tree_order<Element>())
                return DOM::HierarchyRequestError::create("Invalid node type for insertion");
        }
    }

    // 7. Let referenceChild be child’s next sibling.
    JS::GCPtr<Node> reference_child = child->next_sibling();

    // 8. If referenceChild is node, then set referenceChild to node’s next sibling.
    if (reference_child == node)
        reference_child = node->next_sibling();

    // 9. Let previousSibling be child’s previous sibling.
    JS::GCPtr<Node> previous_sibling = child->previous_sibling();

    // 10. Let removedNodes be the empty set.
    Vector<JS::Handle<Node>> removed_nodes;

    // 11. If child’s parent is non-null, then:
    // NOTE: The above can only be false if child is node.
    if (child->parent()) {
        // 1. Set removedNodes to « child ».
        removed_nodes.append(JS::make_handle(*child));

        // 2. Remove child with the suppress observers flag set.
        child->remove(true);
    }

    // 12. Let nodes be node’s children if node is a DocumentFragment node; otherwise « node ».
    Vector<JS::Handle<Node>> nodes;
    if (is<DocumentFragment>(*node))
        nodes = node->children_as_vector();
    else
        nodes.append(JS::make_handle(*node));

    // 13. Insert node into parent before referenceChild with the suppress observers flag set.
    insert_before(node, reference_child, true);

    // 14. Queue a tree mutation record for parent with nodes, removedNodes, previousSibling, and referenceChild.
    queue_tree_mutation_record(StaticNodeList::create(window(), move(nodes)), StaticNodeList::create(window(), move(removed_nodes)), previous_sibling.ptr(), reference_child.ptr());

    // 15. Return child.
    return child;
}

// https://dom.spec.whatwg.org/#concept-node-clone
JS::NonnullGCPtr<Node> Node::clone_node(Document* document, bool clone_children)
{
    // 1. If document is not given, let document be node’s node document.
    if (!document)
        document = m_document.ptr();
    JS::GCPtr<Node> copy;

    // 2. If node is an element, then:
    if (is<Element>(this)) {
        // 1. Let copy be the result of creating an element, given document, node’s local name, node’s namespace, node’s namespace prefix, and node’s is value, with the synchronous custom elements flag unset.
        auto& element = *verify_cast<Element>(this);
        auto element_copy = DOM::create_element(*document, element.local_name(), element.namespace_() /* FIXME: node’s namespace prefix, and node’s is value, with the synchronous custom elements flag unset */);

        // 2. For each attribute in node’s attribute list:
        element.for_each_attribute([&](auto& name, auto& value) {
            // 1. Let copyAttribute be a clone of attribute.
            // 2. Append copyAttribute to copy.
            element_copy->set_attribute(name, value);
        });
        copy = move(element_copy);

    }
    // 3. Otherwise, let copy be a node that implements the same interfaces as node, and fulfills these additional requirements, switching on the interface node implements:
    else if (is<Document>(this)) {
        // Document
        auto document_ = verify_cast<Document>(this);
        auto document_copy = Document::create(Bindings::main_thread_internal_window_object(), document_->url());

        // Set copy’s encoding, content type, URL, origin, type, and mode to those of node.
        document_copy->set_encoding(document_->encoding());
        document_copy->set_content_type(document_->content_type());
        document_copy->set_url(document_->url());
        document_copy->set_origin(document_->origin());
        document_copy->set_document_type(document_->document_type());
        document_copy->set_quirks_mode(document_->mode());
        copy = move(document_copy);
    } else if (is<DocumentType>(this)) {
        // DocumentType
        auto document_type = verify_cast<DocumentType>(this);
        auto document_type_copy = heap().allocate<DocumentType>(realm(), *document);

        // Set copy’s name, public ID, and system ID to those of node.
        document_type_copy->set_name(document_type->name());
        document_type_copy->set_public_id(document_type->public_id());
        document_type_copy->set_system_id(document_type->system_id());
        copy = move(document_type_copy);
    } else if (is<Attribute>(this)) {
        // FIXME:
        // Attr
        // Set copy’s namespace, namespace prefix, local name, and value to those of node.
        dbgln("clone_node() not implemented for Attribute");
    } else if (is<Text>(this)) {
        // Text
        auto text = verify_cast<Text>(this);

        // Set copy’s data to that of node.
        auto text_copy = heap().allocate<Text>(realm(), *document, text->data());
        copy = move(text_copy);
    } else if (is<Comment>(this)) {
        // Comment
        auto comment = verify_cast<Comment>(this);

        // Set copy’s data to that of node.
        auto comment_copy = heap().allocate<Comment>(realm(), *document, comment->data());
        copy = move(comment_copy);
    } else if (is<ProcessingInstruction>(this)) {
        // ProcessingInstruction
        auto processing_instruction = verify_cast<ProcessingInstruction>(this);

        // Set copy’s target and data to those of node.
        auto processing_instruction_copy = heap().allocate<ProcessingInstruction>(realm(), *document, processing_instruction->data(), processing_instruction->target());
        copy = processing_instruction_copy;
    }
    // Otherwise, Do nothing.
    else if (is<DocumentFragment>(this)) {
        copy = heap().allocate<DocumentFragment>(realm(), *document);
    }

    // FIXME: 4. Set copy’s node document and document to copy, if copy is a document, and set copy’s node document to document otherwise.

    // 5. Run any cloning steps defined for node in other applicable specifications and pass copy, node, document and the clone children flag if set, as parameters.
    cloned(*copy, clone_children);

    // 6. If the clone children flag is set, clone all the children of node and append them to copy, with document as specified and the clone children flag being set.
    if (clone_children) {
        for_each_child([&](auto& child) {
            copy->append_child(child.clone_node(document, true));
        });
    }

    // 7. Return copy.
    return *copy;
}

// https://dom.spec.whatwg.org/#dom-node-clonenode
ExceptionOr<JS::NonnullGCPtr<Node>> Node::clone_node_binding(bool deep)
{
    // 1. If this is a shadow root, then throw a "NotSupportedError" DOMException.
    if (is<ShadowRoot>(*this))
        return NotSupportedError::create("Cannot clone shadow root");

    // 2. Return a clone of this, with the clone children flag set if deep is true.
    return clone_node(nullptr, deep);
}

void Node::set_document(Badge<Document>, Document& document)
{
    if (m_document.ptr() == &document)
        return;

    m_document = &document;

    if (needs_style_update() || child_needs_style_update()) {
        // NOTE: We unset and reset the "needs style update" flag here.
        //       This ensures that there's a pending style update in the new document
        //       that will eventually assign some style to this node if needed.
        set_needs_style_update(false);
        set_needs_style_update(true);
    }
}

bool Node::is_editable() const
{
    return parent() && parent()->is_editable();
}

void Node::set_layout_node(Badge<Layout::Node>, Layout::Node* layout_node) const
{
    m_layout_node = layout_node;
}

EventTarget* Node::get_parent(Event const&)
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
        for (auto* ancestor = parent_or_shadow_host(); ancestor; ancestor = ancestor->parent_or_shadow_host()) {
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
        return verify_cast<ShadowRoot>(*this).host();
    return verify_cast<ParentNode>(parent());
}

JS::NonnullGCPtr<NodeList> Node::child_nodes()
{
    // FIXME: This should return the same LiveNodeList object every time,
    //        but that would cause a reference cycle since NodeList refs the root.
    return LiveNodeList::create(window(), *this, [this](auto& node) {
        return is_parent_of(node);
    });
}

Vector<JS::Handle<Node>> Node::children_as_vector() const
{
    Vector<JS::Handle<Node>> nodes;

    for_each_child([&](auto& child) {
        nodes.append(JS::make_handle(child));
    });

    return nodes;
}

void Node::remove_all_children(bool suppress_observers)
{
    while (JS::GCPtr<Node> child = first_child())
        child->remove(suppress_observers);
}

// https://dom.spec.whatwg.org/#dom-node-comparedocumentposition
u16 Node::compare_document_position(JS::GCPtr<Node> other)
{
    enum Position : u16 {
        DOCUMENT_POSITION_EQUAL = 0,
        DOCUMENT_POSITION_DISCONNECTED = 1,
        DOCUMENT_POSITION_PRECEDING = 2,
        DOCUMENT_POSITION_FOLLOWING = 4,
        DOCUMENT_POSITION_CONTAINS = 8,
        DOCUMENT_POSITION_CONTAINED_BY = 16,
        DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC = 32,
    };

    // 1. If this is other, then return zero.
    if (this == other.ptr())
        return DOCUMENT_POSITION_EQUAL;

    // 2. Let node1 be other and node2 be this.
    Node* node1 = other.ptr();
    Node* node2 = this;

    // 3. Let attr1 and attr2 be null.
    Attribute* attr1;
    Attribute* attr2;

    // 4. If node1 is an attribute, then set attr1 to node1 and node1 to attr1’s element.
    if (is<Attribute>(node1)) {
        attr1 = verify_cast<Attribute>(node1);
        node1 = const_cast<Element*>(attr1->owner_element());
    }

    // 5. If node2 is an attribute, then:
    if (is<Attribute>(node2)) {
        // 1. Set attr2 to node2 and node2 to attr2’s element.
        attr2 = verify_cast<Attribute>(node2);
        node2 = const_cast<Element*>(attr2->owner_element());

        // 2. If attr1 and node1 are non-null, and node2 is node1, then:
        if (attr1 && node1 && node2 == node1) {
            // FIXME: 1. For each attr in node2’s attribute list:
            //     1. If attr equals attr1, then return the result of adding DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC and DOCUMENT_POSITION_PRECEDING.
            //     2. If attr equals attr2, then return the result of adding DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC and DOCUMENT_POSITION_FOLLOWING.
        }
    }

    // 6. If node1 or node2 is null, or node1’s root is not node2’s root, then return the result of adding
    // DOCUMENT_POSITION_DISCONNECTED, DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC, and either DOCUMENT_POSITION_PRECEDING or DOCUMENT_POSITION_FOLLOWING, with the constraint that this is to be consistent, together.
    if ((node1 == nullptr || node2 == nullptr) || (&node1->root() != &node2->root()))
        return DOCUMENT_POSITION_DISCONNECTED | DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC | (node1 > node2 ? DOCUMENT_POSITION_PRECEDING : DOCUMENT_POSITION_FOLLOWING);

    // 7. If node1 is an ancestor of node2 and attr1 is null, or node1 is node2 and attr2 is non-null, then return the result of adding DOCUMENT_POSITION_CONTAINS to DOCUMENT_POSITION_PRECEDING.
    if ((node1->is_ancestor_of(*node2) && !attr1) || (node1 == node2 && attr2))
        return DOCUMENT_POSITION_CONTAINS | DOCUMENT_POSITION_PRECEDING;

    // 8. If node1 is a descendant of node2 and attr2 is null, or node1 is node2 and attr1 is non-null, then return the result of adding DOCUMENT_POSITION_CONTAINED_BY to DOCUMENT_POSITION_FOLLOWING.
    if ((node2->is_ancestor_of(*node1) && !attr2) || (node1 == node2 && attr1))
        return DOCUMENT_POSITION_CONTAINED_BY | DOCUMENT_POSITION_FOLLOWING;

    // 9. If node1 is preceding node2, then return DOCUMENT_POSITION_PRECEDING.
    if (node1->is_before(*node2))
        return DOCUMENT_POSITION_PRECEDING;

    // 10. Return DOCUMENT_POSITION_FOLLOWING.
    return DOCUMENT_POSITION_FOLLOWING;
}

// https://dom.spec.whatwg.org/#concept-tree-host-including-inclusive-ancestor
bool Node::is_host_including_inclusive_ancestor_of(Node const& other) const
{
    // An object A is a host-including inclusive ancestor of an object B,
    // if either A is an inclusive ancestor of B,
    if (is_inclusive_ancestor_of(other))
        return true;

    // or if B’s root has a non-null host and A is a host-including inclusive ancestor of B’s root’s host
    if (is<DocumentFragment>(other.root())
        && static_cast<DocumentFragment const&>(other.root()).host()
        && is_inclusive_ancestor_of(*static_cast<DocumentFragment const&>(other.root()).host())) {
        return true;
    }
    return false;
}

// https://dom.spec.whatwg.org/#dom-node-ownerdocument
JS::GCPtr<Document> Node::owner_document() const
{
    // The ownerDocument getter steps are to return null, if this is a document; otherwise this’s node document.
    if (is_document())
        return nullptr;
    return m_document;
}

// This function tells us whether a node is interesting enough to show up
// in the DOM inspector. This hides two things:
// - Non-rendered whitespace
// - Rendered whitespace between block-level elements
bool Node::is_uninteresting_whitespace_node() const
{
    if (!is<Text>(*this))
        return false;
    if (!static_cast<Text const&>(*this).data().is_whitespace())
        return false;
    if (!layout_node())
        return true;
    if (layout_node()->parent()->is_anonymous())
        return true;
    return false;
}

void Node::serialize_tree_as_json(JsonObjectSerializer<StringBuilder>& object) const
{
    MUST(object.add("name"sv, node_name().view()));
    MUST(object.add("id"sv, id()));
    if (is_document()) {
        MUST(object.add("type"sv, "document"));
    } else if (is_element()) {
        MUST(object.add("type"sv, "element"));

        auto const* element = static_cast<DOM::Element const*>(this);
        if (element->has_attributes()) {
            auto attributes = MUST(object.add_object("attributes"sv));
            element->for_each_attribute([&attributes](auto& name, auto& value) {
                MUST(attributes.add(name, value));
            });
            MUST(attributes.finish());
        }

        if (element->is_browsing_context_container()) {
            auto const* container = static_cast<HTML::BrowsingContextContainer const*>(element);
            if (auto const* content_document = container->content_document()) {
                auto children = MUST(object.add_array("children"sv));
                JsonObjectSerializer<StringBuilder> content_document_object = MUST(children.add_object());
                content_document->serialize_tree_as_json(content_document_object);
                MUST(content_document_object.finish());
                MUST(children.finish());
            }
        }
    } else if (is_text()) {
        MUST(object.add("type"sv, "text"));

        auto text_node = static_cast<DOM::Text const*>(this);
        MUST(object.add("text"sv, text_node->data()));
    } else if (is_comment()) {
        MUST(object.add("type"sv, "comment"sv));
        MUST(object.add("data"sv, static_cast<DOM::Comment const&>(*this).data()));
    }

    MUST((object.add("visible"sv, !!layout_node())));

    if (has_child_nodes()) {
        auto children = MUST(object.add_array("children"sv));
        for_each_child([&children](DOM::Node& child) {
            if (child.is_uninteresting_whitespace_node())
                return;
            JsonObjectSerializer<StringBuilder> child_object = MUST(children.add_object());
            child.serialize_tree_as_json(child_object);
            MUST(child_object.finish());
        });

        // Pseudo-elements don't have DOM nodes,so we have to add them separately.
        if (is_element()) {
            auto const* element = static_cast<DOM::Element const*>(this);
            element->serialize_pseudo_elements_as_json(children);
        }

        MUST(children.finish());
    }
}

// https://html.spec.whatwg.org/multipage/webappapis.html#concept-n-script
bool Node::is_scripting_enabled() const
{
    // Scripting is enabled for a node node if node's node document's browsing context is non-null, and scripting is enabled for node's relevant settings object.
    return document().browsing_context() && const_cast<Document&>(document()).relevant_settings_object().is_scripting_enabled();
}

// https://html.spec.whatwg.org/multipage/webappapis.html#concept-n-noscript
bool Node::is_scripting_disabled() const
{
    // Scripting is disabled for a node when scripting is not enabled, i.e., when its node document's browsing context is null or when scripting is disabled for its relevant settings object.
    return !is_scripting_enabled();
}

// https://dom.spec.whatwg.org/#dom-node-contains
bool Node::contains(JS::GCPtr<Node> other) const
{
    // The contains(other) method steps are to return true if other is an inclusive descendant of this; otherwise false (including when other is null).
    return other && other->is_inclusive_descendant_of(*this);
}

// https://dom.spec.whatwg.org/#concept-shadow-including-descendant
bool Node::is_shadow_including_descendant_of(Node const& other) const
{
    // An object A is a shadow-including descendant of an object B,
    // if A is a descendant of B,
    if (is_descendant_of(other))
        return true;

    // or A’s root is a shadow root
    if (!is<ShadowRoot>(root()))
        return false;

    // and A’s root’s host is a shadow-including inclusive descendant of B.
    auto& shadow_root = verify_cast<ShadowRoot>(root());
    // NOTE: While host is nullable because of inheriting from DocumentFragment, shadow roots always have a host.
    return shadow_root.host()->is_shadow_including_inclusive_descendant_of(other);
}

// https://dom.spec.whatwg.org/#concept-shadow-including-inclusive-descendant
bool Node::is_shadow_including_inclusive_descendant_of(Node const& other) const
{
    // A shadow-including inclusive descendant is an object or one of its shadow-including descendants.
    return &other == this || is_shadow_including_descendant_of(other);
}

// https://dom.spec.whatwg.org/#concept-shadow-including-ancestor
bool Node::is_shadow_including_ancestor_of(Node const& other) const
{
    // An object A is a shadow-including ancestor of an object B, if and only if B is a shadow-including descendant of A.
    return other.is_shadow_including_descendant_of(*this);
}

// https://dom.spec.whatwg.org/#concept-shadow-including-inclusive-ancestor
bool Node::is_shadow_including_inclusive_ancestor_of(Node const& other) const
{
    // A shadow-including inclusive ancestor is an object or one of its shadow-including ancestors.
    return other.is_shadow_including_inclusive_descendant_of(*this);
}

// https://dom.spec.whatwg.org/#concept-node-replace-all
void Node::replace_all(JS::GCPtr<Node> node)
{
    // 1. Let removedNodes be parent’s children.
    auto removed_nodes = children_as_vector();

    // 2. Let addedNodes be the empty set.
    Vector<JS::Handle<Node>> added_nodes;

    // 3. If node is a DocumentFragment node, then set addedNodes to node’s children.
    if (node && is<DocumentFragment>(*node)) {
        added_nodes = node->children_as_vector();
    }
    // 4. Otherwise, if node is non-null, set addedNodes to « node ».
    else if (node) {
        added_nodes.append(JS::make_handle(*node));
    }

    // 5. Remove all parent’s children, in tree order, with the suppress observers flag set.
    remove_all_children(true);

    // 6. If node is non-null, then insert node into parent before null with the suppress observers flag set.
    if (node)
        insert_before(*node, nullptr, true);

    // 7. If either addedNodes or removedNodes is not empty, then queue a tree mutation record for parent with addedNodes, removedNodes, null, and null.
    if (!added_nodes.is_empty() || !removed_nodes.is_empty())
        queue_tree_mutation_record(StaticNodeList::create(window(), move(added_nodes)), StaticNodeList::create(window(), move(removed_nodes)), nullptr, nullptr);
}

// https://dom.spec.whatwg.org/#string-replace-all
void Node::string_replace_all(String const& string)
{
    // 1. Let node be null.
    JS::GCPtr<Node> node;

    // 2. If string is not the empty string, then set node to a new Text node whose data is string and node document is parent’s node document.
    if (!string.is_empty())
        node = heap().allocate<Text>(realm(), document(), string);

    // 3. Replace all with node within parent.
    replace_all(node);
}

// https://w3c.github.io/DOM-Parsing/#dfn-fragment-serializing-algorithm
String Node::serialize_fragment(/* FIXME: Requires well-formed flag */) const
{
    // FIXME: 1. Let context document be the value of node's node document.

    // FIXME: 2. If context document is an HTML document, return an HTML serialization of node.
    //        (We currently always do this)
    return HTML::HTMLParser::serialize_html_fragment(*this);

    // FIXME: 3. Otherwise, context document is an XML document; return an XML serialization of node passing the flag require well-formed.
}

// https://dom.spec.whatwg.org/#dom-node-issamenode
bool Node::is_same_node(Node const* other_node) const
{
    // The isSameNode(otherNode) method steps are to return true if otherNode is this; otherwise false.
    return this == other_node;
}

// https://dom.spec.whatwg.org/#dom-node-isequalnode
bool Node::is_equal_node(Node const* other_node) const
{
    // The isEqualNode(otherNode) method steps are to return true if otherNode is non-null and this equals otherNode; otherwise false.
    if (!other_node)
        return false;

    // Fast path for testing a node against itself.
    if (this == other_node)
        return true;

    // A node A equals a node B if all of the following conditions are true:

    // A and B implement the same interfaces.
    if (node_name() != other_node->node_name())
        return false;

    // The following are equal, switching on the interface A implements:
    switch (node_type()) {
    case (u16)NodeType::DOCUMENT_TYPE_NODE: {
        // Its name, public ID, and system ID.
        auto& this_doctype = verify_cast<DocumentType>(*this);
        auto& other_doctype = verify_cast<DocumentType>(*other_node);
        if (this_doctype.name() != other_doctype.name()
            || this_doctype.public_id() != other_doctype.public_id()
            || this_doctype.system_id() != other_doctype.system_id())
            return false;
        break;
    }
    case (u16)NodeType::ELEMENT_NODE: {
        // Its namespace, namespace prefix, local name, and its attribute list’s size.
        auto& this_element = verify_cast<Element>(*this);
        auto& other_element = verify_cast<Element>(*other_node);
        if (this_element.namespace_() != other_element.namespace_()
            || this_element.prefix() != other_element.prefix()
            || this_element.local_name() != other_element.local_name()
            || this_element.attribute_list_size() != other_element.attribute_list_size())
            return false;
        // If A is an element, each attribute in its attribute list has an attribute that equals an attribute in B’s attribute list.
        bool has_same_attributes = true;
        this_element.for_each_attribute([&](auto& name, auto& value) {
            if (other_element.get_attribute(name) != value)
                has_same_attributes = false;
        });
        if (!has_same_attributes)
            return false;
        break;
    }
    case (u16)NodeType::COMMENT_NODE:
    case (u16)NodeType::TEXT_NODE: {
        // Its data.
        auto& this_cdata = verify_cast<CharacterData>(*this);
        auto& other_cdata = verify_cast<CharacterData>(*other_node);
        if (this_cdata.data() != other_cdata.data())
            return false;
        break;
    }
    case (u16)NodeType::PROCESSING_INSTRUCTION_NODE:
    case (u16)NodeType::ATTRIBUTE_NODE:
        TODO();
    default:
        break;
    }

    // A and B have the same number of children.
    size_t this_child_count = child_count();
    size_t other_child_count = other_node->child_count();
    if (this_child_count != other_child_count)
        return false;

    // Each child of A equals the child of B at the identical index.
    // FIXME: This can be made nicer. child_at_index() is O(n).
    for (size_t i = 0; i < this_child_count; ++i) {
        auto* this_child = child_at_index(i);
        auto* other_child = other_node->child_at_index(i);
        VERIFY(this_child);
        VERIFY(other_child);
        if (!this_child->is_equal_node(other_child))
            return false;
    }

    return true;
}

// https://dom.spec.whatwg.org/#in-a-document-tree
bool Node::in_a_document_tree() const
{
    // An element is in a document tree if its root is a document.
    return root().is_document();
}

// https://dom.spec.whatwg.org/#dom-node-getrootnode
JS::NonnullGCPtr<Node> Node::get_root_node(GetRootNodeOptions const& options)
{
    // The getRootNode(options) method steps are to return this’s shadow-including root if options["composed"] is true;
    if (options.composed)
        return shadow_including_root();

    // otherwise this’s root.
    return root();
}

String Node::debug_description() const
{
    StringBuilder builder;
    builder.append(node_name().to_lowercase());
    if (is_element()) {
        auto& element = static_cast<DOM::Element const&>(*this);
        if (auto id = element.get_attribute(HTML::AttributeNames::id); !id.is_null())
            builder.appendff("#{}", id);
        for (auto const& class_name : element.class_names())
            builder.appendff(".{}", class_name);
    }
    return builder.to_string();
}

// https://dom.spec.whatwg.org/#concept-node-length
size_t Node::length() const
{
    // 1. If node is a DocumentType or Attr node, then return 0.
    if (is_document_type() || is_attribute())
        return 0;

    // 2. If node is a CharacterData node, then return node’s data’s length.
    if (is_character_data()) {
        auto* character_data_node = verify_cast<CharacterData>(this);
        return character_data_node->data().length();
    }

    // 3. Return the number of node’s children.
    return child_count();
}

Painting::Paintable const* Node::paintable() const
{
    if (!layout_node())
        return nullptr;
    return layout_node()->paintable();
}

Painting::PaintableBox const* Node::paint_box() const
{
    if (!layout_node())
        return nullptr;
    if (!layout_node()->is_box())
        return nullptr;
    return static_cast<Layout::Box const&>(*layout_node()).paint_box();
}

// https://dom.spec.whatwg.org/#queue-a-mutation-record
void Node::queue_mutation_record(FlyString const& type, String attribute_name, String attribute_namespace, String old_value, JS::NonnullGCPtr<NodeList> added_nodes, JS::NonnullGCPtr<NodeList> removed_nodes, Node* previous_sibling, Node* next_sibling)
{
    // 1. Let interestedObservers be an empty map.
    // mutationObserver -> mappedOldValue
    OrderedHashMap<NonnullRefPtr<MutationObserver>, String> interested_observers;

    // 2. Let nodes be the inclusive ancestors of target.
    Vector<JS::Handle<Node>> nodes;
    nodes.append(JS::make_handle(*this));

    for (auto* parent_node = parent(); parent_node; parent_node = parent_node->parent())
        nodes.append(JS::make_handle(*parent_node));

    // 3. For each node in nodes, and then for each registered of node’s registered observer list:
    for (auto& node : nodes) {
        for (auto& registered_observer : node->m_registered_observer_list) {
            // 1. Let options be registered’s options.
            auto& options = registered_observer.options;

            // 2. If none of the following are true
            //      - node is not target and options["subtree"] is false
            //      - type is "attributes" and options["attributes"] either does not exist or is false
            //      - type is "attributes", options["attributeFilter"] exists, and options["attributeFilter"] does not contain name or namespace is non-null
            //      - type is "characterData" and options["characterData"] either does not exist or is false
            //      - type is "childList" and options["childList"] is false
            //    then:
            if (!(node.ptr() != this && !options.subtree)
                && !(type == MutationType::attributes && (!options.attributes.has_value() || !options.attributes.value()))
                && !(type == MutationType::attributes && options.attribute_filter.has_value() && (!attribute_namespace.is_null() || !options.attribute_filter->contains_slow(attribute_name)))
                && !(type == MutationType::characterData && (!options.character_data.has_value() || !options.character_data.value()))
                && !(type == MutationType::childList && !options.child_list)) {
                // 1. Let mo be registered’s observer.
                auto mutation_observer = registered_observer.observer;

                // 2. If interestedObservers[mo] does not exist, then set interestedObservers[mo] to null.
                if (!interested_observers.contains(mutation_observer))
                    interested_observers.set(mutation_observer, {});

                // 3. If either type is "attributes" and options["attributeOldValue"] is true, or type is "characterData" and options["characterDataOldValue"] is true, then set interestedObservers[mo] to oldValue.
                if ((type == MutationType::attributes && options.attribute_old_value.has_value() && options.attribute_old_value.value()) || (type == MutationType::characterData && options.character_data_old_value.has_value() && options.character_data_old_value.value()))
                    interested_observers.set(mutation_observer, old_value);
            }
        }
    }

    // 4. For each observer → mappedOldValue of interestedObservers:
    for (auto& interested_observer : interested_observers) {
        // 1. Let record be a new MutationRecord object with its type set to type, target set to target, attributeName set to name, attributeNamespace set to namespace, oldValue set to mappedOldValue,
        //    addedNodes set to addedNodes, removedNodes set to removedNodes, previousSibling set to previousSibling, and nextSibling set to nextSibling.
        auto record = MutationRecord::create(window(), type, *this, added_nodes, removed_nodes, previous_sibling, next_sibling, attribute_name, attribute_namespace, /* mappedOldValue */ interested_observer.value);

        // 2. Enqueue record to observer’s record queue.
        interested_observer.key->enqueue_record({}, move(record));
    }

    // 5. Queue a mutation observer microtask.
    Bindings::queue_mutation_observer_microtask(document());
}

// https://dom.spec.whatwg.org/#queue-a-tree-mutation-record
void Node::queue_tree_mutation_record(JS::NonnullGCPtr<NodeList> added_nodes, JS::NonnullGCPtr<NodeList> removed_nodes, Node* previous_sibling, Node* next_sibling)
{
    // 1. Assert: either addedNodes or removedNodes is not empty.
    VERIFY(added_nodes->length() > 0 || removed_nodes->length() > 0);

    // 2. Queue a mutation record of "childList" for target with null, null, null, addedNodes, removedNodes, previousSibling, and nextSibling.
    queue_mutation_record(MutationType::childList, {}, {}, {}, move(added_nodes), move(removed_nodes), previous_sibling, next_sibling);
}

void Node::append_child_impl(JS::NonnullGCPtr<Node> node)
{
    VERIFY(!node->m_parent);

    if (!is_child_allowed(*node))
        return;

    if (m_last_child)
        m_last_child->m_next_sibling = node.ptr();
    node->m_previous_sibling = m_last_child;
    node->m_parent = this;
    m_last_child = node.ptr();
    if (!m_first_child)
        m_first_child = m_last_child;
}

void Node::insert_before_impl(JS::NonnullGCPtr<Node> node, JS::GCPtr<Node> child)
{
    if (!child)
        return append_child_impl(move(node));

    VERIFY(!node->m_parent);
    VERIFY(child->parent() == this);

    node->m_previous_sibling = child->m_previous_sibling;
    node->m_next_sibling = child;

    if (child->m_previous_sibling)
        child->m_previous_sibling->m_next_sibling = node;

    if (m_first_child == child)
        m_first_child = node;

    child->m_previous_sibling = node;

    node->m_parent = this;
}

void Node::remove_child_impl(JS::NonnullGCPtr<Node> node)
{
    VERIFY(node->m_parent.ptr() == this);

    if (m_first_child == node)
        m_first_child = node->m_next_sibling;

    if (m_last_child == node)
        m_last_child = node->m_previous_sibling;

    if (node->m_next_sibling)
        node->m_next_sibling->m_previous_sibling = node->m_previous_sibling;

    if (node->m_previous_sibling)
        node->m_previous_sibling->m_next_sibling = node->m_next_sibling;

    node->m_next_sibling = nullptr;
    node->m_previous_sibling = nullptr;
    node->m_parent = nullptr;
}

bool Node::is_ancestor_of(Node const& other) const
{
    for (auto* ancestor = other.parent(); ancestor; ancestor = ancestor->parent()) {
        if (ancestor == this)
            return true;
    }
    return false;
}

bool Node::is_inclusive_ancestor_of(Node const& other) const
{
    return &other == this || is_ancestor_of(other);
}

bool Node::is_descendant_of(Node const& other) const
{
    return other.is_ancestor_of(*this);
}

bool Node::is_inclusive_descendant_of(Node const& other) const
{
    return other.is_inclusive_ancestor_of(*this);
}

// https://dom.spec.whatwg.org/#concept-tree-following
bool Node::is_following(Node const& other) const
{
    // An object A is following an object B if A and B are in the same tree and A comes after B in tree order.
    for (auto* node = previous_in_pre_order(); node; node = node->previous_in_pre_order()) {
        if (node == &other)
            return true;
    }

    return false;
}

HTML::Window& Node::window() const
{
    return document().window();
}

}
