#include <LibDraw/PNGLoader.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/HTMLImageElement.h>
#include <LibHTML/Layout/LayoutImage.h>
#include <LibHTML/ResourceLoader.h>

HTMLImageElement::HTMLImageElement(Document& document, const String& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLImageElement::~HTMLImageElement()
{
}

void HTMLImageElement::parse_attribute(const String& name, const String& value)
{
    if (name == "src")
        load_image(value);
}

void HTMLImageElement::load_image(const String& src)
{
    URL src_url = document().complete_url(src);
    ResourceLoader::the().load(src_url, [this](auto data) {
        if (data.is_null()) {
            dbg() << "HTMLImageElement: Failed to load " << this->src();
            return;
        }

        m_bitmap = load_png_from_memory(data.data(), data.size());
        document().invalidate_layout();
    });
}

int HTMLImageElement::preferred_width() const
{
    bool ok = false;
    int width = attribute("width").to_int(ok);
    if (ok)
        return width;

    if (m_bitmap)
        return m_bitmap->width();

    return 0;
}

int HTMLImageElement::preferred_height() const
{
    bool ok = false;
    int height = attribute("height").to_int(ok);
    if (ok)
        return height;

    if (m_bitmap)
        return m_bitmap->height();

    return 0;
}

RefPtr<LayoutNode> HTMLImageElement::create_layout_node(const StyleResolver& resolver, const StyleProperties* parent_style) const
{
    auto style = resolver.resolve_style(*this, parent_style);

    auto display_property = style->property(CSS::PropertyID::Display);
    String display = display_property.has_value() ? display_property.release_value()->to_string() : "inline";

    if (display == "none")
        return nullptr;
    return adopt(*new LayoutImage(*this, move(style)));
}

const GraphicsBitmap* HTMLImageElement::bitmap() const
{
    return m_bitmap;
}
