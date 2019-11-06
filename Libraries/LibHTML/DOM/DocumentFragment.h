#pragma once

#include <LibHTML/DOM/ParentNode.h>

class DocumentFragment : public ParentNode {
public:
    DocumentFragment(Document& document)
        : ParentNode(document, NodeType::DOCUMENT_FRAGMENT_NODE)
    {
    }

    virtual String tag_name() const override { return "#document-fragment"; }
};

template<>
inline bool is<DocumentFragment>(const Node& node)
{
    return node.is_document_fragment();
}
