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
#include <LibJS/Parser.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibJS/Runtime/JSONObject.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/NodeList.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Geometry/DOMRect.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/FormAssociatedElement.h>
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

void ConnectionFromClient::connect_to_webdriver(String const& webdriver_ipc_path)
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

void ConnectionFromClient::update_system_fonts(String const& default_font_query, String const& fixed_width_font_query, String const& window_title_font_query)
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
    String process_name;
    if (url.host().is_empty())
        process_name = "WebContent";
    else
        process_name = String::formatted("WebContent: {}", url.host());

    pthread_setname_np(pthread_self(), process_name.characters());
#endif

    page().load(url);
}

void ConnectionFromClient::load_html(String const& html, const URL& url)
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
        m_page_host->paint(pending_paint.content_rect, *pending_paint.bitmap);
        async_did_paint(pending_paint.content_rect, pending_paint.bitmap_id);
    }
    m_pending_paint_requests.clear();
}

void ConnectionFromClient::mouse_down(Gfx::IntPoint const& position, unsigned int button, unsigned int buttons, unsigned int modifiers)
{
    page().handle_mousedown(position, button, buttons, modifiers);
}

void ConnectionFromClient::mouse_move(Gfx::IntPoint const& position, [[maybe_unused]] unsigned int button, unsigned int buttons, unsigned int modifiers)
{
    page().handle_mousemove(position, buttons, modifiers);
}

void ConnectionFromClient::mouse_up(Gfx::IntPoint const& position, unsigned int button, unsigned int buttons, unsigned int modifiers)
{
    page().handle_mouseup(position, button, buttons, modifiers);
}

void ConnectionFromClient::mouse_wheel(Gfx::IntPoint const& position, unsigned int button, unsigned int buttons, unsigned int modifiers, i32 wheel_delta_x, i32 wheel_delta_y)
{
    page().handle_mousewheel(position, button, buttons, modifiers, wheel_delta_x, wheel_delta_y);
}

void ConnectionFromClient::doubleclick(Gfx::IntPoint const& position, unsigned int button, unsigned int buttons, unsigned int modifiers)
{
    page().handle_doubleclick(position, button, buttons, modifiers);
}

void ConnectionFromClient::key_down(i32 key, unsigned int modifiers, u32 code_point)
{
    page().handle_keydown((KeyCode)key, modifiers, code_point);
}

void ConnectionFromClient::key_up(i32 key, unsigned int modifiers, u32 code_point)
{
    page().handle_keyup((KeyCode)key, modifiers, code_point);
}

void ConnectionFromClient::debug_request(String const& request, String const& argument)
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

