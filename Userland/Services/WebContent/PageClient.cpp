/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/ShareableBitmap.h>
#include <LibGfx/SystemTheme.h>
#include <LibJS/Console.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/CSS/SystemColor.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/DOM/Attr.h>
#include <LibWeb/DOM/NamedNodeMap.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/CommandExecutorCPU.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/ViewportPaintable.h>
#include <LibWeb/Platform/Timer.h>
#include <LibWebView/Attribute.h>
#include <WebContent/ConnectionFromClient.h>
#include <WebContent/PageClient.h>
#include <WebContent/PageHost.h>
#include <WebContent/WebContentClientEndpoint.h>
#include <WebContent/WebDriverConnection.h>

#ifdef HAS_ACCELERATED_GRAPHICS
#    include <LibWeb/Painting/CommandExecutorGPU.h>
#endif

namespace WebContent {

static bool s_use_gpu_painter = false;
static bool s_use_experimental_cpu_transform_support = false;

JS_DEFINE_ALLOCATOR(PageClient);

void PageClient::set_use_gpu_painter()
{
    s_use_gpu_painter = true;
}

void PageClient::set_use_experimental_cpu_transform_support()
{
    s_use_experimental_cpu_transform_support = true;
}

JS::NonnullGCPtr<PageClient> PageClient::create(JS::VM& vm, PageHost& page_host, u64 id)
{
    return vm.heap().allocate_without_realm<PageClient>(page_host, id);
}

PageClient::PageClient(PageHost& owner, u64 id)
    : m_owner(owner)
    , m_page(Web::Page::create(Web::Bindings::main_thread_vm(), *this))
    , m_id(id)
{
    setup_palette();

#ifdef HAS_ACCELERATED_GRAPHICS
    if (s_use_gpu_painter) {
        auto context = AccelGfx::Context::create();
        if (context.is_error()) {
            dbgln("Failed to create AccelGfx context: {}", context.error());
            VERIFY_NOT_REACHED();
        }
        m_accelerated_graphics_context = context.release_value();
    }
#endif
}

PageClient::~PageClient() = default;

void PageClient::schedule_repaint()
{
    if (m_paint_state != PaintState::Ready) {
        m_paint_state = PaintState::PaintWhenReady;
        return;
    }
}

bool PageClient::is_ready_to_paint() const
{
    return m_paint_state == PaintState::Ready;
}

void PageClient::ready_to_paint()
{
    auto old_paint_state = exchange(m_paint_state, PaintState::Ready);

    if (old_paint_state == PaintState::PaintWhenReady) {
        // NOTE: Repainting always has to be scheduled from HTML event loop processing steps
        //       to make sure style and layout are up-to-date.
        page().top_level_traversable()->set_needs_display();
    }
}

void PageClient::add_backing_store(i32 front_bitmap_id, Gfx::ShareableBitmap const& front_bitmap, i32 back_bitmap_id, Gfx::ShareableBitmap const& back_bitmap)
{
    m_backing_stores.front_bitmap_id = front_bitmap_id;
    m_backing_stores.back_bitmap_id = back_bitmap_id;
    m_backing_stores.front_bitmap = *const_cast<Gfx::ShareableBitmap&>(front_bitmap).bitmap();
    m_backing_stores.back_bitmap = *const_cast<Gfx::ShareableBitmap&>(back_bitmap).bitmap();
}

void PageClient::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_page);
    visitor.ignore(m_console_clients);
}

ConnectionFromClient& PageClient::client() const
{
    return m_owner.client();
}

void PageClient::set_has_focus(bool has_focus)
{
    m_has_focus = has_focus;
}

void PageClient::setup_palette()
{
    // FIXME: Get the proper palette from our peer somehow
    auto buffer_or_error = Core::AnonymousBuffer::create_with_size(sizeof(Gfx::SystemTheme));
    VERIFY(!buffer_or_error.is_error());
    auto buffer = buffer_or_error.release_value();
    auto* theme = buffer.data<Gfx::SystemTheme>();
    theme->color[to_underlying(Gfx::ColorRole::Window)] = Color(Color::Magenta).value();
    theme->color[to_underlying(Gfx::ColorRole::WindowText)] = Color(Color::Cyan).value();
    m_palette_impl = Gfx::PaletteImpl::create_with_anonymous_buffer(buffer);
}

