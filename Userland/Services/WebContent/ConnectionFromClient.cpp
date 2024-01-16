/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
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
#include <LibWeb/DOM/Attr.h>
#include <LibWeb/DOM/CharacterData.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Storage.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Infra/Strings.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWeb/Loader/ProxyMappings.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/Painting/ViewportPaintable.h>
#include <LibWeb/PermissionsPolicy/AutoplayAllowlist.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
#include <LibWebView/Attribute.h>
#include <WebContent/ConnectionFromClient.h>
#include <WebContent/PageClient.h>
#include <WebContent/PageHost.h>
#include <WebContent/WebContentClientEndpoint.h>
#include <pthread.h>

namespace WebContent {

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> socket)
    : IPC::ConnectionFromClient<WebContentClientEndpoint, WebContentServerEndpoint>(*this, move(socket), 1)
    , m_page_host(PageHost::create(*this))
{
    m_input_event_queue_timer = Web::Platform::Timer::create_single_shot(0, [this] { process_next_input_event(); });
}

void ConnectionFromClient::die()
{
    Web::Platform::EventLoopPlugin::the().quit();
}

PageClient& ConnectionFromClient::page(u64 index)
{
    return m_page_host->page(index);
}

PageClient const& ConnectionFromClient::page(u64 index) const
{
    return m_page_host->page(index);
}

Messages::WebContentServer::GetWindowHandleResponse ConnectionFromClient::get_window_handle()
{
    return page().page().top_level_browsing_context().window_handle();
}

void ConnectionFromClient::set_window_handle(String const& handle)
{
    page().page().top_level_browsing_context().set_window_handle(handle);
}

void ConnectionFromClient::connect_to_webdriver(ByteString const& webdriver_ipc_path)
{
    // FIXME: Propagate this error back to the browser.
    if (auto result = page().connect_to_webdriver(webdriver_ipc_path); result.is_error())
        dbgln("Unable to connect to the WebDriver process: {}", result.error());
}

void ConnectionFromClient::update_system_theme(Core::AnonymousBuffer const& theme_buffer)
{
    Gfx::set_system_theme(theme_buffer);
    auto impl = Gfx::PaletteImpl::create_with_anonymous_buffer(theme_buffer);
    page().set_palette_impl(*impl);
}

void ConnectionFromClient::update_system_fonts(ByteString const& default_font_query, ByteString const& fixed_width_font_query, ByteString const& window_title_font_query)
{
    Gfx::FontDatabase::set_default_font_query(default_font_query);
    Gfx::FontDatabase::set_fixed_width_font_query(fixed_width_font_query);
    Gfx::FontDatabase::set_window_title_font_query(window_title_font_query);
}

void ConnectionFromClient::update_screen_rects(Vector<Web::DevicePixelRect> const& rects, u32 main_screen)
{
    page().set_screen_rects(rects, main_screen);
}

void ConnectionFromClient::load_url(const URL& url)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentServer::LoadURL: url={}", url);

#if defined(AK_OS_SERENITY)
    ByteString process_name;
    if (url.host().has<Empty>() || url.host() == String {})
        process_name = "WebContent";
    else
        process_name = ByteString::formatted("WebContent: {}", url.serialized_host().release_value_but_fixme_should_propagate_errors());

    pthread_setname_np(pthread_self(), process_name.characters());
#endif

    page().page().load(url);
}

void ConnectionFromClient::load_html(ByteString const& html)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentServer::LoadHTML: html={}", html);
    page().page().load_html(html);
}

void ConnectionFromClient::set_viewport_rect(Web::DevicePixelRect const& rect)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentServer::SetViewportRect: rect={}", rect);
    page().set_viewport_rect(rect);
}

