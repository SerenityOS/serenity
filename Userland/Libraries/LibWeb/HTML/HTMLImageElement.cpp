/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web::HTML {

HTMLImageElement::HTMLImageElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
    , m_image_loader(*this)
{
    m_image_loader.on_load = [this] {
        set_needs_style_update(true);
        queue_an_element_task(HTML::Task::Source::DOMManipulation, [this] {
            dispatch_event(DOM::Event::create(EventNames::load));
        });
    };

    m_image_loader.on_fail = [this] {
        dbgln("HTMLImageElement: Resource did fail: {}", src());
        set_needs_style_update(true);
        queue_an_element_task(HTML::Task::Source::DOMManipulation, [this] {
            dispatch_event(DOM::Event::create(EventNames::error));
        });
    };

    m_image_loader.on_animate = [this] {
        if (layout_node())
            layout_node()->set_needs_display();
    };
}

HTMLImageElement::~HTMLImageElement()
{
}

void HTMLImageElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name == HTML::AttributeNames::width) {
            if (auto parsed_value = parse_html_length(document(), value)) {
                style.set_property(CSS::PropertyID::Width, parsed_value.release_nonnull());
            }
        } else if (name == HTML::AttributeNames::height) {
            if (auto parsed_value = parse_html_length(document(), value)) {
                style.set_property(CSS::PropertyID::Height, parsed_value.release_nonnull());
            }
        }
    });
}

void HTMLImageElement::parse_attribute(const FlyString& name, const String& value)
{
    HTMLElement::parse_attribute(name, value);

    if (name == HTML::AttributeNames::src && !value.is_empty())
        m_image_loader.load(document().parse_url(value));
}

RefPtr<Layout::Node> HTMLImageElement::create_layout_node()
{
    auto style = document().style_computer().compute_style(*this);
    if (style->display().is_none())
        return nullptr;
    return adopt_ref(*new Layout::ImageBox(document(), *this, move(style), m_image_loader));
}

const Gfx::Bitmap* HTMLImageElement::bitmap() const
{
    return m_image_loader.bitmap(m_image_loader.current_frame_index());
}

}
