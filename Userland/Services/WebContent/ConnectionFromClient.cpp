/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/JsonObject.h>
#include <AK/QuickSort.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/SystemTheme.h>
#include <LibJS/Console.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Storage.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWeb/Loader/ProxyMappings.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
#include <WebContent/ConnectionFromClient.h>
#include <WebContent/PageHost.h>
#include <WebContent/WebContentClientEndpoint.h>
#include <pthread.h>

namespace WebContent {

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
    : IPC::ConnectionFromClient<WebContentClientEndpoint, WebContentServerEndpoint>(*this, move(socket), 1)
    , m_page_host(PageHost::create(*this))
{
    m_paint_flush_timer = Web::Platform::Timer::create_single_shot(0, [this] { flush_pending_paint_requests(); });
}

void ConnectionFromClient::die()
{
    Web::Platform::EventLoopPlugin::the().quit();
}

Web::Page& ConnectionFromClient::page()
{
    return m_page_host->page();
}

Web::Page const& ConnectionFromClient::page() const
{
    return m_page_host->page();
}

void ConnectionFromClient::connect_to_webdriver(DeprecatedString const& webdriver_ipc_path)
{
    // FIXME: Propagate this error back to the browser.
    if (auto result = m_page_host->connect_to_webdriver(webdriver_ipc_path); result.is_error())
        dbgln("Unable to connect to the WebDriver process: {}", result.error());
}

void ConnectionFromClient::update_system_theme(Core::AnonymousBuffer const& theme_buffer)
{
    Gfx::set_system_theme(theme_buffer);
    auto impl = Gfx::PaletteImpl::create_with_anonymous_buffer(theme_buffer);
    m_page_host->set_palette_impl(*impl);
}

void ConnectionFromClient::update_system_fonts(DeprecatedString const& default_font_query, DeprecatedString const& fixed_width_font_query, DeprecatedString const& window_title_font_query)
{
    Gfx::FontDatabase::set_default_font_query(default_font_query);
    Gfx::FontDatabase::set_fixed_width_font_query(fixed_width_font_query);
    Gfx::FontDatabase::set_window_title_font_query(window_title_font_query);
}

void ConnectionFromClient::update_screen_rects(Vector<Gfx::IntRect> const& rects, u32 main_screen)
{
    m_page_host->set_screen_rects(rects, main_screen);
}

void ConnectionFromClient::load_url(const URL& url)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentServer::LoadURL: url={}", url);

#if defined(AK_OS_SERENITY)
    DeprecatedString process_name;
    if (url.host().is_empty())
        process_name = "WebContent";
    else
        process_name = DeprecatedString::formatted("WebContent: {}", url.host());

    pthread_setname_np(pthread_self(), process_name.characters());
#endif

    page().load(url);
}

void ConnectionFromClient::load_html(DeprecatedString const& html, const URL& url)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentServer::LoadHTML: html={}, url={}", html, url);
    page().load_html(html, url);
}

void ConnectionFromClient::set_viewport_rect(Gfx::IntRect const& rect)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentServer::SetViewportRect: rect={}", rect);
    m_page_host->set_viewport_rect(rect);
}

void ConnectionFromClient::add_backing_store(i32 backing_store_id, Gfx::ShareableBitmap const& bitmap)
{
    m_backing_stores.set(backing_store_id, *bitmap.bitmap());
}

void ConnectionFromClient::remove_backing_store(i32 backing_store_id)
{
    m_backing_stores.remove(backing_store_id);
    m_pending_paint_requests.remove_all_matching([backing_store_id](auto& pending_repaint_request) { return pending_repaint_request.bitmap_id == backing_store_id; });
}

void ConnectionFromClient::paint(Gfx::IntRect const& content_rect, i32 backing_store_id)
{
    for (auto& pending_paint : m_pending_paint_requests) {
        if (pending_paint.bitmap_id == backing_store_id) {
            pending_paint.content_rect = content_rect;
            return;
        }
    }

    auto it = m_backing_stores.find(backing_store_id);
    if (it == m_backing_stores.end()) {
        did_misbehave("Client requested paint with backing store ID");
        return;
    }

    auto& bitmap = *it->value;
    m_pending_paint_requests.append({ content_rect, bitmap, backing_store_id });
    m_paint_flush_timer->start();
}