void ConnectionFromClient::add_backing_store(i32 front_bitmap_id, Gfx::ShareableBitmap const& front_bitmap, i32 back_bitmap_id, Gfx::ShareableBitmap const& back_bitmap)
{
    m_backing_stores.front_bitmap_id = front_bitmap_id;
    m_backing_stores.back_bitmap_id = back_bitmap_id;
    m_backing_stores.front_bitmap = *const_cast<Gfx::ShareableBitmap&>(front_bitmap).bitmap();
    m_backing_stores.back_bitmap = *const_cast<Gfx::ShareableBitmap&>(back_bitmap).bitmap();
}

void ConnectionFromClient::ready_to_paint()
{
    page().ready_to_paint();
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
                report_finished_handling_input_event(page().page().handle_mousedown(
                    event.position, event.screen_position,
                    event.button, event.buttons, event.modifiers));
                break;
            case QueuedMouseEvent::Type::MouseUp:
                report_finished_handling_input_event(page().page().handle_mouseup(
                    event.position, event.screen_position,
                    event.button, event.buttons, event.modifiers));
                break;
            case QueuedMouseEvent::Type::MouseMove:
                // NOTE: We have to notify the client about coalesced MouseMoves,
                //       so we do that by saying none of them were handled by the web page.
                for (size_t i = 0; i < event.coalesced_event_count; ++i) {
                    report_finished_handling_input_event(false);
                }
                report_finished_handling_input_event(page().page().handle_mousemove(
                    event.position, event.screen_position,
                    event.buttons, event.modifiers));
                break;
            case QueuedMouseEvent::Type::DoubleClick:
                report_finished_handling_input_event(page().page().handle_doubleclick(
                    event.position, event.screen_position,
                    event.button, event.buttons, event.modifiers));
                break;
            case QueuedMouseEvent::Type::MouseWheel:
                for (size_t i = 0; i < event.coalesced_event_count; ++i) {
                    report_finished_handling_input_event(false);
                }
                report_finished_handling_input_event(page().page().handle_mousewheel(
                    event.position, event.screen_position,
                    event.button, event.buttons, event.modifiers,
                    event.wheel_delta_x, event.wheel_delta_y));
                break;
            }
        },
        [&](QueuedKeyboardEvent const& event) {
            switch (event.type) {
            case QueuedKeyboardEvent::Type::KeyDown:
                report_finished_handling_input_event(page().page().handle_keydown((KeyCode)event.key, event.modifiers, event.code_point));
                break;
            case QueuedKeyboardEvent::Type::KeyUp:
                report_finished_handling_input_event(page().page().handle_keyup((KeyCode)event.key, event.modifiers, event.code_point));
                break;
            }
        });

    if (!m_input_event_queue.is_empty())
        m_input_event_queue_timer->start();
}

void ConnectionFromClient::mouse_down(Web::DevicePixelPoint position, Web::DevicePixelPoint screen_position, u32 button, u32 buttons, u32 modifiers)
{
    enqueue_input_event(
        QueuedMouseEvent {
            .type = QueuedMouseEvent::Type::MouseDown,
            .position = position,
            .screen_position = screen_position,
            .button = button,
            .buttons = buttons,
            .modifiers = modifiers,
        });
}

