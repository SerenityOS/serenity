#include <LibHTML/CSS/StyleProperties.h>
#include <LibHTML/CSS/StyleValue.h>
#include <LibHTML/DOM/HTMLFontElement.h>

HTMLFontElement::HTMLFontElement(Document& document, const String& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLFontElement::~HTMLFontElement()
{
}

void HTMLFontElement::apply_presentational_hints(StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name.equals_ignoring_case("color")) {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property(CSS::PropertyID::Color, ColorStyleValue::create(color.value()));
        }
    });
}
