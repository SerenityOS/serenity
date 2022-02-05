/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IDAllocator.h>
#include <AK/StringBuilder.h>
#include <LibJS/AST.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibWeb/Bindings/EventWrapper.h>
#include <LibWeb/Bindings/NodeWrapper.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/LiveNodeList.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/ProcessingInstruction.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/HTML/BrowsingContextContainer.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Origin.h>

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

Node::Node(Document& document, NodeType type)
    : EventTarget(static_cast<Bindings::ScriptExecutionContext&>(document))
    , m_document(&document)
    , m_type(type)
    , m_id(allocate_node_id(this))
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

    deallocate_node_id(m_id);
}

const HTML::HTMLAnchorElement* Node::enclosing_link_element() const
{
    for (auto* node = this; node; node = node->parent()) {
        if (is<HTML::HTMLAnchorElement>(*node) && verify_cast<HTML::HTMLAnchorElement>(*node).has_attribute(HTML::AttributeNames::href))
            return verify_cast<HTML::HTMLAnchorElement>(node);
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
    if (is<DocumentFragment>(this) || is<Element>(this))
        return descendant_text_content();
    else if (is<CharacterData>(this))
        return verify_cast<CharacterData>(this)->data();

    // FIXME: Else if this is an Attr node, return this's value.

    return {};
}

// https://dom.spec.whatwg.org/#ref-for-dom-node-textcontent%E2%91%A0
void Node::set_text_content(String const& content)
{
    if (is<DocumentFragment>(this) || is<Element>(this)) {
        string_replace_all(content);
    } else if (is<CharacterData>(this)) {
        // FIXME: CharacterData::set_data is not spec compliant. Make this match the spec when set_data becomes spec compliant.
        //        Do note that this will make this function able to throw an exception.

        auto* character_data_node = verify_cast<CharacterData>(this);
        character_data_node->set_data(content);
    } else {
        // FIXME: Else if this is an Attr node, set an existing attribute value with this and the given value.

        return;
    }

    set_needs_style_update(true);
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
    Node* root = this;
    while (root->parent())
        root = root->parent();
    return *root;
}

// https://dom.spec.whatwg.org/#concept-shadow-including-root
Node& Node::shadow_including_root()
{
    auto& node_root = root();
    if (is<ShadowRoot>(node_root))
        return verify_cast<ShadowRoot>(node_root).host()->shadow_including_root();
    return node_root;
}

// https://dom.spec.whatwg.org/#connected
bool Node::is_connected() const
{
    return shadow_including_root().is_document();
}

Element* Node::parent_element()
{
    if (!parent() || !is<Element>(parent()))
        return nullptr;
    return verify_cast<Element>(parent());
}

const Element* Node::parent_element() const
{
    if (!parent() || !is<Element>(parent()))
        return nullptr;
    return verify_cast<Element>(parent());
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
            auto node_element_child_count = verify_cast<DocumentFragment>(*node).child_element_count();
            if ((node_element_child_count > 1 || node->has_child_of_type<Text>())
                || (node_element_child_count == 1 && (has_child_of_type<Element>() || is<DocumentType>(child.ptr()) || (child && child->has_following_node_of_type_in_tree_order<DocumentType>())))) {
                return DOM::HierarchyRequestError::create("Invalid node type for insertion");
            }
        } else if (is<Element>(*node)) {
            if (has_child_of_type<Element>() || is<DocumentType>(child.ptr()) || (child && child->has_following_node_of_type_in_tree_order<DocumentType>()))
                return DOM::HierarchyRequestError::create("Invalid node type for insertion");
        } else if (is<DocumentType>(*node)) {
            if (has_child_of_type<DocumentType>() || (child && child->has_preceding_node_of_type_in_tree_order<Element>()) || (!child && has_child_of_type<Element>()))
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
        nodes = verify_cast<DocumentFragment>(*node).children_as_vector();
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
        return validity_result.exception();

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

// https://dom.spec.whatwg.org/#concept-node-replace
ExceptionOr<NonnullRefPtr<Node>> Node::replace_child(NonnullRefPtr<Node> node, NonnullRefPtr<Node> child)
{
    // NOTE: This differs slightly from ensure_pre_insertion_validity.
    if (!is<Document>(this) && !is<DocumentFragment>(this) && !is<Element>(this))
        return DOM::HierarchyRequestError::create("Can only insert into a document, document fragment or element");

    if (node->is_host_including_inclusive_ancestor_of(*this))
        return DOM::HierarchyRequestError::create("New node is an ancestor of this node");

    if (child->parent() != this)
        return DOM::NotFoundError::create("This node is not the parent of the given child");

    // FIXME: All the following "Invalid node type for insertion" messages could be more descriptive.

    if (!is<DocumentFragment>(*node) && !is<DocumentType>(*node) && !is<Element>(*node) && !is<Text>(*node) && !is<Comment>(*node) && !is<ProcessingInstruction>(*node))
        return DOM::HierarchyRequestError::create("Invalid node type for insertion");

    if ((is<Text>(*node) && is<Document>(this)) || (is<DocumentType>(*node) && !is<Document>(this)))
        return DOM::HierarchyRequestError::create("Invalid node type for insertion");

    if (is<Document>(this)) {
        if (is<DocumentFragment>(*node)) {
            auto node_element_child_count = verify_cast<DocumentFragment>(*node).child_element_count();
            if ((node_element_child_count > 1 || node->has_child_of_type<Text>())
                || (node_element_child_count == 1 && (first_child_of_type<Element>() != child || child->has_following_node_of_type_in_tree_order<DocumentType>()))) {
                return DOM::HierarchyRequestError::create("Invalid node type for insertion");
            }
        } else if (is<Element>(*node)) {
            if (first_child_of_type<Element>() != child || child->has_following_node_of_type_in_tree_order<DocumentType>())
                return DOM::HierarchyRequestError::create("Invalid node type for insertion");
        } else if (is<DocumentType>(*node)) {
            if (first_child_of_type<DocumentType>() != node || child->has_preceding_node_of_type_in_tree_order<Element>())
                return DOM::HierarchyRequestError::create("Invalid node type for insertion");
        }
    }

    auto reference_child = child->next_sibling();
    if (reference_child == node)
        reference_child = node->next_sibling();

    // FIXME: Let previousSibling be child’s previous sibling. (Currently unused so not included)
    // FIXME: Let removedNodes be the empty set. (Currently unused so not included)

    if (child->parent()) {
        // FIXME: Set removedNodes to « child ».
        child->remove(true);
    }

    // FIXME: Let nodes be node’s children if node is a DocumentFragment node; otherwise « node ». (Currently unused so not included)

    insert_before(node, reference_child, true);

    // FIXME: Queue a tree mutation record for parent with nodes, removedNodes, previousSibling, and referenceChild.

    return child;
}

// https://dom.spec.whatwg.org/#concept-node-clone
NonnullRefPtr<Node> Node::clone_node(Document* document, bool clone_children)
{
    if (!document)
        document = m_document;
    RefPtr<Node> copy;
    if (is<Element>(this)) {
        auto& element = *verify_cast<Element>(this);
        auto element_copy = DOM::create_element(*document, element.local_name(), element.namespace_() /* FIXME: node’s namespace prefix, and node’s is value, with the synchronous custom elements flag unset */);
        element.for_each_attribute([&](auto& name, auto& value) {
            element_copy->set_attribute(name, value);
        });
        copy = move(element_copy);
    } else if (is<Document>(this)) {
        auto document_ = verify_cast<Document>(this);
        auto document_copy = Document::create(document_->url());
        document_copy->set_encoding(document_->encoding());
        document_copy->set_content_type(document_->content_type());
        document_copy->set_origin(document_->origin());
        document_copy->set_quirks_mode(document_->mode());
        // FIXME: Set type ("xml" or "html")
        copy = move(document_copy);
    } else if (is<DocumentType>(this)) {
        auto document_type = verify_cast<DocumentType>(this);
        auto document_type_copy = adopt_ref(*new DocumentType(*document));
        document_type_copy->set_name(document_type->name());
        document_type_copy->set_public_id(document_type->public_id());
        document_type_copy->set_system_id(document_type->system_id());
        copy = move(document_type_copy);
    } else if (is<Text>(this)) {
        auto text = verify_cast<Text>(this);
        auto text_copy = adopt_ref(*new Text(*document, text->data()));
        copy = move(text_copy);
    } else if (is<Comment>(this)) {
        auto comment = verify_cast<Comment>(this);
        auto comment_copy = adopt_ref(*new Comment(*document, comment->data()));
        copy = move(comment_copy);
    } else if (is<ProcessingInstruction>(this)) {
        auto processing_instruction = verify_cast<ProcessingInstruction>(this);
        auto processing_instruction_copy = adopt_ref(*new ProcessingInstruction(*document, processing_instruction->data(), processing_instruction->target()));
        copy = move(processing_instruction_copy);
    } else if (is<DocumentFragment>(this)) {
        auto document_fragment_copy = adopt_ref(*new DocumentFragment(*document));
        copy = move(document_fragment_copy);
    } else {
        dbgln("clone_node() not implemented for NodeType {}", (u16)m_type);
        TODO();
    }
    // FIXME: 4. Set copy’s node document and document to copy, if copy is a document, and set copy’s node document to document otherwise.

    cloned(*copy, clone_children);

    if (clone_children) {
        for_each_child([&](auto& child) {
            copy->append_child(child.clone_node(document, true));
        });
    }
    return copy.release_nonnull();
}

// https://dom.spec.whatwg.org/#dom-node-clonenode
ExceptionOr<NonnullRefPtr<Node>> Node::clone_node_binding(bool deep)
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

JS::Object* Node::create_wrapper(JS::GlobalObject& global_object)
{
    return wrap(global_object, *this);
}

void Node::removed_last_ref()
{
    if (is<Document>(*this)) {
        verify_cast<Document>(*this).removed_last_ref();
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

NonnullRefPtr<NodeList> Node::child_nodes()
{
    // FIXME: This should return the same LiveNodeList object every time,
    //        but that would cause a reference cycle since NodeList refs the root.
    return LiveNodeList::create(*this, [this](auto& node) {
        return is_parent_of(node);
    });
}

NonnullRefPtrVector<Node> Node::children_as_vector() const
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

    if ((node1 == nullptr || node2 == nullptr) || (&node1->root() != &node2->root()))
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
    return is_inclusive_ancestor_of(other) || (is<DocumentFragment>(other.root()) && verify_cast<DocumentFragment>(other.root()).host() && is_inclusive_ancestor_of(*verify_cast<DocumentFragment>(other.root()).host().ptr()));
}

// https://dom.spec.whatwg.org/#dom-node-ownerdocument
RefPtr<Document> Node::owner_document() const
{
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
    object.add("name", node_name().view());
    object.add("id", id());
    if (is_document()) {
        object.add("type", "document");
    } else if (is_element()) {
        object.add("type", "element");

        auto const* element = static_cast<DOM::Element const*>(this);
        if (element->has_attributes()) {
            auto attributes = object.add_object("attributes");
            element->for_each_attribute([&attributes](auto& name, auto& value) {
                attributes.add(name, value);
            });
        }

        if (element->is_browsing_context_container()) {
            auto const* container = static_cast<HTML::BrowsingContextContainer const*>(element);
            if (auto const* content_document = container->content_document()) {
                auto children = object.add_array("children");
                JsonObjectSerializer<StringBuilder> content_document_object = children.add_object();
                content_document->serialize_tree_as_json(content_document_object);
            }
        }
    } else if (is_text()) {
        object.add("type", "text");

        auto text_node = static_cast<DOM::Text const*>(this);
        object.add("text", text_node->data());
    } else if (is_comment()) {
        object.add("type"sv, "comment"sv);
        object.add("data"sv, static_cast<DOM::Comment const&>(*this).data());
    }

    if (has_child_nodes()) {
        auto children = object.add_array("children");
        for_each_child([&children](DOM::Node& child) {
            if (child.is_uninteresting_whitespace_node())
                return;
            JsonObjectSerializer<StringBuilder> child_object = children.add_object();
            child.serialize_tree_as_json(child_object);
        });
    }
}

// https://html.spec.whatwg.org/multipage/webappapis.html#concept-n-noscript
bool Node::is_scripting_disabled() const
{
    // FIXME: or when scripting is disabled for its relevant settings object.
    return !document().browsing_context();
}

// https://dom.spec.whatwg.org/#dom-node-contains
bool Node::contains(RefPtr<Node> other) const
{
    return other && other->is_inclusive_descendant_of(*this);
}

// https://dom.spec.whatwg.org/#concept-shadow-including-descendant
bool Node::is_shadow_including_descendant_of(Node const& other) const
{
    if (is_descendant_of(other))
        return true;

    if (!is<ShadowRoot>(root()))
        return false;

    auto& shadow_root = verify_cast<ShadowRoot>(root());

    // NOTE: While host is nullable because of inheriting from DocumentFragment, shadow roots always have a host.
    return shadow_root.host()->is_shadow_including_inclusive_descendant_of(other);
}

// https://dom.spec.whatwg.org/#concept-shadow-including-inclusive-descendant
bool Node::is_shadow_including_inclusive_descendant_of(Node const& other) const
{
    return &other == this || is_shadow_including_descendant_of(other);
}

// https://dom.spec.whatwg.org/#concept-shadow-including-ancestor
bool Node::is_shadow_including_ancestor_of(Node const& other) const
{
    return other.is_shadow_including_descendant_of(*this);
}

// https://dom.spec.whatwg.org/#concept-shadow-including-inclusive-ancestor
bool Node::is_shadow_including_inclusive_ancestor_of(Node const& other) const
{
    return other.is_shadow_including_inclusive_descendant_of(*this);
}

// https://dom.spec.whatwg.org/#concept-node-replace-all
void Node::replace_all(RefPtr<Node> node)
{
    // FIXME: Let removedNodes be parent’s children. (Current unused so not included)
    // FIXME: Let addedNodes be the empty set. (Currently unused so not included)
    // FIXME: If node is a DocumentFragment node, then set addedNodes to node’s children.
    // FIXME: Otherwise, if node is non-null, set addedNodes to « node ».

    remove_all_children(true);

    if (node)
        insert_before(*node, nullptr, true);

    // FIXME: If either addedNodes or removedNodes is not empty, then queue a tree mutation record for parent with addedNodes, removedNodes, null, and null.
}

// https://dom.spec.whatwg.org/#string-replace-all
void Node::string_replace_all(String const& string)
{
    RefPtr<Node> node;

    if (!string.is_empty())
        node = make_ref_counted<Text>(document(), string);

    replace_all(node);
}

// https://w3c.github.io/DOM-Parsing/#dfn-fragment-serializing-algorithm
String Node::serialize_fragment(/* FIXME: Requires well-formed flag */) const
{
    // FIXME: Let context document be the value of node's node document.

    // FIXME: If context document is an HTML document, return an HTML serialization of node.
    //        (We currently always do this)
    return HTML::HTMLParser::serialize_html_fragment(*this);

    // FIXME: Otherwise, context document is an XML document; return an XML serialization of node passing the flag require well-formed.
}

// https://dom.spec.whatwg.org/#dom-node-issamenode
bool Node::is_same_node(Node const* other_node) const
{
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
NonnullRefPtr<Node> Node::get_root_node(GetRootNodeOptions const& options)
{
    // The getRootNode(options) method steps are to return this’s shadow-including root if options["composed"] is true; otherwise this’s root.
    if (options.composed)
        return shadow_including_root();

    return root();
}

}
