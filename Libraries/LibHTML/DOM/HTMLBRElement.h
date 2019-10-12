#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLBRElement final : public HTMLElement {
public:
    HTMLBRElement(Document&, const String& tag_name);
    virtual ~HTMLBRElement() override;

    virtual RefPtr<LayoutNode> create_layout_node(const StyleResolver&, const StyleProperties* parent_style) const override;
};

template<>
inline bool is<HTMLBRElement>(const Node& node)
{
    return is<Element>(node) && to<Element>(node).tag_name().to_lowercase() == "br";
}