bool PageClient::is_connection_open() const
{
    return client().is_open();
}

Gfx::Palette PageClient::palette() const
{
    return Gfx::Palette(*m_palette_impl);
}

void PageClient::set_palette_impl(Gfx::PaletteImpl& impl)
{
    m_palette_impl = impl;
    if (auto* document = page().top_level_browsing_context().active_document())
        document->invalidate_style();
}

void PageClient::set_preferred_color_scheme(Web::CSS::PreferredColorScheme color_scheme)
{
    m_preferred_color_scheme = color_scheme;
    if (auto* document = page().top_level_browsing_context().active_document())
        document->invalidate_style();
}

void PageClient::set_is_scripting_enabled(bool is_scripting_enabled)
{
    page().set_is_scripting_enabled(is_scripting_enabled);
}

void PageClient::set_window_position(Web::DevicePixelPoint position)
{
    page().set_window_position(position);
}

void PageClient::set_window_size(Web::DevicePixelSize size)
{
    page().set_window_size(size);
}

Web::Layout::Viewport* PageClient::layout_root()
{
    auto* document = page().top_level_browsing_context().active_document();
    if (!document)
        return nullptr;
    return document->layout_node();
}

void PageClient::paint_next_frame()
{
    if (!m_backing_stores.back_bitmap) {
        return;
    }

    auto& back_bitmap = *m_backing_stores.back_bitmap;
    auto viewport_rect = page().css_to_device_rect(page().top_level_traversable()->viewport_rect());
    paint(viewport_rect, back_bitmap);

    auto& backing_stores = m_backing_stores;
    swap(backing_stores.front_bitmap, backing_stores.back_bitmap);
    swap(backing_stores.front_bitmap_id, backing_stores.back_bitmap_id);

    m_paint_state = PaintState::WaitingForClient;
    client().async_did_paint(m_id, viewport_rect.to_type<int>(), backing_stores.front_bitmap_id);
}

void PageClient::paint(Web::DevicePixelRect const& content_rect, Gfx::Bitmap& target, Web::PaintOptions paint_options)
{
    Web::Painting::CommandList painting_commands;
    Web::Painting::RecordingPainter recording_painter(painting_commands);

    Gfx::IntRect bitmap_rect { {}, content_rect.size().to_type<int>() };
    recording_painter.fill_rect(bitmap_rect, Web::CSS::SystemColor::canvas());

    Web::HTML::Navigable::PaintConfig paint_config;
    paint_config.paint_overlay = paint_options.paint_overlay == Web::PaintOptions::PaintOverlay::Yes;
    paint_config.should_show_line_box_borders = m_should_show_line_box_borders;
    paint_config.has_focus = m_has_focus;
    page().top_level_traversable()->paint(recording_painter, paint_config);

    if (s_use_gpu_painter) {
#ifdef HAS_ACCELERATED_GRAPHICS
        Web::Painting::CommandExecutorGPU painting_command_executor(*m_accelerated_graphics_context, target);
        painting_commands.execute(painting_command_executor);
#else
        static bool has_warned_about_configuration = false;

        if (!has_warned_about_configuration) {
            warnln("\033[31;1mConfigured to use GPU painter, but current platform does not have accelerated graphics\033[0m");
            has_warned_about_configuration = true;
        }
#endif
    } else {
        Web::Painting::CommandExecutorCPU painting_command_executor(target, s_use_experimental_cpu_transform_support);
        painting_commands.execute(painting_command_executor);
    }
}

void PageClient::set_viewport_rect(Web::DevicePixelRect const& rect)
{
    page().top_level_traversable()->set_viewport_rect(page().device_to_css_rect(rect));
}

void PageClient::page_did_request_cursor_change(Gfx::StandardCursor cursor)
{
    client().async_did_request_cursor_change(m_id, (u32)cursor);
}

void PageClient::page_did_layout()
{
    auto* layout_root = this->layout_root();
    VERIFY(layout_root);
    if (layout_root->paintable_box()->has_scrollable_overflow())
        m_content_size = page().enclosing_device_rect(layout_root->paintable_box()->scrollable_overflow_rect().value()).size();
    else
        m_content_size = page().enclosing_device_rect(layout_root->paintable_box()->absolute_rect()).size();
    client().async_did_layout(m_id, m_content_size.to_type<int>());
}

