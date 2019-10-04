#include <LibHTML/CSS/StyleProperties.h>
#include <LibHTML/CSS/StyleValue.h>
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
