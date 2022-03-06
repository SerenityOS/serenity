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

class PDFViewer : public GUI::AbstractScrollableWidget {
    C_OBJECT(PDFViewer)

public:
    virtual ~PDFViewer() override = default;

    ALWAYS_INLINE u32 current_page() const { return m_current_page_index; }
    ALWAYS_INLINE void set_current_page(u32 current_page) { m_current_page_index = current_page; }

    ALWAYS_INLINE const RefPtr<PDF::Document>& document() const { return m_document; }
    void set_document(RefPtr<PDF::Document>);

    Function<void(u32 new_page)> on_page_change;

    void zoom_in();
    void zoom_out();
    void reset_zoom();
    void rotate(int degrees);

protected:
    PDFViewer();

    virtual void paint_event(GUI::PaintEvent&) override;
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
    PDF::PDFErrorOr<NonnullRefPtr<Gfx::Bitmap>> render_page(const PDF::Page&);

    RefPtr<PDF::Document> m_document;
    u32 m_current_page_index { 0 };
    Vector<HashMap<u32, RenderedPage>> m_rendered_page_list;

    u8 m_zoom_level { initial_zoom_level };

    Gfx::IntPoint m_pan_starting_position;
    int m_rotations { 0 };
};
