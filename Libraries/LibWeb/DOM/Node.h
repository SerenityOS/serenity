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
#include <LibWeb/TreeNode.h>

namespace Web::DOM {

enum class NodeType : unsigned {
    INVALID = 0,
    ELEMENT_NODE = 1,
    TEXT_NODE = 3,
    COMMENT_NODE = 8,
    DOCUMENT_NODE = 9,
    DOCUMENT_TYPE_NODE = 10,
    DOCUMENT_FRAGMENT_NODE = 11,
};

class Node
    : public TreeNode<Node>
    , public EventTarget
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::NodeWrapper;

    using TreeNode<Node>::ref;
    using TreeNode<Node>::unref;

    // ^EventTarget
    virtual void ref_event_target() final { ref(); }
    virtual void unref_event_target() final { unref(); }
    virtual void dispatch_event(NonnullRefPtr<Event>) final;
    virtual Bindings::EventTargetWrapper* create_wrapper(JS::GlobalObject&) override;

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
    virtual bool is_svg_element() const { return false; }

    virtual bool is_editable() const;

    RefPtr<Node> append_child(NonnullRefPtr<Node>, bool notify = true);
    RefPtr<Node> insert_before(NonnullRefPtr<Node> node, RefPtr<Node> child, bool notify = true);
    void remove_all_children();

    virtual RefPtr<LayoutNode> create_layout_node(const CSS::StyleProperties* parent_style);

    virtual FlyString node_name() const = 0;

    virtual String text_content() const;
    void set_text_content(const String&);

    Document& document() { return *m_document; }
    const Document& document() const { return *m_document; }

    const HTML::HTMLAnchorElement* enclosing_link_element() const;
    const HTML::HTMLElement* enclosing_html_element() const;

    String child_text_content() const;

    virtual bool is_html_element() const { return false; }
    virtual bool is_unknown_html_element() const { return false; }

    const Node* root() const;
    bool is_connected() const;

    Node* parent_node() { return parent(); }
    const Node* parent_node() const { return parent(); }

    Element* parent_element();
    const Element* parent_element() const;

    virtual void inserted_into(Node&) { }
    virtual void removed_from(Node&) { }
    virtual void children_changed() { }

    const LayoutNode* layout_node() const { return m_layout_node; }
    LayoutNode* layout_node() { return m_layout_node; }

    void set_layout_node(Badge<LayoutNode>, LayoutNode* layout_node) const { m_layout_node = layout_node; }

    virtual bool is_child_allowed(const Node&) const { return true; }

    bool needs_style_update() const { return m_needs_style_update; }
    void set_needs_style_update(bool value) { m_needs_style_update = value; }

    void invalidate_style();

    bool is_link() const;

    virtual void document_did_attach_to_frame(Frame&) { }
    virtual void document_will_detach_from_frame(Frame&) { }

    void set_document(Badge<Document>, Document&);

protected:
    Node(Document&, NodeType);

    Document* m_document { nullptr };
    mutable LayoutNode* m_layout_node { nullptr };
    NodeType m_type { NodeType::INVALID };
    bool m_needs_style_update { true };
};

}
