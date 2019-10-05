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

RefPtr<LayoutNode> Text::create_layout_node(const StyleResolver&, const StyleProperties*) const
{
    return adopt(*new LayoutText(*this));
}