void ConnectionFromClient::flush_pending_paint_requests()
{
    for (auto& pending_paint : m_pending_paint_requests) {
        m_page_host->paint(pending_paint.content_rect.to_type<Web::DevicePixels>(), *pending_paint.bitmap);
        async_did_paint(pending_paint.content_rect, pending_paint.bitmap_id);
    }
    m_pending_paint_requests.clear();
}

void ConnectionFromClient::mouse_down(Gfx::IntPoint position, unsigned int button, unsigned int buttons, unsigned int modifiers)
{
    report_finished_handling_input_event(page().handle_mousedown(position.to_type<Web::DevicePixels>(), button, buttons, modifiers));
}

void ConnectionFromClient::mouse_move(Gfx::IntPoint position, [[maybe_unused]] unsigned int button, unsigned int buttons, unsigned int modifiers)
{
    report_finished_handling_input_event(page().handle_mousemove(position.to_type<Web::DevicePixels>(), buttons, modifiers));
}

void ConnectionFromClient::mouse_up(Gfx::IntPoint position, unsigned int button, unsigned int buttons, unsigned int modifiers)
{
    report_finished_handling_input_event(page().handle_mouseup(position.to_type<Web::DevicePixels>(), button, buttons, modifiers));
}

void ConnectionFromClient::mouse_wheel(Gfx::IntPoint position, unsigned int button, unsigned int buttons, unsigned int modifiers, i32 wheel_delta_x, i32 wheel_delta_y)
{
    report_finished_handling_input_event(page().handle_mousewheel(position.to_type<Web::DevicePixels>(), button, buttons, modifiers, wheel_delta_x, wheel_delta_y));
}

void ConnectionFromClient::doubleclick(Gfx::IntPoint position, unsigned int button, unsigned int buttons, unsigned int modifiers)
{
    report_finished_handling_input_event(page().handle_doubleclick(position.to_type<Web::DevicePixels>(), button, buttons, modifiers));
}

void ConnectionFromClient::key_down(i32 key, unsigned int modifiers, u32 code_point)
{
    report_finished_handling_input_event(page().handle_keydown((KeyCode)key, modifiers, code_point));
}

void ConnectionFromClient::key_up(i32 key, unsigned int modifiers, u32 code_point)
{
    report_finished_handling_input_event(page().handle_keyup((KeyCode)key, modifiers, code_point));
}

void ConnectionFromClient::report_finished_handling_input_event(bool event_was_handled)
{
    async_did_finish_handling_input_event(event_was_handled);
}

void ConnectionFromClient::debug_request(DeprecatedString const& request, DeprecatedString const& argument)
{
    if (request == "dump-dom-tree") {
        if (auto* doc = page().top_level_browsing_context().active_document())
            Web::dump_tree(*doc);
    }

    if (request == "dump-layout-tree") {
        if (auto* doc = page().top_level_browsing_context().active_document()) {
            if (auto* icb = doc->layout_node())
                Web::dump_tree(*icb);
        }
    }

    if (request == "dump-stacking-context-tree") {
        if (auto* doc = page().top_level_browsing_context().active_document()) {
            if (auto* icb = doc->layout_node()) {
                if (auto* stacking_context = icb->paint_box()->stacking_context())
                    stacking_context->dump();
            }
        }
    }

    if (request == "dump-style-sheets") {
        if (auto* doc = page().top_level_browsing_context().active_document()) {
            for (auto& sheet : doc->style_sheets().sheets()) {
                Web::dump_sheet(sheet);
            }
        }
    }

    if (request == "collect-garbage") {
        Web::Bindings::main_thread_vm().heap().collect_garbage(JS::Heap::CollectionType::CollectGarbage, true);
    }

    if (request == "set-line-box-borders") {
        bool state = argument == "on";
        m_page_host->set_should_show_line_box_borders(state);
        page().top_level_browsing_context().set_needs_display(page().top_level_browsing_context().viewport_rect());
    }

    if (request == "clear-cache") {
        Web::ResourceLoader::the().clear_cache();
    }

    if (request == "spoof-user-agent") {
        Web::ResourceLoader::the().set_user_agent(argument);
    }

    if (request == "same-origin-policy") {
        m_page_host->page().set_same_origin_policy_enabled(argument == "on");
    }

    if (request == "scripting") {
        m_page_host->page().set_is_scripting_enabled(argument == "on");
    }

    if (request == "block-pop-ups") {
        m_page_host->page().set_should_block_pop_ups(argument == "on");
    }

    if (request == "dump-local-storage") {
        if (auto* doc = page().top_level_browsing_context().active_document())
            doc->window().local_storage()->dump();
    }
}

