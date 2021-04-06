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

#pragma once

#include <AK/Badge.h>
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
    virtual bool dispatch_event(NonnullRefPtr<Event>) final;
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

    // NOTE: This is intended for the JS bindings.
    u16 node_type() const { return (u16)m_type; }

    virtual bool is_editable() const;

    NonnullRefPtr<Node> pre_insert(NonnullRefPtr<Node>, RefPtr<Node>);
    NonnullRefPtr<Node> pre_remove(NonnullRefPtr<Node>);

    NonnullRefPtr<Node> append_child(NonnullRefPtr<Node>);
    void insert_before(NonnullRefPtr<Node> node, RefPtr<Node> child, bool suppress_observers = false);
    void remove(bool suppress_observers = false);
    void remove_all_children(bool suppress_observers = false);

    // NOTE: This is intended for the JS bindings.
    bool has_child_nodes() const { return has_children(); }
    NonnullRefPtrVector<Node> child_nodes() const;

    virtual RefPtr<Layout::Node> create_layout_node();

    virtual FlyString node_name() const = 0;

    virtual String text_content() const;
    void set_text_content(const String&);

    Document& document() { return *m_document; }
    const Document& document() const { return *m_document; }

    const HTML::HTMLAnchorElement* enclosing_link_element() const;
    const HTML::HTMLElement* enclosing_html_element() const;
    const HTML::HTMLElement* enclosing_html_element_with_attribute(const FlyString&) const;

    String child_text_content() const;

    Node* root();
    const Node* root() const
    {
        return const_cast<Node*>(this)->root();
    }

    Node* shadow_including_root();
    const Node* shadow_including_root() const
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
    virtual void adopted_from(const Document&) { }

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

    size_t element_child_count() const;

protected:
    Node(Document&, NodeType);

    Document* m_document { nullptr };
    mutable WeakPtr<Layout::Node> m_layout_node;
    NodeType m_type { NodeType::INVALID };
    bool m_needs_style_update { false };
    bool m_child_needs_style_update { false };
};

}
