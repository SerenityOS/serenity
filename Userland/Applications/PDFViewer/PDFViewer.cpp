/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PDFViewer.h"
#include <AK/Array.h>
#include <AK/BinarySearch.h>
#include <LibConfig/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibPDF/Renderer.h>

static constexpr int PAGE_PADDING = 10;

static constexpr Array zoom_levels = {
    17,
    21,
    26,
    33,
    41,
    51,
    64,
    80,
    100,
    120,
    144,
    173,
    207,
    249,
    299,
    358,
    430
};

PDFViewer::PDFViewer()
{
    set_should_hide_unnecessary_scrollbars(true);
    set_focus_policy(GUI::FocusPolicy::StrongFocus);
    set_scrollbars_enabled(true);

    start_timer(30'000);

    m_page_view_mode = static_cast<PageViewMode>(Config::read_i32("PDFViewer", "Display", "PageMode", 0));
}

PDF::PDFErrorOr<void> PDFViewer::set_document(RefPtr<PDF::Document> document)
{
    m_document = document;
    m_current_page_index = document->get_first_page_index();
    m_zoom_level = initial_zoom_level;
    m_rendered_page_list.clear();

    m_rendered_page_list.ensure_capacity(document->get_page_count());
    for (u32 i = 0; i < document->get_page_count(); i++)
        m_rendered_page_list.unchecked_append(HashMap<u32, RenderedPage>());

    TRY(cache_page_dimensions(true));
    update();

    return {};
}

PDF::PDFErrorOr<NonnullRefPtr<Gfx::Bitmap>> PDFViewer::get_rendered_page(u32 index)
{
    auto& rendered_page_map = m_rendered_page_list[index];
    auto existing_rendered_page = rendered_page_map.get(m_zoom_level);
    if (existing_rendered_page.has_value() && existing_rendered_page.value().rotation == m_rotations)
        return existing_rendered_page.value().bitmap;

    auto rendered_page = TRY(render_page(index));
    rendered_page_map.set(m_zoom_level, { rendered_page, m_rotations });
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

    auto handle_error = [&]<typename T>(PDF::PDFErrorOr<T> maybe_error) {
        if (maybe_error.is_error()) {
            auto error = maybe_error.release_error();
            GUI::MessageBox::show_error(nullptr, String::formatted("Error rendering page:\n{}", error.message()));
            return true;
        }
        return false;
    };

    if (m_page_view_mode == PageViewMode::Single) {
        auto maybe_page = get_rendered_page(m_current_page_index);
        if (handle_error(maybe_page))
            return;

        auto page = maybe_page.release_value();
        set_content_size(page->size());

        painter.translate(frame_thickness(), frame_thickness());
        painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

        int x = max(0, (width() - page->width()) / 2);
        int y = max(0, (height() - page->height()) / 2);

        painter.blit({ x, y }, *page, page->rect());
        return;
    }

    set_content_size({ m_page_dimension_cache.max_width, m_page_dimension_cache.total_height });

    size_t first_page_index = 0;
    size_t last_page_index = 0;

    binary_search(m_page_dimension_cache.render_info, vertical_scrollbar().value(), &first_page_index, [](int height, PageDimensionCache::RenderInfo const& render_info) {
        return height - render_info.total_height_before_this_page;
    });

    binary_search(m_page_dimension_cache.render_info, vertical_scrollbar().value() + height(), &last_page_index, [](int height, PageDimensionCache::RenderInfo const& render_info) {
        return height - render_info.total_height_before_this_page;
    });

    auto initial_offset = m_page_dimension_cache.render_info[first_page_index].total_height_before_this_page - vertical_scrollbar().value();

    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), initial_offset);
    auto middle = height() / 2;
    auto y_offset = initial_offset;

    for (size_t page_index = first_page_index; page_index <= last_page_index; page_index++) {
        auto maybe_page = get_rendered_page(page_index);
        if (handle_error(maybe_page))
            return;

        auto page = maybe_page.release_value();

        auto x = max(0, (width() - page->width()) / 2);

        painter.blit({ x, PAGE_PADDING }, *page, page->rect());
        auto diff_y = page->height() + PAGE_PADDING * 2;
        painter.translate(0, diff_y);

        if (y_offset < middle && y_offset + diff_y >= middle)
            change_page(page_index);

        y_offset += diff_y;
    }
}

void PDFViewer::set_current_page(u32 current_page)
{
    m_current_page_index = current_page;
    vertical_scrollbar().set_value(m_page_dimension_cache.render_info[current_page].total_height_before_this_page);
    update();
}

void PDFViewer::resize_event(GUI::ResizeEvent&)
{
    for (auto& map : m_rendered_page_list)
        map.clear();
    if (m_document)
        MUST(cache_page_dimensions());
    update();
}

