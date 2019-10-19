#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/PNGLoader.h>
#include <LibHTML/CSS/StyleValue.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/Frame.h>
#include <LibHTML/ResourceLoader.h>

StyleValue::StyleValue(Type type)
    : m_type(type)
{
}

StyleValue::~StyleValue()
{
}

String IdentifierStyleValue::to_string() const
{
    switch (id()) {
    case CSS::ValueID::Invalid:
        return "(invalid)";
    case CSS::ValueID::VendorSpecificLink:
        return "-libhtml-link";
    default:
        ASSERT_NOT_REACHED();
    }
}

Color IdentifierStyleValue::to_color(const Document& document) const
{
    if (id() == CSS::ValueID::VendorSpecificLink)
        return document.link_color();
    return {};
}

ImageStyleValue::ImageStyleValue(const URL& url, Document& document)
    : StyleValue(Type::Image)
    , m_url(url)
    , m_document(document.make_weak_ptr())
{
    NonnullRefPtr<ImageStyleValue> protector(*this);
    ResourceLoader::the().load(url, [this, protector](auto& data) {
        if (!m_document)
            return;
        m_bitmap = load_png_from_memory(data.data(), data.size());
        if (!m_bitmap)
            return;
        // FIXME: Do less than a full repaint if possible?
        m_document->frame()->set_needs_display({});
    });
}
