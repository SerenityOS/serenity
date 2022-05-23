/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/RefPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <LibGUI/Model.h>

namespace GUI {

class TreeViewModel final : public Model {
public:
    static NonnullRefPtr<TreeViewModel> create()
    {
        return adopt_ref(*new TreeViewModel());
    }

    virtual ~TreeViewModel() override = default;

    virtual int row_count(ModelIndex const& = {}) const override;
    virtual int column_count(ModelIndex const& = {}) const override { return 1; }
    virtual Variant data(ModelIndex const&, ModelRole role) const override;
    virtual ModelIndex parent_index(ModelIndex const&) const override;
    virtual ModelIndex index(int row, int column, ModelIndex const& parent = {}) const override;

    class Node
        : public RefCounted<Node>
        , public Weakable<Node> {
    public:
        Node(String text, Optional<Icon> icon, Node* parent_node = nullptr)
            : m_text(move(text))
            , m_icon(move(icon))
            , m_parent_node(parent_node)
        {
        }

        virtual ~Node() = default;

        template<typename NodeType = Node, typename... Args>
        NonnullRefPtr<NodeType> add_node(String text, Optional<Icon> icon, Args&&... args) requires(IsBaseOf<Node, NodeType>)
        {
            auto node = adopt_ref(*new NodeType(move(text), move(icon), this, forward<Args>(args)...));
            m_child_nodes.append(*static_cast<Node const*>(node.ptr()));
            return node;
        }

        String const& text() const { return m_text; }
        Optional<Icon> const& icon() const { return m_icon; }

        Node const* parent_node() const { return m_parent_node; }
        Node* parent_node() { return m_parent_node; }

        NonnullRefPtrVector<Node> const& child_nodes() const { return m_child_nodes; }
        NonnullRefPtrVector<Node>& child_nodes() { return m_child_nodes; }

    private:
        String m_text;
        Optional<Icon> m_icon;
        WeakPtr<Node> m_parent_node;
        NonnullRefPtrVector<Node> m_child_nodes;
    };

    NonnullRefPtrVector<Node> const& nodes() const { return m_nodes; }
    NonnullRefPtrVector<Node>& nodes() { return m_nodes; }

    template<typename NodeType = Node, typename... Args>
    NonnullRefPtr<NodeType> add_node(String text, Optional<Icon> icon, Args&&... args) requires(IsBaseOf<Node, NodeType>)
    {
        auto node = adopt_ref(*new NodeType(move(text), move(icon), nullptr, forward<Args>(args)...));
        m_nodes.append(*static_cast<Node const*>(node.ptr()));
        return node;
    }

    Optional<ModelIndex> index_for_node(Node const&, ModelIndex const& parent = {}) const;

private:
    TreeViewModel() = default;

    NonnullRefPtrVector<Node> m_nodes;
};

}
