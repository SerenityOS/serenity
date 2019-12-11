#pragma once

#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/ImageDecoder.h>
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
    const ImageDecoder* image_decoder() const { return m_image_decoder; }

private:
    void load_image(const String& src);

    virtual RefPtr<LayoutNode> create_layout_node(const StyleProperties* parent_style) const override;

    RefPtr<ImageDecoder> m_image_decoder;
    mutable RefPtr<GraphicsBitmap> m_bitmap;
    ByteBuffer m_encoded_data;
};