Messages::WebContentServer::SerializeSourceResponse ConnectionFromClient::serialize_source()
{
    if (auto* doc = page().top_level_browsing_context().active_document()) {
        auto result = doc->serialize_fragment(Web::DOMParsing::RequireWellFormed::Yes);
        if (!result.is_error())
            return { result.release_value() };

        auto source = MUST(doc->serialize_fragment(Web::DOMParsing::RequireWellFormed::No));
        return { move(source) };
    }

    return { {} };
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

        auto serialize_json = [](Web::CSS::StyleProperties const& properties) -> String {
            StringBuilder builder;

            auto serializer = MUST(JsonObjectSerializer<>::try_create(builder));
            properties.for_each_property([&](auto property_id, auto& value) {
                MUST(serializer.add(Web::CSS::string_from_property_id(property_id), value.to_string()));
            });
            MUST(serializer.finish());

            return builder.to_string();
        };

        auto serialize_custom_properties_json = [](Web::DOM::Element const& element) -> String {
            StringBuilder builder;
            auto serializer = MUST(JsonObjectSerializer<>::try_create(builder));
            HashTable<String> seen_properties;

            auto const* element_to_check = &element;
            while (element_to_check) {
                for (auto const& property : element_to_check->custom_properties()) {
                    if (!seen_properties.contains(property.key)) {
                        seen_properties.set(property.key);
                        MUST(serializer.add(property.key, property.value.value->to_string()));
                    }
                }

                element_to_check = element_to_check->parent_element();
            }

            MUST(serializer.finish());

            return builder.to_string();
        };
        auto serialize_node_box_sizing_json = [](Web::Layout::Node const* layout_node) -> String {
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
                MUST(serializer.add("content_width"sv, paint_box->content_width()));
                MUST(serializer.add("content_height"sv, paint_box->content_height()));
            } else {
                MUST(serializer.add("content_width"sv, 0));
                MUST(serializer.add("content_height"sv, 0));
            }

            MUST(serializer.finish());
            return builder.to_string();
        };

        if (pseudo_element.has_value()) {
            auto pseudo_element_node = element.get_pseudo_element_node(pseudo_element.value());
            if (!pseudo_element_node)
                return { false, "", "", "", "" };

            // FIXME: Pseudo-elements only exist as Layout::Nodes, which don't have style information
            //        in a format we can use. So, we run the StyleComputer again to get the specified
            //        values, and have to ignore the computed values and custom properties.
            auto pseudo_element_style = page().focused_context().active_document()->style_computer().compute_style(element, pseudo_element);
            String computed_values = serialize_json(pseudo_element_style);
            String resolved_values = "{}";
            String custom_properties_json = "{}";
            String node_box_sizing_json = serialize_node_box_sizing_json(pseudo_element_node.ptr());
            return { true, computed_values, resolved_values, custom_properties_json, node_box_sizing_json };
        }

        String computed_values = serialize_json(*element.computed_css_values());
        String resolved_values_json = serialize_json(element.resolved_css_values());
        String custom_properties_json = serialize_custom_properties_json(element);
        String node_box_sizing_json = serialize_node_box_sizing_json(element.layout_node());
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

void ConnectionFromClient::js_console_input(String const& js_source)
{
    if (m_console_client)
        m_console_client->handle_input(js_source);
}

void ConnectionFromClient::run_javascript(String const& js_source)
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

static Optional<Web::DOM::Element&> find_element_by_id(i32 element_id)
{
    auto* node = Web::DOM::Node::from_id(element_id);
    if (!node || !node->is_element())
        return {};

    return verify_cast<Web::DOM::Element>(*node);
}

// https://w3c.github.io/webdriver/#dfn-scrolls-into-view
void ConnectionFromClient::scroll_element_into_view(i32 element_id)
{
    auto element = find_element_by_id(element_id);
    if (!element.has_value())
        return;

    // 1. Let options be the following ScrollIntoViewOptions:
    Web::DOM::ScrollIntoViewOptions options {};
    // Logical scroll position "block"
    //     "end"
    options.block = Web::Bindings::ScrollLogicalPosition::End;
    // Logical scroll position "inline"
    //     "nearest"
    options.inline_ = Web::Bindings::ScrollLogicalPosition::Nearest;

    // 2. Run Function.[[Call]](scrollIntoView, options) with element as the this value.
    element->scroll_into_view(options);
}

// https://w3c.github.io/webdriver/#dfn-calculate-the-absolute-position
static Gfx::IntPoint calculate_absolute_position_of_element(Web::Page const& page, JS::NonnullGCPtr<Web::Geometry::DOMRect> rect)
{
    // 1. Let rect be the value returned by calling getBoundingClientRect().

    // 2. Let window be the associated window of current top-level browsing context.
    auto const* window = page.top_level_browsing_context().active_window();

    // 3. Let x be (scrollX of window + rect’s x coordinate).
    auto x = (window ? static_cast<int>(window->scroll_x()) : 0) + static_cast<int>(rect->x());

    // 4. Let y be (scrollY of window + rect’s y coordinate).
    auto y = (window ? static_cast<int>(window->scroll_y()) : 0) + static_cast<int>(rect->y());

    // 5. Return a pair of (x, y).
    return { x, y };
}

