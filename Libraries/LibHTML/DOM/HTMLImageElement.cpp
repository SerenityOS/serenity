#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/HTMLImageElement.h>
#include <LibHTML/Layout/LayoutImage.h>

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
    if (src_url.protocol() == "file") {
        m_bitmap = GraphicsBitmap::load_from_file(src_url.path());
    } else {
        // FIXME: Implement! This whole thing should be at a different layer though..
        ASSERT_NOT_REACHED();
    }
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

    auto display_property = style->property("display");
    String display = display_property.has_value() ? display_property.release_value()->to_string() : "inline";

    if (display == "none")
        return nullptr;
    return adopt(*new LayoutImage(*this, move(style)));
}

const GraphicsBitmap* HTMLImageElement::bitmap() const
{
    return m_bitmap;
}
