#pragma once

#include <LibDraw/GraphicsBitmap.h>
#include <LibHTML/DOM/HTMLElement.h>

class HTMLImageElement : public HTMLElement {
public:
    HTMLImageElement(Document&, const String& tag_name);
    virtual ~HTMLImageElement() override;

    virtual void parse_attribute(const String& name, const String& value) override;

    String alt() const { return attribute("alt"); }
    String src() const { return attribute("src"); }
    int preferred_width() const;
    int preferred_height() const;

    const GraphicsBitmap* bitmap() const;

private:
    void load_image(const String& src);

    virtual RefPtr<LayoutNode> create_layout_node(const StyleResolver&, const StyleProperties* parent_style) const override;

    mutable RefPtr<GraphicsBitmap> m_bitmap;
};
