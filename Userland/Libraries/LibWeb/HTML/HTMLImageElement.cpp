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

HTMLImageElement::HTMLImageElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : FormAssociatedElement(document, move(qualified_name))
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

RefPtr<Layout::Node> HTMLImageElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return adopt_ref(*new Layout::ImageBox(document(), *this, move(style), m_image_loader));
}

const Gfx::Bitmap* HTMLImageElement::bitmap() const
{
    return m_image_loader.bitmap(m_image_loader.current_frame_index());
}

// https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-width
unsigned HTMLImageElement::width() const
{
    const_cast<DOM::Document&>(document()).update_layout();

    // Return the rendered width of the image, in CSS pixels, if the image is being rendered.
    if (layout_node() && is<Layout::Box>(*layout_node()))
        return static_cast<Layout::Box const&>(*layout_node()).content_width();

    // ...or else the density-corrected intrinsic width and height of the image, in CSS pixels,
    // if the image has intrinsic dimensions and is available but not being rendered.
    if (m_image_loader.has_image())
        return m_image_loader.width();

    // ...or else 0, if the image is not available or does not have intrinsic dimensions.
    return 0;
}

void HTMLImageElement::set_width(unsigned width)
{
    set_attribute(HTML::AttributeNames::width, String::number(width));
}

// https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-height
unsigned HTMLImageElement::height() const
{
    const_cast<DOM::Document&>(document()).update_layout();

    // Return the rendered height of the image, in CSS pixels, if the image is being rendered.
    if (layout_node() && is<Layout::Box>(*layout_node()))
        return static_cast<Layout::Box const&>(*layout_node()).content_height();

    // ...or else the density-corrected intrinsic height and height of the image, in CSS pixels,
    // if the image has intrinsic dimensions and is available but not being rendered.
    if (m_image_loader.has_image())
        return m_image_loader.height();

    // ...or else 0, if the image is not available or does not have intrinsic dimensions.
    return 0;
}

void HTMLImageElement::set_height(unsigned height)
{
    set_attribute(HTML::AttributeNames::height, String::number(height));
}

}
