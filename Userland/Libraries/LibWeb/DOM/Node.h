/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/DeprecatedString.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/RefPtr.h>
#include <AK/TypeCasts.h>
#include <AK/Vector.h>
#include <LibWeb/DOM/AccessibilityTreeNode.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/DOMParsing/XMLSerializer.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::DOM {

enum class NodeType : u16 {
    INVALID = 0,
    ELEMENT_NODE = 1,
    ATTRIBUTE_NODE = 2,
    TEXT_NODE = 3,
    CDATA_SECTION_NODE = 4,
    ENTITY_REFERENCE_NODE = 5,
    ENTITY_NODE = 6,
    PROCESSING_INSTRUCTION_NODE = 7,
    COMMENT_NODE = 8,
    DOCUMENT_NODE = 9,
    DOCUMENT_TYPE_NODE = 10,
    DOCUMENT_FRAGMENT_NODE = 11,
    NOTATION_NODE = 12
};

enum class NameOrDescription {
    Name,
    Description
};

struct GetRootNodeOptions {
    bool composed { false };
};

class Node : public EventTarget {
    WEB_PLATFORM_OBJECT(Node, EventTarget);

public:
    ParentNode* parent_or_shadow_host();
    ParentNode const* parent_or_shadow_host() const { return const_cast<Node*>(this)->parent_or_shadow_host(); }

    Element* parent_or_shadow_host_element();
    Element const* parent_or_shadow_host_element() const { return const_cast<Node*>(this)->parent_or_shadow_host_element(); }

    virtual ~Node();

    NodeType type() const { return m_type; }
    bool is_element() const { return type() == NodeType::ELEMENT_NODE; }
    bool is_text() const { return type() == NodeType::TEXT_NODE; }
    bool is_document() const { return type() == NodeType::DOCUMENT_NODE; }
    bool is_document_type() const { return type() == NodeType::DOCUMENT_TYPE_NODE; }
    bool is_comment() const { return type() == NodeType::COMMENT_NODE; }
    bool is_character_data() const { return type() == NodeType::TEXT_NODE || type() == NodeType::COMMENT_NODE; }
    bool is_document_fragment() const { return type() == NodeType::DOCUMENT_FRAGMENT_NODE; }
    bool is_parent_node() const { return is_element() || is_document() || is_document_fragment(); }
    bool is_slottable() const { return is_element() || is_text(); }
    bool is_attribute() const { return type() == NodeType::ATTRIBUTE_NODE; }
    bool is_cdata_section() const { return type() == NodeType::CDATA_SECTION_NODE; }
    virtual bool is_shadow_root() const { return false; }

    virtual bool requires_svg_container() const { return false; }
    virtual bool is_svg_container() const { return false; }
    virtual bool is_svg_svg_element() const { return false; }

    bool in_a_document_tree() const;

    // NOTE: This is intended for the JS bindings.
    u16 node_type() const { return (u16)m_type; }

    virtual bool is_editable() const;

    virtual bool is_html_element() const { return false; }
    virtual bool is_html_html_element() const { return false; }
    virtual bool is_html_anchor_element() const { return false; }
    virtual bool is_html_base_element() const { return false; }
    virtual bool is_html_template_element() const { return false; }
    virtual bool is_browsing_context_container() const { return false; }

    WebIDL::ExceptionOr<JS::NonnullGCPtr<Node>> pre_insert(JS::NonnullGCPtr<Node>, JS::GCPtr<Node>);
    WebIDL::ExceptionOr<JS::NonnullGCPtr<Node>> pre_remove(JS::NonnullGCPtr<Node>);

    WebIDL::ExceptionOr<JS::NonnullGCPtr<Node>> append_child(JS::NonnullGCPtr<Node>);
    WebIDL::ExceptionOr<JS::NonnullGCPtr<Node>> remove_child(JS::NonnullGCPtr<Node>);

    void insert_before(JS::NonnullGCPtr<Node> node, JS::GCPtr<Node> child, bool suppress_observers = false);
    void remove(bool suppress_observers = false);
    void remove_all_children(bool suppress_observers = false);
    u16 compare_document_position(JS::GCPtr<Node> other);