void PageClient::page_did_change_title(ByteString const& title)
{
    client().async_did_change_title(m_id, title);
}

void PageClient::page_did_change_url(URL::URL const& url)
{
    client().async_did_change_url(m_id, url);
}

void PageClient::page_did_request_navigate_back()
{
    client().async_did_request_navigate_back(m_id);
}

void PageClient::page_did_request_navigate_forward()
{
    client().async_did_request_navigate_forward(m_id);
}

void PageClient::page_did_request_refresh()
{
    client().async_did_request_refresh(m_id);
}

Gfx::IntSize PageClient::page_did_request_resize_window(Gfx::IntSize size)
{
    return client().did_request_resize_window(m_id, size);
}

Gfx::IntPoint PageClient::page_did_request_reposition_window(Gfx::IntPoint position)
{
    return client().did_request_reposition_window(m_id, position);
}

void PageClient::page_did_request_restore_window()
{
    client().async_did_request_restore_window(m_id);
}

Gfx::IntRect PageClient::page_did_request_maximize_window()
{
    return client().did_request_maximize_window(m_id);
}

Gfx::IntRect PageClient::page_did_request_minimize_window()
{
    return client().did_request_minimize_window(m_id);
}

Gfx::IntRect PageClient::page_did_request_fullscreen_window()
{
    return client().did_request_fullscreen_window(m_id);
}

void PageClient::page_did_request_scroll(i32 x_delta, i32 y_delta)
{
    client().async_did_request_scroll(m_id, x_delta, y_delta);
}

void PageClient::page_did_request_scroll_to(Web::CSSPixelPoint scroll_position)
{
    // NOTE: The viewport scroll position is updated preemptively, so that subsequent
    //       viewport offset calculation could use new offset even before actual
    //       scroll on browser side happens.
    auto viewport = page().top_level_traversable()->viewport_rect();
    viewport.set_location(scroll_position);
    page().top_level_traversable()->set_viewport_rect(viewport);

    auto device_scroll_position = page().css_to_device_point(scroll_position);
    client().async_did_request_scroll_to(m_id, device_scroll_position.to_type<int>());
}

void PageClient::page_did_enter_tooltip_area(Web::CSSPixelPoint content_position, ByteString const& title)
{
    auto device_position = page().css_to_device_point(content_position);
    client().async_did_enter_tooltip_area(m_id, { device_position.x(), device_position.y() }, title);
}

void PageClient::page_did_leave_tooltip_area()
{
    client().async_did_leave_tooltip_area(m_id);
}

void PageClient::page_did_hover_link(URL::URL const& url)
{
    client().async_did_hover_link(m_id, url);
}

void PageClient::page_did_unhover_link()
{
    client().async_did_unhover_link(m_id);
}

void PageClient::page_did_middle_click_link(URL::URL const& url, [[maybe_unused]] ByteString const& target, [[maybe_unused]] unsigned modifiers)
{
    client().async_did_middle_click_link(m_id, url, target, modifiers);
}

void PageClient::page_did_start_loading(URL::URL const& url, bool is_redirect)
{
    client().async_did_start_loading(m_id, url, is_redirect);
}

void PageClient::page_did_create_new_document(Web::DOM::Document& document)
{
    initialize_js_console(document);
}

void PageClient::page_did_change_active_document_in_top_level_browsing_context(Web::DOM::Document& document)
{
    VERIFY(m_console_clients.contains(document));
    m_top_level_document_console_client = *m_console_clients.get(document).value();
}

void PageClient::page_did_destroy_document(Web::DOM::Document& document)
{
    destroy_js_console(document);
}

void PageClient::page_did_finish_loading(URL::URL const& url)
{
    client().async_did_finish_loading(m_id, url);
}

void PageClient::page_did_finish_text_test()
{
    client().async_did_finish_text_test(m_id);
}

void PageClient::page_did_request_context_menu(Web::CSSPixelPoint content_position)
{
    client().async_did_request_context_menu(m_id, page().css_to_device_point(content_position).to_type<int>());
}

