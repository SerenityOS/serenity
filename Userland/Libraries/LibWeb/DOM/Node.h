/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/TypeCasts.h>
#include <AK/Vector.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/TreeNode.h>

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

struct GetRootNodeOptions {
    bool composed { false };
};

class Node
    : public TreeNode<Node>
    , public EventTarget
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::NodeWrapper;

    using TreeNode<Node>::ref;
    using TreeNode<Node>::unref;

    ParentNode* parent_or_shadow_host();
    const ParentNode* parent_or_shadow_host() const { return const_cast<Node*>(this)->parent_or_shadow_host(); }

    // ^EventTarget
    virtual void ref_event_target() final { ref(); }
    virtual void unref_event_target() final { unref(); }
    virtual JS::Object* create_wrapper(JS::GlobalObject&) override;

    virtual ~Node();

    void removed_last_ref();

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

    virtual bool requires_svg_container() const { return false; }
    virtual bool is_svg_container() const { return false; }

    bool in_a_document_tree() const;

    // NOTE: This is intended for the JS bindings.
    u16 node_type() const { return (u16)m_type; }

    virtual bool is_editable() const;

    virtual bool is_html_html_element() const { return false; }
    virtual bool is_html_template_element() const { return false; }
    virtual bool is_browsing_context_container() const { return false; }

    ExceptionOr<NonnullRefPtr<Node>> pre_insert(NonnullRefPtr<Node>, RefPtr<Node>);
    ExceptionOr<NonnullRefPtr<Node>> pre_remove(NonnullRefPtr<Node>);

    ExceptionOr<NonnullRefPtr<Node>> append_child(NonnullRefPtr<Node>);
    void insert_before(NonnullRefPtr<Node> node, RefPtr<Node> child, bool suppress_observers = false);
    void remove(bool suppress_observers = false);
    void remove_all_children(bool suppress_observers = false);
    u16 compare_document_position(RefPtr<Node> other);

    ExceptionOr<NonnullRefPtr<Node>> replace_child(NonnullRefPtr<Node> node, NonnullRefPtr<Node> child);

    NonnullRefPtr<Node> clone_node(Document* document = nullptr, bool clone_children = false);
    ExceptionOr<NonnullRefPtr<Node>> clone_node_binding(bool deep);

    // NOTE: This is intended for the JS bindings.
    bool has_child_nodes() const { return has_children(); }
    NonnullRefPtr<NodeList> child_nodes();
    NonnullRefPtrVector<Node> children_as_vector() const;

    virtual FlyString node_name() const = 0;

    String descendant_text_content() const;
    String text_content() const;
    void set_text_content(String const&);

    Document& document() { return *m_document; }
    const Document& document() const { return *m_document; }

    RefPtr<Document> owner_document() const;

    const HTML::HTMLAnchorElement* enclosing_link_element() const;
    const HTML::HTMLElement* enclosing_html_element() const;
    const HTML::HTMLElement* enclosing_html_element_with_attribute(const FlyString&) const;

    String child_text_content() const;

    Node& root();
    const Node& root() const
    {
        return const_cast<Node*>(this)->root();
    }

    Node& shadow_including_root();
    const Node& shadow_including_root() const
    {
        return const_cast<Node*>(this)->shadow_including_root();
    }

    bool is_connected() const;

    Node* parent_node() { return parent(); }
    const Node* parent_node() const { return parent(); }

    Element* parent_element();
    const Element* parent_element() const;

    virtual void inserted();
    virtual void removed_from(Node*) { }
    virtual void children_changed() { }
    virtual void adopted_from(Document&) { }
    virtual void cloned(Node&, bool) {};

    const Layout::Node* layout_node() const { return m_layout_node; }
    Layout::Node* layout_node() { return m_layout_node; }

    void set_layout_node(Badge<Layout::Node>, Layout::Node*) const;

    virtual bool is_child_allowed(const Node&) const { return true; }

    bool needs_style_update() const { return m_needs_style_update; }
    void set_needs_style_update(bool);

    bool child_needs_style_update() const { return m_child_needs_style_update; }
    void set_child_needs_style_update(bool b) { m_child_needs_style_update = b; }

    void invalidate_style();

    bool is_link() const;

    void set_document(Badge<Document>, Document&);

    virtual EventTarget* get_parent(const Event&) override;

    template<typename T>
    bool fast_is() const = delete;

    ExceptionOr<void> ensure_pre_insertion_validity(NonnullRefPtr<Node> node, RefPtr<Node> child) const;

    bool is_host_including_inclusive_ancestor_of(const Node&) const;

    bool is_scripting_disabled() const;

    bool contains(RefPtr<Node>) const;

    // Used for dumping the DOM Tree
    void serialize_tree_as_json(JsonObjectSerializer<StringBuilder>&) const;

    bool is_shadow_including_descendant_of(Node const&) const;
    bool is_shadow_including_inclusive_descendant_of(Node const&) const;
    bool is_shadow_including_ancestor_of(Node const&) const;
    bool is_shadow_including_inclusive_ancestor_of(Node const&) const;

    i32 id() const { return m_id; }
    static Node* from_id(i32 node_id);

    String serialize_fragment() const;

    void replace_all(RefPtr<Node>);
    void string_replace_all(String const&);

    bool is_same_node(Node const*) const;
    bool is_equal_node(Node const*) const;

    NonnullRefPtr<Node> get_root_node(GetRootNodeOptions const& options = {});

    bool is_uninteresting_whitespace_node() const;

protected:
    Node(Document&, NodeType);

    Document* m_document { nullptr };
    mutable WeakPtr<Layout::Node> m_layout_node;
    NodeType m_type { NodeType::INVALID };
    bool m_needs_style_update { false };
    bool m_child_needs_style_update { false };

    i32 m_id;
};

}
