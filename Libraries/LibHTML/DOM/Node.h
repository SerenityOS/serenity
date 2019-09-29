#pragma once

#include <AK/Badge.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibHTML/TreeNode.h>

enum class NodeType : unsigned {
    INVALID = 0,
    ELEMENT_NODE = 1,
    TEXT_NODE = 3,
    DOCUMENT_NODE = 9,
};

class Document;
class HTMLElement;
class HTMLAnchorElement;
class ParentNode;
class LayoutNode;
class StyleResolver;
class StyleProperties;

class Node : public TreeNode<Node> {
public:
    virtual ~Node();

    NodeType type() const { return m_type; }
    bool is_element() const { return type() == NodeType::ELEMENT_NODE; }
    bool is_text() const { return type() == NodeType::TEXT_NODE; }
    bool is_document() const { return type() == NodeType::DOCUMENT_NODE; }
    bool is_parent_node() const { return is_element() || is_document(); }

    RefPtr<LayoutNode> create_layout_node(const StyleResolver&, const StyleProperties* parent_properties) const;
    RefPtr<LayoutNode> create_layout_tree(const StyleResolver&, const StyleProperties* parent_properties) const;

    virtual String tag_name() const = 0;

    virtual String text_content() const;

    Document& document() { return m_document; }
    const Document& document() const { return m_document; }

    const HTMLAnchorElement* enclosing_link_element() const;
    const HTMLElement* enclosing_html_element() const;

    virtual bool is_html_element() const { return false; }

    const Node* first_child_with_tag_name(const StringView& tag_name) const
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->tag_name() == tag_name)
                return child;
        }
        return nullptr;
    }

protected:
    Node(Document&, NodeType);

    Document& m_document;
    NodeType m_type { NodeType::INVALID };
};
