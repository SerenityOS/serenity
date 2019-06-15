#include <LibHTML/Text.h>

Text::Text(const String& data)
    : Node(NodeType::TEXT_NODE)
    , m_data(data)
{
}

Text::~Text()
{
}


