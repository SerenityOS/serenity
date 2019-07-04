#include <LibHTML/DOM/Text.h>
#include <LibHTML/Layout/LayoutText.h>

Text::Text(const String& data)
    : Node(NodeType::TEXT_NODE)
    , m_data(data)
{
}

Text::~Text()
{
}
