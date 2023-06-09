/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibGfx/Rect.h>
#include <LibJS/Heap/Cell.h>
#include <LibWeb/Layout/Node.h>

namespace Web::Layout {

struct LineBoxFragmentCoordinate {
    size_t line_box_index { 0 };
    size_t fragment_index { 0 };
};

class Box : public NodeWithStyleAndBoxModelMetrics {
    JS_CELL(Box, NodeWithStyleAndBoxModelMetrics);

public:
    Painting::PaintableBox const* paintable_box() const;

    virtual void set_needs_display() override;

    bool is_body() const;

    // https://www.w3.org/TR/css-images-3/#natural-dimensions
    Optional<CSSPixels> natural_width() const { return m_natural_width; }
    Optional<CSSPixels> natural_height() const { return m_natural_height; }
    Optional<float> natural_aspect_ratio() const { return m_natural_aspect_ratio; }

    bool has_natural_width() const { return natural_width().has_value(); }
    bool has_natural_height() const { return natural_height().has_value(); }
    bool has_natural_aspect_ratio() const { return natural_aspect_ratio().has_value(); }

    void set_natural_width(Optional<CSSPixels> width) { m_natural_width = width; }
    void set_natural_height(Optional<CSSPixels> height) { m_natural_height = height; }
    void set_natural_aspect_ratio(Optional<float> ratio) { m_natural_aspect_ratio = ratio; }

    virtual ~Box() override;

    virtual void did_set_content_size() { }

    virtual JS::GCPtr<Painting::Paintable> create_paintable() const override;

    bool is_scroll_container() const;

    bool is_scrollable() const;
    CSSPixelPoint scroll_offset() const { return m_scroll_offset; }
    void set_scroll_offset(CSSPixelPoint);

protected:
    Box(DOM::Document&, DOM::Node*, NonnullRefPtr<CSS::StyleProperties>);
    Box(DOM::Document&, DOM::Node*, CSS::ComputedValues);

private:
    virtual bool is_box() const final { return true; }

    CSSPixelPoint m_scroll_offset;

    Optional<CSSPixels> m_natural_width;
    Optional<CSSPixels> m_natural_height;
    Optional<float> m_natural_aspect_ratio;
};

template<>
inline bool Node::fast_is<Box>() const { return is_box(); }

}
