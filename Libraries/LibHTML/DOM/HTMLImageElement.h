#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLImageElement : public HTMLElement {
public:
    HTMLImageElement(Document&, const String& tag_name);
    virtual ~HTMLImageElement() override;

    String alt() const { return attribute("alt"); }
    String src() const { return attribute("src"); }

private:
    virtual RefPtr<LayoutNode> create_layout_node(const StyleResolver&, const StyleProperties* parent_style) const override;
};
