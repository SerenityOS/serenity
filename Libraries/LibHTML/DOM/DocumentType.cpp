#include <LibHTML/DOM/DocumentType.h>

DocumentType::DocumentType(Document& document)
    : Node(document, NodeType::DOCUMENT_TYPE_NODE)
{
}

DocumentType::~DocumentType()
{
}