void PDFViewer::mousewheel_event(GUI::MouseEvent& event)
{
    if (!m_document)
        return;

    bool scrolled_down = event.wheel_delta_y() > 0;

    if (event.ctrl()) {
        if (scrolled_down) {
            zoom_out();
        } else {
            zoom_in();
        }
        return;
    }

    auto& scrollbar = event.shift() ? horizontal_scrollbar() : vertical_scrollbar();

    if (m_page_view_mode == PageViewMode::Multiple) {
        if (scrolled_down) {
            if (scrollbar.value() != scrollbar.max())
                scrollbar.increase_slider_by(20);
        } else {
            if (scrollbar.value() > 0)
                scrollbar.decrease_slider_by(20);
        }
    } else {
        if (scrolled_down) {
            if (scrollbar.value() == scrollbar.max()) {
                if (m_current_page_index < m_document->get_page_count() - 1) {
                    change_page(m_current_page_index + 1);
                    scrollbar.set_value(0);
                }
            } else {
                scrollbar.increase_slider_by(20);
            }
        } else {
            if (scrollbar.value() == 0) {
                if (m_current_page_index > 0) {
                    change_page(m_current_page_index - 1);
                    scrollbar.set_value(scrollbar.max());
                }
            } else {
                scrollbar.decrease_slider_by(20);
            }
        }
    }

    update();
}

void PDFViewer::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Middle) {
        m_pan_starting_position = to_content_position(event.position());
        set_override_cursor(Gfx::StandardCursor::Drag);
    }
}

void PDFViewer::mouseup_event(GUI::MouseEvent&)
{
    set_override_cursor(Gfx::StandardCursor::None);
}

void PDFViewer::mousemove_event(GUI::MouseEvent& event)
{
    if (event.buttons() & GUI::MouseButton::Middle) {
        auto delta = to_content_position(event.position()) - m_pan_starting_position;
        horizontal_scrollbar().decrease_slider_by(delta.x());
        vertical_scrollbar().decrease_slider_by(delta.y());
        update();
    }
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
    if (m_zoom_level < zoom_levels.size() - 1) {
        m_zoom_level++;
        MUST(cache_page_dimensions());
        update();
    }
}

void PDFViewer::zoom_out()
{
    if (m_zoom_level > 0) {
        m_zoom_level--;
        MUST(cache_page_dimensions());
        update();
    }
}

void PDFViewer::reset_zoom()
{
    m_zoom_level = initial_zoom_level;
    MUST(cache_page_dimensions());
    update();
}

void PDFViewer::rotate(int degrees)
{
    m_rotations = (m_rotations + degrees + 360) % 360;
    MUST(cache_page_dimensions());
    update();
}

void PDFViewer::set_page_view_mode(PageViewMode mode)
{
    m_page_view_mode = mode;
    Config::write_i32("PDFViewer", "Display", "PageMode", static_cast<i32>(mode));
    update();
}

PDF::PDFErrorOr<NonnullRefPtr<Gfx::Bitmap>> PDFViewer::render_page(u32 page_index)
{
    auto page = TRY(m_document->get_page(page_index));
    auto& page_size = m_page_dimension_cache.render_info[page_index].size;
    auto bitmap = TRY(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, page_size.to_type<int>()));

    TRY(PDF::Renderer::render(*m_document, page, bitmap));

    if (page.rotate + m_rotations != 0) {
        int rotation_count = ((page.rotate + m_rotations) / 90) % 4;
        if (rotation_count == 3) {
            bitmap = TRY(bitmap->rotated(Gfx::RotationDirection::CounterClockwise));
        } else {
            for (int i = 0; i < rotation_count; i++)
                bitmap = TRY(bitmap->rotated(Gfx::RotationDirection::Clockwise));
        }
    }

    return bitmap;
}

PDF::PDFErrorOr<void> PDFViewer::cache_page_dimensions(bool recalculate_fixed_info)
{
    if (recalculate_fixed_info)
        m_page_dimension_cache.page_info.clear_with_capacity();

    if (m_page_dimension_cache.page_info.is_empty()) {
        m_page_dimension_cache.page_info.ensure_capacity(m_document->get_page_count());
        for (size_t i = 0; i < m_document->get_page_count(); i++) {
            auto page = TRY(m_document->get_page(i));
            auto box = page.media_box;
            m_page_dimension_cache.page_info.unchecked_append(PageDimensionCache::PageInfo {
                { box.width(), box.height() },
                page.rotate,
            });
        }
    }

    auto zoom_scale_factor = static_cast<float>(zoom_levels[m_zoom_level]) / 100.0f;

    m_page_dimension_cache.render_info.clear_with_capacity();
    m_page_dimension_cache.render_info.ensure_capacity(m_page_dimension_cache.page_info.size());

    float max_width = 0;
    float total_height = 0;

    for (size_t i = 0; i < m_page_dimension_cache.page_info.size(); i++) {
        auto& [size, rotation] = m_page_dimension_cache.page_info[i];
        rotation += m_rotations;
        auto page_scale_factor = size.height() / size.width();

        auto height = static_cast<float>(this->height() - 2 * frame_thickness()) * zoom_scale_factor - PAGE_PADDING * 2;
        auto width = height / page_scale_factor;
        if (rotation % 2)
            swap(width, height);

        max_width = max(max_width, width);

        m_page_dimension_cache.render_info.append({
            { width, height },
            total_height,
        });

        total_height += height;
    }

    m_page_dimension_cache.max_width = max_width;
    m_page_dimension_cache.total_height = total_height;

    return {};
}

void PDFViewer::change_page(u32 new_page)
{
    m_current_page_index = new_page;
    if (on_page_change)
        on_page_change(m_current_page_index);
}
