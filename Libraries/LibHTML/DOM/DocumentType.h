#pragma once

#include <LibHTML/DOM/Node.h>

class DocumentType final : public Node {
public:
    explicit DocumentType(Document&);
    virtual ~DocumentType() override;

    virtual String tag_name() const override { return "!DOCTYPE"; }
};

template<>
inline bool is<DocumentType>(const Node& node)
{
    return node.type() == NodeType::DOCUMENT_TYPE_NODE;
}
