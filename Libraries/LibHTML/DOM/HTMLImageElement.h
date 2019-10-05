#pragma once

#include <LibDraw/GraphicsBitmap.h>
#include <LibHTML/DOM/HTMLElement.h>

class HTMLImageElement : public HTMLElement {
public:
    HTMLImageElement(Document&, const String& tag_name);
    virtual ~HTMLImageElement() override;

    String alt() const { return attribute("alt"); }
    String src() const { return attribute("src"); }

    const GraphicsBitmap* bitmap() const;

private:
    virtual RefPtr<LayoutNode> create_layout_node(const StyleResolver&, const StyleProperties* parent_style) const override;

    mutable RefPtr<GraphicsBitmap> m_bitmap;
};