void ConnectionFromClient::mouse_move(Web::DevicePixelPoint position, Web::DevicePixelPoint screen_position, [[maybe_unused]] u32 button, u32 buttons, u32 modifiers)
{
    auto event = QueuedMouseEvent {
        .type = QueuedMouseEvent::Type::MouseMove,
        .position = position,
        .screen_position = screen_position,
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

void ConnectionFromClient::mouse_up(Web::DevicePixelPoint position, Web::DevicePixelPoint screen_position, u32 button, u32 buttons, u32 modifiers)
{
    enqueue_input_event(
        QueuedMouseEvent {
            .type = QueuedMouseEvent::Type::MouseUp,
            .position = position,
            .screen_position = screen_position,
            .button = button,
            .buttons = buttons,
            .modifiers = modifiers,
        });
}

void ConnectionFromClient::mouse_wheel(Web::DevicePixelPoint position, Web::DevicePixelPoint screen_position, u32 button, u32 buttons, u32 modifiers, Web::DevicePixels wheel_delta_x, Web::DevicePixels wheel_delta_y)
{
    auto event = QueuedMouseEvent {
        .type = QueuedMouseEvent::Type::MouseWheel,
        .position = position,
        .screen_position = screen_position,
        .button = button,
        .buttons = buttons,
        .modifiers = modifiers,
        .wheel_delta_x = wheel_delta_x,
        .wheel_delta_y = wheel_delta_y,
    };

    // OPTIMIZATION: Coalesce with previous unprocessed event if the previous event is also a MouseWheel event.
    if (!m_input_event_queue.is_empty()
        && m_input_event_queue.tail().has<QueuedMouseEvent>()
        && m_input_event_queue.tail().get<QueuedMouseEvent>().type == QueuedMouseEvent::Type::MouseWheel) {
        auto const& last_event = m_input_event_queue.tail().get<QueuedMouseEvent>();
        event.coalesced_event_count = last_event.coalesced_event_count + 1;
        event.wheel_delta_x += last_event.wheel_delta_x;
        event.wheel_delta_y += last_event.wheel_delta_y;
        m_input_event_queue.tail() = event;
        return;
    }

    enqueue_input_event(move(event));
}

void ConnectionFromClient::doubleclick(Web::DevicePixelPoint position, Web::DevicePixelPoint screen_position, u32 button, u32 buttons, u32 modifiers)
{
    enqueue_input_event(
        QueuedMouseEvent {
            .type = QueuedMouseEvent::Type::DoubleClick,
            .position = position,
            .screen_position = screen_position,
            .button = button,
            .buttons = buttons,
            .modifiers = modifiers,
        });
}

void ConnectionFromClient::key_down(i32 key, u32 modifiers, u32 code_point)
{
    enqueue_input_event(
        QueuedKeyboardEvent {
            .type = QueuedKeyboardEvent::Type::KeyDown,
            .key = key,
            .modifiers = modifiers,
            .code_point = code_point,
        });
}

void ConnectionFromClient::key_up(i32 key, u32 modifiers, u32 code_point)
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

void ConnectionFromClient::debug_request(ByteString const& request, ByteString const& argument)
{
    if (request == "dump-session-history") {
        auto const& traversable = page().page().top_level_traversable();
        Web::dump_tree(*traversable);
    }

    if (request == "dump-dom-tree") {
        if (auto* doc = page().page().top_level_browsing_context().active_document())
            Web::dump_tree(*doc);
        return;
    }

    if (request == "dump-layout-tree") {
        if (auto* doc = page().page().top_level_browsing_context().active_document()) {
            if (auto* viewport = doc->layout_node())
                Web::dump_tree(*viewport);
        }
        return;
    }

    if (request == "dump-paint-tree") {
        if (auto* doc = page().page().top_level_browsing_context().active_document()) {
            if (auto* paintable = doc->paintable())
                Web::dump_tree(*paintable);
        }
        return;
    }

    if (request == "dump-stacking-context-tree") {
        if (auto* doc = page().page().top_level_browsing_context().active_document()) {
            if (auto* viewport = doc->layout_node()) {
                if (auto* stacking_context = viewport->paintable_box()->stacking_context())
                    stacking_context->dump();
            }
        }
        return;
    }

    if (request == "dump-style-sheets") {
        if (auto* doc = page().page().top_level_browsing_context().active_document()) {
            for (auto& sheet : doc->style_sheets().sheets()) {
                if (auto result = Web::dump_sheet(sheet); result.is_error())
                    dbgln("Failed to dump style sheets: {}", result.error());
            }
        }
        return;
    }

    if (request == "dump-all-resolved-styles") {
        if (auto* doc = page().page().top_level_browsing_context().active_document()) {
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
                        dbgln("|  {} = {}", Web::CSS::string_from_property_id(static_cast<Web::CSS::PropertyID>(i)), properties[i].has_value() ? properties[i]->style->to_string() : ""_string);
                    dbgln("---");
                }
            }
        }
        return;
    }

    if (request == "collect-garbage") {
        Web::Bindings::main_thread_vm().heap().collect_garbage(JS::Heap::CollectionType::CollectGarbage, true);
        return;
    }

    if (request == "set-line-box-borders") {
        bool state = argument == "on";
        page().set_should_show_line_box_borders(state);
        page().page().top_level_traversable()->set_needs_display(page().page().top_level_traversable()->viewport_rect());
        return;
    }

    if (request == "clear-cache") {
        Web::ResourceLoader::the().clear_cache();
        return;
    }

    if (request == "spoof-user-agent") {
        Web::ResourceLoader::the().set_user_agent(MUST(String::from_byte_string(argument)));
        return;
    }

    if (request == "same-origin-policy") {
        page().page().set_same_origin_policy_enabled(argument == "on");
        return;
    }

    if (request == "scripting") {
        page().page().set_is_scripting_enabled(argument == "on");
        return;
    }

    if (request == "block-pop-ups") {
        page().page().set_should_block_pop_ups(argument == "on");
        return;
    }

    if (request == "dump-local-storage") {
        if (auto* document = page().page().top_level_browsing_context().active_document())
            document->window().local_storage().release_value_but_fixme_should_propagate_errors()->dump();
        return;
    }

    if (request == "load-reference-page") {
        if (auto* document = page().page().top_level_browsing_context().active_document()) {
            auto maybe_link = document->query_selector("link[rel=match]"sv);
            if (maybe_link.is_error() || !maybe_link.value()) {
                // To make sure that we fail the ref-test if the link is missing, load the error page.
                load_html("<h1>Failed to find &lt;link rel=&quot;match&quot; /&gt; in ref test page!</h1> Make sure you added it.");
            } else {
                auto link = maybe_link.release_value();
                auto url = document->parse_url(link->get_attribute_value(Web::HTML::AttributeNames::href));
                load_url(url);
            }
        }
        return;
    }
}

