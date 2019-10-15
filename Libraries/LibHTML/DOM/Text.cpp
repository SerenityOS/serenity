#include <LibHTML/DOM/Text.h>
#include <LibHTML/Layout/LayoutText.h>

Text::Text(Document& document, const String& data)
    : CharacterData(document, NodeType::TEXT_NODE, data)
{
}

Text::~Text()
{
}

RefPtr<LayoutNode> Text::create_layout_node(const StyleProperties*) const
{
    return adopt(*new LayoutText(*this));
}