    WebIDL::ExceptionOr<JS::NonnullGCPtr<Node>> replace_child(JS::NonnullGCPtr<Node> node, JS::NonnullGCPtr<Node> child);

    JS::NonnullGCPtr<Node> clone_node(Document* document = nullptr, bool clone_children = false);
    WebIDL::ExceptionOr<JS::NonnullGCPtr<Node>> clone_node_binding(bool deep);

    // NOTE: This is intended for the JS bindings.
    bool has_child_nodes() const { return has_children(); }
    JS::NonnullGCPtr<NodeList> child_nodes();
    Vector<JS::Handle<Node>> children_as_vector() const;

    virtual DeprecatedFlyString node_name() const = 0;

    DeprecatedString base_uri() const;

    DeprecatedString descendant_text_content() const;
    DeprecatedString text_content() const;
    void set_text_content(DeprecatedString const&);

    DeprecatedString node_value() const;
    void set_node_value(DeprecatedString const&);

    Document& document() { return *m_document; }
    Document const& document() const { return *m_document; }

    JS::GCPtr<Document> owner_document() const;

    const HTML::HTMLAnchorElement* enclosing_link_element() const;
    const HTML::HTMLElement* enclosing_html_element() const;
    const HTML::HTMLElement* enclosing_html_element_with_attribute(DeprecatedFlyString const&) const;

    DeprecatedString child_text_content() const;

    Node& root();
    Node const& root() const
    {
        return const_cast<Node*>(this)->root();
    }

    Node& shadow_including_root();
    Node const& shadow_including_root() const
    {
        return const_cast<Node*>(this)->shadow_including_root();
    }

    bool is_connected() const;

    Node* parent_node() { return parent(); }
    Node const* parent_node() const { return parent(); }

    Element* parent_element();
    Element const* parent_element() const;

    virtual void inserted();
    virtual void removed_from(Node*) { }
    virtual void children_changed() { }
    virtual void adopted_from(Document&) { }
    virtual void cloned(Node&, bool) {};

    Layout::Node const* layout_node() const { return m_layout_node; }
    Layout::Node* layout_node() { return m_layout_node; }

    Painting::PaintableBox const* paint_box() const;
    Painting::Paintable const* paintable() const;

    void set_layout_node(Badge<Layout::Node>, JS::NonnullGCPtr<Layout::Node>);
    void detach_layout_node(Badge<DOM::Document>);

    virtual bool is_child_allowed(Node const&) const { return true; }

    bool needs_style_update() const { return m_needs_style_update; }
    void set_needs_style_update(bool);

    bool child_needs_style_update() const { return m_child_needs_style_update; }
    void set_child_needs_style_update(bool b) { m_child_needs_style_update = b; }

    void invalidate_style();

    void set_document(Badge<Document>, Document&);

    virtual EventTarget* get_parent(Event const&) override;

    template<typename T>
    bool fast_is() const = delete;

    WebIDL::ExceptionOr<void> ensure_pre_insertion_validity(JS::NonnullGCPtr<Node> node, JS::GCPtr<Node> child) const;

    bool is_host_including_inclusive_ancestor_of(Node const&) const;

    bool is_scripting_enabled() const;
    bool is_scripting_disabled() const;

    bool contains(JS::GCPtr<Node>) const;

    // Used for dumping the DOM Tree
    void serialize_tree_as_json(JsonObjectSerializer<StringBuilder>&) const;

    bool is_shadow_including_descendant_of(Node const&) const;
    bool is_shadow_including_inclusive_descendant_of(Node const&) const;
    bool is_shadow_including_ancestor_of(Node const&) const;
    bool is_shadow_including_inclusive_ancestor_of(Node const&) const;

    i32 id() const { return m_id; }
    static Node* from_id(i32 node_id);

    WebIDL::ExceptionOr<DeprecatedString> serialize_fragment(DOMParsing::RequireWellFormed) const;

    void replace_all(JS::GCPtr<Node>);
    void string_replace_all(DeprecatedString const&);