void PageClient::page_did_request_link_context_menu(Web::CSSPixelPoint content_position, URL::URL const& url, ByteString const& target, unsigned modifiers)
{
    client().async_did_request_link_context_menu(m_id, page().css_to_device_point(content_position).to_type<int>(), url, target, modifiers);
}

void PageClient::page_did_request_image_context_menu(Web::CSSPixelPoint content_position, URL::URL const& url, ByteString const& target, unsigned modifiers, Gfx::Bitmap const* bitmap_pointer)
{
    auto bitmap = bitmap_pointer ? bitmap_pointer->to_shareable_bitmap() : Gfx::ShareableBitmap();
    client().async_did_request_image_context_menu(m_id, page().css_to_device_point(content_position).to_type<int>(), url, target, modifiers, bitmap);
}

void PageClient::page_did_request_media_context_menu(Web::CSSPixelPoint content_position, ByteString const& target, unsigned modifiers, Web::Page::MediaContextMenu menu)
{
    client().async_did_request_media_context_menu(m_id, page().css_to_device_point(content_position).to_type<int>(), target, modifiers, move(menu));
}

void PageClient::page_did_request_alert(String const& message)
{
    client().async_did_request_alert(m_id, message);
}

void PageClient::alert_closed()
{
    page().alert_closed();
}

void PageClient::page_did_request_confirm(String const& message)
{
    client().async_did_request_confirm(m_id, message);
}

void PageClient::confirm_closed(bool accepted)
{
    page().confirm_closed(accepted);
}

void PageClient::page_did_request_prompt(String const& message, String const& default_)
{
    client().async_did_request_prompt(m_id, message, default_);
}

void PageClient::page_did_request_set_prompt_text(String const& text)
{
    client().async_did_request_set_prompt_text(m_id, text);
}

void PageClient::prompt_closed(Optional<String> response)
{
    page().prompt_closed(move(response));
}

void PageClient::color_picker_update(Optional<Color> picked_color, Web::HTML::ColorPickerUpdateState state)
{
    page().color_picker_update(picked_color, state);
}

void PageClient::select_dropdown_closed(Optional<u32> const& selected_item_id)
{
    page().select_dropdown_closed(selected_item_id);
}

Web::WebIDL::ExceptionOr<void> PageClient::toggle_media_play_state()
{
    return page().toggle_media_play_state();
}

void PageClient::toggle_media_mute_state()
{
    page().toggle_media_mute_state();
}

Web::WebIDL::ExceptionOr<void> PageClient::toggle_media_loop_state()
{
    return page().toggle_media_loop_state();
}

Web::WebIDL::ExceptionOr<void> PageClient::toggle_media_controls_state()
{
    return page().toggle_media_controls_state();
}

void PageClient::set_user_style(String source)
{
    page().set_user_style(source);
}

void PageClient::page_did_request_accept_dialog()
{
    client().async_did_request_accept_dialog(m_id);
}

void PageClient::page_did_request_dismiss_dialog()
{
    client().async_did_request_dismiss_dialog(m_id);
}

void PageClient::page_did_change_favicon(Gfx::Bitmap const& favicon)
{
    client().async_did_change_favicon(m_id, favicon.to_shareable_bitmap());
}

Vector<Web::Cookie::Cookie> PageClient::page_did_request_all_cookies(URL::URL const& url)
{
    return client().did_request_all_cookies(m_id, url);
}

Optional<Web::Cookie::Cookie> PageClient::page_did_request_named_cookie(URL::URL const& url, String const& name)
{
    return client().did_request_named_cookie(m_id, url, name);
}

String PageClient::page_did_request_cookie(URL::URL const& url, Web::Cookie::Source source)
{
    auto response = client().send_sync_but_allow_failure<Messages::WebContentClient::DidRequestCookie>(m_id, move(url), source);
    if (!response) {
        dbgln("WebContent client disconnected during DidRequestCookie. Exiting peacefully.");
        exit(0);
    }
    return response->take_cookie();
}

void PageClient::page_did_set_cookie(URL::URL const& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source)
{
    auto response = client().send_sync_but_allow_failure<Messages::WebContentClient::DidSetCookie>(m_id, url, cookie, source);
    if (!response) {
        dbgln("WebContent client disconnected during DidSetCookie. Exiting peacefully.");
        exit(0);
    }
}