void ConnectionFromClient::get_source()
{
    if (auto* doc = page().top_level_browsing_context().active_document()) {
        async_did_get_source(doc->url(), doc->source());
    }
}

void ConnectionFromClient::inspect_dom_tree()
{
    if (auto* doc = page().top_level_browsing_context().active_document()) {
        async_did_get_dom_tree(doc->dump_dom_tree_as_json());
    }
}

Messages::WebContentServer::InspectDomNodeResponse ConnectionFromClient::inspect_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement> const& pseudo_element)
{
    auto& top_context = page().top_level_browsing_context();

    top_context.for_each_in_inclusive_subtree([&](auto& ctx) {
        if (ctx.active_document() != nullptr) {
            ctx.active_document()->set_inspected_node(nullptr);
        }
        return IterationDecision::Continue;
    });

    Web::DOM::Node* node = Web::DOM::Node::from_id(node_id);
    // Note: Nodes without layout (aka non-visible nodes, don't have style computed)
    if (!node || !node->layout_node()) {
        return { false, "", "", "", "" };
    }

    // FIXME: Pass the pseudo-element here.
    node->document().set_inspected_node(node);

    if (node->is_element()) {
        auto& element = verify_cast<Web::DOM::Element>(*node);
        if (!element.computed_css_values())
            return { false, "", "", "", "" };

        auto serialize_json = [](Web::CSS::StyleProperties const& properties) -> DeprecatedString {
            StringBuilder builder;

            auto serializer = MUST(JsonObjectSerializer<>::try_create(builder));
            properties.for_each_property([&](auto property_id, auto& value) {
                MUST(serializer.add(Web::CSS::string_from_property_id(property_id), value.to_deprecated_string()));
            });
            MUST(serializer.finish());

            return builder.to_deprecated_string();
        };

        auto serialize_custom_properties_json = [](Web::DOM::Element const& element) -> DeprecatedString {
            StringBuilder builder;
            auto serializer = MUST(JsonObjectSerializer<>::try_create(builder));
            HashTable<DeprecatedString> seen_properties;

            auto const* element_to_check = &element;
            while (element_to_check) {
                for (auto const& property : element_to_check->custom_properties()) {
                    if (!seen_properties.contains(property.key)) {
                        seen_properties.set(property.key);
                        MUST(serializer.add(property.key, property.value.value->to_deprecated_string()));
                    }
                }

                element_to_check = element_to_check->parent_element();
            }

            MUST(serializer.finish());

            return builder.to_deprecated_string();
        };
        auto serialize_node_box_sizing_json = [](Web::Layout::Node const* layout_node) -> DeprecatedString {
            if (!layout_node || !layout_node->is_box()) {
                return "{}";
            }
            auto* box = static_cast<Web::Layout::Box const*>(layout_node);
            auto box_model = box->box_model();
            StringBuilder builder;
            auto serializer = MUST(JsonObjectSerializer<>::try_create(builder));
            MUST(serializer.add("padding_top"sv, box_model.padding.top));
            MUST(serializer.add("padding_right"sv, box_model.padding.right));
            MUST(serializer.add("padding_bottom"sv, box_model.padding.bottom));
            MUST(serializer.add("padding_left"sv, box_model.padding.left));
            MUST(serializer.add("margin_top"sv, box_model.margin.top));
            MUST(serializer.add("margin_right"sv, box_model.margin.right));
            MUST(serializer.add("margin_bottom"sv, box_model.margin.bottom));
            MUST(serializer.add("margin_left"sv, box_model.margin.left));
            MUST(serializer.add("border_top"sv, box_model.border.top));
            MUST(serializer.add("border_right"sv, box_model.border.right));
            MUST(serializer.add("border_bottom"sv, box_model.border.bottom));
            MUST(serializer.add("border_left"sv, box_model.border.left));
            if (auto* paint_box = box->paint_box()) {
                MUST(serializer.add("content_width"sv, paint_box->content_width().value()));
                MUST(serializer.add("content_height"sv, paint_box->content_height().value()));
            } else {
                MUST(serializer.add("content_width"sv, 0));
                MUST(serializer.add("content_height"sv, 0));
            }

            MUST(serializer.finish());
            return builder.to_deprecated_string();
        };

        if (pseudo_element.has_value()) {
            auto pseudo_element_node = element.get_pseudo_element_node(pseudo_element.value());
            if (!pseudo_element_node)
                return { false, "", "", "", "" };

            // FIXME: Pseudo-elements only exist as Layout::Nodes, which don't have style information
            //        in a format we can use. So, we run the StyleComputer again to get the specified
            //        values, and have to ignore the computed values and custom properties.
            auto pseudo_element_style = MUST(page().focused_context().active_document()->style_computer().compute_style(element, pseudo_element));
            DeprecatedString computed_values = serialize_json(pseudo_element_style);
            DeprecatedString resolved_values = "{}";
            DeprecatedString custom_properties_json = "{}";
            DeprecatedString node_box_sizing_json = serialize_node_box_sizing_json(pseudo_element_node.ptr());
            return { true, computed_values, resolved_values, custom_properties_json, node_box_sizing_json };
        }

        DeprecatedString computed_values = serialize_json(*element.computed_css_values());
        DeprecatedString resolved_values_json = serialize_json(element.resolved_css_values());
        DeprecatedString custom_properties_json = serialize_custom_properties_json(element);
        DeprecatedString node_box_sizing_json = serialize_node_box_sizing_json(element.layout_node());
        return { true, computed_values, resolved_values_json, custom_properties_json, node_box_sizing_json };
    }

    return { false, "", "", "", "" };
}