    bool is_same_node(Node const*) const;
    bool is_equal_node(Node const*) const;

    JS::NonnullGCPtr<Node> get_root_node(GetRootNodeOptions const& options = {});

    bool is_uninteresting_whitespace_node() const;

    DeprecatedString debug_description() const;

    size_t length() const;

    auto& registered_observers_list() { return m_registered_observer_list; }
    auto const& registered_observers_list() const { return m_registered_observer_list; }

    void add_registered_observer(RegisteredObserver& registered_observer) { m_registered_observer_list.append(registered_observer); }

    void queue_mutation_record(DeprecatedFlyString const& type, DeprecatedString attribute_name, DeprecatedString attribute_namespace, DeprecatedString old_value, JS::NonnullGCPtr<NodeList> added_nodes, JS::NonnullGCPtr<NodeList> removed_nodes, Node* previous_sibling, Node* next_sibling) const;

    // https://dom.spec.whatwg.org/#concept-shadow-including-descendant
    template<typename Callback>
    IterationDecision for_each_shadow_including_descendant(Callback);

    Node* parent() { return m_parent.ptr(); }
    Node const* parent() const { return m_parent.ptr(); }

    bool has_children() const { return m_first_child; }
    Node* next_sibling() { return m_next_sibling.ptr(); }
    Node* previous_sibling() { return m_previous_sibling.ptr(); }
    Node* first_child() { return m_first_child.ptr(); }
    Node* last_child() { return m_last_child.ptr(); }
    Node const* next_sibling() const { return m_next_sibling.ptr(); }
    Node const* previous_sibling() const { return m_previous_sibling.ptr(); }
    Node const* first_child() const { return m_first_child.ptr(); }
    Node const* last_child() const { return m_last_child.ptr(); }

    size_t child_count() const
    {
        size_t count = 0;
        for (auto* child = first_child(); child; child = child->next_sibling())
            ++count;
        return count;
    }

