/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PDFViewer.h"
#include <AK/StringBuilder.h>
#include <LibGUI/Action.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibPDF/Renderer.h>

static constexpr int PAGE_PADDING = 25;

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
    m_zoom_percent = 100;
    m_rendered_page_map.clear();

    for (u32 i = 0; i < document->get_page_count(); i++)
        m_rendered_page_map.append(HashMap<u32, RefPtr<Gfx::Bitmap>>());

    update();
}

RefPtr<Gfx::Bitmap> PDFViewer::get_rendered_page(u32 index)
{
    auto rendered_page_map = m_rendered_page_map[index];
    auto existing_rendered_page = rendered_page_map.get(index);
    if (existing_rendered_page.has_value())
        return existing_rendered_page.value();

    auto rendered_page = render_page(m_document->get_page(index));
    rendered_page_map.set(index, rendered_page);
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

    auto page = get_rendered_page(m_current_page_index);
    set_content_size(page->size());

    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    int x = max(0, (width() - page->width()) / 2);
    int y = max(0, (height() - page->height()) / 2);

    painter.blit({ x, y }, *page, page->rect());
}

void PDFViewer::mousewheel_event(GUI::MouseEvent& event)
{
    if (event.ctrl()) {
        if (event.wheel_delta() > 0) {
            zoom_out();
        } else {
            zoom_in();
        }
        update();

        return;
    }

    if (event.wheel_delta() > 0) {
        if (vertical_scrollbar().value() == vertical_scrollbar().max()) {
            if (m_current_page_index < m_document->get_page_count() - 1) {
                vertical_scrollbar().set_value(0);
                m_current_page_index++;
            }
        } else {
            vertical_scrollbar().set_value(vertical_scrollbar().value() + 20);
        }
    } else {
        if (vertical_scrollbar().value() == 0) {
            if (m_current_page_index > 0) {
                vertical_scrollbar().set_value(vertical_scrollbar().max());
                m_current_page_index--;
            }
        } else {
            vertical_scrollbar().set_value(vertical_scrollbar().value() - 20);
        }
    }
    update();
}

void PDFViewer::zoom_in()
{
    m_zoom_percent *= 1.2f;
    if (m_zoom_percent > 1000)
        m_zoom_percent = 1000;
}

void PDFViewer::zoom_out()
{
    m_zoom_percent *= 0.8f;
    if (m_zoom_percent < 10)
        m_zoom_percent = 10;
}

RefPtr<Gfx::Bitmap> PDFViewer::render_page(const PDF::Page& page)
{
    auto zoom_scale_factor = static_cast<float>(m_zoom_percent) / 100.0f;

    auto page_width = page.media_box.upper_right_x - page.media_box.lower_left_x;
    auto page_height = page.media_box.upper_right_y - page.media_box.lower_left_y;
    auto page_scale_factor = page_height / page_width;

    auto height = static_cast<float>(this->height() - 2 * frame_thickness() - PAGE_PADDING * 2) * zoom_scale_factor;
    auto width = height / page_scale_factor;

    auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { width, height });

    PDF::Renderer::render(*m_document, page, bitmap);

    if (page.rotate != 0) {
        int rotation_count = (page.rotate / 90) % 4;
        if (rotation_count == 3) {
            bitmap = bitmap->rotated(Gfx::RotationDirection::CounterClockwise);
        } else {
            for (int i = 0; i < rotation_count; i++)
                bitmap = bitmap->rotated(Gfx::RotationDirection::Clockwise);
        }
    }

    return bitmap;
}
