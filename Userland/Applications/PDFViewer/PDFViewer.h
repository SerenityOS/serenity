/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGfx/Bitmap.h>
#include <LibPDF/Document.h>

class PDFViewer : public GUI::AbstractScrollableWidget {
    C_OBJECT(PDFViewer)

public:
    virtual ~PDFViewer() override;

    void set_document(RefPtr<PDF::Document>);

protected:
    PDFViewer();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;

private:
    RefPtr<Gfx::Bitmap> get_rendered_page(u32 index);
    RefPtr<Gfx::Bitmap> render_page(const PDF::Page&);

    void zoom_in();
    void zoom_out();

    RefPtr<PDF::Document> m_document;
    u32 m_current_page_index { 0 };
    Vector<HashMap<u32, RefPtr<Gfx::Bitmap>>> m_rendered_page_map;

    // 100% is normal, ranges from 10% to 1000%
    u16 m_zoom_percent { 100 };
};
