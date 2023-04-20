/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::HTML {

HTMLImageElement::HTMLImageElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
    , m_image_loader(*this)
{
    m_image_loader.on_load = [this] {
        set_needs_style_update(true);
        this->document().set_needs_layout();
        queue_an_element_task(HTML::Task::Source::DOMManipulation, [this] {
            dispatch_event(DOM::Event::create(this->realm(), EventNames::load).release_value_but_fixme_should_propagate_errors());
            m_load_event_delayer.clear();
        });
    };

    m_image_loader.on_fail = [this] {
        dbgln("HTMLImageElement: Resource did fail: {}", src());
        set_needs_style_update(true);
        this->document().set_needs_layout();
        queue_an_element_task(HTML::Task::Source::DOMManipulation, [this] {
            dispatch_event(DOM::Event::create(this->realm(), EventNames::error).release_value_but_fixme_should_propagate_errors());
            m_load_event_delayer.clear();
        });
    };

    m_image_loader.on_animate = [this] {
        if (layout_node())
            layout_node()->set_needs_display();
    };
}

HTMLImageElement::~HTMLImageElement() = default;

JS::ThrowCompletionOr<void> HTMLImageElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLImageElementPrototype>(realm, "HTMLImageElement"));

    return {};
}

void HTMLImageElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name == HTML::AttributeNames::width) {
            if (auto parsed_value = parse_dimension_value(value))
                style.set_property(CSS::PropertyID::Width, parsed_value.release_nonnull());
        } else if (name == HTML::AttributeNames::height) {
            if (auto parsed_value = parse_dimension_value(value))
                style.set_property(CSS::PropertyID::Height, parsed_value.release_nonnull());
        } else if (name == HTML::AttributeNames::hspace) {
            if (auto parsed_value = parse_dimension_value(value)) {
                style.set_property(CSS::PropertyID::MarginLeft, *parsed_value);
                style.set_property(CSS::PropertyID::MarginRight, *parsed_value);
            }
        } else if (name == HTML::AttributeNames::vspace) {
            if (auto parsed_value = parse_dimension_value(value)) {
                style.set_property(CSS::PropertyID::MarginTop, *parsed_value);
                style.set_property(CSS::PropertyID::MarginBottom, *parsed_value);
            }
        }
    });
}

void HTMLImageElement::parse_attribute(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    HTMLElement::parse_attribute(name, value);

    if (name == HTML::AttributeNames::src && !value.is_empty()) {
        m_image_loader.load(document().parse_url(value));
        m_load_event_delayer.emplace(document());
    }

    if (name == HTML::AttributeNames::alt) {
        if (layout_node())
            verify_cast<Layout::ImageBox>(*layout_node()).dom_node_did_update_alt_text({});
    }
}

JS::GCPtr<Layout::Node> HTMLImageElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return heap().allocate_without_realm<Layout::ImageBox>(document(), *this, move(style), m_image_loader);
}

Gfx::Bitmap const* HTMLImageElement::bitmap() const
{
    return m_image_loader.bitmap(m_image_loader.current_frame_index());
}

// https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-width
unsigned HTMLImageElement::width() const
{
    const_cast<DOM::Document&>(document()).update_layout();

    // Return the rendered width of the image, in CSS pixels, if the image is being rendered.
    if (auto* paintable_box = this->paintable_box())
        return paintable_box->content_width().value();

    // NOTE: This step seems to not be in the spec, but all browsers do it.
    auto width_attr = get_attribute(HTML::AttributeNames::width);
    if (auto converted = width_attr.to_uint(); converted.has_value())
        return *converted;

    // ...or else the density-corrected intrinsic width and height of the image, in CSS pixels,
    // if the image has intrinsic dimensions and is available but not being rendered.
    if (m_image_loader.has_image())
        return m_image_loader.width();

    // ...or else 0, if the image is not available or does not have intrinsic dimensions.
    return 0;
}

void HTMLImageElement::set_width(unsigned width)
{
    MUST(set_attribute(HTML::AttributeNames::width, DeprecatedString::number(width)));
}

// https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-height
unsigned HTMLImageElement::height() const
{
    const_cast<DOM::Document&>(document()).update_layout();

    // Return the rendered height of the image, in CSS pixels, if the image is being rendered.
    if (auto* paintable_box = this->paintable_box())
        return paintable_box->content_height().value();

    // NOTE: This step seems to not be in the spec, but all browsers do it.
    auto height_attr = get_attribute(HTML::AttributeNames::height);
    if (auto converted = height_attr.to_uint(); converted.has_value())
        return *converted;

    // ...or else the density-corrected intrinsic height and height of the image, in CSS pixels,
    // if the image has intrinsic dimensions and is available but not being rendered.
    if (m_image_loader.has_image())
        return m_image_loader.height();

    // ...or else 0, if the image is not available or does not have intrinsic dimensions.
    return 0;
}

void HTMLImageElement::set_height(unsigned height)
{
    MUST(set_attribute(HTML::AttributeNames::height, DeprecatedString::number(height)));
}

// https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-naturalwidth
unsigned HTMLImageElement::natural_width() const
{
    // Return the density-corrected intrinsic width of the image, in CSS pixels,
    // if the image has intrinsic dimensions and is available.
    if (m_image_loader.has_image())
        return m_image_loader.width();

    // ...or else 0.
    return 0;
}

// https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-naturalheight
unsigned HTMLImageElement::natural_height() const
{
    // Return the density-corrected intrinsic height of the image, in CSS pixels,
    // if the image has intrinsic dimensions and is available.
    if (m_image_loader.has_image())
        return m_image_loader.height();

    // ...or else 0.
    return 0;
}

// https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-complete
bool HTMLImageElement::complete() const
{
    // The IDL attribute complete must return true if any of the following conditions is true:

    // - Both the src attribute and the srcset attribute are omitted.
    if (!has_attribute(HTML::AttributeNames::src) && !has_attribute(HTML::AttributeNames::srcset))
        return true;

    // - The srcset attribute is omitted and the src attribute's value is the empty string.
    if (!has_attribute(HTML::AttributeNames::srcset) && attribute(HTML::AttributeNames::src) == ""sv)
        return true;

    // - The img element's current request's state is completely available and its pending request is null.
    // - The img element's current request's state is broken and its pending request is null.
    // FIXME: This is ad-hoc and should be updated once we are loading images via the Fetch mechanism.
    if (m_image_loader.has_loaded_or_failed())
        return true;

    return false;
}

Optional<ARIA::Role> HTMLImageElement::default_role() const
{
    // https://www.w3.org/TR/html-aria/#el-img
    // https://www.w3.org/TR/html-aria/#el-img-no-alt
    if (alt().is_null() || !alt().is_empty())
        return ARIA::Role::img;
    // https://www.w3.org/TR/html-aria/#el-img-empty-alt
    return ARIA::Role::presentation;
}

}
