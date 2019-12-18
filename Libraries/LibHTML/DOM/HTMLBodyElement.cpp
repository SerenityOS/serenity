#include <LibHTML/CSS/StyleProperties.h>
#include <LibHTML/CSS/StyleValue.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/HTMLBodyElement.h>

HTMLBodyElement::HTMLBodyElement(Document& document, const String& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLBodyElement::~HTMLBodyElement()
{
}

void HTMLBodyElement::apply_presentational_hints(StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name.equals_ignoring_case("bgcolor")) {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property(CSS::PropertyID::BackgroundColor, ColorStyleValue::create(color.value()));
        } else if (name.equals_ignoring_case("text")) {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property(CSS::PropertyID::Color, ColorStyleValue::create(color.value()));
        } else if (name.equals_ignoring_case("background")) {
            style.set_property(CSS::PropertyID::BackgroundImage, ImageStyleValue::create(document().complete_url(value), const_cast<Document&>(document())));
        }
    });
}

void HTMLBodyElement::parse_attribute(const String& name, const String& value)
{
    if (name.equals_ignoring_case("link")) {
        auto color = Color::from_string(value);
        if (color.has_value())
            document().set_link_color(color.value());
    } else if (name.equals_ignoring_case("alink")) {
        auto color = Color::from_string(value);
        if (color.has_value())
            document().set_active_link_color(color.value());
    } else if (name.equals_ignoring_case("vlink")) {
        auto color = Color::from_string(value);
        if (color.has_value())
            document().set_visited_link_color(color.value());
    }
}