void PageClient::page_did_update_cookie(Web::Cookie::Cookie cookie)
{
    client().async_did_update_cookie(m_id, move(cookie));
}

void PageClient::page_did_update_resource_count(i32 count_waiting)
{
    client().async_did_update_resource_count(m_id, count_waiting);
}

PageClient::NewWebViewResult PageClient::page_did_request_new_web_view(Web::HTML::ActivateTab activate_tab, Web::HTML::WebViewHints hints, Web::HTML::TokenizedFeature::NoOpener no_opener)
{
    auto& new_client = m_owner.create_page();

    Optional<u64> page_id;
    if (no_opener == Web::HTML::TokenizedFeature::NoOpener::Yes) {
        // FIXME: Create an abstraction to let this WebContent process know about a new process we create?
        // FIXME: For now, just create a new page in the same process anyway
    }

    page_id = new_client.m_id;

    auto response = client().send_sync_but_allow_failure<Messages::WebContentClient::DidRequestNewWebView>(m_id, activate_tab, hints, page_id);
    if (!response) {
        dbgln("WebContent client disconnected during DidRequestNewWebView. Exiting peacefully.");
        exit(0);
    }

    return { &new_client.page(), response->take_handle() };
}

void PageClient::page_did_request_activate_tab()
{
    client().async_did_request_activate_tab(m_id);
}

void PageClient::page_did_close_top_level_traversable()
{
    // FIXME: Rename this IPC call
    client().async_did_close_browsing_context(m_id);

    // NOTE: This only removes the strong reference the PageHost has for this PageClient.
    //       It will be GC'd 'later'.
    m_owner.remove_page({}, m_id);
}

void PageClient::page_did_update_navigation_buttons_state(bool back_enabled, bool forward_enabled)
{
    client().async_did_update_navigation_buttons_state(m_id, back_enabled, forward_enabled);
}

void PageClient::request_file(Web::FileRequest file_request)
{
    client().request_file(m_id, move(file_request));
}

void PageClient::page_did_request_color_picker(Color current_color)
{
    client().async_did_request_color_picker(m_id, current_color);
}

void PageClient::page_did_request_file_picker(Web::HTML::FileFilter accepted_file_types, Web::HTML::AllowMultipleFiles allow_multiple_files)
{
    client().async_did_request_file_picker(m_id, move(accepted_file_types), allow_multiple_files);
}

void PageClient::page_did_request_select_dropdown(Web::CSSPixelPoint content_position, Web::CSSPixels minimum_width, Vector<Web::HTML::SelectItem> items)
{
    client().async_did_request_select_dropdown(m_id, page().css_to_device_point(content_position).to_type<int>(), minimum_width * device_pixels_per_css_pixel(), items);
}

void PageClient::page_did_change_theme_color(Gfx::Color color)
{
    client().async_did_change_theme_color(m_id, color);
}

void PageClient::page_did_insert_clipboard_entry(String data, String presentation_style, String mime_type)
{
    client().async_did_insert_clipboard_entry(m_id, move(data), move(presentation_style), move(mime_type));
}

void PageClient::page_did_change_audio_play_state(Web::HTML::AudioPlayState play_state)
{
    client().async_did_change_audio_play_state(m_id, play_state);
}

IPC::File PageClient::request_worker_agent()
{
    auto response = client().send_sync_but_allow_failure<Messages::WebContentClient::RequestWorkerAgent>(m_id);
    if (!response) {
        dbgln("WebContent client disconnected during RequestWorkerAgent. Exiting peacefully.");
        exit(0);
    }

    return response->take_socket();
}

void PageClient::inspector_did_load()
{
    client().async_inspector_did_load(m_id);
}

void PageClient::inspector_did_select_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement::Type> const& pseudo_element)
{
    client().async_inspector_did_select_dom_node(m_id, node_id, pseudo_element);
}

void PageClient::inspector_did_set_dom_node_text(i32 node_id, String const& text)
{
    client().async_inspector_did_set_dom_node_text(m_id, node_id, text);
}

void PageClient::inspector_did_set_dom_node_tag(i32 node_id, String const& tag)
{
    client().async_inspector_did_set_dom_node_tag(m_id, node_id, tag);
}