Messages::WebContentServer::GetHoveredNodeIdResponse ConnectionFromClient::get_hovered_node_id()
{
    if (auto* document = page().top_level_browsing_context().active_document()) {
        auto hovered_node = document->hovered_node();
        if (hovered_node)
            return hovered_node->id();
    }
    return (i32)0;
}

void ConnectionFromClient::initialize_js_console(Badge<PageHost>)
{
    auto* document = page().top_level_browsing_context().active_document();
    auto realm = document->realm().make_weak_ptr();
    if (m_realm.ptr() == realm.ptr())
        return;

    auto& console_object = *realm->intrinsics().console_object();
    m_realm = realm;
    m_console_client = make<WebContentConsoleClient>(console_object.console(), *m_realm, *this);
    console_object.console().set_client(*m_console_client.ptr());
}

void ConnectionFromClient::js_console_input(DeprecatedString const& js_source)
{
    if (m_console_client)
        m_console_client->handle_input(js_source);
}

void ConnectionFromClient::run_javascript(DeprecatedString const& js_source)
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

void ConnectionFromClient::js_console_request_messages(i32 start_index)
{
    if (m_console_client)
        m_console_client->send_messages(start_index);
}

Messages::WebContentServer::TakeDocumentScreenshotResponse ConnectionFromClient::take_document_screenshot()
{
    auto* document = page().top_level_browsing_context().active_document();
    if (!document || !document->document_element())
        return { {} };

    auto const& content_size = m_page_host->content_size();
    Web::DevicePixelRect rect { { 0, 0 }, content_size };

    auto bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, rect.size().to_type<int>()).release_value_but_fixme_should_propagate_errors();
    m_page_host->paint(rect, *bitmap);

    return { bitmap->to_shareable_bitmap() };
}