void ConnectionFromClient::get_source()
{
    if (auto* doc = page().page().top_level_browsing_context().active_document()) {
        async_did_get_source(doc->url(), doc->source().to_byte_string());
    }
}

void ConnectionFromClient::inspect_dom_tree()
{
    if (auto* doc = page().page().top_level_browsing_context().active_document()) {
        async_did_inspect_dom_tree(doc->dump_dom_tree_as_json().to_byte_string());
    }
}

void ConnectionFromClient::inspect_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement::Type> const& pseudo_element)
{
    auto& top_context = page().page().top_level_browsing_context();

    top_context.for_each_in_inclusive_subtree([&](auto& ctx) {
        if (ctx.active_document() != nullptr) {
            ctx.active_document()->set_inspected_node(nullptr, {});
        }
        return IterationDecision::Continue;
    });

    Web::DOM::Node* node = Web::DOM::Node::from_unique_id(node_id);
    // Note: Nodes without layout (aka non-visible nodes, don't have style computed)
    if (!node || !node->layout_node()) {
        async_did_inspect_dom_node(false, {}, {}, {}, {}, {});
        return;
    }

    node->document().set_inspected_node(node, pseudo_element);

    if (node->is_element()) {
        auto& element = verify_cast<Web::DOM::Element>(*node);
        if (!element.computed_css_values()) {
            async_did_inspect_dom_node(false, {}, {}, {}, {}, {});
            return;
        }

        auto serialize_json = [](Web::CSS::StyleProperties const& properties) -> ByteString {
            StringBuilder builder;

            auto serializer = MUST(JsonObjectSerializer<>::try_create(builder));
            properties.for_each_property([&](auto property_id, auto& value) {
                MUST(serializer.add(Web::CSS::string_from_property_id(property_id), value.to_string().to_byte_string()));
            });
            MUST(serializer.finish());

            return builder.to_byte_string();
        };

        auto serialize_custom_properties_json = [](Web::DOM::Element const& element, Optional<Web::CSS::Selector::PseudoElement::Type> pseudo_element) -> ByteString {
            StringBuilder builder;
            auto serializer = MUST(JsonObjectSerializer<>::try_create(builder));
            HashTable<FlyString> seen_properties;

            auto const* element_to_check = &element;
            while (element_to_check) {
                for (auto const& property : element_to_check->custom_properties(pseudo_element)) {
                    if (!seen_properties.contains(property.key)) {
                        seen_properties.set(property.key);
                        MUST(serializer.add(property.key, property.value.value->to_string()));
                    }
                }

                element_to_check = element_to_check->parent_element();
            }

            MUST(serializer.finish());

            return builder.to_byte_string();
        };
        auto serialize_node_box_sizing_json = [](Web::Layout::Node const* layout_node) -> ByteString {
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
            return builder.to_byte_string();
        };

        auto serialize_aria_properties_state_json = [](Web::DOM::Element const& element) -> ByteString {
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
            return builder.to_byte_string();
        };

        if (pseudo_element.has_value()) {
            auto pseudo_element_node = element.get_pseudo_element_node(pseudo_element.value());
            if (!pseudo_element_node) {
                async_did_inspect_dom_node(false, {}, {}, {}, {}, {});
                return;
            }

            // FIXME: Pseudo-elements only exist as Layout::Nodes, which don't have style information
            //        in a format we can use. So, we run the StyleComputer again to get the specified
            //        values, and have to ignore the computed values and custom properties.
            auto pseudo_element_style = MUST(page().page().focused_context().active_document()->style_computer().compute_style(element, pseudo_element));
            ByteString computed_values = serialize_json(pseudo_element_style);
            ByteString resolved_values = "{}";
            ByteString custom_properties_json = serialize_custom_properties_json(element, pseudo_element);
            ByteString node_box_sizing_json = serialize_node_box_sizing_json(pseudo_element_node.ptr());

            async_did_inspect_dom_node(true, move(computed_values), move(resolved_values), move(custom_properties_json), move(node_box_sizing_json), {});
            return;
        }

        ByteString computed_values = serialize_json(*element.computed_css_values());
        ByteString resolved_values = serialize_json(element.resolved_css_values());
        ByteString custom_properties_json = serialize_custom_properties_json(element, {});
        ByteString node_box_sizing_json = serialize_node_box_sizing_json(element.layout_node());
        ByteString aria_properties_state_json = serialize_aria_properties_state_json(element);

        async_did_inspect_dom_node(true, move(computed_values), move(resolved_values), move(custom_properties_json), move(node_box_sizing_json), move(aria_properties_state_json));
        return;
    }

    async_did_inspect_dom_node(false, {}, {}, {}, {}, {});
}

