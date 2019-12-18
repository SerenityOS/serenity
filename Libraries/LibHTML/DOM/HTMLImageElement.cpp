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
    if (name.equals_ignoring_case("src"))
        load_image(value);
}

void HTMLImageElement::load_image(const String& src)
{
    URL src_url = document().complete_url(src);
    ResourceLoader::the().load(src_url, [this, weak_element = make_weak_ptr()](auto data) {
        if (!weak_element) {
            dbg() << "HTMLImageElement: Load completed after element destroyed.";
            return;
        }
        if (data.is_null()) {
            dbg() << "HTMLImageElement: Failed to load " << this->src();
            return;
        }

        m_encoded_data = data;
        m_image_decoder = ImageDecoder::create(m_encoded_data.data(), m_encoded_data.size());
        document().update_layout();
    });
}

int HTMLImageElement::preferred_width() const
{
    bool ok = false;
    int width = attribute("width").to_int(ok);
    if (ok)
        return width;

    if (m_image_decoder)
        return m_image_decoder->width();

    return 0;
}

int HTMLImageElement::preferred_height() const
{
    bool ok = false;
    int height = attribute("height").to_int(ok);
    if (ok)
        return height;

    if (m_image_decoder)
        return m_image_decoder->height();

    return 0;
}

RefPtr<LayoutNode> HTMLImageElement::create_layout_node(const StyleProperties* parent_style) const
{
    auto style = document().style_resolver().resolve_style(*this, parent_style);
    auto display = style->string_or_fallback(CSS::PropertyID::Display, "inline");
    if (display == "none")
        return nullptr;
    return adopt(*new LayoutImage(*this, move(style)));
}

const GraphicsBitmap* HTMLImageElement::bitmap() const
{
    if (!m_image_decoder)
        return nullptr;
    return m_image_decoder->bitmap();
}