    Node* child_at_index(int index)
    {
        int count = 0;
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (count == index)
                return child;
            ++count;
        }
        return nullptr;
    }

    Node const* child_at_index(int index) const
    {
        return const_cast<Node*>(this)->child_at_index(index);
    }

    // https://dom.spec.whatwg.org/#concept-tree-index
    size_t index() const
    {
        // The index of an object is its number of preceding siblings, or 0 if it has none.
        size_t index = 0;
        for (auto* node = previous_sibling(); node; node = node->previous_sibling())
            ++index;
        return index;
    }

    Optional<size_t> index_of_child(Node const& search_child)
    {
        VERIFY(search_child.parent() == this);
        size_t index = 0;
        auto* child = first_child();
        VERIFY(child);

        do {
            if (child == &search_child)
                return index;
            index++;
        } while (child && (child = child->next_sibling()));
        return {};
    }

    template<typename ChildType>
    Optional<size_t> index_of_child(Node const& search_child)
    {
        VERIFY(search_child.parent() == this);
        size_t index = 0;
        auto* child = first_child();
        VERIFY(child);

        do {
            if (!is<ChildType>(child))
                continue;
            if (child == &search_child)
                return index;
            index++;
        } while (child && (child = child->next_sibling()));
        return {};
    }

    bool is_ancestor_of(Node const&) const;
    bool is_inclusive_ancestor_of(Node const&) const;
    bool is_descendant_of(Node const&) const;
    bool is_inclusive_descendant_of(Node const&) const;

    bool is_following(Node const&) const;

    Node* next_in_pre_order()
    {
        if (first_child())
            return first_child();
        Node* node;
        if (!(node = next_sibling())) {
            node = parent();
            while (node && !node->next_sibling())
                node = node->parent();
            if (node)
                node = node->next_sibling();
        }
        return node;
    }

    Node* next_in_pre_order(Node const* stay_within)
    {
        if (first_child())
            return first_child();

        Node* node = static_cast<Node*>(this);
        Node* next = nullptr;
        while (!(next = node->next_sibling())) {
            node = node->parent();
            if (!node || node == stay_within)
                return nullptr;
        }
        return next;
    }

    Node const* next_in_pre_order() const
    {
        return const_cast<Node*>(this)->next_in_pre_order();
    }

    Node const* next_in_pre_order(Node const* stay_within) const
    {
        return const_cast<Node*>(this)->next_in_pre_order(stay_within);
    }

    Node* previous_in_pre_order()
    {
        if (auto* node = previous_sibling()) {
            while (node->last_child())
                node = node->last_child();

            return node;
        }

        return parent();
    }

    Node const* previous_in_pre_order() const
    {
        return const_cast<Node*>(this)->previous_in_pre_order();
    }

    bool is_before(Node const& other) const
    {
        if (this == &other)
            return false;
        for (auto* node = this; node; node = node->next_in_pre_order()) {
            if (node == &other)
                return true;
        }
        return false;
    }

    // https://dom.spec.whatwg.org/#concept-tree-preceding (Object A is 'typename U' and Object B is 'this')
    template<typename U>
    bool has_preceding_node_of_type_in_tree_order() const
    {
        for (auto* node = previous_in_pre_order(); node; node = node->previous_in_pre_order()) {
            if (is<U>(node))
                return true;
        }
        return false;
    }

    // https://dom.spec.whatwg.org/#concept-tree-following (Object A is 'typename U' and Object B is 'this')
    template<typename U>
    bool has_following_node_of_type_in_tree_order() const
    {
        for (auto* node = next_in_pre_order(); node; node = node->next_in_pre_order()) {
            if (is<U>(node))
                return true;
        }
        return false;
    }

    template<typename Callback>
    IterationDecision for_each_in_inclusive_subtree(Callback callback) const
    {
        if (callback(static_cast<Node const&>(*this)) == IterationDecision::Break)
            return IterationDecision::Break;
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_inclusive_subtree(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename Callback>
    IterationDecision for_each_in_inclusive_subtree(Callback callback)
    {
        if (callback(static_cast<Node&>(*this)) == IterationDecision::Break)
            return IterationDecision::Break;
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_inclusive_subtree(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename U, typename Callback>
    IterationDecision for_each_in_inclusive_subtree_of_type(Callback callback)
    {
        if (is<U>(static_cast<Node&>(*this))) {
            if (callback(static_cast<U&>(*this)) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->template for_each_in_inclusive_subtree_of_type<U>(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename U, typename Callback>
    IterationDecision for_each_in_inclusive_subtree_of_type(Callback callback) const
    {
        if (is<U>(static_cast<Node const&>(*this))) {
            if (callback(static_cast<U const&>(*this)) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->template for_each_in_inclusive_subtree_of_type<U>(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename Callback>
    IterationDecision for_each_in_subtree(Callback callback) const
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_inclusive_subtree(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename Callback>
    IterationDecision for_each_in_subtree(Callback callback)
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_inclusive_subtree(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename U, typename Callback>
    IterationDecision for_each_in_subtree_of_type(Callback callback)
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->template for_each_in_inclusive_subtree_of_type<U>(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename U, typename Callback>
    IterationDecision for_each_in_subtree_of_type(Callback callback) const
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->template for_each_in_inclusive_subtree_of_type<U>(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename Callback>
    void for_each_child(Callback callback) const
    {
        return const_cast<Node*>(this)->template for_each_child(move(callback));
    }

    template<typename Callback>
    void for_each_child(Callback callback)
    {
        for (auto* node = first_child(); node; node = node->next_sibling())
            callback(*node);
    }

    template<typename U, typename Callback>
    void for_each_child_of_type(Callback callback)
    {
        for (auto* node = first_child(); node; node = node->next_sibling()) {
            if (is<U>(node))
                callback(verify_cast<U>(*node));
        }
    }

    template<typename U, typename Callback>
    void for_each_child_of_type(Callback callback) const
    {
        return const_cast<Node*>(this)->template for_each_child_of_type<U>(move(callback));
    }

    template<typename U>
    U const* next_sibling_of_type() const
    {
        return const_cast<Node*>(this)->template next_sibling_of_type<U>();
    }

    template<typename U>
    inline U* next_sibling_of_type()
    {
        for (auto* sibling = next_sibling(); sibling; sibling = sibling->next_sibling()) {
            if (is<U>(*sibling))
                return &verify_cast<U>(*sibling);
        }
        return nullptr;
    }

    template<typename U>
    U const* previous_sibling_of_type() const
    {
        return const_cast<Node*>(this)->template previous_sibling_of_type<U>();
    }

    template<typename U>
    U* previous_sibling_of_type()
    {
        for (auto* sibling = previous_sibling(); sibling; sibling = sibling->previous_sibling()) {
            if (is<U>(*sibling))
                return &verify_cast<U>(*sibling);
        }
        return nullptr;
    }

    template<typename U>
    U const* first_child_of_type() const
    {
        return const_cast<Node*>(this)->template first_child_of_type<U>();
    }

    template<typename U>
    U const* last_child_of_type() const
    {
        return const_cast<Node*>(this)->template last_child_of_type<U>();
    }

    template<typename U>
    U* first_child_of_type()
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (is<U>(*child))
                return &verify_cast<U>(*child);
        }
        return nullptr;
    }

    template<typename U>
    U* last_child_of_type()
    {
        for (auto* child = last_child(); child; child = child->previous_sibling()) {
            if (is<U>(*child))
                return &verify_cast<U>(*child);
        }
        return nullptr;
    }

    template<typename U>
    bool has_child_of_type() const
    {
        return first_child_of_type<U>() != nullptr;
    }

    template<typename U>
    U const* first_ancestor_of_type() const
    {
        return const_cast<Node*>(this)->template first_ancestor_of_type<U>();
    }

    template<typename U>
    U* first_ancestor_of_type()
    {
        for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
            if (is<U>(*ancestor))
                return &verify_cast<U>(*ancestor);
        }
        return nullptr;
    }

    bool is_parent_of(Node const& other) const
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (&other == child)
                return true;
        }
        return false;
    }

    ErrorOr<String> accessible_name(Document const&) const;
    ErrorOr<String> accessible_description(Document const&) const;

protected:
    Node(JS::Realm&, Document&, NodeType);
    Node(Document&, NodeType);

    virtual void visit_edges(Cell::Visitor&) override;
    virtual void finalize() override;

    JS::GCPtr<Document> m_document;
    JS::GCPtr<Layout::Node> m_layout_node;
    NodeType m_type { NodeType::INVALID };
    bool m_needs_style_update { false };
    bool m_child_needs_style_update { false };

    i32 m_id;

    // https://dom.spec.whatwg.org/#registered-observer-list
    // "Nodes have a strong reference to registered observers in their registered observer list." https://dom.spec.whatwg.org/#garbage-collection
    Vector<RegisteredObserver&> m_registered_observer_list;

    void build_accessibility_tree(AccessibilityTreeNode& parent);

    ErrorOr<String> name_or_description(NameOrDescription, Document const&, HashTable<i32>&) const;

private:
    void queue_tree_mutation_record(JS::NonnullGCPtr<NodeList> added_nodes, JS::NonnullGCPtr<NodeList> removed_nodes, Node* previous_sibling, Node* next_sibling);

    void insert_before_impl(JS::NonnullGCPtr<Node>, JS::GCPtr<Node> child);
    void append_child_impl(JS::NonnullGCPtr<Node>);
    void remove_child_impl(JS::NonnullGCPtr<Node>);

    static Optional<StringView> first_valid_id(DeprecatedString const&, Document const&);
    static ErrorOr<void> append_without_space(StringBuilder, StringView const&);
    static ErrorOr<void> append_with_space(StringBuilder, StringView const&);
    static ErrorOr<void> prepend_without_space(StringBuilder, StringView const&);
    static ErrorOr<void> prepend_with_space(StringBuilder, StringView const&);

    JS::GCPtr<Node> m_parent;
    JS::GCPtr<Node> m_first_child;
    JS::GCPtr<Node> m_last_child;
    JS::GCPtr<Node> m_next_sibling;
    JS::GCPtr<Node> m_previous_sibling;

    JS::GCPtr<NodeList> m_child_nodes;
};

}
