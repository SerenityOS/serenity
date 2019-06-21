#pragma once

#include <AK/Badge.h>
#include <AK/RetainPtr.h>
#include <AK/Vector.h>

enum class NodeType : unsigned {
    INVALID = 0,
    ELEMENT_NODE = 1,
    TEXT_NODE = 3,
    DOCUMENT_NODE = 9,
};

class LayoutNode;
class ParentNode;

class Node {
public:
    virtual ~Node();

    void ref();
    void deref();
    int ref_count() const { return m_retain_count; }

    ParentNode* parent_node() { return m_parent_node; }
    const ParentNode* parent_node() const { return m_parent_node; }

    void set_parent_node(Badge<ParentNode>, ParentNode* parent_node) { m_parent_node = parent_node; }

    NodeType type() const { return m_type; }
    bool is_element() const { return type() == NodeType::ELEMENT_NODE; }
    bool is_text() const { return type() == NodeType::TEXT_NODE; }
    bool is_document() const { return type() == NodeType::DOCUMENT_NODE; }
    bool is_parent_node() const { return is_element() || is_document(); }

    Node* next_sibling() { return m_next_sibling; }
    Node* previous_sibling() { return m_previous_sibling; }
    const Node* next_sibling() const { return m_next_sibling; }
    const Node* previous_sibling() const { return m_previous_sibling; }

    void set_next_sibling(Node* node) { m_next_sibling = node; }
    void set_previous_sibling(Node* node) { m_previous_sibling = node; }

    virtual RetainPtr<LayoutNode> create_layout_node();

    const LayoutNode* layout_node() const { return m_layout_node; }
    LayoutNode* layout_node() { return m_layout_node; }

    void set_layout_node(Retained<LayoutNode>);

protected:
    explicit Node(NodeType);

    int m_retain_count { 1 };
    NodeType m_type { NodeType::INVALID };
    ParentNode* m_parent_node { nullptr };
    Node* m_next_sibling { nullptr };
    Node* m_previous_sibling { nullptr };
    RetainPtr<LayoutNode> m_layout_node;
};
