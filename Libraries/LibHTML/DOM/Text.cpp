#include <LibHTML/DOM/Text.h>
#include <LibHTML/Layout/LayoutText.h>

Text::Text(Document& document, const String& data)
    : Node(document, NodeType::TEXT_NODE)
    , m_data(data)
{
}

Text::~Text()
{
}