static Vector<WebView::Attribute> named_node_map_to_vector(JS::NonnullGCPtr<Web::DOM::NamedNodeMap> map)
{
    Vector<WebView::Attribute> attributes;
    attributes.ensure_capacity(map->length());

    for (size_t i = 0; i < map->length(); ++i) {
        auto const* attribute = map->item(i);
        VERIFY(attribute);

        attributes.empend(attribute->name().to_string(), attribute->value());
    }

    return attributes;
}

void PageClient::inspector_did_add_dom_node_attributes(i32 node_id, JS::NonnullGCPtr<Web::DOM::NamedNodeMap> attributes)
{
    client().async_inspector_did_add_dom_node_attributes(m_id, node_id, named_node_map_to_vector(attributes));
}

void PageClient::inspector_did_replace_dom_node_attribute(i32 node_id, size_t attribute_index, JS::NonnullGCPtr<Web::DOM::NamedNodeMap> replacement_attributes)
{
    client().async_inspector_did_replace_dom_node_attribute(m_id, node_id, attribute_index, named_node_map_to_vector(replacement_attributes));
}

void PageClient::inspector_did_request_dom_tree_context_menu(i32 node_id, Web::CSSPixelPoint position, String const& type, Optional<String> const& tag, Optional<size_t> const& attribute_index)
{
    client().async_inspector_did_request_dom_tree_context_menu(m_id, node_id, page().css_to_device_point(position).to_type<int>(), type, tag, attribute_index);
}

void PageClient::inspector_did_execute_console_script(String const& script)
{
    client().async_inspector_did_execute_console_script(m_id, script);
}

ErrorOr<void> PageClient::connect_to_webdriver(ByteString const& webdriver_ipc_path)
{
    VERIFY(!m_webdriver);
    m_webdriver = TRY(WebDriverConnection::connect(*this, webdriver_ipc_path));

    if (m_owner.on_webdriver_connection)
        m_owner.on_webdriver_connection(*m_webdriver);

    return {};
}

void PageClient::initialize_js_console(Web::DOM::Document& document)
{
    auto& realm = document.realm();
    auto console_object = realm.intrinsics().console_object();
    auto console_client = heap().allocate_without_realm<WebContentConsoleClient>(console_object->console(), document.realm(), *this);
    console_object->console().set_client(*console_client);

    m_console_clients.set(document, console_client);
}

void PageClient::destroy_js_console(Web::DOM::Document& document)
{
    m_console_clients.remove(document);
}

void PageClient::js_console_input(ByteString const& js_source)
{
    if (m_top_level_document_console_client)
        m_top_level_document_console_client->handle_input(js_source);
}

void PageClient::run_javascript(ByteString const& js_source)
{
    auto* active_document = page().top_level_browsing_context().active_document();

    if (!active_document)
        return;

    // This is partially based on "execute a javascript: URL request" https://html.spec.whatwg.org/multipage/browsing-the-web.html#javascript-protocol

    // Let settings be browsingContext's active document's relevant settings object.
    auto& settings = active_document->relevant_settings_object();

    // Let baseURL be settings's API base URL.
    auto base_url = settings.api_base_url();

    // Let script be the result of creating a classic script given scriptSource, settings, baseURL, and the default classic script fetch options.
    // FIXME: This doesn't pass in "default classic script fetch options"
    // FIXME: What should the filename be here?
    auto script = Web::HTML::ClassicScript::create("(client connection run_javascript)", js_source, settings, move(base_url));

    // Let evaluationStatus be the result of running the classic script script.
    auto evaluation_status = script->run();

    if (evaluation_status.is_error())
        dbgln("Exception :(");
}

void PageClient::js_console_request_messages(i32 start_index)
{
    if (m_top_level_document_console_client)
        m_top_level_document_console_client->send_messages(start_index);
}

void PageClient::did_output_js_console_message(i32 message_index)
{
    client().async_did_output_js_console_message(m_id, message_index);
}

void PageClient::console_peer_did_misbehave(char const* reason)
{
    client().did_misbehave(reason);
}

void PageClient::did_get_js_console_messages(i32 start_index, Vector<ByteString> message_types, Vector<ByteString> messages)
{
    client().async_did_get_js_console_messages(m_id, start_index, move(message_types), move(messages));
}

}
