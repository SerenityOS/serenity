/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021-2023, Linus Groh <linusg@serenityos.org>
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
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Console.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibWeb/ARIA/RoleType.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Storage.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWeb/Loader/ProxyMappings.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/PermissionsPolicy/AutoplayAllowlist.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
#include <WebContent/ConnectionFromClient.h>
#include <WebContent/PageHost.h>
#include <WebContent/WebContentClientEndpoint.h>
#include <pthread.h>

namespace WebContent {

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> socket)
    : IPC::ConnectionFromClient<WebContentClientEndpoint, WebContentServerEndpoint>(*this, move(socket), 1)
    , m_page_host(PageHost::create(*this))
{
    m_paint_flush_timer = Web::Platform::Timer::create_single_shot(0, [this] { flush_pending_paint_requests(); });
    m_input_event_queue_timer = Web::Platform::Timer::create_single_shot(0, [this] { process_next_input_event(); });
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

void ConnectionFromClient::set_use_javascript_bytecode(bool use_bytecode)
{
    JS::Bytecode::Interpreter::set_enabled(use_bytecode);
}

Messages::WebContentServer::GetWindowHandleResponse ConnectionFromClient::get_window_handle()
{
    return m_page_host->page().top_level_browsing_context().window_handle();
}

void ConnectionFromClient::set_window_handle(String const& handle)
{
    m_page_host->page().top_level_browsing_context().set_window_handle(handle);
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
    if (url.host().has<Empty>() || url.host() == String {})
        process_name = "WebContent";
    else
        process_name = DeprecatedString::formatted("WebContent: {}", url.serialized_host().release_value_but_fixme_should_propagate_errors());

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
    m_page_host->set_viewport_rect(rect.to_type<Web::DevicePixels>());
}

void ConnectionFromClient::add_backing_store(i32 backing_store_id, Gfx::ShareableBitmap const& bitmap)
{
    m_backing_stores.set(backing_store_id, *const_cast<Gfx::ShareableBitmap&>(bitmap).bitmap());
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

void ConnectionFromClient::process_next_input_event()
{
    if (m_input_event_queue.is_empty())
        return;

    auto event = m_input_event_queue.dequeue();
    event.visit(
        [&](QueuedMouseEvent const& event) {
            switch (event.type) {
            case QueuedMouseEvent::Type::MouseDown:
                report_finished_handling_input_event(page().handle_mousedown(
                    event.position.to_type<Web::DevicePixels>(),
                    event.button, event.buttons, event.modifiers));
                break;
            case QueuedMouseEvent::Type::MouseUp:
                report_finished_handling_input_event(page().handle_mouseup(
                    event.position.to_type<Web::DevicePixels>(),
                    event.button, event.buttons, event.modifiers));
                break;
            case QueuedMouseEvent::Type::MouseMove:
                // NOTE: We have to notify the client about coalesced MouseMoves,
                //       so we do that by saying none of them were handled by the web page.
                for (size_t i = 0; i < event.coalesced_event_count; ++i) {
                    report_finished_handling_input_event(false);
                }
                report_finished_handling_input_event(page().handle_mousemove(
                    event.position.to_type<Web::DevicePixels>(),
                    event.buttons, event.modifiers));
                break;
            case QueuedMouseEvent::Type::DoubleClick:
                report_finished_handling_input_event(page().handle_doubleclick(
                    event.position.to_type<Web::DevicePixels>(),
                    event.button, event.buttons, event.modifiers));
                break;
            case QueuedMouseEvent::Type::MouseWheel:
                report_finished_handling_input_event(page().handle_mousewheel(
                    event.position.to_type<Web::DevicePixels>(),
                    event.button, event.buttons, event.modifiers, event.wheel_delta_x, event.wheel_delta_y));
                break;
            }
        },
        [&](QueuedKeyboardEvent const& event) {
            switch (event.type) {
            case QueuedKeyboardEvent::Type::KeyDown:
                report_finished_handling_input_event(page().handle_keydown((KeyCode)event.key, event.modifiers, event.code_point));
                break;
            case QueuedKeyboardEvent::Type::KeyUp:
                report_finished_handling_input_event(page().handle_keyup((KeyCode)event.key, event.modifiers, event.code_point));
                break;
            }
        });

    if (!m_input_event_queue.is_empty())
        m_input_event_queue_timer->start();
}

void ConnectionFromClient::mouse_down(Gfx::IntPoint position, unsigned int button, unsigned int buttons, unsigned int modifiers)
{
    enqueue_input_event(
        QueuedMouseEvent {
            .type = QueuedMouseEvent::Type::MouseDown,
            .position = position,
            .button = button,
            .buttons = buttons,
            .modifiers = modifiers,
        });
}

void ConnectionFromClient::mouse_move(Gfx::IntPoint position, [[maybe_unused]] unsigned int button, unsigned int buttons, unsigned int modifiers)
{
    auto event = QueuedMouseEvent {
        .type = QueuedMouseEvent::Type::MouseMove,
        .position = position,
        .button = button,
        .buttons = buttons,
        .modifiers = modifiers,
    };

    // OPTIMIZATION: Coalesce with previous unprocessed event iff the previous event is also a MouseMove event.
    if (!m_input_event_queue.is_empty()
        && m_input_event_queue.tail().has<QueuedMouseEvent>()
        && m_input_event_queue.tail().get<QueuedMouseEvent>().type == QueuedMouseEvent::Type::MouseMove) {
        event.coalesced_event_count = m_input_event_queue.tail().get<QueuedMouseEvent>().coalesced_event_count + 1;
        m_input_event_queue.tail() = event;
        return;
    }

    enqueue_input_event(move(event));
}

void ConnectionFromClient::mouse_up(Gfx::IntPoint position, unsigned int button, unsigned int buttons, unsigned int modifiers)
{
    enqueue_input_event(
        QueuedMouseEvent {
            .type = QueuedMouseEvent::Type::MouseUp,
            .position = position,
            .button = button,
            .buttons = buttons,
            .modifiers = modifiers,
        });
}

void ConnectionFromClient::mouse_wheel(Gfx::IntPoint position, unsigned int button, unsigned int buttons, unsigned int modifiers, i32 wheel_delta_x, i32 wheel_delta_y)
{
    enqueue_input_event(
        QueuedMouseEvent {
            .type = QueuedMouseEvent::Type::MouseWheel,
            .position = position,
            .button = button,
            .buttons = buttons,
            .modifiers = modifiers,
            .wheel_delta_x = wheel_delta_x,
            .wheel_delta_y = wheel_delta_y,
        });
}

void ConnectionFromClient::doubleclick(Gfx::IntPoint position, unsigned int button, unsigned int buttons, unsigned int modifiers)
{
    enqueue_input_event(
        QueuedMouseEvent {
            .type = QueuedMouseEvent::Type::DoubleClick,
            .position = position,
            .button = button,
            .buttons = buttons,
            .modifiers = modifiers,
        });
}

void ConnectionFromClient::key_down(i32 key, unsigned int modifiers, u32 code_point)
{
    enqueue_input_event(
        QueuedKeyboardEvent {
            .type = QueuedKeyboardEvent::Type::KeyDown,
            .key = key,
            .modifiers = modifiers,
            .code_point = code_point,
        });
}

void ConnectionFromClient::key_up(i32 key, unsigned int modifiers, u32 code_point)
{
    enqueue_input_event(
        QueuedKeyboardEvent {
            .type = QueuedKeyboardEvent::Type::KeyUp,
            .key = key,
            .modifiers = modifiers,
            .code_point = code_point,
        });
}

void ConnectionFromClient::enqueue_input_event(Variant<QueuedMouseEvent, QueuedKeyboardEvent> event)
{
    m_input_event_queue.enqueue(move(event));
    m_input_event_queue_timer->start();
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
            if (auto* viewport = doc->layout_node())
                Web::dump_tree(*viewport);
        }
    }

    if (request == "dump-paint-tree") {
        if (auto* doc = page().top_level_browsing_context().active_document()) {
            if (auto* paintable = doc->paintable())
                Web::dump_tree(*paintable);
        }
    }

    if (request == "dump-stacking-context-tree") {
        if (auto* doc = page().top_level_browsing_context().active_document()) {
            if (auto* viewport = doc->layout_node()) {
                if (auto* stacking_context = viewport->paintable_box()->stacking_context())
                    stacking_context->dump();
            }
        }
    }

    if (request == "dump-style-sheets") {
        if (auto* doc = page().top_level_browsing_context().active_document()) {
            for (auto& sheet : doc->style_sheets().sheets()) {
                if (auto result = Web::dump_sheet(sheet); result.is_error())
                    dbgln("Failed to dump style sheets: {}", result.error());
            }
        }
    }

    if (request == "dump-all-resolved-styles") {
        if (auto* doc = page().top_level_browsing_context().active_document()) {
            Queue<Web::DOM::Node*> elements_to_visit;
            elements_to_visit.enqueue(doc->document_element());
            while (!elements_to_visit.is_empty()) {
                auto element = elements_to_visit.dequeue();
                for (auto& child : element->children_as_vector())
                    elements_to_visit.enqueue(child.ptr());
                if (element->is_element()) {
                    auto styles = doc->style_computer().compute_style(*static_cast<Web::DOM::Element*>(element)).release_value_but_fixme_should_propagate_errors();
                    dbgln("+ Element {}", element->debug_description());
                    auto& properties = styles->properties();
                    for (size_t i = 0; i < properties.size(); ++i)
                        dbgln("|  {} = {}", Web::CSS::string_from_property_id(static_cast<Web::CSS::PropertyID>(i)), properties[i].has_value() ? properties[i]->style->to_string() : ""_short_string);
                    dbgln("---");
                }
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
        if (auto* document = page().top_level_browsing_context().active_document())
            document->window().local_storage().release_value_but_fixme_should_propagate_errors()->dump();
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
            ctx.active_document()->set_inspected_node(nullptr, {});
        }
        return IterationDecision::Continue;
    });

    Web::DOM::Node* node = Web::DOM::Node::from_id(node_id);
    // Note: Nodes without layout (aka non-visible nodes, don't have style computed)
    if (!node || !node->layout_node()) {
        return { false, "", "", "", "", "" };
    }

    node->document().set_inspected_node(node, pseudo_element);

    if (node->is_element()) {
        auto& element = verify_cast<Web::DOM::Element>(*node);
        if (!element.computed_css_values())
            return { false, "", "", "", "", "" };

        auto serialize_json = [](Web::CSS::StyleProperties const& properties) -> DeprecatedString {
            StringBuilder builder;

            auto serializer = MUST(JsonObjectSerializer<>::try_create(builder));
            properties.for_each_property([&](auto property_id, auto& value) {
                MUST(serializer.add(Web::CSS::string_from_property_id(property_id), value.to_string().release_value_but_fixme_should_propagate_errors().to_deprecated_string()));
            });
            MUST(serializer.finish());

            return builder.to_deprecated_string();
        };

        auto serialize_custom_properties_json = [](Web::DOM::Element const& element, Optional<Web::CSS::Selector::PseudoElement> pseudo_element) -> DeprecatedString {
            StringBuilder builder;
            auto serializer = MUST(JsonObjectSerializer<>::try_create(builder));
            HashTable<DeprecatedString> seen_properties;

            auto const* element_to_check = &element;
            while (element_to_check) {
                for (auto const& property : element_to_check->custom_properties(pseudo_element)) {
                    if (!seen_properties.contains(property.key)) {
                        seen_properties.set(property.key);
                        MUST(serializer.add(property.key, property.value.value->to_string().release_value_but_fixme_should_propagate_errors().to_deprecated_string()));
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
            MUST(serializer.add("padding_top"sv, box_model.padding.top.to_double()));
            MUST(serializer.add("padding_right"sv, box_model.padding.right.to_double()));
            MUST(serializer.add("padding_bottom"sv, box_model.padding.bottom.to_double()));
            MUST(serializer.add("padding_left"sv, box_model.padding.left.to_double()));
            MUST(serializer.add("margin_top"sv, box_model.margin.top.to_double()));
            MUST(serializer.add("margin_right"sv, box_model.margin.right.to_double()));
            MUST(serializer.add("margin_bottom"sv, box_model.margin.bottom.to_double()));
            MUST(serializer.add("margin_left"sv, box_model.margin.left.to_double()));
            MUST(serializer.add("border_top"sv, box_model.border.top.to_double()));
            MUST(serializer.add("border_right"sv, box_model.border.right.to_double()));
            MUST(serializer.add("border_bottom"sv, box_model.border.bottom.to_double()));
            MUST(serializer.add("border_left"sv, box_model.border.left.to_double()));
            if (auto* paintable_box = box->paintable_box()) {
                MUST(serializer.add("content_width"sv, paintable_box->content_width().to_double()));
                MUST(serializer.add("content_height"sv, paintable_box->content_height().to_double()));
            } else {
                MUST(serializer.add("content_width"sv, 0));
                MUST(serializer.add("content_height"sv, 0));
            }

            MUST(serializer.finish());
            return builder.to_deprecated_string();
        };

        auto serialize_aria_properties_state_json = [](Web::DOM::Element const& element) -> DeprecatedString {
            auto role_name = element.role_or_default();
            if (!role_name.has_value()) {
                return "";
            }
            auto aria_data = MUST(Web::ARIA::AriaData::build_data(element));
            auto role = MUST(Web::ARIA::RoleType::build_role_object(role_name.value(), element.is_focusable(), *aria_data));

            StringBuilder builder;
            auto serializer = MUST(JsonObjectSerializer<>::try_create(builder));
            MUST(role->serialize_as_json(serializer));
            MUST(serializer.finish());
            return builder.to_deprecated_string();
        };

        if (pseudo_element.has_value()) {
            auto pseudo_element_node = element.get_pseudo_element_node(pseudo_element.value());
            if (!pseudo_element_node)
                return { false, "", "", "", "", "" };

            // FIXME: Pseudo-elements only exist as Layout::Nodes, which don't have style information
            //        in a format we can use. So, we run the StyleComputer again to get the specified
            //        values, and have to ignore the computed values and custom properties.
            auto pseudo_element_style = MUST(page().focused_context().active_document()->style_computer().compute_style(element, pseudo_element));
            DeprecatedString computed_values = serialize_json(pseudo_element_style);
            DeprecatedString resolved_values = "{}";
            DeprecatedString custom_properties_json = serialize_custom_properties_json(element, pseudo_element);
            DeprecatedString node_box_sizing_json = serialize_node_box_sizing_json(pseudo_element_node.ptr());
            return { true, computed_values, resolved_values, custom_properties_json, node_box_sizing_json, "" };
        }

        DeprecatedString computed_values = serialize_json(*element.computed_css_values());
        DeprecatedString resolved_values_json = serialize_json(element.resolved_css_values());
        DeprecatedString custom_properties_json = serialize_custom_properties_json(element, {});
        DeprecatedString node_box_sizing_json = serialize_node_box_sizing_json(element.layout_node());
        DeprecatedString aria_properties_state_json = serialize_aria_properties_state_json(element);
        return { true, computed_values, resolved_values_json, custom_properties_json, node_box_sizing_json, aria_properties_state_json };
    }

    return { false, "", "", "", "", "" };
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

    auto console_object = realm->intrinsics().console_object();
    m_realm = realm;
    m_console_client = make<WebContentConsoleClient>(console_object->console(), *m_realm, *this);
    console_object->console().set_client(*m_console_client.ptr());
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

    auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, rect.size().to_type<int>()).release_value_but_fixme_should_propagate_errors();
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
    document->update_layout();
    auto* layout_root = document->layout_node();
    if (!layout_root)
        return DeprecatedString { "(no layout tree)" };
    StringBuilder builder;
    Web::dump_tree(builder, *layout_root);
    return builder.to_deprecated_string();
}

Messages::WebContentServer::DumpTextResponse ConnectionFromClient::dump_text()
{
    auto* document = page().top_level_browsing_context().active_document();
    if (!document)
        return DeprecatedString { "(no DOM tree)" };
    if (!document->body())
        return DeprecatedString { "(no body)" };
    return document->body()->inner_text();
}

void ConnectionFromClient::set_content_filters(Vector<String> const& filters)
{
    Web::ContentFilter::the().set_patterns(filters).release_value_but_fixme_should_propagate_errors();
}

void ConnectionFromClient::set_autoplay_allowed_on_all_websites()
{
    auto& autoplay_allowlist = Web::PermissionsPolicy::AutoplayAllowlist::the();
    autoplay_allowlist.enable_globally();
}

void ConnectionFromClient::set_autoplay_allowlist(Vector<String> const& allowlist)
{
    auto& autoplay_allowlist = Web::PermissionsPolicy::AutoplayAllowlist::the();
    autoplay_allowlist.enable_for_origins(allowlist).release_value_but_fixme_should_propagate_errors();
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

void ConnectionFromClient::set_device_pixels_per_css_pixel(float device_pixels_per_css_pixel)
{
    m_page_host->set_device_pixels_per_css_pixel(device_pixels_per_css_pixel);
}

void ConnectionFromClient::set_window_position(Gfx::IntPoint position)
{
    m_page_host->set_window_position(position.to_type<Web::DevicePixels>());
}

void ConnectionFromClient::set_window_size(Gfx::IntSize size)
{
    m_page_host->set_window_size(size.to_type<Web::DevicePixels>());
}

Messages::WebContentServer::GetLocalStorageEntriesResponse ConnectionFromClient::get_local_storage_entries()
{
    auto* document = page().top_level_browsing_context().active_document();
    auto local_storage = document->window().local_storage().release_value_but_fixme_should_propagate_errors();
    return local_storage->map();
}

Messages::WebContentServer::GetSessionStorageEntriesResponse ConnectionFromClient::get_session_storage_entries()
{
    auto* document = page().top_level_browsing_context().active_document();
    auto session_storage = document->window().session_storage().release_value_but_fixme_should_propagate_errors();
    return session_storage->map();
}

void ConnectionFromClient::handle_file_return(i32 error, Optional<IPC::File> const& file, i32 request_id)
{
    auto file_request = m_requested_files.take(request_id);

    VERIFY(file_request.has_value());
    VERIFY(file_request.value().on_file_request_finish);

    file_request.value().on_file_request_finish(error != 0 ? Error::from_errno(error) : ErrorOr<i32> { file->take_fd() });
}

void ConnectionFromClient::request_file(Web::FileRequest file_request)
{
    i32 const id = last_id++;

    auto path = file_request.path();
    m_requested_files.set(id, move(file_request));

    async_did_request_file(path, id);
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

void ConnectionFromClient::prompt_closed(Optional<String> const& response)
{
    m_page_host->prompt_closed(response);
}

void ConnectionFromClient::toggle_media_play_state()
{
    m_page_host->toggle_media_play_state().release_value_but_fixme_should_propagate_errors();
}

void ConnectionFromClient::toggle_media_mute_state()
{
    m_page_host->toggle_media_mute_state();
}

void ConnectionFromClient::toggle_media_loop_state()
{
    m_page_host->toggle_media_loop_state().release_value_but_fixme_should_propagate_errors();
}

void ConnectionFromClient::toggle_media_controls_state()
{
    m_page_host->toggle_media_controls_state().release_value_but_fixme_should_propagate_errors();
}

void ConnectionFromClient::inspect_accessibility_tree()
{
    if (auto* doc = page().top_level_browsing_context().active_document()) {
        async_did_get_accessibility_tree(doc->dump_accessibility_tree_as_json());
    }
}

}
