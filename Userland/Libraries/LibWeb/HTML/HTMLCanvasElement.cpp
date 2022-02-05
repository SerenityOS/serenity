/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/Checked.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/PNGWriter.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/CanvasRenderingContext2D.h>
#include <LibWeb/HTML/HTMLCanvasElement.h>
#include <LibWeb/Layout/CanvasBox.h>

namespace Web::HTML {

static constexpr auto max_canvas_area = 16384 * 16384;

HTMLCanvasElement::HTMLCanvasElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLCanvasElement::~HTMLCanvasElement()
{
}

unsigned HTMLCanvasElement::width() const
{
    return attribute(HTML::AttributeNames::width).to_uint().value_or(300);
}

unsigned HTMLCanvasElement::height() const
{
    return attribute(HTML::AttributeNames::height).to_uint().value_or(150);
}

void HTMLCanvasElement::set_width(unsigned value)
{
    set_attribute(HTML::AttributeNames::width, String::number(value));
}

void HTMLCanvasElement::set_height(unsigned value)
{
    set_attribute(HTML::AttributeNames::height, String::number(value));
}

RefPtr<Layout::Node> HTMLCanvasElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return adopt_ref(*new Layout::CanvasBox(document(), *this, move(style)));
}

CanvasRenderingContext2D* HTMLCanvasElement::get_context(String type)
{
    if (type != "2d")
        return nullptr;
    if (!m_context)
        m_context = CanvasRenderingContext2D::create(*this);
    return m_context;
}

static Gfx::IntSize bitmap_size_for_canvas(const HTMLCanvasElement& canvas)
{
    auto width = canvas.width();
    auto height = canvas.height();

    Checked<size_t> area = width;
    area *= height;

    if (area.has_overflow()) {
        dbgln("Refusing to create {}x{} canvas (overflow)", width, height);
        return {};
    }
    if (area.value() > max_canvas_area) {
        dbgln("Refusing to create {}x{} canvas (exceeds maximum size)", width, height);
        return {};
    }
    return Gfx::IntSize(width, height);
}

bool HTMLCanvasElement::create_bitmap()
{
    auto size = bitmap_size_for_canvas(*this);
    if (size.is_empty()) {
        m_bitmap = nullptr;
        return false;
    }
    if (!m_bitmap || m_bitmap->size() != size) {
        auto bitmap_or_error = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, size);
        if (bitmap_or_error.is_error())
            return false;
        m_bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
    }
    return m_bitmap;
}

String HTMLCanvasElement::to_data_url(const String& type, [[maybe_unused]] Optional<double> quality) const
{
    if (!m_bitmap)
        return {};
    if (type != "image/png")
        return {};
    auto encoded_bitmap = Gfx::PNGWriter::encode(*m_bitmap);
    return AK::URL::create_with_data(type, encode_base64(encoded_bitmap), true).to_string();
}

}