Messages::WebContentServer::GetSelectedTextResponse ConnectionFromClient::get_selected_text()
{
    return page().focused_context().selected_text();
}

void ConnectionFromClient::select_all()
{
    page().focused_context().select_all();
    page().client().page_did_change_selection();
}

Messages::WebContentServer::DumpLayoutTreeResponse ConnectionFromClient::dump_layout_tree()
{
    auto* document = page().top_level_browsing_context().active_document();
    if (!document)
        return DeprecatedString { "(no DOM tree)" };
    auto* layout_root = document->layout_node();
    if (!layout_root)
        return DeprecatedString { "(no layout tree)" };
    StringBuilder builder;
    Web::dump_tree(builder, *layout_root);
    return builder.to_deprecated_string();
}

void ConnectionFromClient::set_content_filters(Vector<DeprecatedString> const& filters)
{
    for (auto& filter : filters)
        Web::ContentFilter::the().add_pattern(filter);
}

void ConnectionFromClient::set_proxy_mappings(Vector<DeprecatedString> const& proxies, HashMap<DeprecatedString, size_t> const& mappings)
{
    auto keys = mappings.keys();
    quick_sort(keys, [&](auto& a, auto& b) { return a.length() < b.length(); });

    OrderedHashMap<DeprecatedString, size_t> sorted_mappings;
    for (auto& key : keys) {
        auto value = *mappings.get(key);
        if (value >= proxies.size())
            continue;
        sorted_mappings.set(key, value);
    }

    Web::ProxyMappings::the().set_mappings(proxies, move(sorted_mappings));
}

void ConnectionFromClient::set_preferred_color_scheme(Web::CSS::PreferredColorScheme const& color_scheme)
{
    m_page_host->set_preferred_color_scheme(color_scheme);
}

void ConnectionFromClient::set_has_focus(bool has_focus)
{
    m_page_host->set_has_focus(has_focus);
}

void ConnectionFromClient::set_is_scripting_enabled(bool is_scripting_enabled)
{
    m_page_host->set_is_scripting_enabled(is_scripting_enabled);
}

void ConnectionFromClient::set_window_position(Gfx::IntPoint position)
{
    m_page_host->set_window_position(position);
}

void ConnectionFromClient::set_window_size(Gfx::IntSize size)
{
    m_page_host->set_window_size(size);
}

Messages::WebContentServer::GetLocalStorageEntriesResponse ConnectionFromClient::get_local_storage_entries()
{
    auto* document = page().top_level_browsing_context().active_document();
    auto local_storage = document->window().local_storage();
    return local_storage->map();
}

Messages::WebContentServer::GetSessionStorageEntriesResponse ConnectionFromClient::get_session_storage_entries()
{
    auto* document = page().top_level_browsing_context().active_document();
    auto session_storage = document->window().session_storage();
    return session_storage->map();
}

void ConnectionFromClient::handle_file_return(i32 error, Optional<IPC::File> const& file, i32 request_id)
{
    auto result = m_requested_files.get(request_id);
    VERIFY(result.has_value());

    VERIFY(result.value()->on_file_request_finish);
    result.value()->on_file_request_finish(error != 0 ? Error::from_errno(error) : ErrorOr<i32> { file->take_fd() });
    m_requested_files.remove(request_id);
}

void ConnectionFromClient::request_file(NonnullRefPtr<Web::FileRequest>& file_request)
{
    i32 const id = last_id++;
    m_requested_files.set(id, file_request);

    async_did_request_file(file_request->path(), id);
}

void ConnectionFromClient::set_system_visibility_state(bool visible)
{
    m_page_host->page().top_level_browsing_context().set_system_visibility_state(
        visible
            ? Web::HTML::VisibilityState::Visible
            : Web::HTML::VisibilityState::Hidden);
}

void ConnectionFromClient::alert_closed()
{
    m_page_host->alert_closed();
}

void ConnectionFromClient::confirm_closed(bool accepted)
{
    m_page_host->confirm_closed(accepted);
}

void ConnectionFromClient::prompt_closed(DeprecatedString const& response)
{
    m_page_host->prompt_closed(response);
}

}
