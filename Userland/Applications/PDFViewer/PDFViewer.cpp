/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PDFViewer.h"
#include <LibGUI/Action.h>
#include <LibGUI/Painter.h>
#include <LibPDF/Renderer.h>

PDFViewer::PDFViewer()
{
    set_should_hide_unnecessary_scrollbars(true);
    set_focus_policy(GUI::FocusPolicy::StrongFocus);
    set_scrollbars_enabled(true);
}

PDFViewer::~PDFViewer()
{
}

void PDFViewer::set_document(RefPtr<PDF::Document> document)
{
    m_document = document;
    m_current_page_index = document->get_first_page_index();
    update();
}

RefPtr<Gfx::Bitmap> PDFViewer::get_rendered_page(u32 index)
{
    auto existing_rendered_page = m_rendered_pages.get(index);
    if (existing_rendered_page.has_value())
        return existing_rendered_page.value();

    auto rendered_page = render_page(m_document->get_page(index));
    m_rendered_pages.set(index, rendered_page);
    return rendered_page;
}

void PDFViewer::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Color(0x80, 0x80, 0x80));

    if (!m_document)
        return;

    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    auto page = get_rendered_page(m_current_page_index);

    auto total_width = width() - frame_thickness() * 2;
    auto total_height = height() - frame_thickness() * 2;
    auto bitmap_width = page->width();
    auto bitmap_height = page->height();

    Gfx::IntPoint p { (total_width - bitmap_width) / 2, (total_height - bitmap_height) / 2 };

    painter.blit(p, *page, page->rect());
}

void PDFViewer::mousewheel_event(GUI::MouseEvent& event)
{
    if (event.wheel_delta() > 0) {
        if (m_current_page_index < m_document->get_page_count() - 1)
            m_current_page_index++;
    } else if (m_current_page_index > 0) {
        m_current_page_index--;
    }
    update();
}

RefPtr<Gfx::Bitmap> PDFViewer::render_page(const PDF::Page& page)
{
    float page_width = page.media_box.upper_right_x - page.media_box.lower_left_x;
    float page_height = page.media_box.upper_right_y - page.media_box.lower_left_y;
    float page_scale_factor = page_height / page_width;

    float width = 300.0f;
    float height = width * page_scale_factor;
    auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { width, height });

    PDF::Renderer::render(*m_document, page, bitmap);
    return bitmap;
}