static Gfx::IntRect calculate_absolute_rect_of_element(Web::Page const& page, Web::DOM::Element const& element)
{
    auto bounding_rect = element.get_bounding_client_rect();
    auto coordinates = calculate_absolute_position_of_element(page, bounding_rect);

    return Gfx::IntRect {
        coordinates.x(),
        coordinates.y(),
        static_cast<int>(bounding_rect->width()),
        static_cast<int>(bounding_rect->height())
    };
}

Messages::WebContentServer::IsElementEnabledResponse ConnectionFromClient::is_element_enabled(i32 element_id)
{
    auto element = find_element_by_id(element_id);
    if (!element.has_value())
        return { false };

    auto* document = page().top_level_browsing_context().active_document();
    if (!document)
        return { false };

    bool enabled = !document->is_xml_document();

    if (enabled && is<Web::HTML::FormAssociatedElement>(*element)) {
        auto& form_associated_element = dynamic_cast<Web::HTML::FormAssociatedElement&>(*element);
        enabled = form_associated_element.enabled();
    }

    return { enabled };
}

Messages::WebContentServer::TakeElementScreenshotResponse ConnectionFromClient::take_element_screenshot(i32 element_id)
{
    auto element = find_element_by_id(element_id);
    if (!element.has_value())
        return { {} };

    auto viewport_rect = page().top_level_browsing_context().viewport_rect();

    auto rect = calculate_absolute_rect_of_element(page(), *element);
    rect.intersect(viewport_rect);

    auto bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, rect.size()).release_value_but_fixme_should_propagate_errors();
    m_page_host->paint(rect, *bitmap);

    return { bitmap->to_shareable_bitmap() };
}

Messages::WebContentServer::TakeDocumentScreenshotResponse ConnectionFromClient::take_document_screenshot()
{
    auto* document = page().top_level_browsing_context().active_document();
    if (!document || !document->document_element())
        return { {} };

    auto bounding_rect = document->document_element()->get_bounding_client_rect();
    auto position = calculate_absolute_position_of_element(page(), bounding_rect);
    auto const& content_size = m_page_host->content_size();

    Gfx::IntRect rect {
        position.x(),
        position.y(),
        content_size.width() - position.x(),
        content_size.height() - position.y(),
    };

    auto bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, rect.size()).release_value_but_fixme_should_propagate_errors();
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
        return String { "(no DOM tree)" };
    auto* layout_root = document->layout_node();
    if (!layout_root)
        return String { "(no layout tree)" };
    StringBuilder builder;
    Web::dump_tree(builder, *layout_root);
    return builder.to_string();
}

void ConnectionFromClient::set_content_filters(Vector<String> const& filters)
{
    for (auto& filter : filters)
        Web::ContentFilter::the().add_pattern(filter);
}

void ConnectionFromClient::set_proxy_mappings(Vector<String> const& proxies, HashMap<String, size_t> const& mappings)
{
    auto keys = mappings.keys();
    quick_sort(keys, [&](auto& a, auto& b) { return a.length() < b.length(); });

    OrderedHashMap<String, size_t> sorted_mappings;
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

void ConnectionFromClient::set_window_position(Gfx::IntPoint const& position)
{
    m_page_host->set_window_position(position);
}

void ConnectionFromClient::set_window_size(Gfx::IntSize const& size)
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

Messages::WebContentServer::WebdriverExecuteScriptResponse ConnectionFromClient::webdriver_execute_script(String const& body, Vector<String> const& json_arguments, Optional<u64> const& timeout, bool async)
{
    auto& page = m_page_host->page();

    auto* window = page.top_level_browsing_context().active_window();
    auto& vm = window->vm();

    auto arguments = JS::MarkedVector<JS::Value> { vm.heap() };
    for (auto const& argument_string : json_arguments) {
        // NOTE: These are assumed to be valid JSON values.
        auto json_value = MUST(JsonValue::from_string(argument_string));
        arguments.append(JS::JSONObject::parse_json_value(vm, json_value));
    }

    auto result = async
        ? Web::WebDriver::execute_async_script(page, body, move(arguments), timeout)
        : Web::WebDriver::execute_script(page, body, move(arguments), timeout);
    return { result.type, result.value.serialized<StringBuilder>() };
}

}
