/*
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <LibCore/DateTime.h>
#include <LibCore/StandardPaths.h>
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibWebView/ViewImplementation.h>

namespace WebView {

ViewImplementation::ViewImplementation()
{
    m_backing_store_shrink_timer = Core::Timer::create_single_shot(3000, [this] {
        resize_backing_stores_if_needed(WindowResizeInProgress::No);
    }).release_value_but_fixme_should_propagate_errors();

    m_repeated_crash_timer = Core::Timer::create_single_shot(1000, [this] {
        // Reset the "crashing a lot" counter after 1 second in case we just
        // happen to be visiting crashy websites a lot.
        this->m_crash_count = 0;
    }).release_value_but_fixme_should_propagate_errors();

    on_request_file = [this](auto const& path, auto request_id) {
        auto file = Core::File::open(path, Core::File::OpenMode::Read);

        if (file.is_error())
            client().async_handle_file_return(file.error().code(), {}, request_id);
        else
            client().async_handle_file_return(0, IPC::File(*file.value()), request_id);
    };
}

WebContentClient& ViewImplementation::client()
{
    VERIFY(m_client_state.client);
    return *m_client_state.client;
}

WebContentClient const& ViewImplementation::client() const
{
    VERIFY(m_client_state.client);
    return *m_client_state.client;
}

void ViewImplementation::load(AK::URL const& url)
{
    m_url = url;
    client().async_load_url(url);
}

void ViewImplementation::load_html(StringView html, AK::URL const& url)
{
    m_url = url;
    client().async_load_html(html, url);
}

void ViewImplementation::load_empty_document()
{
    load_html(""sv, {});
}

void ViewImplementation::zoom_in()
{
    if (m_zoom_level >= ZOOM_MAX_LEVEL)
        return;
    m_zoom_level += ZOOM_STEP;
    update_zoom();
}

void ViewImplementation::zoom_out()
{
    if (m_zoom_level <= ZOOM_MIN_LEVEL)
        return;
    m_zoom_level -= ZOOM_STEP;
    update_zoom();
}

void ViewImplementation::reset_zoom()
{
    m_zoom_level = 1.0f;
    update_zoom();
}

void ViewImplementation::set_preferred_color_scheme(Web::CSS::PreferredColorScheme color_scheme)
{
    client().async_set_preferred_color_scheme(color_scheme);
}

DeprecatedString ViewImplementation::selected_text()
{
    return client().get_selected_text();
}

void ViewImplementation::select_all()
{
    client().async_select_all();
}

void ViewImplementation::get_source()
{
    client().async_get_source();
}

void ViewImplementation::inspect_dom_tree()
{
    client().async_inspect_dom_tree();
}

void ViewImplementation::inspect_accessibility_tree()
{
    client().async_inspect_accessibility_tree();
}

ErrorOr<ViewImplementation::DOMNodeProperties> ViewImplementation::inspect_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement> pseudo_element)
{
    auto response = client().inspect_dom_node(node_id, pseudo_element);
    if (!response.has_style())
        return Error::from_string_view("Inspected node returned no style"sv);
    return DOMNodeProperties {
        .computed_style_json = TRY(String::from_deprecated_string(response.take_computed_style())),
        .resolved_style_json = TRY(String::from_deprecated_string(response.take_resolved_style())),
        .custom_properties_json = TRY(String::from_deprecated_string(response.take_custom_properties())),
        .node_box_sizing_json = TRY(String::from_deprecated_string(response.take_node_box_sizing())),
        .aria_properties_state_json = TRY(String::from_deprecated_string(response.take_aria_properties_state())),
    };
}

void ViewImplementation::clear_inspected_dom_node()
{
    client().inspect_dom_node(0, {});
}

i32 ViewImplementation::get_hovered_node_id()
{
    return client().get_hovered_node_id();
}

void ViewImplementation::debug_request(DeprecatedString const& request, DeprecatedString const& argument)
{
    client().async_debug_request(request, argument);
}

void ViewImplementation::run_javascript(StringView js_source)
{
    client().async_run_javascript(js_source);
}

void ViewImplementation::js_console_input(DeprecatedString const& js_source)
{
    client().async_js_console_input(js_source);
}

void ViewImplementation::js_console_request_messages(i32 start_index)
{
    client().async_js_console_request_messages(start_index);
}

void ViewImplementation::alert_closed()
{
    client().async_alert_closed();
}

void ViewImplementation::confirm_closed(bool accepted)
{
    client().async_confirm_closed(accepted);
}

void ViewImplementation::prompt_closed(Optional<String> response)
{
    client().async_prompt_closed(move(response));
}

void ViewImplementation::toggle_media_play_state()
{
    client().async_toggle_media_play_state();
}

void ViewImplementation::toggle_media_mute_state()
{
    client().async_toggle_media_mute_state();
}

void ViewImplementation::toggle_media_loop_state()
{
    client().async_toggle_media_loop_state();
}

void ViewImplementation::toggle_media_controls_state()
{
    client().async_toggle_media_controls_state();
}

void ViewImplementation::handle_resize()
{
    resize_backing_stores_if_needed(WindowResizeInProgress::Yes);
    m_backing_store_shrink_timer->restart();
}

void ViewImplementation::resize_backing_stores_if_needed(WindowResizeInProgress window_resize_in_progress)
{
    if (m_client_state.has_usable_bitmap) {
        // NOTE: We keep the outgoing front bitmap as a backup so we have something to paint until we get a new one.
        m_backup_bitmap = m_client_state.front_bitmap.bitmap;
        m_backup_bitmap_size = m_client_state.front_bitmap.last_painted_size;
    }

    m_client_state.has_usable_bitmap = false;

    auto viewport_rect = this->viewport_rect();
    if (viewport_rect.is_empty())
        return;

    Gfx::IntSize minimum_needed_size;

    if (window_resize_in_progress == WindowResizeInProgress::Yes) {
        // Pad the minimum needed size so that we don't have to keep reallocating backing stores while the window is being resized.
        minimum_needed_size = { viewport_rect.width() + 256, viewport_rect.height() + 256 };
    } else {
        // If we're not in the middle of a resize, we can shrink the backing store size to match the viewport size.
        minimum_needed_size = viewport_rect.size();
        m_client_state.front_bitmap = {};
        m_client_state.back_bitmap = {};
    }

    auto reallocate_backing_store_if_needed = [&](SharedBitmap& backing_store) {
        if (!backing_store.bitmap || !backing_store.bitmap->size().contains(minimum_needed_size)) {
            if (auto new_bitmap_or_error = Gfx::Bitmap::create_shareable(Gfx::BitmapFormat::BGRA8888, minimum_needed_size); !new_bitmap_or_error.is_error()) {
                if (backing_store.bitmap)
                    client().async_remove_backing_store(backing_store.id);

                backing_store.pending_paints = 0;
                backing_store.bitmap = new_bitmap_or_error.release_value();
                backing_store.id = m_client_state.next_bitmap_id++;
                client().async_add_backing_store(backing_store.id, backing_store.bitmap->to_shareable_bitmap());
            }
            backing_store.last_painted_size = viewport_rect.size();
        }
    };

    reallocate_backing_store_if_needed(m_client_state.front_bitmap);
    reallocate_backing_store_if_needed(m_client_state.back_bitmap);

    request_repaint();
}

void ViewImplementation::request_repaint()
{
    // If this widget was instantiated but not yet added to a window,
    // it won't have a back bitmap yet, so we can just skip repaint requests.
    if (!m_client_state.back_bitmap.bitmap)
        return;
    // Don't request a repaint until pending paint requests have finished.
    if (m_client_state.back_bitmap.pending_paints) {
        m_client_state.got_repaint_requests_while_painting = true;
        return;
    }
    m_client_state.back_bitmap.pending_paints++;
    client().async_paint(viewport_rect(), m_client_state.back_bitmap.id);
}

void ViewImplementation::handle_web_content_process_crash()
{
    dbgln("WebContent process crashed!");

    ++m_crash_count;
    constexpr size_t max_reasonable_crash_count = 5U;
    if (m_crash_count >= max_reasonable_crash_count) {
        dbgln("WebContent has crashed {} times in quick succession! Not restarting...", m_crash_count);
        m_repeated_crash_timer->stop();
        return;
    }
    m_repeated_crash_timer->restart();

    create_client();
    VERIFY(m_client_state.client);

    // Don't keep a stale backup bitmap around.
    m_backup_bitmap = nullptr;

    handle_resize();
    StringBuilder builder;
    builder.append("<html><head><title>Crashed: "sv);
    builder.append(escape_html_entities(m_url.to_deprecated_string()));
    builder.append("</title></head><body>"sv);
    builder.append("<h1>Web page crashed"sv);
    if (!m_url.host().has<Empty>()) {
        builder.appendff(" on {}", escape_html_entities(m_url.serialized_host().release_value_but_fixme_should_propagate_errors()));
    }
    builder.append("</h1>"sv);
    auto escaped_url = escape_html_entities(m_url.to_deprecated_string());
    builder.appendff("The web page <a href=\"{}\">{}</a> has crashed.<br><br>You can reload the page to try again.", escaped_url, escaped_url);
    builder.append("</body></html>"sv);
    load_html(builder.to_deprecated_string(), m_url);
}

ErrorOr<void> ViewImplementation::take_screenshot(ScreenshotType type)
{
    Gfx::ShareableBitmap bitmap;

    switch (type) {
    case ScreenshotType::Visible:
        if (auto* visible_bitmap = m_client_state.has_usable_bitmap ? m_client_state.front_bitmap.bitmap.ptr() : m_backup_bitmap.ptr())
            bitmap = visible_bitmap->to_shareable_bitmap();
        break;
    case ScreenshotType::Full:
        bitmap = client().take_document_screenshot();
        break;
    }

    if (!bitmap.is_valid())
        return Error::from_string_view("Failed to take a screenshot of the current tab"sv);

    LexicalPath path { Core::StandardPaths::downloads_directory() };
    path = path.append(TRY(Core::DateTime::now().to_string("screenshot-%Y-%m-%d-%H-%M-%S.png"sv)));

    auto encoded = TRY(Gfx::PNGWriter::encode(*bitmap.bitmap()));

    auto screenshot_file = TRY(Core::File::open(path.string(), Core::File::OpenMode::Write));
    TRY(screenshot_file->write_until_depleted(encoded));

    return {};
}

}