void ConnectionFromClient::inspect_accessibility_tree()
{
    if (auto* doc = page().page().top_level_browsing_context().active_document()) {
        async_did_inspect_accessibility_tree(doc->dump_accessibility_tree_as_json().to_byte_string());
    }
}

void ConnectionFromClient::get_hovered_node_id()
{
    i32 node_id = 0;

    if (auto* document = page().page().top_level_browsing_context().active_document()) {
        if (auto* hovered_node = document->hovered_node())
            node_id = hovered_node->unique_id();
    }

    async_did_get_hovered_node_id(node_id);
}

void ConnectionFromClient::set_dom_node_text(i32 node_id, String const& text)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node || (!dom_node->is_text() && !dom_node->is_comment())) {
        async_did_finish_editing_dom_node({});
        return;
    }

    auto& character_data = static_cast<Web::DOM::CharacterData&>(*dom_node);
    character_data.set_data(text);

    async_did_finish_editing_dom_node(character_data.unique_id());
}

void ConnectionFromClient::set_dom_node_tag(i32 node_id, String const& name)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node || !dom_node->is_element() || !dom_node->parent()) {
        async_did_finish_editing_dom_node({});
        return;
    }

    auto& element = static_cast<Web::DOM::Element&>(*dom_node);
    auto new_element = Web::DOM::create_element(element.document(), name, element.namespace_uri(), element.prefix(), element.is_value()).release_value_but_fixme_should_propagate_errors();

    element.for_each_attribute([&](auto const& attribute) {
        new_element->set_attribute_value(attribute.local_name(), attribute.value(), attribute.prefix(), attribute.namespace_uri());
    });

    while (auto* child_node = element.first_child()) {
        MUST(element.remove_child(*child_node));
        MUST(new_element->append_child(*child_node));
    }

    element.parent()->replace_child(*new_element, element).release_value_but_fixme_should_propagate_errors();
    async_did_finish_editing_dom_node(new_element->unique_id());
}

