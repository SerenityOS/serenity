/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
#include <LibWeb/Origin.h>

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
            auto node_element_child_count = downcast<DocumentFragment>(*node).child_element_count();
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
            TreeNode<Node>::append_child(node_to_insert);
        else
            TreeNode<Node>::insert_before(node_to_insert, child);

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
ExceptionOr<NonnullRefPtr<Node>> Node::pre_insert(NonnullRefPtr<Node> node, RefPtr<Node> child)
{
    auto validity_result = ensure_pre_insertion_validity(node, child);
    if (validity_result.is_exception())
        return NonnullRefPtr<DOMException>(validity_result.exception());

    auto reference_child = child;
    if (reference_child == node)
        reference_child = node->next_sibling();

    insert_before(node, reference_child);
    return node;
}

// https://dom.spec.whatwg.org/#concept-node-pre-remove
ExceptionOr<NonnullRefPtr<Node>> Node::pre_remove(NonnullRefPtr<Node> child)
{
    if (child->parent() != this)
        return DOM::NotFoundError::create("Child does not belong to this node");

    child->remove();

    return child;
}

// https://dom.spec.whatwg.org/#concept-node-append
ExceptionOr<NonnullRefPtr<Node>> Node::append_child(NonnullRefPtr<Node> node)
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

// https://dom.spec.whatwg.org/#concept-node-clone
NonnullRefPtr<Node> Node::clone_node(Document* document, bool clone_children) const
{
    if (!document)
        document = m_document;
    RefPtr<Node> copy;
    if (is<Element>(this)) {
        auto& element = *downcast<Element>(this);
        auto qualified_name = QualifiedName(element.local_name(), element.prefix(), element.namespace_());
        auto element_copy = adopt_ref(*new Element(*document, move(qualified_name)));
        element.for_each_attribute([&](auto& name, auto& value) {
            element_copy->set_attribute(name, value);
        });
        copy = move(element_copy);
    } else if (is<Document>(this)) {
        auto document_ = downcast<Document>(this);
        auto document_copy = Document::create(document_->url());
        document_copy->set_encoding(document_->encoding());
        document_copy->set_content_type(document_->content_type());
        document_copy->set_origin(document_->origin());
        document_copy->set_quirks_mode(document_->mode());
        // FIXME: Set type ("xml" or "html")
        copy = move(document_copy);
    } else if (is<DocumentType>(this)) {
        auto document_type = downcast<DocumentType>(this);
        auto document_type_copy = adopt_ref(*new DocumentType(*document));
        document_type_copy->set_name(document_type->name());
        document_type_copy->set_public_id(document_type->public_id());
        document_type_copy->set_system_id(document_type->system_id());
        copy = move(document_type_copy);
    } else if (is<Text>(this)) {
        auto text = downcast<Text>(this);
        auto text_copy = adopt_ref(*new Text(*document, text->data()));
        copy = move(text_copy);
    } else if (is<Comment>(this)) {
        auto comment = downcast<Comment>(this);
        auto comment_copy = adopt_ref(*new Comment(*document, comment->data()));
        copy = move(comment_copy);
    } else if (is<ProcessingInstruction>(this)) {
        auto processing_instruction = downcast<ProcessingInstruction>(this);
        auto processing_instruction_copy = adopt_ref(*new ProcessingInstruction(*document, processing_instruction->data(), processing_instruction->target()));
        copy = move(processing_instruction_copy);
    } else {
        dbgln("clone_node() not implemented for NodeType {}", (u16)m_type);
        TODO();
    }
    // FIXME: 4. Set copy’s node document and document to copy, if copy is a document, and set copy’s node document to document otherwise.
    // FIXME: 5. Run any cloning steps defined for node in other applicable specifications and pass copy, node, document and the clone children flag if set, as parameters.
    if (clone_children) {
        for_each_child([&](auto& child) {
            copy->append_child(child.clone_node(document, true));
        });
    }
    return copy.release_nonnull();
}

// https://dom.spec.whatwg.org/#dom-node-clonenode
ExceptionOr<NonnullRefPtr<Node>> Node::clone_node_binding(bool deep) const
{
    if (is<ShadowRoot>(*this))
        return NotSupportedError::create("Cannot clone shadow root");
    return clone_node(nullptr, deep);
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

// https://dom.spec.whatwg.org/#dom-node-comparedocumentposition
u16 Node::compare_document_position(RefPtr<Node> other)
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

    if (this == other)
        return DOCUMENT_POSITION_EQUAL;

    Node* node1 = other.ptr();
    Node* node2 = this;

    // FIXME: Once LibWeb supports attribute nodes fix to follow the specification.
    VERIFY(node1->type() != NodeType::ATTRIBUTE_NODE && node2->type() != NodeType::ATTRIBUTE_NODE);

    if ((node1 == nullptr || node2 == nullptr) || (node1->root() != node2->root()))
        return DOCUMENT_POSITION_DISCONNECTED | DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC | (node1 > node2 ? DOCUMENT_POSITION_PRECEDING : DOCUMENT_POSITION_FOLLOWING);

    if (node1->is_ancestor_of(*node2))
        return DOCUMENT_POSITION_CONTAINS | DOCUMENT_POSITION_PRECEDING;

    if (node2->is_ancestor_of(*node1))
        return DOCUMENT_POSITION_CONTAINED_BY | DOCUMENT_POSITION_FOLLOWING;

    if (node1->is_before(*node2))
        return DOCUMENT_POSITION_PRECEDING;
    else
        return DOCUMENT_POSITION_FOLLOWING;
}

// https://dom.spec.whatwg.org/#concept-tree-host-including-inclusive-ancestor
bool Node::is_host_including_inclusive_ancestor_of(const Node& other) const
{
    return is_inclusive_ancestor_of(other) || (is<DocumentFragment>(other.root()) && downcast<DocumentFragment>(other.root())->host() && is_inclusive_ancestor_of(*downcast<DocumentFragment>(other.root())->host().ptr()));
}

// https://dom.spec.whatwg.org/#dom-node-ownerdocument
RefPtr<Document> Node::owner_document() const
{
    if (is_document())
        return nullptr;
    return m_document;
}

}
