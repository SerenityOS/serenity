#pragma once

#include <AK/Badge.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <LibHTML/TreeNode.h>

enum class NodeType : unsigned {
    INVALID = 0,
    ELEMENT_NODE = 1,
    TEXT_NODE = 3,
    DOCUMENT_NODE = 9,
};

class LayoutNode;
class ParentNode;

class Node : public TreeNode<Node> {
public:
    virtual ~Node();

    NodeType type() const { return m_type; }
    bool is_element() const { return type() == NodeType::ELEMENT_NODE; }
    bool is_text() const { return type() == NodeType::TEXT_NODE; }
    bool is_document() const { return type() == NodeType::DOCUMENT_NODE; }
    bool is_parent_node() const { return is_element() || is_document(); }

    virtual RefPtr<LayoutNode> create_layout_node();

    const LayoutNode* layout_node() const { return m_layout_node; }
    LayoutNode* layout_node() { return m_layout_node; }

    void set_layout_node(NonnullRefPtr<LayoutNode>);

protected:
    explicit Node(NodeType);

    NodeType m_type { NodeType::INVALID };
    RefPtr<LayoutNode> m_layout_node;
};