void ConnectionFromClient::add_dom_node_attributes(i32 node_id, Vector<WebView::Attribute> const& attributes)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node || !dom_node->is_element()) {
        async_did_finish_editing_dom_node({});
        return;
    }

    auto& element = static_cast<Web::DOM::Element&>(*dom_node);

    for (auto const& attribute : attributes)
        element.set_attribute(attribute.name, attribute.value).release_value_but_fixme_should_propagate_errors();

    async_did_finish_editing_dom_node(element.unique_id());
}

void ConnectionFromClient::replace_dom_node_attribute(i32 node_id, String const& name, Vector<WebView::Attribute> const& replacement_attributes)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node || !dom_node->is_element()) {
        async_did_finish_editing_dom_node({});
        return;
    }

    auto& element = static_cast<Web::DOM::Element&>(*dom_node);
    bool should_remove_attribute = true;

    for (auto const& attribute : replacement_attributes) {
        if (should_remove_attribute && Web::Infra::is_ascii_case_insensitive_match(name, attribute.name))
            should_remove_attribute = false;

        element.set_attribute(attribute.name, attribute.value).release_value_but_fixme_should_propagate_errors();
    }

    if (should_remove_attribute)
        element.remove_attribute(name);

    async_did_finish_editing_dom_node(element.unique_id());
}

void ConnectionFromClient::create_child_element(i32 node_id)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node) {
        async_did_finish_editing_dom_node({});
        return;
    }

    auto element = Web::DOM::create_element(dom_node->document(), Web::HTML::TagNames::div, Web::Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    dom_node->append_child(element).release_value_but_fixme_should_propagate_errors();

    async_did_finish_editing_dom_node(element->unique_id());
}

void ConnectionFromClient::create_child_text_node(i32 node_id)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node) {
        async_did_finish_editing_dom_node({});
        return;
    }

    auto text_node = dom_node->heap().allocate<Web::DOM::Text>(dom_node->realm(), dom_node->document(), "text"_string);
    dom_node->append_child(text_node).release_value_but_fixme_should_propagate_errors();

    async_did_finish_editing_dom_node(text_node->unique_id());
}

void ConnectionFromClient::clone_dom_node(i32 node_id)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node || !dom_node->parent_node()) {
        async_did_finish_editing_dom_node({});
        return;
    }

    auto dom_node_clone = dom_node->clone_node(nullptr, true);
    dom_node->parent_node()->insert_before(dom_node_clone, dom_node->next_sibling());

    async_did_finish_editing_dom_node(dom_node_clone->unique_id());
}

void ConnectionFromClient::remove_dom_node(i32 node_id)
{
    auto* active_document = page().page().top_level_browsing_context().active_document();
    if (!active_document) {
        async_did_finish_editing_dom_node({});
        return;
    }

    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node) {
        async_did_finish_editing_dom_node({});
        return;
    }

    auto* previous_dom_node = dom_node->previous_sibling();
    if (!previous_dom_node)
        previous_dom_node = dom_node->parent();

    dom_node->remove();

    // FIXME: When nodes are removed from the DOM, the associated layout nodes become stale and still
    //        remain in the layout tree. This has to be fixed, this just causes everything to be recomputed
    //        which really hurts performance.
    active_document->force_layout();

    async_did_finish_editing_dom_node(previous_dom_node->unique_id());
}

