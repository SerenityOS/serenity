/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <AK/IDAllocator.h>
#include <AK/StringBuilder.h>
#include <LibJS/Heap/DeferGC.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibRegex/Regex.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Bindings/NodePrototype.h>
#include <LibWeb/DOM/Attr.h>
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
#include <LibWeb/HTML/CustomElements/CustomElementReactionNames.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLStyleElement.h>
#include <LibWeb/HTML/Navigable.h>
#include <LibWeb/HTML/NavigableContainer.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/Infra/CharacterTypes.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Layout/Viewport.h>

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

Node::~Node() = default;

void Node::finalize()
{
    Base::finalize();

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
    visitor.visit(m_child_nodes);

    visitor.visit(m_layout_node);

    for (auto& registered_observer : m_registered_observer_list)
        visitor.visit(registered_observer);
}

// https://dom.spec.whatwg.org/#dom-node-baseuri
DeprecatedString Node::base_uri() const
{
    // Return this’s node document’s document base URL, serialized.
    return document().base_url().to_deprecated_string();
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

const HTML::HTMLElement* Node::enclosing_html_element_with_attribute(DeprecatedFlyString const& attribute) const
{
    for (auto* node = this; node; node = node->parent()) {
        if (is<HTML::HTMLElement>(*node) && verify_cast<HTML::HTMLElement>(*node).has_attribute(attribute))
            return verify_cast<HTML::HTMLElement>(node);
    }
    return nullptr;
}

// https://dom.spec.whatwg.org/#concept-descendant-text-content
DeprecatedString Node::descendant_text_content() const
{
    StringBuilder builder;
    for_each_in_subtree_of_type<Text>([&](auto& text_node) {
        builder.append(text_node.data());
        return IterationDecision::Continue;
    });
    return builder.to_deprecated_string();
}

// https://dom.spec.whatwg.org/#dom-node-textcontent
DeprecatedString Node::text_content() const
{
    // The textContent getter steps are to return the following, switching on the interface this implements:

    // If DocumentFragment or Element, return the descendant text content of this.
    if (is<DocumentFragment>(this) || is<Element>(this))
        return descendant_text_content();

    // If CharacterData, return this’s data.
    if (is<CharacterData>(this))
        return static_cast<CharacterData const&>(*this).data();

    // If Attr node, return this's value.
    if (is<Attr>(*this))
        return static_cast<Attr const&>(*this).value();

    // Otherwise, return null
    return {};
}

// https://dom.spec.whatwg.org/#ref-for-dom-node-textcontent%E2%91%A0
void Node::set_text_content(DeprecatedString const& content)
{
    // The textContent setter steps are to, if the given value is null, act as if it was the empty string instead,
    // and then do as described below, switching on the interface this implements:

    // If DocumentFragment or Element, string replace all with the given value within this.
    if (is<DocumentFragment>(this) || is<Element>(this)) {
        string_replace_all(content);
    }

    // If CharacterData, replace data with node this, offset 0, count this’s length, and data the given value.
    else if (is<CharacterData>(this)) {

        auto* character_data_node = verify_cast<CharacterData>(this);
        character_data_node->set_data(content);

        // FIXME: CharacterData::set_data is not spec compliant. Make this match the spec when set_data becomes spec compliant.
        //        Do note that this will make this function able to throw an exception.
    }

    // If Attr, set an existing attribute value with this and the given value.
    if (is<Attr>(*this)) {
        static_cast<Attr&>(*this).set_value(content);
    }

    // Otherwise, do nothing.

    document().invalidate_style();
    document().invalidate_layout();
}

// https://dom.spec.whatwg.org/#dom-node-nodevalue
DeprecatedString Node::node_value() const
{
    // The nodeValue getter steps are to return the following, switching on the interface this implements:

    // If Attr, return this’s value.
    if (is<Attr>(this)) {
        return verify_cast<Attr>(this)->value();
    }

    // If CharacterData, return this’s data.
    if (is<CharacterData>(this)) {
        return verify_cast<CharacterData>(this)->data();
    }

    // Otherwise, return null.
    return {};
}

// https://dom.spec.whatwg.org/#ref-for-dom-node-nodevalue%E2%91%A0
void Node::set_node_value(DeprecatedString const& value)
{
    // The nodeValue setter steps are to, if the given value is null, act as if it was the empty string instead,
    // and then do as described below, switching on the interface this implements:

    // If Attr, set an existing attribute value with this and the given value.
    if (is<Attr>(this)) {
        verify_cast<Attr>(this)->set_value(value);
    } else if (is<CharacterData>(this)) {
        // If CharacterData, replace data with node this, offset 0, count this’s length, and data the given value.
        verify_cast<CharacterData>(this)->set_data(value);
    }

    // Otherwise, do nothing.
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#node-navigable
JS::GCPtr<HTML::Navigable> Node::navigable() const
{
    // To get the node navigable of a node node, return the navigable whose active document is node's node document,
    // or null if there is no such navigable.
    return HTML::Navigable::navigable_with_active_document(const_cast<Document&>(document()));
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
        if (auto* shadow_root = node.is_element() ? static_cast<DOM::Element&>(node).shadow_root_internal() : nullptr) {
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

DeprecatedString Node::child_text_content() const
{
    if (!is<ParentNode>(*this))
        return DeprecatedString::empty();

    StringBuilder builder;
    verify_cast<ParentNode>(*this).for_each_child([&](auto& child) {
        if (is<Text>(child))
            builder.append(verify_cast<Text>(child).text_content());
    });
    return builder.to_deprecated_string();
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
        return static_cast<ShadowRoot&>(node_root).host()->shadow_including_root();
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
WebIDL::ExceptionOr<void> Node::ensure_pre_insertion_validity(JS::NonnullGCPtr<Node> node, JS::GCPtr<Node> child) const
{
    // 1. If parent is not a Document, DocumentFragment, or Element node, then throw a "HierarchyRequestError" DOMException.
    if (!is<Document>(this) && !is<DocumentFragment>(this) && !is<Element>(this))
        return WebIDL::HierarchyRequestError::create(realm(), "Can only insert into a document, document fragment or element");

    // 2. If node is a host-including inclusive ancestor of parent, then throw a "HierarchyRequestError" DOMException.
    if (node->is_host_including_inclusive_ancestor_of(*this))
        return WebIDL::HierarchyRequestError::create(realm(), "New node is an ancestor of this node");

    // 3. If child is non-null and its parent is not parent, then throw a "NotFoundError" DOMException.
    if (child && child->parent() != this)
        return WebIDL::NotFoundError::create(realm(), "This node is not the parent of the given child");

    // FIXME: All the following "Invalid node type for insertion" messages could be more descriptive.
    // 4. If node is not a DocumentFragment, DocumentType, Element, or CharacterData node, then throw a "HierarchyRequestError" DOMException.
    if (!is<DocumentFragment>(*node) && !is<DocumentType>(*node) && !is<Element>(*node) && !is<Text>(*node) && !is<Comment>(*node) && !is<ProcessingInstruction>(*node))
        return WebIDL::HierarchyRequestError::create(realm(), "Invalid node type for insertion");

    // 5. If either node is a Text node and parent is a document, or node is a doctype and parent is not a document, then throw a "HierarchyRequestError" DOMException.
    if ((is<Text>(*node) && is<Document>(this)) || (is<DocumentType>(*node) && !is<Document>(this)))
        return WebIDL::HierarchyRequestError::create(realm(), "Invalid node type for insertion");

    // 6. If parent is a document, and any of the statements below, switched on the interface node implements, are true, then throw a "HierarchyRequestError" DOMException.
    if (is<Document>(this)) {
        // DocumentFragment
        if (is<DocumentFragment>(*node)) {
            // If node has more than one element child or has a Text node child.
            // Otherwise, if node has one element child and either parent has an element child, child is a doctype, or child is non-null and a doctype is following child.
            auto node_element_child_count = verify_cast<DocumentFragment>(*node).child_element_count();
            if ((node_element_child_count > 1 || node->has_child_of_type<Text>())
                || (node_element_child_count == 1 && (has_child_of_type<Element>() || is<DocumentType>(child.ptr()) || (child && child->has_following_node_of_type_in_tree_order<DocumentType>())))) {
                return WebIDL::HierarchyRequestError::create(realm(), "Invalid node type for insertion");
            }
        } else if (is<Element>(*node)) {
            // Element
            // If parent has an element child, child is a doctype, or child is non-null and a doctype is following child.
            if (has_child_of_type<Element>() || is<DocumentType>(child.ptr()) || (child && child->has_following_node_of_type_in_tree_order<DocumentType>()))
                return WebIDL::HierarchyRequestError::create(realm(), "Invalid node type for insertion");
        } else if (is<DocumentType>(*node)) {
            // DocumentType
            // parent has a doctype child, child is non-null and an element is preceding child, or child is null and parent has an element child.
            if (has_child_of_type<DocumentType>() || (child && child->has_preceding_node_of_type_in_tree_order<Element>()) || (!child && has_child_of_type<Element>()))
                return WebIDL::HierarchyRequestError::create(realm(), "Invalid node type for insertion");
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
        node->queue_tree_mutation_record({}, nodes, nullptr, nullptr);
    }

    // 5. If child is non-null, then:
    if (child) {
        // 1. For each live range whose start node is parent and start offset is greater than child’s index, increase its start offset by count.
        for (auto& range : Range::live_ranges()) {
            if (range->start_container() == this && range->start_offset() > child->index())
                MUST(range->set_start(*range->start_container(), range->start_offset() + count));
        }

        // 2. For each live range whose end node is parent and end offset is greater than child’s index, increase its end offset by count.
        for (auto& range : Range::live_ranges()) {
            if (range->end_container() == this && range->end_offset() > child->index())
                MUST(range->set_end(*range->end_container(), range->end_offset() + count));
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

        // 7. For each shadow-including inclusive descendant inclusiveDescendant of node, in shadow-including tree order:
        node_to_insert->for_each_shadow_including_inclusive_descendant([&](Node& inclusive_descendant) {
            // 1. Run the insertion steps with inclusiveDescendant.
            inclusive_descendant.inserted();

            // 2. If inclusiveDescendant is connected, then:
            // NOTE: This is not specified here in the spec, but these steps can only be performed on an element.
            if (inclusive_descendant.is_connected() && is<DOM::Element>(inclusive_descendant)) {
                auto& element = static_cast<DOM::Element&>(inclusive_descendant);

                // 1. If inclusiveDescendant is custom, then enqueue a custom element callback reaction with inclusiveDescendant,
                //    callback name "connectedCallback", and an empty argument list.
                if (element.is_custom()) {
                    JS::MarkedVector<JS::Value> empty_arguments { vm().heap() };
                    element.enqueue_a_custom_element_callback_reaction(HTML::CustomElementReactionNames::connectedCallback, move(empty_arguments));
                }

                // 2. Otherwise, try to upgrade inclusiveDescendant.
                // NOTE: If this successfully upgrades inclusiveDescendant, its connectedCallback will be enqueued automatically during
                //       the upgrade an element algorithm.
                else {
                    element.try_to_upgrade();
                }
            }

            return IterationDecision::Continue;
        });
    }

    // 8. If suppress observers flag is unset, then queue a tree mutation record for parent with nodes, « », previousSibling, and child.
    if (!suppress_observers) {
        queue_tree_mutation_record(move(nodes), {}, previous_sibling.ptr(), child.ptr());
    }

    // 9. Run the children changed steps for parent.
    children_changed();

    // FIXME: This will need to become smarter when we implement the :has() selector.
    invalidate_style();

    document().invalidate_layout();
}

// https://dom.spec.whatwg.org/#concept-node-pre-insert
WebIDL::ExceptionOr<JS::NonnullGCPtr<Node>> Node::pre_insert(JS::NonnullGCPtr<Node> node, JS::GCPtr<Node> child)
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
WebIDL::ExceptionOr<JS::NonnullGCPtr<Node>> Node::remove_child(JS::NonnullGCPtr<Node> child)
{
    // The removeChild(child) method steps are to return the result of pre-removing child from this.
    return pre_remove(child);
}

// https://dom.spec.whatwg.org/#concept-node-pre-remove
WebIDL::ExceptionOr<JS::NonnullGCPtr<Node>> Node::pre_remove(JS::NonnullGCPtr<Node> child)
{
    // 1. If child’s parent is not parent, then throw a "NotFoundError" DOMException.
    if (child->parent() != this)
        return WebIDL::NotFoundError::create(realm(), "Child does not belong to this node");

    // 2. Remove child.
    child->remove();

    // 3. Return child.
    return child;
}

// https://dom.spec.whatwg.org/#concept-node-append
WebIDL::ExceptionOr<JS::NonnullGCPtr<Node>> Node::append_child(JS::NonnullGCPtr<Node> node)
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
            MUST(range->set_start(*parent, index));
    }

    // 5. For each live range whose end node is an inclusive descendant of node, set its end to (parent, index).
    for (auto& range : Range::live_ranges()) {
        if (range->end_container()->is_inclusive_descendant_of(*this))
            MUST(range->set_end(*parent, index));
    }

    // 6. For each live range whose start node is parent and start offset is greater than index, decrease its start offset by 1.
    for (auto& range : Range::live_ranges()) {
        if (range->start_container() == parent && range->start_offset() > index)
            MUST(range->set_start(*range->start_container(), range->start_offset() - 1));
    }

    // 7. For each live range whose end node is parent and end offset is greater than index, decrease its end offset by 1.
    for (auto& range : Range::live_ranges()) {
        if (range->end_container() == parent && range->end_offset() > index)
            MUST(range->set_end(*range->end_container(), range->end_offset() - 1));
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

    // 16. Let isParentConnected be parent’s connected.
    bool is_parent_connected = parent->is_connected();

    // 17. If node is custom and isParentConnected is true, then enqueue a custom element callback reaction with node,
    //     callback name "disconnectedCallback", and an empty argument list.
    // Spec Note: It is intentional for now that custom elements do not get parent passed.
    //            This might change in the future if there is a need.
    if (is<DOM::Element>(*this)) {
        auto& element = static_cast<DOM::Element&>(*this);

        if (element.is_custom() && is_parent_connected) {
            JS::MarkedVector<JS::Value> empty_arguments { vm().heap() };
            element.enqueue_a_custom_element_callback_reaction(HTML::CustomElementReactionNames::disconnectedCallback, move(empty_arguments));
        }
    }

    // 18. For each shadow-including descendant descendant of node, in shadow-including tree order, then:
    for_each_shadow_including_descendant([&](Node& descendant) {
        // 1. Run the removing steps with descendant
        descendant.removed_from(nullptr);

        // 2. If descendant is custom and isParentConnected is true, then enqueue a custom element callback reaction with descendant,
        //    callback name "disconnectedCallback", and an empty argument list.
        if (is<DOM::Element>(descendant)) {
            auto& element = static_cast<DOM::Element&>(descendant);

            if (element.is_custom() && is_parent_connected) {
                JS::MarkedVector<JS::Value> empty_arguments { vm().heap() };
                element.enqueue_a_custom_element_callback_reaction(HTML::CustomElementReactionNames::disconnectedCallback, move(empty_arguments));
            }
        }

        return IterationDecision::Continue;
    });

    // 19. For each inclusive ancestor inclusiveAncestor of parent, and then for each registered of inclusiveAncestor’s registered observer list,
    //     if registered’s options["subtree"] is true, then append a new transient registered observer
    //     whose observer is registered’s observer, options is registered’s options, and source is registered to node’s registered observer list.
    for (auto* inclusive_ancestor = parent; inclusive_ancestor; inclusive_ancestor = inclusive_ancestor->parent()) {
        for (auto& registered : inclusive_ancestor->m_registered_observer_list) {
            if (registered->options().subtree) {
                auto transient_observer = TransientRegisteredObserver::create(registered->observer(), registered->options(), registered);
                m_registered_observer_list.append(move(transient_observer));
            }
        }
    }

    // 20. If suppress observers flag is unset, then queue a tree mutation record for parent with « », « node », oldPreviousSibling, and oldNextSibling.
    if (!suppress_observers) {
        parent->queue_tree_mutation_record({}, { *this }, old_previous_sibling.ptr(), old_next_sibling.ptr());
    }

    // 21. Run the children changed steps for parent.
    parent->children_changed();

    // Since the tree structure has changed, we need to invalidate both style and layout.
    // In the future, we should find a way to only invalidate the parts that actually need it.
    document().invalidate_style();
    document().invalidate_layout();
}

// https://dom.spec.whatwg.org/#concept-node-replace
WebIDL::ExceptionOr<JS::NonnullGCPtr<Node>> Node::replace_child(JS::NonnullGCPtr<Node> node, JS::NonnullGCPtr<Node> child)
{
    // If parent is not a Document, DocumentFragment, or Element node, then throw a "HierarchyRequestError" DOMException.
    if (!is<Document>(this) && !is<DocumentFragment>(this) && !is<Element>(this))
        return WebIDL::HierarchyRequestError::create(realm(), "Can only insert into a document, document fragment or element");

    // 2. If node is a host-including inclusive ancestor of parent, then throw a "HierarchyRequestError" DOMException.
    if (node->is_host_including_inclusive_ancestor_of(*this))
        return WebIDL::HierarchyRequestError::create(realm(), "New node is an ancestor of this node");

    // 3. If child’s parent is not parent, then throw a "NotFoundError" DOMException.
    if (child->parent() != this)
        return WebIDL::NotFoundError::create(realm(), "This node is not the parent of the given child");

    // FIXME: All the following "Invalid node type for insertion" messages could be more descriptive.

    // 4. If node is not a DocumentFragment, DocumentType, Element, or CharacterData node, then throw a "HierarchyRequestError" DOMException.
    if (!is<DocumentFragment>(*node) && !is<DocumentType>(*node) && !is<Element>(*node) && !is<Text>(*node) && !is<Comment>(*node) && !is<ProcessingInstruction>(*node))
        return WebIDL::HierarchyRequestError::create(realm(), "Invalid node type for insertion");

    // 5. If either node is a Text node and parent is a document, or node is a doctype and parent is not a document, then throw a "HierarchyRequestError" DOMException.
    if ((is<Text>(*node) && is<Document>(this)) || (is<DocumentType>(*node) && !is<Document>(this)))
        return WebIDL::HierarchyRequestError::create(realm(), "Invalid node type for insertion");

    // If parent is a document, and any of the statements below, switched on the interface node implements, are true, then throw a "HierarchyRequestError" DOMException.
    if (is<Document>(this)) {
        // DocumentFragment
        if (is<DocumentFragment>(*node)) {
            // If node has more than one element child or has a Text node child.
            // Otherwise, if node has one element child and either parent has an element child that is not child or a doctype is following child.
            auto node_element_child_count = verify_cast<DocumentFragment>(*node).child_element_count();
            if ((node_element_child_count > 1 || node->has_child_of_type<Text>())
                || (node_element_child_count == 1 && (first_child_of_type<Element>() != child || child->has_following_node_of_type_in_tree_order<DocumentType>()))) {
                return WebIDL::HierarchyRequestError::create(realm(), "Invalid node type for insertion");
            }
        } else if (is<Element>(*node)) {
            // Element
            // parent has an element child that is not child or a doctype is following child.
            if (first_child_of_type<Element>() != child || child->has_following_node_of_type_in_tree_order<DocumentType>())
                return WebIDL::HierarchyRequestError::create(realm(), "Invalid node type for insertion");
        } else if (is<DocumentType>(*node)) {
            // DocumentType
            // parent has a doctype child that is not child, or an element is preceding child.
            if (first_child_of_type<DocumentType>() != node || child->has_preceding_node_of_type_in_tree_order<Element>())
                return WebIDL::HierarchyRequestError::create(realm(), "Invalid node type for insertion");
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
    queue_tree_mutation_record(move(nodes), move(removed_nodes), previous_sibling.ptr(), reference_child.ptr());

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
        auto element_copy = DOM::create_element(*document, element.local_name(), element.namespace_(), element.prefix(), element.is_value(), false).release_value_but_fixme_should_propagate_errors();

        // 2. For each attribute in node’s attribute list:
        element.for_each_attribute([&](auto& name, auto& value) {
            // 1. Let copyAttribute be a clone of attribute.
            // 2. Append copyAttribute to copy.
            MUST(element_copy->set_attribute(name, value));
        });
        copy = move(element_copy);

    }
    // 3. Otherwise, let copy be a node that implements the same interfaces as node, and fulfills these additional requirements, switching on the interface node implements:
    else if (is<Document>(this)) {
        // Document
        auto document_ = verify_cast<Document>(this);
        auto document_copy = Document::create(this->realm(), document_->url()).release_value_but_fixme_should_propagate_errors();

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
        auto document_type_copy = heap().allocate<DocumentType>(realm(), *document).release_allocated_value_but_fixme_should_propagate_errors();

        // Set copy’s name, public ID, and system ID to those of node.
        document_type_copy->set_name(document_type->name());
        document_type_copy->set_public_id(document_type->public_id());
        document_type_copy->set_system_id(document_type->system_id());
        copy = move(document_type_copy);
    } else if (is<Attr>(this)) {
        // Attr
        // Set copy’s namespace, namespace prefix, local name, and value to those of node.
        auto& attr = static_cast<Attr&>(*this);
        copy = attr.clone(*document);
    } else if (is<Text>(this)) {
        // Text
        auto text = verify_cast<Text>(this);

        // Set copy’s data to that of node.
        auto text_copy = heap().allocate<Text>(realm(), *document, text->data()).release_allocated_value_but_fixme_should_propagate_errors();
        copy = move(text_copy);
    } else if (is<Comment>(this)) {
        // Comment
        auto comment = verify_cast<Comment>(this);

        // Set copy’s data to that of node.
        auto comment_copy = heap().allocate<Comment>(realm(), *document, comment->data()).release_allocated_value_but_fixme_should_propagate_errors();
        copy = move(comment_copy);
    } else if (is<ProcessingInstruction>(this)) {
        // ProcessingInstruction
        auto processing_instruction = verify_cast<ProcessingInstruction>(this);

        // Set copy’s target and data to those of node.
        auto processing_instruction_copy = heap().allocate<ProcessingInstruction>(realm(), *document, processing_instruction->data(), processing_instruction->target()).release_allocated_value_but_fixme_should_propagate_errors();
        copy = processing_instruction_copy;
    }
    // Otherwise, Do nothing.
    else if (is<DocumentFragment>(this)) {
        copy = heap().allocate<DocumentFragment>(realm(), *document).release_allocated_value_but_fixme_should_propagate_errors();
    }

    // FIXME: 4. Set copy’s node document and document to copy, if copy is a document, and set copy’s node document to document otherwise.

    // 5. Run any cloning steps defined for node in other applicable specifications and pass copy, node, document and the clone children flag if set, as parameters.
    cloned(*copy, clone_children);

    // 6. If the clone children flag is set, clone all the children of node and append them to copy, with document as specified and the clone children flag being set.
    if (clone_children) {
        for_each_child([&](auto& child) {
            MUST(copy->append_child(child.clone_node(document, true)));
        });
    }

    // 7. Return copy.
    return *copy;
}

// https://dom.spec.whatwg.org/#dom-node-clonenode
WebIDL::ExceptionOr<JS::NonnullGCPtr<Node>> Node::clone_node_binding(bool deep)
{
    // 1. If this is a shadow root, then throw a "NotSupportedError" DOMException.
    if (is<ShadowRoot>(*this))
        return WebIDL::NotSupportedError::create(realm(), "Cannot clone shadow root");

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

void Node::set_layout_node(Badge<Layout::Node>, JS::NonnullGCPtr<Layout::Node> layout_node)
{
    m_layout_node = layout_node;
}

void Node::detach_layout_node(Badge<Layout::TreeBuilder>)
{
    m_layout_node = nullptr;
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
        return static_cast<ShadowRoot&>(*this).host();
    return verify_cast<ParentNode>(parent());
}

Element* Node::parent_or_shadow_host_element()
{
    if (is<ShadowRoot>(*this))
        return static_cast<ShadowRoot&>(*this).host();
    if (!parent())
        return nullptr;
    if (is<Element>(*parent()))
        return static_cast<Element*>(parent());
    if (is<ShadowRoot>(*parent()))
        return static_cast<ShadowRoot&>(*parent()).host();
    return nullptr;
}

JS::NonnullGCPtr<NodeList> Node::child_nodes()
{
    if (!m_child_nodes) {
        m_child_nodes = LiveNodeList::create(realm(), *this, LiveNodeList::Scope::Children, [](auto&) {
            return true;
        }).release_value_but_fixme_should_propagate_errors();
    }
    return *m_child_nodes;
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
    // 1. If this is other, then return zero.
    if (this == other.ptr())
        return DOCUMENT_POSITION_EQUAL;

    // 2. Let node1 be other and node2 be this.
    Node* node1 = other.ptr();
    Node* node2 = this;

    // 3. Let attr1 and attr2 be null.
    Attr* attr1 = nullptr;
    Attr* attr2 = nullptr;

    // 4. If node1 is an attribute, then set attr1 to node1 and node1 to attr1’s element.
    if (is<Attr>(node1)) {
        attr1 = verify_cast<Attr>(node1);
        node1 = const_cast<Element*>(attr1->owner_element());
    }

    // 5. If node2 is an attribute, then:
    if (is<Attr>(node2)) {
        // 1. Set attr2 to node2 and node2 to attr2’s element.
        attr2 = verify_cast<Attr>(node2);
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

    Vector<Node*> node1_ancestors;
    for (auto* node = node1; node; node = node->parent())
        node1_ancestors.append(node);

    Vector<Node*> node2_ancestors;
    for (auto* node = node2; node; node = node->parent())
        node2_ancestors.append(node);

    auto it_node1_ancestors = node1_ancestors.rbegin();
    auto it_node2_ancestors = node2_ancestors.rbegin();
    // Walk ancestor chains of both nodes starting from root
    while (it_node1_ancestors != node1_ancestors.rend() && it_node2_ancestors != node2_ancestors.rend()) {
        auto* ancestor1 = *it_node1_ancestors;
        auto* ancestor2 = *it_node2_ancestors;

        // If ancestors of nodes at the same level in the tree are different then preceding node is the one with lower sibling position
        if (ancestor1 != ancestor2) {
            auto* node = ancestor1;
            while (node) {
                if (node == ancestor2)
                    return DOCUMENT_POSITION_PRECEDING;
                node = node->next_sibling();
            }
            return DOCUMENT_POSITION_FOLLOWING;
        }

        it_node1_ancestors++;
        it_node2_ancestors++;
    }

    // NOTE: If nodes in ancestors chains are the same but one chain is longer, then one node is ancestor of another.
    //       The node with shorter ancestors chain is the ancestor.
    //       The node with longer ancestors chain is the descendant.

    // 7. If node1 is an ancestor of node2 and attr1 is null, or node1 is node2 and attr2 is non-null, then return the result of adding DOCUMENT_POSITION_CONTAINS to DOCUMENT_POSITION_PRECEDING.
    if ((node1_ancestors.size() < node2_ancestors.size() && !attr1) || (node1 == node2 && attr2))
        return DOCUMENT_POSITION_CONTAINS | DOCUMENT_POSITION_PRECEDING;

    // 8. If node1 is a descendant of node2 and attr2 is null, or node1 is node2 and attr1 is non-null, then return the result of adding DOCUMENT_POSITION_CONTAINED_BY to DOCUMENT_POSITION_FOLLOWING.
    if ((node1_ancestors.size() > node2_ancestors.size() && !attr2) || (node1 == node2 && attr1))
        return DOCUMENT_POSITION_CONTAINED_BY | DOCUMENT_POSITION_FOLLOWING;

    // 9. If node1 is preceding node2, then return DOCUMENT_POSITION_PRECEDING.
    if (node1_ancestors.size() < node2_ancestors.size())
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
    if (auto parent = layout_node()->parent(); parent && parent->is_anonymous())
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

        if (element->is_navigable_container()) {
            auto const* container = static_cast<HTML::NavigableContainer const*>(element);
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
    } else if (is_shadow_root()) {
        MUST(object.add("type"sv, "shadow-root"));
        MUST(object.add("mode"sv, static_cast<DOM::ShadowRoot const&>(*this).mode() == Bindings::ShadowRootMode::Open ? "open"sv : "closed"sv));
    }

    MUST((object.add("visible"sv, !!layout_node())));

    if (has_child_nodes() || (is_element() && static_cast<DOM::Element const*>(this)->is_shadow_host())) {
        auto children = MUST(object.add_array("children"sv));
        auto add_child = [&children](DOM::Node const& child) {
            if (child.is_uninteresting_whitespace_node())
                return;
            JsonObjectSerializer<StringBuilder> child_object = MUST(children.add_object());
            child.serialize_tree_as_json(child_object);
            MUST(child_object.finish());
        };
        for_each_child(add_child);

        if (is_element()) {
            auto const* element = static_cast<DOM::Element const*>(this);

            // Pseudo-elements don't have DOM nodes,so we have to add them separately.
            element->serialize_pseudo_elements_as_json(children);

            if (element->is_shadow_host())
                add_child(*element->shadow_root_internal());
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
    if (!added_nodes.is_empty() || !removed_nodes.is_empty()) {
        queue_tree_mutation_record(move(added_nodes), move(removed_nodes), nullptr, nullptr);
    }
}

// https://dom.spec.whatwg.org/#string-replace-all
void Node::string_replace_all(DeprecatedString const& string)
{
    // 1. Let node be null.
    JS::GCPtr<Node> node;

    // 2. If string is not the empty string, then set node to a new Text node whose data is string and node document is parent’s node document.
    if (!string.is_empty())
        node = heap().allocate<Text>(realm(), document(), string).release_allocated_value_but_fixme_should_propagate_errors();

    // 3. Replace all with node within parent.
    replace_all(node);
}

// https://w3c.github.io/DOM-Parsing/#dfn-fragment-serializing-algorithm
WebIDL::ExceptionOr<DeprecatedString> Node::serialize_fragment(DOMParsing::RequireWellFormed require_well_formed) const
{
    // 1. Let context document be the value of node's node document.
    auto const& context_document = document();

    // 2. If context document is an HTML document, return an HTML serialization of node.
    if (context_document.is_html_document())
        return HTML::HTMLParser::serialize_html_fragment(*this);

    // 3. Otherwise, context document is an XML document; return an XML serialization of node passing the flag require well-formed.
    return DOMParsing::serialize_node_to_xml_string(*this, require_well_formed);
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
    case (u16)NodeType::ATTRIBUTE_NODE: {
        // Its namespace, local name, and value.
        auto& this_attr = verify_cast<Attr>(*this);
        auto& other_attr = verify_cast<Attr>(*other_node);
        if (this_attr.namespace_uri() != other_attr.namespace_uri())
            return false;
        if (this_attr.local_name() != other_attr.local_name())
            return false;
        if (this_attr.value() != other_attr.value())
            return false;
        break;
    }
    case (u16)NodeType::PROCESSING_INSTRUCTION_NODE: {
        // Its target and data.
        auto& this_processing_instruction = verify_cast<ProcessingInstruction>(*this);
        auto& other_processing_instruction = verify_cast<ProcessingInstruction>(*other_node);
        if (this_processing_instruction.target() != other_processing_instruction.target())
            return false;
        if (this_processing_instruction.data() != other_processing_instruction.data())
            return false;
        break;
    }
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

DeprecatedString Node::debug_description() const
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
    return builder.to_deprecated_string();
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

Painting::PaintableBox const* Node::paintable_box() const
{
    if (!layout_node())
        return nullptr;
    if (!layout_node()->is_box())
        return nullptr;
    return static_cast<Layout::Box const&>(*layout_node()).paintable_box();
}

Painting::PaintableBox* Node::paintable_box()
{
    if (!layout_node())
        return nullptr;
    if (!layout_node()->is_box())
        return nullptr;
    return static_cast<Layout::Box&>(*layout_node()).paintable_box();
}

// https://dom.spec.whatwg.org/#queue-a-mutation-record
void Node::queue_mutation_record(FlyString const& type, DeprecatedString attribute_name, DeprecatedString attribute_namespace, DeprecatedString old_value, Vector<JS::Handle<Node>> added_nodes, Vector<JS::Handle<Node>> removed_nodes, Node* previous_sibling, Node* next_sibling) const
{
    // NOTE: We defer garbage collection until the end of the scope, since we can't safely use MutationObserver* as a hashmap key otherwise.
    // FIXME: This is a total hack.
    JS::DeferGC defer_gc(heap());

    // 1. Let interestedObservers be an empty map.
    // mutationObserver -> mappedOldValue
    OrderedHashMap<MutationObserver*, DeprecatedString> interested_observers;

    // 2. Let nodes be the inclusive ancestors of target.
    Vector<JS::Handle<Node const>> nodes;
    nodes.append(JS::make_handle(*this));

    for (auto* parent_node = parent(); parent_node; parent_node = parent_node->parent())
        nodes.append(JS::make_handle(*parent_node));

    // 3. For each node in nodes, and then for each registered of node’s registered observer list:
    for (auto& node : nodes) {
        for (auto& registered_observer : node->m_registered_observer_list) {
            // 1. Let options be registered’s options.
            auto& options = registered_observer->options();

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
                auto mutation_observer = registered_observer->observer();

                // 2. If interestedObservers[mo] does not exist, then set interestedObservers[mo] to null.
                if (!interested_observers.contains(mutation_observer))
                    interested_observers.set(mutation_observer, {});

                // 3. If either type is "attributes" and options["attributeOldValue"] is true, or type is "characterData" and options["characterDataOldValue"] is true, then set interestedObservers[mo] to oldValue.
                if ((type == MutationType::attributes && options.attribute_old_value.has_value() && options.attribute_old_value.value()) || (type == MutationType::characterData && options.character_data_old_value.has_value() && options.character_data_old_value.value()))
                    interested_observers.set(mutation_observer, old_value);
            }
        }
    }

    // OPTIMIZATION: If there are no interested observers, bail without doing any more work.
    if (interested_observers.is_empty())
        return;

    auto added_nodes_list = StaticNodeList::create(realm(), move(added_nodes)).release_value_but_fixme_should_propagate_errors();
    auto removed_nodes_list = StaticNodeList::create(realm(), move(removed_nodes)).release_value_but_fixme_should_propagate_errors();

    // 4. For each observer → mappedOldValue of interestedObservers:
    for (auto& interested_observer : interested_observers) {
        // 1. Let record be a new MutationRecord object with its type set to type, target set to target, attributeName set to name, attributeNamespace set to namespace, oldValue set to mappedOldValue,
        //    addedNodes set to addedNodes, removedNodes set to removedNodes, previousSibling set to previousSibling, and nextSibling set to nextSibling.
        auto record = MutationRecord::create(realm(), type, *this, added_nodes_list, removed_nodes_list, previous_sibling, next_sibling, attribute_name, attribute_namespace, /* mappedOldValue */ interested_observer.value).release_value_but_fixme_should_propagate_errors();

        // 2. Enqueue record to observer’s record queue.
        interested_observer.key->enqueue_record({}, move(record));
    }

    // 5. Queue a mutation observer microtask.
    Bindings::queue_mutation_observer_microtask(document());
}

// https://dom.spec.whatwg.org/#queue-a-tree-mutation-record
void Node::queue_tree_mutation_record(Vector<JS::Handle<Node>> added_nodes, Vector<JS::Handle<Node>> removed_nodes, Node* previous_sibling, Node* next_sibling)
{
    // 1. Assert: either addedNodes or removedNodes is not empty.
    VERIFY(added_nodes.size() > 0 || removed_nodes.size() > 0);

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

void Node::build_accessibility_tree(AccessibilityTreeNode& parent)
{
    if (is_uninteresting_whitespace_node())
        return;

    if (is_document()) {
        auto* document = static_cast<DOM::Document*>(this);
        auto* document_element = document->document_element();
        if (document_element && document_element->include_in_accessibility_tree()) {
            parent.set_value(document_element);
            if (document_element->has_child_nodes())
                document_element->for_each_child([&parent](DOM::Node& child) {
                    child.build_accessibility_tree(parent);
                });
        }
    } else if (is_element()) {
        auto const* element = static_cast<DOM::Element const*>(this);

        if (is<HTML::HTMLScriptElement>(element) || is<HTML::HTMLStyleElement>(element))
            return;

        if (element->include_in_accessibility_tree()) {
            auto current_node = AccessibilityTreeNode::create(&document(), this).release_value_but_fixme_should_propagate_errors();
            parent.append_child(current_node);
            if (has_child_nodes()) {
                for_each_child([&current_node](DOM::Node& child) {
                    child.build_accessibility_tree(*current_node);
                });
            }
        } else if (has_child_nodes()) {
            for_each_child([&parent](DOM::Node& child) {
                child.build_accessibility_tree(parent);
            });
        }
    } else if (is_text()) {
        parent.append_child(AccessibilityTreeNode::create(&document(), this).release_value_but_fixme_should_propagate_errors());
        if (has_child_nodes()) {
            for_each_child([&parent](DOM::Node& child) {
                child.build_accessibility_tree(parent);
            });
        }
    }
}

// https://www.w3.org/TR/accname-1.2/#mapping_additional_nd_te
ErrorOr<String> Node::name_or_description(NameOrDescription target, Document const& document, HashTable<i32>& visited_nodes) const
{
    // The text alternative for a given element is computed as follows:
    // 1. Set the root node to the given element, the current node to the root node, and the total accumulated text to the empty string (""). If the root node's role prohibits naming, return the empty string ("").
    auto const* root_node = this;
    auto const* current_node = root_node;
    StringBuilder total_accumulated_text;
    visited_nodes.set(id());

    if (is_element()) {
        auto const* element = static_cast<DOM::Element const*>(this);
        // 2. Compute the text alternative for the current node:
        // A. If the current node is hidden and is not directly referenced by aria-labelledby or aria-describedby, nor directly referenced by a native host language text alternative element (e.g. label in HTML) or attribute, return the empty string.
        // FIXME: Check for references
        if (element->aria_hidden() == "true" || !layout_node())
            return String {};
        // B. Otherwise:
        // - if computing a name, and the current node has an aria-labelledby attribute that contains at least one valid IDREF, and the current node is not already part of an aria-labelledby traversal,
        //   process its IDREFs in the order they occur:
        // - or, if computing a description, and the current node has an aria-describedby attribute that contains at least one valid IDREF, and the current node is not already part of an aria-describedby traversal,
        //   process its IDREFs in the order they occur:
        if ((target == NameOrDescription::Name && Node::first_valid_id(element->aria_labelled_by(), document).has_value())
            || (target == NameOrDescription::Description && Node::first_valid_id(element->aria_described_by(), document).has_value())) {

            // i. Set the accumulated text to the empty string.
            total_accumulated_text.clear();

            Vector<StringView> id_list;
            if (target == NameOrDescription::Name) {
                id_list = element->aria_labelled_by().split_view(Infra::is_ascii_whitespace);
            } else {
                id_list = element->aria_described_by().split_view(Infra::is_ascii_whitespace);
            }
            // ii. For each IDREF:
            for (auto const& id_ref : id_list) {
                auto node = document.get_element_by_id(id_ref);
                if (!node)
                    continue;

                if (visited_nodes.contains(node->id()))
                    continue;
                // a. Set the current node to the node referenced by the IDREF.
                current_node = node;
                // b. Compute the text alternative of the current node beginning with step 2. Set the result to that text alternative.
                auto result = TRY(node->name_or_description(target, document, visited_nodes));
                // c. Append the result, with a space, to the accumulated text.
                TRY(Node::append_with_space(total_accumulated_text, result));
            }
            // iii. Return the accumulated text.
            return total_accumulated_text.to_string();
        }
        // C. Otherwise, if computing a name, and if the current node has an aria-label attribute whose value is not the empty string, nor, when trimmed of white space, is not the empty string:
        if (target == NameOrDescription::Name && !element->aria_label().is_empty() && !element->aria_label().trim_whitespace().is_empty()) {
            // TODO: - If traversal of the current node is due to recursion and the current node is an embedded control as defined in step 2E, ignore aria-label and skip to rule 2E.
            // - Otherwise, return the value of aria-label.
            return String::from_deprecated_string(element->aria_label());
        }
        // TODO: D. Otherwise, if the current node's native markup provides an attribute (e.g. title) or element (e.g. HTML label) that defines a text alternative,
        //      return that alternative in the form of a flat string as defined by the host language, unless the element is marked as presentational (role="presentation" or role="none").

        // TODO: E. Otherwise, if the current node is a control embedded within the label (e.g. the label element in HTML or any element directly referenced by aria-labelledby) for another widget, where the user can adjust the embedded
        //          control's value, then include the embedded control as part of the text alternative in the following manner:
        //   - If the embedded control has role textbox, return its value.
        //   - If the embedded control has role menu button, return the text alternative of the button.
        //   - If the embedded control has role combobox or listbox, return the text alternative of the chosen option.
        //   - If the embedded control has role range (e.g., a spinbutton or slider):
        //      - If the aria-valuetext property is present, return its value,
        //      - Otherwise, if the aria-valuenow property is present, return its value,
        //      - Otherwise, use the value as specified by a host language attribute.

        // F. Otherwise, if the current node's role allows name from content, or if the current node is referenced by aria-labelledby, aria-describedby, or is a native host language text alternative element (e.g. label in HTML), or is a descendant of a native host language text alternative element:
        auto role = element->role_or_default();
        if (role.has_value() && ARIA::allows_name_from_content(role.value())) {
            // i. Set the accumulated text to the empty string.
            total_accumulated_text.clear();
            // ii. Check for CSS generated textual content associated with the current node and include it in the accumulated text. The CSS :before and :after pseudo elements [CSS2] can provide textual content for elements that have a content model.
            auto before = element->get_pseudo_element_node(CSS::Selector::PseudoElement::Before);
            auto after = element->get_pseudo_element_node(CSS::Selector::PseudoElement::After);
            // - For :before pseudo elements, User agents MUST prepend CSS textual content, without a space, to the textual content of the current node.
            if (before)
                TRY(Node::prepend_without_space(total_accumulated_text, before->computed_values().content().data));

            // - For :after pseudo elements, User agents MUST append CSS textual content, without a space, to the textual content of the current node.
            if (after)
                TRY(Node::append_without_space(total_accumulated_text, after->computed_values().content().data));

            // iii. For each child node of the current node:
            element->for_each_child([&total_accumulated_text, current_node, target, &document, &visited_nodes](
                                        DOM::Node const& child_node) mutable {
                if (visited_nodes.contains(child_node.id()))
                    return;

                // a. Set the current node to the child node.
                current_node = &child_node;

                // b. Compute the text alternative of the current node beginning with step 2. Set the result to that text alternative.
                auto result = MUST(current_node->name_or_description(target, document, visited_nodes));

                // c. Append the result to the accumulated text.
                total_accumulated_text.append(result);
            });
            // iv. Return the accumulated text.
            return total_accumulated_text.to_string();
            // Important: Each node in the subtree is consulted only once. If text has been collected from a descendant, but is referenced by another IDREF in some descendant node, then that second, or subsequent, reference is not followed. This is done to avoid infinite loops.
        }
    }

    // G. Otherwise, if the current node is a Text node, return its textual contents.
    if (is_text()) {
        auto const* text_node = static_cast<DOM::Text const*>(this);
        return String::from_deprecated_string(text_node->data());
    }
    // TODO: H. Otherwise, if the current node is a descendant of an element whose Accessible Name or Accessible Description is being computed, and contains descendants, proceed to 2F.i.

    // I. Otherwise, if the current node has a Tooltip attribute, return its value.
    // https://www.w3.org/TR/accname-1.2/#dfn-tooltip-attribute
    // Any host language attribute that would result in a user agent generating a tooltip such as in response to a mouse hover in desktop user agents.
    // FIXME: Support SVG tooltips and CSS tooltips
    if (is<HTML::HTMLElement>(this)) {
        auto const* element = static_cast<HTML::HTMLElement const*>(this);
        auto tooltip = element->title();
        if (!tooltip.is_empty() && !tooltip.is_null())
            return String::from_deprecated_string(tooltip);
    }
    // Append the result of each step above, with a space, to the total accumulated text.
    //
    // After all steps are completed, the total accumulated text is used as the accessible name or accessible description of the element that initiated the computation.
    return total_accumulated_text.to_string();
}

// https://www.w3.org/TR/accname-1.2/#mapping_additional_nd_name
ErrorOr<String> Node::accessible_name(Document const& document) const
{
    HashTable<i32> visited_nodes;
    // User agents MUST compute an accessible name using the rules outlined below in the section titled Accessible Name and Description Computation.
    return name_or_description(NameOrDescription::Name, document, visited_nodes);
}

// https://www.w3.org/TR/accname-1.2/#mapping_additional_nd_description
ErrorOr<String> Node::accessible_description(Document const& document) const
{
    // If aria-describedby is present, user agents MUST compute the accessible description by concatenating the text alternatives for elements referenced by an aria-describedby attribute on the current element.
    // The text alternatives for the referenced elements are computed using a number of methods, outlined below in the section titled Accessible Name and Description Computation.
    if (is_element()) {
        HashTable<i32> visited_nodes;
        StringBuilder builder;
        auto const* element = static_cast<Element const*>(this);
        auto id_list = element->aria_described_by().split_view(Infra::is_ascii_whitespace);
        for (auto const& id : id_list) {
            if (auto description_element = document.get_element_by_id(id)) {
                auto description = TRY(
                    description_element->name_or_description(NameOrDescription::Description, document,
                        visited_nodes));
                if (!description.is_empty()) {
                    if (builder.is_empty()) {
                        builder.append(description);
                    } else {
                        builder.append(" "sv);
                        builder.append(description);
                    }
                }
            }
        }
        return builder.to_string();
    }
    return String {};
}

Optional<StringView> Node::first_valid_id(DeprecatedString const& value, Document const& document)
{
    auto id_list = value.split_view(Infra::is_ascii_whitespace);
    for (auto const& id : id_list) {
        if (document.get_element_by_id(id))
            return id;
    }
    return {};
}

// https://www.w3.org/TR/accname-1.2/#mapping_additional_nd_te
ErrorOr<void> Node::append_without_space(StringBuilder x, StringView const& result)
{
    // - If X is empty, copy the result to X.
    // - If X is non-empty, copy the result to the end of X.
    TRY(x.try_append(result));
    return {};
}

// https://www.w3.org/TR/accname-1.2/#mapping_additional_nd_te
ErrorOr<void> Node::append_with_space(StringBuilder x, StringView const& result)
{
    // - If X is empty, copy the result to X.
    if (x.is_empty()) {
        TRY(x.try_append(result));
    } else {
        // - If X is non-empty, add a space to the end of X and then copy the result to X after the space.
        TRY(x.try_append(" "sv));
        TRY(x.try_append(result));
    }
    return {};
}

// https://www.w3.org/TR/accname-1.2/#mapping_additional_nd_te
ErrorOr<void> Node::prepend_without_space(StringBuilder x, StringView const& result)
{
    // - If X is empty, copy the result to X.
    if (x.is_empty()) {
        x.append(result);
    } else {
        // - If X is non-empty, copy the result to the start of X.
        auto temp = TRY(x.to_string());
        x.clear();
        TRY(x.try_append(result));
        TRY(x.try_append(temp));
    }
    return {};
}

// https://www.w3.org/TR/accname-1.2/#mapping_additional_nd_te
ErrorOr<void> Node::prepend_with_space(StringBuilder x, StringView const& result)
{
    // - If X is empty, copy the result to X.
    if (x.is_empty()) {
        TRY(x.try_append(result));
    } else {
        // - If X is non-empty, copy the result to the start of X, and add a space after the copy.
        auto temp = TRY(x.to_string());
        x.clear();
        TRY(x.try_append(result));
        TRY(x.try_append(" "sv));
        TRY(x.try_append(temp));
    }
    return {};
}

}
