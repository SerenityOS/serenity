/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PDFViewer.h"
#include <LibGUI/Action.h>
#include <LibGUI/Painter.h>
#include <LibPDF/Renderer.h>

static constexpr int PAGE_PADDING = 25;

PDFViewer::PDFViewer()
{
    set_should_hide_unnecessary_scrollbars(true);
    set_focus_policy(GUI::FocusPolicy::StrongFocus);
    set_scrollbars_enabled(true);

    start_timer(30'000);
}

void PDFViewer::set_document(RefPtr<PDF::Document> document)
{
    m_document = document;
    m_current_page_index = document->get_first_page_index();
    m_zoom_level = initial_zoom_level;
    m_rendered_page_list.clear();

    m_rendered_page_list.ensure_capacity(document->get_page_count());
    for (u32 i = 0; i < document->get_page_count(); i++)
        m_rendered_page_list.unchecked_append(HashMap<u32, RefPtr<Gfx::Bitmap>>());

    update();
}

RefPtr<Gfx::Bitmap> PDFViewer::get_rendered_page(u32 index)
{
    auto& rendered_page_map = m_rendered_page_list[index];
    auto existing_rendered_page = rendered_page_map.get(m_zoom_level);
    if (existing_rendered_page.has_value())
        return existing_rendered_page.value();

    auto rendered_page = render_page(m_document->get_page(index));
    rendered_page_map.set(m_zoom_level, rendered_page);
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
    if (!m_document)
        return;

    bool scrolled_down = event.wheel_delta() > 0;

    if (event.ctrl()) {
        if (scrolled_down) {
            zoom_out();
        } else {
            zoom_in();
        }
    } else {
        auto& scrollbar = event.shift() ? horizontal_scrollbar() : vertical_scrollbar();

        if (scrolled_down) {
            if (scrollbar.value() == scrollbar.max()) {
                if (m_current_page_index < m_document->get_page_count() - 1) {
                    m_current_page_index++;
                    if (on_page_change)
                        on_page_change(m_current_page_index);
                    scrollbar.set_value(0);
                }
            } else {
                scrollbar.set_value(scrollbar.value() + 20);
            }
        } else {
            if (scrollbar.value() == 0) {
                if (m_current_page_index > 0) {
                    m_current_page_index--;
                    if (on_page_change)
                        on_page_change(m_current_page_index);
                    scrollbar.set_value(scrollbar.max());
                }
            } else {
                scrollbar.set_value(scrollbar.value() - 20);
            }
        }
    }

    update();
}

void PDFViewer::timer_event(Core::TimerEvent&)
{
    // Clear the bitmap vector of all pages except the current page
    for (size_t i = 0; i < m_rendered_page_list.size(); i++) {
        if (i != m_current_page_index)
            m_rendered_page_list[i].clear();
    }
}

void PDFViewer::zoom_in()
{
    if (m_zoom_level < number_of_zoom_levels - 1)
        m_zoom_level++;
}

void PDFViewer::zoom_out()
{
    if (m_zoom_level > 0)
        m_zoom_level--;
}

RefPtr<Gfx::Bitmap> PDFViewer::render_page(const PDF::Page& page)
{
    auto zoom_scale_factor = static_cast<float>(zoom_levels[m_zoom_level]) / 100.0f;

    auto page_width = page.media_box.upper_right_x - page.media_box.lower_left_x;
    auto page_height = page.media_box.upper_right_y - page.media_box.lower_left_y;
    auto page_scale_factor = page_height / page_width;

    auto height = static_cast<float>(this->height() - 2 * frame_thickness() - PAGE_PADDING * 2) * zoom_scale_factor;
    auto width = height / page_scale_factor;
    auto bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, { width, height }).release_value_but_fixme_should_propagate_errors();

    PDF::Renderer::render(*m_document, page, bitmap);

    if (page.rotate != 0) {
        int rotation_count = (page.rotate / 90) % 4;
        if (rotation_count == 3) {
            bitmap = bitmap->rotated(Gfx::RotationDirection::CounterClockwise).release_value_but_fixme_should_propagate_errors();
        } else {
            for (int i = 0; i < rotation_count; i++)
                bitmap = bitmap->rotated(Gfx::RotationDirection::Clockwise).release_value_but_fixme_should_propagate_errors();
        }
    }

    return bitmap;
}