void ConnectionFromClient::get_dom_node_html(i32 node_id)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node)
        return;

    // FIXME: Implement Element's outerHTML attribute.
    auto container = Web::DOM::create_element(dom_node->document(), Web::HTML::TagNames::div, Web::Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    container->append_child(dom_node->clone_node(nullptr, true)).release_value_but_fixme_should_propagate_errors();

    auto html = container->inner_html().release_value_but_fixme_should_propagate_errors();
    async_did_get_dom_node_html(move(html));
}

void ConnectionFromClient::initialize_js_console(Badge<PageClient>, Web::DOM::Document& document)
{
    auto& realm = document.realm();
    auto console_object = realm.intrinsics().console_object();
    auto console_client = make<WebContentConsoleClient>(console_object->console(), document.realm(), *this);
    console_object->console().set_client(*console_client);

    VERIFY(document.browsing_context());
    if (document.browsing_context()->is_top_level()) {
        m_top_level_document_console_client = console_client->make_weak_ptr();
    }

    m_console_clients.set(&document, move(console_client));
}

void ConnectionFromClient::destroy_js_console(Badge<PageClient>, Web::DOM::Document& document)
{
    m_console_clients.remove(&document);
}

void ConnectionFromClient::js_console_input(ByteString const& js_source)
{
    if (m_top_level_document_console_client)
        m_top_level_document_console_client->handle_input(js_source);
}

void ConnectionFromClient::run_javascript(ByteString const& js_source)
{
    auto* active_document = page().page().top_level_browsing_context().active_document();

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
    if (m_top_level_document_console_client)
        m_top_level_document_console_client->send_messages(start_index);
}

void ConnectionFromClient::take_document_screenshot()
{
    auto* document = page().page().top_level_browsing_context().active_document();
    if (!document || !document->document_element()) {
        async_did_take_screenshot({});
        return;
    }

    auto const& content_size = page().content_size();
    Web::DevicePixelRect rect { { 0, 0 }, content_size };

    auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, rect.size().to_type<int>()).release_value_but_fixme_should_propagate_errors();
    page().paint(rect, *bitmap);

    async_did_take_screenshot(bitmap->to_shareable_bitmap());
}

void ConnectionFromClient::take_dom_node_screenshot(i32 node_id)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node || !dom_node->paintable_box()) {
        async_did_take_screenshot({});
        return;
    }

    auto rect = page().page().enclosing_device_rect(dom_node->paintable_box()->absolute_border_box_rect());

    auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, rect.size().to_type<int>()).release_value_but_fixme_should_propagate_errors();
    page().paint(rect, *bitmap, { .paint_overlay = Web::PaintOptions::PaintOverlay::No });

    async_did_take_screenshot(bitmap->to_shareable_bitmap());
}

Messages::WebContentServer::DumpGcGraphResponse ConnectionFromClient::dump_gc_graph()
{
    auto gc_graph_json = Web::Bindings::main_thread_vm().heap().dump_graph();
    return MUST(String::from_byte_string(gc_graph_json.to_byte_string()));
}

Messages::WebContentServer::GetSelectedTextResponse ConnectionFromClient::get_selected_text()
{
    return page().page().focused_context().selected_text().to_byte_string();
}

void ConnectionFromClient::select_all()
{
    page().page().focused_context().select_all();
}

Messages::WebContentServer::DumpLayoutTreeResponse ConnectionFromClient::dump_layout_tree()
{
    auto* document = page().page().top_level_browsing_context().active_document();
    if (!document)
        return ByteString { "(no DOM tree)" };
    document->update_layout();
    auto* layout_root = document->layout_node();
    if (!layout_root)
        return ByteString { "(no layout tree)" };
    StringBuilder builder;
    Web::dump_tree(builder, *layout_root);
    return builder.to_byte_string();
}

