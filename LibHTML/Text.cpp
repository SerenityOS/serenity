#include <LibHTML/Text.h>
#include <LibHTML/LayoutText.h>

Text::Text(const String& data)
    : Node(NodeType::TEXT_NODE)
    , m_data(data)
{
}

Text::~Text()
{
}

RetainPtr<LayoutNode> Text::create_layout_node()
{
    return adopt(*new LayoutText(*this));
}
