/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/DecodedImageData.h>

namespace Web::SVG {

class SVGDecodedImageData final : public HTML::DecodedImageData {
    JS_CELL(SVGDecodedImageData, Cell);
    JS_DECLARE_ALLOCATOR(SVGDecodedImageData);

public:
    static ErrorOr<JS::NonnullGCPtr<SVGDecodedImageData>> create(JS::Realm&, JS::NonnullGCPtr<Page>, AK::URL const&, ByteBuffer encoded_svg);
    virtual ~SVGDecodedImageData() override;

    virtual RefPtr<Gfx::ImmutableBitmap> bitmap(size_t frame_index, Gfx::IntSize) const override;

    virtual Optional<CSSPixels> intrinsic_width() const override;
    virtual Optional<CSSPixels> intrinsic_height() const override;
    virtual Optional<CSSPixelFraction> intrinsic_aspect_ratio() const override;

    // FIXME: Support SVG animations. :^)
    virtual int frame_duration(size_t) const override { return 0; }
    virtual size_t frame_count() const override { return 1; }
    virtual size_t loop_count() const override { return 0; }
    virtual bool is_animated() const override { return false; }

    DOM::Document const& svg_document() const { return *m_document; }

    virtual void visit_edges(Cell::Visitor& visitor) override;

private:
    class SVGPageClient;
    SVGDecodedImageData(JS::NonnullGCPtr<Page>, JS::NonnullGCPtr<SVGPageClient>, JS::NonnullGCPtr<DOM::Document>, JS::NonnullGCPtr<SVG::SVGSVGElement>);

    RefPtr<Gfx::Bitmap> render(Gfx::IntSize) const;

    mutable HashMap<Gfx::IntSize, NonnullRefPtr<Gfx::ImmutableBitmap>> m_cached_rendered_bitmaps;

    JS::NonnullGCPtr<Page> m_page;
    JS::NonnullGCPtr<SVGPageClient> m_page_client;

    JS::NonnullGCPtr<DOM::Document> m_document;
    JS::NonnullGCPtr<SVG::SVGSVGElement> m_root_element;
};

}