Messages::WebContentServer::DumpPaintTreeResponse ConnectionFromClient::dump_paint_tree()
{
    auto* document = page().page().top_level_browsing_context().active_document();
    if (!document)
        return ByteString { "(no DOM tree)" };
    document->update_layout();
    auto* layout_root = document->layout_node();
    if (!layout_root)
        return ByteString { "(no layout tree)" };
    if (!layout_root->paintable())
        return ByteString { "(no paint tree)" };
    StringBuilder builder;
    Web::dump_tree(builder, *layout_root->paintable());
    return builder.to_byte_string();
}

Messages::WebContentServer::DumpTextResponse ConnectionFromClient::dump_text()
{
    auto* document = page().page().top_level_browsing_context().active_document();
    if (!document)
        return ByteString { "(no DOM tree)" };
    if (!document->body())
        return ByteString { "(no body)" };
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

void ConnectionFromClient::set_proxy_mappings(Vector<ByteString> const& proxies, HashMap<ByteString, size_t> const& mappings)
{
    auto keys = mappings.keys();
    quick_sort(keys, [&](auto& a, auto& b) { return a.length() < b.length(); });

    OrderedHashMap<ByteString, size_t> sorted_mappings;
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
    page().set_preferred_color_scheme(color_scheme);
}

void ConnectionFromClient::set_has_focus(bool has_focus)
{
    page().set_has_focus(has_focus);
}

void ConnectionFromClient::set_is_scripting_enabled(bool is_scripting_enabled)
{
    page().set_is_scripting_enabled(is_scripting_enabled);
}

void ConnectionFromClient::set_device_pixels_per_css_pixel(float device_pixels_per_css_pixel)
{
    page().set_device_pixels_per_css_pixel(device_pixels_per_css_pixel);
}

void ConnectionFromClient::set_window_position(Web::DevicePixelPoint position)
{
    page().set_window_position(position);
}

void ConnectionFromClient::set_window_size(Web::DevicePixelSize size)
{
    page().set_window_size(size);
}

Messages::WebContentServer::GetLocalStorageEntriesResponse ConnectionFromClient::get_local_storage_entries()
{
    auto* document = page().page().top_level_browsing_context().active_document();
    auto local_storage = document->window().local_storage().release_value_but_fixme_should_propagate_errors();
    return local_storage->map();
}

Messages::WebContentServer::GetSessionStorageEntriesResponse ConnectionFromClient::get_session_storage_entries()
{
    auto* document = page().page().top_level_browsing_context().active_document();
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
    page().page().top_level_traversable()->set_system_visibility_state(
        visible
            ? Web::HTML::VisibilityState::Visible
            : Web::HTML::VisibilityState::Hidden);
}

void ConnectionFromClient::alert_closed()
{
    page().page().alert_closed();
}

void ConnectionFromClient::confirm_closed(bool accepted)
{
    page().page().confirm_closed(accepted);
}

void ConnectionFromClient::prompt_closed(Optional<String> const& response)
{
    page().page().prompt_closed(response);
}

void ConnectionFromClient::color_picker_update(Optional<Color> const& picked_color, Web::HTML::ColorPickerUpdateState const& state)
{
    page().page().color_picker_update(picked_color, state);
}

void ConnectionFromClient::select_dropdown_closed(Optional<String> const& value)
{
    page().page().select_dropdown_closed(value);
}

void ConnectionFromClient::toggle_media_play_state()
{
    page().page().toggle_media_play_state().release_value_but_fixme_should_propagate_errors();
}

void ConnectionFromClient::toggle_media_mute_state()
{
    page().page().toggle_media_mute_state();
}

void ConnectionFromClient::toggle_media_loop_state()
{
    page().page().toggle_media_loop_state().release_value_but_fixme_should_propagate_errors();
}

void ConnectionFromClient::toggle_media_controls_state()
{
    page().page().toggle_media_controls_state().release_value_but_fixme_should_propagate_errors();
}

void ConnectionFromClient::set_user_style(String const& source)
{
    page().page().set_user_style(source);
}

void ConnectionFromClient::enable_inspector_prototype()
{
    Web::HTML::Window::set_inspector_object_exposed(true);
}

}
