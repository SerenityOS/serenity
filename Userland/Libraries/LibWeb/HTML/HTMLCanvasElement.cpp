/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/Checked.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/CanvasRenderingContext2D.h>
#include <LibWeb/HTML/HTMLCanvasElement.h>
#include <LibWeb/Layout/CanvasBox.h>

namespace Web::HTML {

static constexpr auto max_canvas_area = 16384 * 16384;

HTMLCanvasElement::HTMLCanvasElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLCanvasElement::~HTMLCanvasElement() = default;

JS::ThrowCompletionOr<void> HTMLCanvasElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLCanvasElementPrototype>(realm, "HTMLCanvasElement"));

    return {};
}

void HTMLCanvasElement::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    m_context.visit(
        [&](JS::NonnullGCPtr<CanvasRenderingContext2D>& context) {
            visitor.visit(context.ptr());
        },
        [&](JS::NonnullGCPtr<WebGL::WebGLRenderingContext>& context) {
            visitor.visit(context.ptr());
        },
        [](Empty) {
        });
}

unsigned HTMLCanvasElement::width() const
{
    return attribute(HTML::AttributeNames::width).to_uint().value_or(300);
}

unsigned HTMLCanvasElement::height() const
{
    return attribute(HTML::AttributeNames::height).to_uint().value_or(150);
}

void HTMLCanvasElement::reset_context_to_default_state()
{
    m_context.visit(
        [](JS::NonnullGCPtr<CanvasRenderingContext2D>& context) {
            context->reset_to_default_state();
        },
        [](JS::NonnullGCPtr<WebGL::WebGLRenderingContext>&) {
            TODO();
        },
        [](Empty) {
            // Do nothing.
        });
}

WebIDL::ExceptionOr<void> HTMLCanvasElement::set_width(unsigned value)
{
    TRY(set_attribute(HTML::AttributeNames::width, DeprecatedString::number(value)));
    m_bitmap = nullptr;
    reset_context_to_default_state();
    return {};
}

WebIDL::ExceptionOr<void> HTMLCanvasElement::set_height(unsigned value)
{
    TRY(set_attribute(HTML::AttributeNames::height, DeprecatedString::number(value)));
    m_bitmap = nullptr;
    reset_context_to_default_state();
    return {};
}

JS::GCPtr<Layout::Node> HTMLCanvasElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return heap().allocate_without_realm<Layout::CanvasBox>(document(), *this, move(style));
}

HTMLCanvasElement::HasOrCreatedContext HTMLCanvasElement::create_2d_context()
{
    if (!m_context.has<Empty>())
        return m_context.has<JS::NonnullGCPtr<CanvasRenderingContext2D>>() ? HasOrCreatedContext::Yes : HasOrCreatedContext::No;

    m_context = CanvasRenderingContext2D::create(realm(), *this).release_value_but_fixme_should_propagate_errors();
    return HasOrCreatedContext::Yes;
}

JS::ThrowCompletionOr<HTMLCanvasElement::HasOrCreatedContext> HTMLCanvasElement::create_webgl_context(JS::Value options)
{
    if (!m_context.has<Empty>())
        return m_context.has<JS::NonnullGCPtr<WebGL::WebGLRenderingContext>>() ? HasOrCreatedContext::Yes : HasOrCreatedContext::No;

    auto maybe_context = TRY(WebGL::WebGLRenderingContext::create(realm(), *this, options));
    if (!maybe_context)
        return HasOrCreatedContext::No;

    m_context = JS::NonnullGCPtr<WebGL::WebGLRenderingContext>(*maybe_context);
    return HasOrCreatedContext::Yes;
}

// https://html.spec.whatwg.org/multipage/canvas.html#dom-canvas-getcontext
JS::ThrowCompletionOr<HTMLCanvasElement::RenderingContext> HTMLCanvasElement::get_context(DeprecatedString const& type, JS::Value options)
{
    // 1. If options is not an object, then set options to null.
    if (!options.is_object())
        options = JS::js_null();

    // 2. Set options to the result of converting options to a JavaScript value.
    // NOTE: No-op.

    // 3. Run the steps in the cell of the following table whose column header matches this canvas element's canvas context mode and whose row header matches contextId:
    // NOTE: See the spec for the full table.
    if (type == "2d"sv) {
        if (create_2d_context() == HasOrCreatedContext::Yes)
            return JS::make_handle(*m_context.get<JS::NonnullGCPtr<HTML::CanvasRenderingContext2D>>());

        return Empty {};
    }

    // NOTE: The WebGL spec says "experimental-webgl" is also acceptable and must be equivalent to "webgl". Other engines accept this, so we do too.
    if (type.is_one_of("webgl"sv, "experimental-webgl"sv)) {
        if (TRY(create_webgl_context(options)) == HasOrCreatedContext::Yes)
            return JS::make_handle(*m_context.get<JS::NonnullGCPtr<WebGL::WebGLRenderingContext>>());

        return Empty {};
    }

    return Empty {};
}

static Gfx::IntSize bitmap_size_for_canvas(HTMLCanvasElement const& canvas, size_t minimum_width, size_t minimum_height)
{
    auto width = max(canvas.width(), minimum_width);
    auto height = max(canvas.height(), minimum_height);

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

bool HTMLCanvasElement::create_bitmap(size_t minimum_width, size_t minimum_height)
{
    auto size = bitmap_size_for_canvas(*this, minimum_width, minimum_height);
    if (size.is_empty()) {
        m_bitmap = nullptr;
        return false;
    }
    if (!m_bitmap || m_bitmap->size() != size) {
        auto bitmap_or_error = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, size);
        if (bitmap_or_error.is_error())
            return false;
        m_bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
    }
    return m_bitmap;
}

DeprecatedString HTMLCanvasElement::to_data_url(DeprecatedString const& type, [[maybe_unused]] Optional<double> quality) const
{
    if (!m_bitmap)
        return {};
    if (type != "image/png")
        return {};
    auto encoded_bitmap_or_error = Gfx::PNGWriter::encode(*m_bitmap);
    if (encoded_bitmap_or_error.is_error()) {
        dbgln("Gfx::PNGWriter failed to encode the HTMLCanvasElement: {}", encoded_bitmap_or_error.error());
        return {};
    }
    auto base64_encoded_or_error = encode_base64(encoded_bitmap_or_error.value());
    if (base64_encoded_or_error.is_error()) {
        // FIXME: propagate error
        return {};
    }
    return AK::URL::create_with_data(type, base64_encoded_or_error.release_value(), true).to_deprecated_string();
}

void HTMLCanvasElement::present()
{
    m_context.visit(
        [](JS::NonnullGCPtr<CanvasRenderingContext2D>&) {
            // Do nothing, CRC2D writes directly to the canvas bitmap.
        },
        [](JS::NonnullGCPtr<WebGL::WebGLRenderingContext>& context) {
            context->present();
        },
        [](Empty) {
            // Do nothing.
        });
}

}
