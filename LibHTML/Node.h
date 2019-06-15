#pragma once

#include <AK/Retained.h>
#include <AK/Vector.h>

enum class NodeType : unsigned {
    INVALID = 0,
    ELEMENT_NODE = 1,
    TEXT_NODE = 3,
    DOCUMENT_NODE = 9,
};

class Node {
public:
    virtual ~Node();

    void retain();
    void release();
    int retain_count() const { return m_retain_count; }

    NodeType type() const { return m_type; }
    bool is_element() const { return type() == NodeType::ELEMENT_NODE; }
    bool is_text() const { return type() == NodeType::TEXT_NODE; }
    bool is_document() const { return type() == NodeType::DOCUMENT_NODE; }
    bool is_parent_node() const { return is_element() || is_document(); }

    Node* next_sibling() { return m_next_sibling; }
    Node* previous_sibling() { return m_previous_sibling; }
    void set_next_sibling(Node* node) { m_next_sibling = node; }
    void set_previous_sibling(Node* node) { m_previous_sibling = node; }

protected:
    explicit Node(NodeType);

    int m_retain_count { 1 };
    NodeType m_type { NodeType::INVALID };
    Vector<Node*> m_children;
    Node* m_next_sibling { nullptr };
    Node* m_previous_sibling { nullptr };
};

