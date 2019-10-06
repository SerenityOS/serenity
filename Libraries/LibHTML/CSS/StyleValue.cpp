#include <LibHTML/CSS/StyleValue.h>
#include <LibHTML/DOM/Document.h>

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
