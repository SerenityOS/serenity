/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <LibGfx/Forward.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/WebGL/WebGLRenderingContext.h>

namespace Web::HTML {

class HTMLCanvasElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLCanvasElement, HTMLElement);

public:
    using RenderingContext = Variant<NonnullRefPtr<CanvasRenderingContext2D>, NonnullRefPtr<WebGL::WebGLRenderingContext>, Empty>;

    virtual ~HTMLCanvasElement() override;

    Gfx::Bitmap const* bitmap() const { return m_bitmap; }
    Gfx::Bitmap* bitmap() { return m_bitmap; }
    bool create_bitmap(size_t minimum_width = 0, size_t minimum_height = 0);

    JS::ThrowCompletionOr<RenderingContext> get_context(String const& type, JS::Value options);

    unsigned width() const;
    unsigned height() const;

    void set_width(unsigned);
    void set_height(unsigned);

    String to_data_url(String const& type, Optional<double> quality) const;

    void present();

private:
    HTMLCanvasElement(DOM::Document&, DOM::QualifiedName);

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    enum class HasOrCreatedContext {
        No,
        Yes,
    };

    HasOrCreatedContext create_2d_context();
    JS::ThrowCompletionOr<HasOrCreatedContext> create_webgl_context(JS::Value options);
    void reset_context_to_default_state();

    RefPtr<Gfx::Bitmap> m_bitmap;
    RenderingContext m_context;
};

}

WRAPPER_HACK(HTMLCanvasElement, Web::HTML)
