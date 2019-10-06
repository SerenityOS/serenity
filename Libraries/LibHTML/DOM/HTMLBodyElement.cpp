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
        if (name == "bgcolor") {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property("background-color", ColorStyleValue::create(color.value()));
        } else if (name == "text") {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property("color", ColorStyleValue::create(color.value()));
        }
    });
}

void HTMLBodyElement::parse_attribute(const String& name, const String& value)
{
    if (name == "link") {
        auto color = Color::from_string(value);
        if (color.has_value())
            document().set_link_color(color.value());
    } else if (name == "alink") {
        auto color = Color::from_string(value);
        if (color.has_value())
            document().set_active_link_color(color.value());
    } else if (name == "vlink") {
        auto color = Color::from_string(value);
        if (color.has_value())
            document().set_visited_link_color(color.value());
    }
}
