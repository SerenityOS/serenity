#include <LibGUI/GButton.h>
#include <LibGUI/GTextBox.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/HTMLFormElement.h>
#include <LibHTML/DOM/HTMLInputElement.h>
#include <LibHTML/Frame.h>
#include <LibHTML/HtmlView.h>
#include <LibHTML/Layout/LayoutWidget.h>

HTMLInputElement::HTMLInputElement(Document& document, const String& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLInputElement::~HTMLInputElement()
{
}

RefPtr<LayoutNode> HTMLInputElement::create_layout_node(const StyleProperties*) const
{
    ASSERT(document().frame());
    auto& frame = *document().frame();
    ASSERT(frame.html_view());
    auto& html_view = const_cast<HtmlView&>(*frame.html_view());

    RefPtr<GWidget> widget;
    if (type() == "submit") {
        auto button = GButton::construct(value(), &html_view);
        int text_width = Font::default_font().width(value());
        button->set_relative_rect(0, 0, text_width + 20, 20);
        button->on_click = [this](auto&) {
            if (auto* form = first_ancestor_of_type<HTMLFormElement>()) {
                // FIXME: Remove this const_cast once we have a non-const first_ancestor_of_type.
                const_cast<HTMLFormElement*>(form)->submit();
            }
        };
        widget = button;
    } else {
        auto text_box = GTextBox::construct(&html_view);
        text_box->set_text(value());
        text_box->on_change = [this] {
            auto& widget = to<LayoutWidget>(layout_node())->widget();
            const_cast<HTMLInputElement*>(this)->set_attribute("value", static_cast<const GTextBox&>(widget).text());
        };
        int text_width = Font::default_font().width(value());
        text_box->set_relative_rect(0, 0, text_width + 20, 20);
        widget = text_box;
    }

    return adopt(*new LayoutWidget(*this, *widget));
}
