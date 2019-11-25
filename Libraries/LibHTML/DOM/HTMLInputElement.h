#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLInputElement : public HTMLElement {
public:
    HTMLInputElement(Document&, const String& tag_name);
    virtual ~HTMLInputElement() override;

    virtual RefPtr<LayoutNode> create_layout_node(const StyleProperties* parent_style) const override;

    String type() const { return attribute("type"); }
    String value() const { return attribute("value"); }
    String name() const { return attribute("name"); }
};

template<>
inline bool is<HTMLInputElement>(const Node& node)
{
    return is<Element>(node) && to<Element>(node).tag_name().to_lowercase() == "input";
}
