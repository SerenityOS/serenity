/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <LibGfx/Forward.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLCanvasElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLCanvasElementWrapper;

    HTMLCanvasElement(DOM::Document&, QualifiedName);
    virtual ~HTMLCanvasElement() override;

    const Gfx::Bitmap* bitmap() const { return m_bitmap; }
    Gfx::Bitmap* bitmap() { return m_bitmap; }
    bool create_bitmap();

    CanvasRenderingContext2D* get_context(String type);

    unsigned width() const;
    unsigned height() const;

    void set_width(unsigned);
    void set_height(unsigned);

    String to_data_url(const String& type, Optional<double> quality) const;

private:
    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    RefPtr<Gfx::Bitmap> m_bitmap;
    RefPtr<CanvasRenderingContext2D> m_context;
};

}
