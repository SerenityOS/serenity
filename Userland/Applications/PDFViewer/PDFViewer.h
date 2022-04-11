/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGfx/Bitmap.h>
#include <LibPDF/Document.h>

static constexpr size_t initial_zoom_level = 8;

struct PageDimensionCache {
    // Fixed for a given document
    struct PageInfo {
        Gfx::FloatSize size;
        int rotation;
    };

    // Based on PageInfo, also depends on some dynamic factors like
    // zoom level and app size
    struct RenderInfo {
        Gfx::FloatSize size;
        float total_height_before_this_page;
    };

    Vector<PageInfo> page_info;
    Vector<RenderInfo> render_info;
    float max_width;
    float total_height;
};

class PDFViewer : public GUI::AbstractScrollableWidget {
    C_OBJECT(PDFViewer)

public:
    enum class PageViewMode {
        Single,
        Multiple,
    };

    virtual ~PDFViewer() override = default;

    ALWAYS_INLINE u32 current_page() const { return m_current_page_index; }
    void set_current_page(u32 current_page);

    ALWAYS_INLINE RefPtr<PDF::Document> const& document() const { return m_document; }
    PDF::PDFErrorOr<void> set_document(RefPtr<PDF::Document>);

    Function<void(u32 new_page)> on_page_change;

    void zoom_in();
    void zoom_out();
    void reset_zoom();
    void rotate(int degrees);

    PageViewMode page_view_mode() const { return m_page_view_mode; }
    void set_page_view_mode(PageViewMode);

protected:
    PDFViewer();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

private:
    struct RenderedPage {
        NonnullRefPtr<Gfx::Bitmap> bitmap;
        int rotation;
    };

    PDF::PDFErrorOr<NonnullRefPtr<Gfx::Bitmap>> get_rendered_page(u32 index);
    PDF::PDFErrorOr<NonnullRefPtr<Gfx::Bitmap>> render_page(u32 page_index);
    PDF::PDFErrorOr<void> cache_page_dimensions(bool recalculate_fixed_info = false);
    void change_page(u32 new_page);

    RefPtr<PDF::Document> m_document;
    u32 m_current_page_index { 0 };
    Vector<HashMap<u32, RenderedPage>> m_rendered_page_list;

    u8 m_zoom_level { initial_zoom_level };
    PageDimensionCache m_page_dimension_cache;
    PageViewMode m_page_view_mode;

    Gfx::IntPoint m_pan_starting_position;
    int m_rotations { 0 };
};
