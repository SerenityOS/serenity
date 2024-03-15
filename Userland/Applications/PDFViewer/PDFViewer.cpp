/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PDFViewer.h"
#include <AK/Array.h>
#include <AK/BinarySearch.h>
#include <AK/HashFunctions.h>
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

    m_page_view_mode = static_cast<PageViewMode>(Config::read_i32("PDFViewer"sv, "Display"sv, "PageMode"sv, 0));
    m_rendering_preferences.show_clipping_paths = Config::read_bool("PDFViewer"sv, "Rendering"sv, "ShowClippingPaths"sv, false);
    m_rendering_preferences.show_images = Config::read_bool("PDFViewer"sv, "Rendering"sv, "ShowImages"sv, true);
    m_rendering_preferences.show_hidden_text = Config::read_bool("PDFViewer"sv, "Rendering"sv, "ShowHiddenText"sv, false);
    m_rendering_preferences.show_diagnostics = Config::read_bool("PDFViewer"sv, "Rendering"sv, "ShowDiagnostics"sv, false);
    m_rendering_preferences.clip_images = Config::read_bool("PDFViewer"sv, "Rendering"sv, "ClipImages"sv, true);
    m_rendering_preferences.clip_paths = Config::read_bool("PDFViewer"sv, "Rendering"sv, "ClipPaths"sv, true);
    m_rendering_preferences.clip_text = Config::read_bool("PDFViewer"sv, "Rendering"sv, "ClipText"sv, true);
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
    auto key = pair_int_hash(m_rendering_preferences.hash(), m_zoom_level);
    auto& rendered_page_map = m_rendered_page_list[index];
    auto existing_rendered_page = rendered_page_map.get(key);
    if (existing_rendered_page.has_value() && existing_rendered_page.value().rotation == m_rotations)
        return existing_rendered_page.value().bitmap;

    auto rendered_page = TRY(render_page(index));
    rendered_page_map.set(key, { rendered_page, m_rotations });
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

    auto handle_error = [&](PDF::Error& error) {
        warnln("{}", error.message());
        GUI::MessageBox::show_error(nullptr, "Failed to render the page."sv);
        m_document.clear();
    };

    if (m_page_view_mode == PageViewMode::Single) {
        auto maybe_page = get_rendered_page(m_current_page_index);
        if (maybe_page.is_error()) {
            handle_error(maybe_page.error());
            return;
        }

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
        if (maybe_page.is_error()) {
            handle_error(maybe_page.error());
            return;
        }

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

void PDFViewer::set_show_rendering_diagnostics(bool show_diagnostics)
{
    m_rendering_preferences.show_diagnostics = show_diagnostics;
    Config::write_bool("PDFViewer"sv, "Rendering"sv, "ShowDiagnostics"sv, show_diagnostics);
    update();
}

void PDFViewer::set_show_clipping_paths(bool show_clipping_paths)
{
    m_rendering_preferences.show_clipping_paths = show_clipping_paths;
    Config::write_bool("PDFViewer"sv, "Rendering"sv, "ShowClippingPaths"sv, show_clipping_paths);
    update();
}

void PDFViewer::set_show_images(bool show_images)
{
    m_rendering_preferences.show_images = show_images;
    Config::write_bool("PDFViewer"sv, "Rendering"sv, "ShowImages"sv, show_images);
    update();
}

void PDFViewer::set_show_hidden_text(bool show_hidden_text)
{
    m_rendering_preferences.show_hidden_text = show_hidden_text;
    Config::write_bool("PDFViewer"sv, "Rendering"sv, "ShowHiddenText"sv, show_hidden_text);
    update();
}

void PDFViewer::set_clip_images(bool clip_images)
{
    m_rendering_preferences.clip_images = clip_images;
    Config::write_bool("PDFViewer"sv, "Rendering"sv, "ClipImages"sv, clip_images);
    update();
}

void PDFViewer::set_clip_paths(bool clip_paths)
{
    m_rendering_preferences.clip_paths = clip_paths;
    Config::write_bool("PDFViewer"sv, "Rendering"sv, "ClipPaths"sv, clip_paths);
    update();
}

void PDFViewer::set_clip_text(bool clip_text)
{
    m_rendering_preferences.clip_text = clip_text;
    Config::write_bool("PDFViewer"sv, "Rendering"sv, "ClipText"sv, clip_text);
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
    auto delta = abs(event.wheel_delta_y() * 20);

    if (m_page_view_mode == PageViewMode::Multiple) {
        if (scrolled_down) {
            if (scrollbar.value() != scrollbar.max())
                scrollbar.increase_slider_by(delta);
        } else {
            if (scrollbar.value() > 0)
                scrollbar.decrease_slider_by(delta);
        }
    } else {
        if (scrolled_down) {
            if (scrollbar.value() == scrollbar.max()) {
                if (m_current_page_index < m_document->get_page_count() - 1) {
                    change_page(m_current_page_index + 1);
                    scrollbar.set_value(0);
                }
            } else {
                scrollbar.increase_slider_by(delta);
            }
        } else {
            if (scrollbar.value() == 0) {
                if (m_current_page_index > 0) {
                    change_page(m_current_page_index - 1);
                    scrollbar.set_value(scrollbar.max());
                }
            } else {
                scrollbar.decrease_slider_by(delta);
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
    Config::write_i32("PDFViewer"sv, "Display"sv, "PageMode"sv, static_cast<i32>(mode));
    update();
}

PDF::PDFErrorOr<NonnullRefPtr<Gfx::Bitmap>> PDFViewer::render_page(u32 page_index)
{
    auto page = TRY(m_document->get_page(page_index));
    auto& page_size = m_page_dimension_cache.render_info[page_index].size;
    auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, page_size.to_type<int>()));

    auto maybe_errors = PDF::Renderer::render(*m_document, page, bitmap, Color::White, m_rendering_preferences);
    if (maybe_errors.is_error()) {
        auto errors = maybe_errors.release_error();
        on_render_errors(page_index, errors);
        return bitmap;
    }

    return TRY(PDF::Renderer::apply_page_rotation(bitmap, page, m_rotations));
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
