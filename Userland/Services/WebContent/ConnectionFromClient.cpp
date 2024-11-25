/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2024, Sam Atkins <sam@ladybird.org>
 * Copyright (c) 2021-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2023-2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/QuickSort.h>
#include <LibCore/EventLoop.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/SystemTheme.h>
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
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/SelectedFile.h>
#include <LibWeb/HTML/Storage.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Infra/Strings.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWeb/Loader/ProxyMappings.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Loader/UserAgent.h>
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

#ifdef AK_OS_SERENITY
#    include <pthread.h>
#endif

namespace WebContent {

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> socket)
    : IPC::ConnectionFromClient<WebContentClientEndpoint, WebContentServerEndpoint>(*this, move(socket), 1)
    , m_page_host(PageHost::create(*this))
{
    m_input_event_queue_timer = Web::Platform::Timer::create_single_shot(0, [this] { process_next_input_event(); });
}

ConnectionFromClient::~ConnectionFromClient() = default;

void ConnectionFromClient::die()
{
    Web::Platform::EventLoopPlugin::the().quit();
}

Optional<PageClient&> ConnectionFromClient::page(u64 index, SourceLocation location)
{
    if (auto page = m_page_host->page(index); page.has_value())
        return *page;

    dbgln("ConnectionFromClient::{}: Did not find a page with ID {}", location.function_name(), index);
    return {};
}

Optional<PageClient const&> ConnectionFromClient::page(u64 index, SourceLocation location) const
{
    if (auto page = m_page_host->page(index); page.has_value())
        return *page;

    dbgln("ConnectionFromClient::{}: Did not find a page with ID {}", location.function_name(), index);
    return {};
}

Messages::WebContentServer::GetWindowHandleResponse ConnectionFromClient::get_window_handle(u64 page_id)
{
    if (auto page = this->page(page_id); page.has_value())
        return page->page().top_level_traversable()->window_handle();
    return String {};
}

void ConnectionFromClient::set_window_handle(u64 page_id, String const& handle)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().top_level_traversable()->set_window_handle(handle);
}

void ConnectionFromClient::connect_to_webdriver(u64 page_id, ByteString const& webdriver_ipc_path)
{
    if (auto page = this->page(page_id); page.has_value()) {
        // FIXME: Propagate this error back to the browser.
        if (auto result = page->connect_to_webdriver(webdriver_ipc_path); result.is_error())
            dbgln("Unable to connect to the WebDriver process: {}", result.error());
    }
}

void ConnectionFromClient::update_system_theme(u64 page_id, Core::AnonymousBuffer const& theme_buffer)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return;

    Gfx::set_system_theme(theme_buffer);
    auto impl = Gfx::PaletteImpl::create_with_anonymous_buffer(theme_buffer);
    page->set_palette_impl(*impl);
}

void ConnectionFromClient::update_system_fonts(u64, ByteString const& default_font_query, ByteString const& fixed_width_font_query, ByteString const& window_title_font_query)
{
    Gfx::FontDatabase::set_default_font_query(default_font_query);
    Gfx::FontDatabase::set_fixed_width_font_query(fixed_width_font_query);
    Gfx::FontDatabase::set_window_title_font_query(window_title_font_query);
}

void ConnectionFromClient::update_screen_rects(u64 page_id, Vector<Web::DevicePixelRect> const& rects, u32 main_screen)
{
    if (auto page = this->page(page_id); page.has_value())
        page->set_screen_rects(rects, main_screen);
}

void ConnectionFromClient::load_url(u64 page_id, const URL::URL& url)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return;

#if defined(AK_OS_SERENITY)
    ByteString process_name;
    if (url.host().has<Empty>() || url.host() == String {})
        process_name = "WebContent";
    else
        process_name = ByteString::formatted("WebContent: {}", url.serialized_host().release_value_but_fixme_should_propagate_errors());

    pthread_setname_np(pthread_self(), process_name.characters());
#endif

    page->page().load(url);
}

void ConnectionFromClient::load_html(u64 page_id, ByteString const& html)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().load_html(html);
}

void ConnectionFromClient::reload(u64 page_id)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().reload();
}

void ConnectionFromClient::traverse_the_history_by_delta(u64 page_id, i32 delta)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().traverse_the_history_by_delta(delta);
}

void ConnectionFromClient::set_viewport_size(u64 page_id, Web::DevicePixelSize const size)
{
    if (auto page = this->page(page_id); page.has_value())
        page->set_viewport_size(size);
}

void ConnectionFromClient::add_backing_store(u64 page_id, i32 front_bitmap_id, Gfx::ShareableBitmap const& front_bitmap, i32 back_bitmap_id, Gfx::ShareableBitmap const& back_bitmap)
{
    if (auto page = this->page(page_id); page.has_value())
        page->add_backing_store(front_bitmap_id, front_bitmap, back_bitmap_id, back_bitmap);
}

void ConnectionFromClient::ready_to_paint(u64 page_id)
{
    if (auto page = this->page(page_id); page.has_value())
        page->ready_to_paint();
}

void ConnectionFromClient::process_next_input_event()
{
    if (m_input_event_queue.is_empty())
        return;

    auto event = m_input_event_queue.dequeue();

    auto page = this->page(event.page_id);
    if (!page.has_value())
        return;

    auto result = event.event.visit(
        [&](Web::KeyEvent const& event) {
            switch (event.type) {
            case Web::KeyEvent::Type::KeyDown:
                return page->page().handle_keydown(event.key, event.modifiers, event.code_point);
            case Web::KeyEvent::Type::KeyUp:
                return page->page().handle_keyup(event.key, event.modifiers, event.code_point);
            }
            VERIFY_NOT_REACHED();
        },
        [&](Web::MouseEvent const& event) {
            switch (event.type) {
            case Web::MouseEvent::Type::MouseDown:
                return page->page().handle_mousedown(event.position, event.screen_position, event.button, event.buttons, event.modifiers);
            case Web::MouseEvent::Type::MouseUp:
                return page->page().handle_mouseup(event.position, event.screen_position, event.button, event.buttons, event.modifiers);
            case Web::MouseEvent::Type::MouseMove:
                return page->page().handle_mousemove(event.position, event.screen_position, event.buttons, event.modifiers);
            case Web::MouseEvent::Type::MouseWheel:
                return page->page().handle_mousewheel(event.position, event.screen_position, event.button, event.buttons, event.modifiers, event.wheel_delta_x, event.wheel_delta_y);
            case Web::MouseEvent::Type::DoubleClick:
                return page->page().handle_doubleclick(event.position, event.screen_position, event.button, event.buttons, event.modifiers);
            }
            VERIFY_NOT_REACHED();
        },
        [&](Web::DragEvent& event) {
            return page->page().handle_drag_and_drop_event(event.type, event.position, event.screen_position, event.button, event.buttons, event.modifiers, move(event.files));
        });

    // We have to notify the client about coalesced events, so we do that by saying none of them were handled by the web page.
    for (size_t i = 0; i < event.coalesced_event_count; ++i)
        report_finished_handling_input_event(event.page_id, Web::EventResult::Dropped);
    report_finished_handling_input_event(event.page_id, result);

    if (!m_input_event_queue.is_empty())
        m_input_event_queue_timer->start();
}

void ConnectionFromClient::key_event(u64 page_id, Web::KeyEvent const& event)
{
    enqueue_input_event({ page_id, move(const_cast<Web::KeyEvent&>(event)), 0 });
}

void ConnectionFromClient::mouse_event(u64 page_id, Web::MouseEvent const& event)
{
    // OPTIMIZATION: Coalesce consecutive unprocessed mouse move and wheel events.
    auto event_to_coalesce = [&]() -> Web::MouseEvent const* {
        if (m_input_event_queue.is_empty())
            return nullptr;
        if (m_input_event_queue.tail().page_id != page_id)
            return nullptr;

        if (event.type != Web::MouseEvent::Type::MouseMove && event.type != Web::MouseEvent::Type::MouseWheel)
            return nullptr;

        if (auto const* mouse_event = m_input_event_queue.tail().event.get_pointer<Web::MouseEvent>()) {
            if (mouse_event->type == event.type)
                return mouse_event;
        }

        return nullptr;
    };

    if (auto const* last_mouse_event = event_to_coalesce()) {
        auto& mutable_event = const_cast<Web::MouseEvent&>(event);
        mutable_event.wheel_delta_x += last_mouse_event->wheel_delta_x;
        mutable_event.wheel_delta_y += last_mouse_event->wheel_delta_y;

        m_input_event_queue.tail().event = move(mutable_event);
        ++m_input_event_queue.tail().coalesced_event_count;

        return;
    }

    enqueue_input_event({ page_id, move(const_cast<Web::MouseEvent&>(event)), 0 });
}

void ConnectionFromClient::drag_event(u64 page_id, Web::DragEvent const& event)
{
    enqueue_input_event({ page_id, move(const_cast<Web::DragEvent&>(event)), 0 });
}

void ConnectionFromClient::enqueue_input_event(QueuedInputEvent event)
{
    m_input_event_queue.enqueue(move(event));
    m_input_event_queue_timer->start();
}

void ConnectionFromClient::report_finished_handling_input_event(u64 page_id, Web::EventResult event_result)
{
    async_did_finish_handling_input_event(page_id, event_result);
}

void ConnectionFromClient::debug_request(u64 page_id, ByteString const& request, ByteString const& argument)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return;

    if (request == "dump-session-history") {
        auto const& traversable = page->page().top_level_traversable();
        Web::dump_tree(*traversable);
    }

    if (request == "dump-dom-tree") {
        if (auto* doc = page->page().top_level_browsing_context().active_document())
            Web::dump_tree(*doc);
        return;
    }

    if (request == "dump-layout-tree") {
        if (auto* doc = page->page().top_level_browsing_context().active_document()) {
            if (auto* viewport = doc->layout_node())
                Web::dump_tree(*viewport);
        }
        return;
    }

    if (request == "dump-paint-tree") {
        if (auto* doc = page->page().top_level_browsing_context().active_document()) {
            if (auto* paintable = doc->paintable())
                Web::dump_tree(*paintable);
        }
        return;
    }

    if (request == "dump-stacking-context-tree") {
        if (auto* doc = page->page().top_level_browsing_context().active_document()) {
            if (auto* viewport = doc->layout_node()) {
                if (auto* stacking_context = viewport->paintable_box()->stacking_context())
                    stacking_context->dump();
            }
        }
        return;
    }

    if (request == "dump-style-sheets") {
        if (auto* doc = page->page().top_level_browsing_context().active_document()) {
            dbgln("=== In document: ===");
            for (auto& sheet : doc->style_sheets().sheets()) {
                Web::dump_sheet(sheet);
            }

            doc->for_each_shadow_root([&](auto& shadow_root) {
                dbgln("=== In shadow root {}: ===", shadow_root.host()->debug_description());
                shadow_root.for_each_css_style_sheet([&](auto& sheet) {
                    Web::dump_sheet(sheet);
                });
            });
        }
        return;
    }

    if (request == "dump-all-resolved-styles") {
        if (auto* doc = page->page().top_level_browsing_context().active_document()) {
            Queue<Web::DOM::Node*> elements_to_visit;
            elements_to_visit.enqueue(doc->document_element());
            while (!elements_to_visit.is_empty()) {
                auto element = elements_to_visit.dequeue();
                for (auto& child : element->children_as_vector())
                    elements_to_visit.enqueue(child.ptr());
                if (element->is_element()) {
                    auto styles = doc->style_computer().compute_style(*static_cast<Web::DOM::Element*>(element));
                    dbgln("+ Element {}", element->debug_description());
                    for (size_t i = 0; i < Web::CSS::StyleProperties::number_of_properties; ++i) {
                        auto property = styles->maybe_null_property(static_cast<Web::CSS::PropertyID>(i));
                        dbgln("|  {} = {}", Web::CSS::string_from_property_id(static_cast<Web::CSS::PropertyID>(i)), property ? property->to_string() : ""_string);
                    }
                    dbgln("---");
                }
            }
        }
        return;
    }

    if (request == "collect-garbage") {
        // NOTE: We use deferred_invoke here to ensure that GC runs with as little on the stack as possible.
        Core::deferred_invoke([] {
            Web::Bindings::main_thread_vm().heap().collect_garbage(JS::Heap::CollectionType::CollectGarbage, true);
        });
        return;
    }

    if (request == "set-line-box-borders") {
        bool state = argument == "on";
        page->set_should_show_line_box_borders(state);
        page->page().top_level_traversable()->set_needs_display(page->page().top_level_traversable()->viewport_rect());
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
        page->page().set_same_origin_policy_enabled(argument == "on");
        return;
    }

    if (request == "scripting") {
        page->page().set_is_scripting_enabled(argument == "on");
        return;
    }

    if (request == "block-pop-ups") {
        page->page().set_should_block_pop_ups(argument == "on");
        return;
    }

    if (request == "dump-local-storage") {
        if (auto* document = page->page().top_level_browsing_context().active_document())
            document->window()->local_storage().release_value_but_fixme_should_propagate_errors()->dump();
        return;
    }

    if (request == "load-reference-page") {
        if (auto* document = page->page().top_level_browsing_context().active_document()) {
            auto maybe_link = document->query_selector("link[rel=match]"sv);
            if (maybe_link.is_error() || !maybe_link.value()) {
                // To make sure that we fail the ref-test if the link is missing, load the error page->
                load_html(page_id, "<h1>Failed to find &lt;link rel=&quot;match&quot; /&gt; in ref test page!</h1> Make sure you added it.");
            } else {
                auto link = maybe_link.release_value();
                auto url = document->parse_url(link->get_attribute_value(Web::HTML::AttributeNames::href));
                load_url(page_id, url);
            }
        }
        return;
    }

    if (request == "navigator-compatibility-mode") {
        Web::NavigatorCompatibilityMode compatibility_mode;
        if (argument == "chrome") {
            compatibility_mode = Web::NavigatorCompatibilityMode::Chrome;
        } else if (argument == "gecko") {
            compatibility_mode = Web::NavigatorCompatibilityMode::Gecko;
        } else if (argument == "webkit") {
            compatibility_mode = Web::NavigatorCompatibilityMode::WebKit;
        } else {
            dbgln("Unknown navigator compatibility mode '{}', defaulting to Chrome", argument);
            compatibility_mode = Web::NavigatorCompatibilityMode::Chrome;
        }

        Web::ResourceLoader::the().set_navigator_compatibility_mode(compatibility_mode);
        return;
    }
}

void ConnectionFromClient::get_source(u64 page_id)
{
    if (auto page = this->page(page_id); page.has_value()) {
        if (auto* doc = page->page().top_level_browsing_context().active_document())
            async_did_get_source(page_id, doc->url(), doc->base_url(), doc->source());
    }
}

void ConnectionFromClient::inspect_dom_tree(u64 page_id)
{
    if (auto page = this->page(page_id); page.has_value()) {
        if (auto* doc = page->page().top_level_browsing_context().active_document())
            async_did_inspect_dom_tree(page_id, doc->dump_dom_tree_as_json().to_byte_string());
    }
}

void ConnectionFromClient::inspect_dom_node(u64 page_id, i32 node_id, Optional<Web::CSS::Selector::PseudoElement::Type> const& pseudo_element)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return;

    auto& top_context = page->page().top_level_browsing_context();

    top_context.for_each_in_inclusive_subtree([&](auto& ctx) {
        if (ctx.active_document() != nullptr) {
            ctx.active_document()->set_inspected_node(nullptr, {});
        }
        return Web::TraversalDecision::Continue;
    });

    Web::DOM::Node* node = Web::DOM::Node::from_unique_id(node_id);
    // Note: Nodes without layout (aka non-visible nodes, don't have style computed)
    if (!node || !node->layout_node()) {
        async_did_inspect_dom_node(page_id, false, {}, {}, {}, {}, {}, {});
        return;
    }

    node->document().set_inspected_node(node, pseudo_element);

    if (node->is_element()) {
        auto& element = verify_cast<Web::DOM::Element>(*node);
        if (!element.computed_css_values()) {
            async_did_inspect_dom_node(page_id, false, {}, {}, {}, {}, {}, {});
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

        auto serialize_fonts_json = [](Web::CSS::StyleProperties const& properties) -> ByteString {
            StringBuilder builder;
            auto serializer = MUST(JsonArraySerializer<>::try_create(builder));

            auto const& font_list = properties.computed_font_list();
            font_list.for_each_font_entry([&serializer](Gfx::FontCascadeList::Entry const& entry) {
                auto const& font = entry.font;
                auto font_json_object = MUST(serializer.add_object());
                MUST(font_json_object.add("name"sv, font->family()));
                MUST(font_json_object.add("size"sv, font->point_size()));
                MUST(font_json_object.add("weight"sv, font->weight()));
                MUST(font_json_object.add("variant"sv, font->variant()));
                MUST(font_json_object.finish());
            });
            MUST(serializer.finish());
            return builder.to_byte_string();
        };

        if (pseudo_element.has_value()) {
            auto pseudo_element_node = element.get_pseudo_element_node(pseudo_element.value());
            if (!pseudo_element_node) {
                async_did_inspect_dom_node(page_id, false, {}, {}, {}, {}, {}, {});
                return;
            }

            auto pseudo_element_style = element.pseudo_element_computed_css_values(pseudo_element.value());
            ByteString computed_values = serialize_json(*pseudo_element_style);
            ByteString resolved_values = serialize_json(*element.resolved_css_values(pseudo_element.value()));
            ByteString custom_properties_json = serialize_custom_properties_json(element, pseudo_element);
            ByteString node_box_sizing_json = serialize_node_box_sizing_json(pseudo_element_node.ptr());
            ByteString fonts_json = serialize_fonts_json(*pseudo_element_style);

            async_did_inspect_dom_node(page_id, true, move(computed_values), move(resolved_values), move(custom_properties_json), move(node_box_sizing_json), {}, move(fonts_json));
            return;
        }

        ByteString computed_values = serialize_json(*element.computed_css_values());
        ByteString resolved_values = serialize_json(element.resolved_css_values());
        ByteString custom_properties_json = serialize_custom_properties_json(element, {});
        ByteString node_box_sizing_json = serialize_node_box_sizing_json(element.layout_node());
        ByteString aria_properties_state_json = serialize_aria_properties_state_json(element);
        ByteString fonts_json = serialize_fonts_json(*element.computed_css_values());

        async_did_inspect_dom_node(page_id, true, move(computed_values), move(resolved_values), move(custom_properties_json), move(node_box_sizing_json), move(aria_properties_state_json), move(fonts_json));
        return;
    }

    async_did_inspect_dom_node(page_id, false, {}, {}, {}, {}, {}, {});
}

void ConnectionFromClient::inspect_accessibility_tree(u64 page_id)
{
    if (auto page = this->page(page_id); page.has_value()) {
        if (auto* doc = page->page().top_level_browsing_context().active_document())
            async_did_inspect_accessibility_tree(page_id, doc->dump_accessibility_tree_as_json().to_byte_string());
    }
}

void ConnectionFromClient::get_hovered_node_id(u64 page_id)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return;

    i32 node_id = 0;

    if (auto* document = page->page().top_level_browsing_context().active_document()) {
        if (auto* hovered_node = document->hovered_node())
            node_id = hovered_node->unique_id();
    }

    async_did_get_hovered_node_id(page_id, node_id);
}

void ConnectionFromClient::list_style_sheets(u64 page_id)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return;

    async_inspector_did_list_style_sheets(page_id, page->list_style_sheets());
}

void ConnectionFromClient::request_style_sheet_source(u64 page_id, Web::CSS::StyleSheetIdentifier const& identifier)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return;

    if (auto* document = page->page().top_level_browsing_context().active_document()) {
        if (auto stylesheet = document->get_style_sheet_source(identifier); stylesheet.has_value())
            async_did_get_style_sheet_source(page_id, identifier, document->base_url(), stylesheet.value());
    }
}

void ConnectionFromClient::set_dom_node_text(u64 page_id, i32 node_id, String const& text)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node || (!dom_node->is_text() && !dom_node->is_comment())) {
        async_did_finish_editing_dom_node(page_id, {});
        return;
    }

    auto& character_data = static_cast<Web::DOM::CharacterData&>(*dom_node);
    character_data.set_data(text);

    async_did_finish_editing_dom_node(page_id, character_data.unique_id());
}

void ConnectionFromClient::set_dom_node_tag(u64 page_id, i32 node_id, String const& name)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node || !dom_node->is_element() || !dom_node->parent()) {
        async_did_finish_editing_dom_node(page_id, {});
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
    async_did_finish_editing_dom_node(page_id, new_element->unique_id());
}

void ConnectionFromClient::add_dom_node_attributes(u64 page_id, i32 node_id, Vector<WebView::Attribute> const& attributes)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node || !dom_node->is_element()) {
        async_did_finish_editing_dom_node(page_id, {});
        return;
    }

    auto& element = static_cast<Web::DOM::Element&>(*dom_node);

    for (auto const& attribute : attributes) {
        // NOTE: We ignore invalid attributes for now, but we may want to send feedback to the user that this failed.
        (void)element.set_attribute(attribute.name, attribute.value);
    }

    async_did_finish_editing_dom_node(page_id, element.unique_id());
}

void ConnectionFromClient::replace_dom_node_attribute(u64 page_id, i32 node_id, String const& name, Vector<WebView::Attribute> const& replacement_attributes)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node || !dom_node->is_element()) {
        async_did_finish_editing_dom_node(page_id, {});
        return;
    }

    auto& element = static_cast<Web::DOM::Element&>(*dom_node);
    bool should_remove_attribute = true;

    for (auto const& attribute : replacement_attributes) {
        if (should_remove_attribute && Web::Infra::is_ascii_case_insensitive_match(name, attribute.name))
            should_remove_attribute = false;

        // NOTE: We ignore invalid attributes for now, but we may want to send feedback to the user that this failed.
        (void)element.set_attribute(attribute.name, attribute.value);
    }

    if (should_remove_attribute)
        element.remove_attribute(name);

    async_did_finish_editing_dom_node(page_id, element.unique_id());
}

void ConnectionFromClient::create_child_element(u64 page_id, i32 node_id)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node) {
        async_did_finish_editing_dom_node(page_id, {});
        return;
    }

    auto element = Web::DOM::create_element(dom_node->document(), Web::HTML::TagNames::div, Web::Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    dom_node->append_child(element).release_value_but_fixme_should_propagate_errors();

    async_did_finish_editing_dom_node(page_id, element->unique_id());
}

void ConnectionFromClient::create_child_text_node(u64 page_id, i32 node_id)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node) {
        async_did_finish_editing_dom_node(page_id, {});
        return;
    }

    auto text_node = dom_node->heap().allocate<Web::DOM::Text>(dom_node->realm(), dom_node->document(), "text"_string);
    dom_node->append_child(text_node).release_value_but_fixme_should_propagate_errors();

    async_did_finish_editing_dom_node(page_id, text_node->unique_id());
}

void ConnectionFromClient::clone_dom_node(u64 page_id, i32 node_id)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node || !dom_node->parent_node()) {
        async_did_finish_editing_dom_node(page_id, {});
        return;
    }

    auto dom_node_clone = MUST(dom_node->clone_node(nullptr, true));
    dom_node->parent_node()->insert_before(dom_node_clone, dom_node->next_sibling());

    async_did_finish_editing_dom_node(page_id, dom_node_clone->unique_id());
}

void ConnectionFromClient::remove_dom_node(u64 page_id, i32 node_id)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return;

    auto* active_document = page->page().top_level_browsing_context().active_document();
    if (!active_document) {
        async_did_finish_editing_dom_node(page_id, {});
        return;
    }

    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node) {
        async_did_finish_editing_dom_node(page_id, {});
        return;
    }

    auto* previous_dom_node = dom_node->previous_sibling();
    if (!previous_dom_node)
        previous_dom_node = dom_node->parent();

    dom_node->remove();

    async_did_finish_editing_dom_node(page_id, previous_dom_node->unique_id());
}

void ConnectionFromClient::get_dom_node_html(u64 page_id, i32 node_id)
{
    auto* dom_node = Web::DOM::Node::from_unique_id(node_id);
    if (!dom_node)
        return;

    String html;

    if (dom_node->is_element()) {
        auto const& element = static_cast<Web::DOM::Element const&>(*dom_node);
        html = element.outer_html().release_value_but_fixme_should_propagate_errors();
    } else if (dom_node->is_text() || dom_node->is_comment()) {
        auto const& character_data = static_cast<Web::DOM::CharacterData const&>(*dom_node);
        html = character_data.data();
    } else {
        return;
    }

    async_did_get_dom_node_html(page_id, move(html));
}

void ConnectionFromClient::take_document_screenshot(u64 page_id)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return;

    page->queue_screenshot_task({});
}

void ConnectionFromClient::take_dom_node_screenshot(u64 page_id, i32 node_id)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return;

    page->queue_screenshot_task(node_id);
}

static void append_page_text(Web::Page& page, StringBuilder& builder)
{
    auto* document = page.top_level_browsing_context().active_document();
    if (!document) {
        builder.append("(no DOM tree)"sv);
        return;
    }

    auto* body = document->body();
    if (!body) {
        builder.append("(no body)"sv);
        return;
    }

    builder.append(body->inner_text());
}

static void append_layout_tree(Web::Page& page, StringBuilder& builder)
{
    auto* document = page.top_level_browsing_context().active_document();
    if (!document) {
        builder.append("(no DOM tree)"sv);
        return;
    }

    document->update_layout();

    auto* layout_root = document->layout_node();
    if (!layout_root) {
        builder.append("(no layout tree)"sv);
        return;
    }

    Web::dump_tree(builder, *layout_root);
}

static void append_paint_tree(Web::Page& page, StringBuilder& builder)
{
    auto* document = page.top_level_browsing_context().active_document();
    if (!document) {
        builder.append("(no DOM tree)"sv);
        return;
    }

    document->update_layout();

    auto* layout_root = document->layout_node();
    if (!layout_root) {
        builder.append("(no layout tree)"sv);
        return;
    }
    if (!layout_root->paintable()) {
        builder.append("(no paint tree)"sv);
        return;
    }

    Web::dump_tree(builder, *layout_root->paintable());
}

static void append_gc_graph(StringBuilder& builder)
{
    auto gc_graph = Web::Bindings::main_thread_vm().heap().dump_graph();
    gc_graph.serialize(builder);
}

void ConnectionFromClient::request_internal_page_info(u64 page_id, WebView::PageInfoType type)
{
    auto page = this->page(page_id);
    if (!page.has_value()) {
        async_did_get_internal_page_info(page_id, type, "(no page)"_string);
        return;
    }

    StringBuilder builder;

    if (has_flag(type, WebView::PageInfoType::Text)) {
        append_page_text(page->page(), builder);
    }

    if (has_flag(type, WebView::PageInfoType::LayoutTree)) {
        if (!builder.is_empty())
            builder.append("\n"sv);
        append_layout_tree(page->page(), builder);
    }

    if (has_flag(type, WebView::PageInfoType::PaintTree)) {
        if (!builder.is_empty())
            builder.append("\n"sv);
        append_paint_tree(page->page(), builder);
    }

    if (has_flag(type, WebView::PageInfoType::GCGraph)) {
        if (!builder.is_empty())
            builder.append("\n"sv);
        append_gc_graph(builder);
    }

    async_did_get_internal_page_info(page_id, type, MUST(builder.to_string()));
}

Messages::WebContentServer::GetSelectedTextResponse ConnectionFromClient::get_selected_text(u64 page_id)
{
    if (auto page = this->page(page_id); page.has_value())
        return page->page().focused_navigable().selected_text().to_byte_string();
    return ByteString {};
}

void ConnectionFromClient::select_all(u64 page_id)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().focused_navigable().select_all();
}

void ConnectionFromClient::find_in_page(u64 page_id, String const& query, CaseSensitivity case_sensitivity)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return;

    auto result = page->page().find_in_page({ .string = query, .case_sensitivity = case_sensitivity });
    async_did_find_in_page(page_id, result.current_match_index, result.total_match_count);
}

void ConnectionFromClient::find_in_page_next_match(u64 page_id)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return;

    auto result = page->page().find_in_page_next_match();
    async_did_find_in_page(page_id, result.current_match_index, result.total_match_count);
}

void ConnectionFromClient::find_in_page_previous_match(u64 page_id)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return;

    auto result = page->page().find_in_page_previous_match();
    async_did_find_in_page(page_id, result.current_match_index, result.total_match_count);
}

void ConnectionFromClient::paste(u64 page_id, String const& text)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().focused_navigable().paste(text);
}

void ConnectionFromClient::set_content_filters(u64, Vector<String> const& filters)
{
    Web::ContentFilter::the().set_patterns(filters).release_value_but_fixme_should_propagate_errors();
}

void ConnectionFromClient::set_autoplay_allowed_on_all_websites(u64)
{
    auto& autoplay_allowlist = Web::PermissionsPolicy::AutoplayAllowlist::the();
    autoplay_allowlist.enable_globally();
}

void ConnectionFromClient::set_autoplay_allowlist(u64, Vector<String> const& allowlist)
{
    auto& autoplay_allowlist = Web::PermissionsPolicy::AutoplayAllowlist::the();
    autoplay_allowlist.enable_for_origins(allowlist).release_value_but_fixme_should_propagate_errors();
}

void ConnectionFromClient::set_proxy_mappings(u64, Vector<ByteString> const& proxies, HashMap<ByteString, size_t> const& mappings)
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

void ConnectionFromClient::set_preferred_color_scheme(u64 page_id, Web::CSS::PreferredColorScheme const& color_scheme)
{
    if (auto page = this->page(page_id); page.has_value())
        page->set_preferred_color_scheme(color_scheme);
}

void ConnectionFromClient::set_preferred_contrast(u64 page_id, Web::CSS::PreferredContrast const& contrast)
{
    if (auto page = this->page(page_id); page.has_value())
        page->set_preferred_contrast(contrast);
}

void ConnectionFromClient::set_preferred_motion(u64 page_id, Web::CSS::PreferredMotion const& motion)
{
    if (auto page = this->page(page_id); page.has_value())
        page->set_preferred_motion(motion);
}

void ConnectionFromClient::set_preferred_languages(u64, Vector<String> const& preferred_languages)
{
    // FIXME: Whenever the user agent needs to make the navigator.languages attribute of a Window or WorkerGlobalScope
    // object global return a new set of language tags, the user agent must queue a global task on the DOM manipulation
    // task source given global to fire an event named languagechange at global, and wait until that task begins to be
    // executed before actually returning a new value.
    Web::ResourceLoader::the().set_preferred_languages(preferred_languages);
}

void ConnectionFromClient::set_enable_do_not_track(u64, bool enable)
{
    Web::ResourceLoader::the().set_enable_do_not_track(enable);
}

void ConnectionFromClient::set_has_focus(u64 page_id, bool has_focus)
{
    if (auto page = this->page(page_id); page.has_value())
        page->set_has_focus(has_focus);
}

void ConnectionFromClient::set_is_scripting_enabled(u64 page_id, bool is_scripting_enabled)
{
    if (auto page = this->page(page_id); page.has_value())
        page->set_is_scripting_enabled(is_scripting_enabled);
}

void ConnectionFromClient::set_device_pixels_per_css_pixel(u64 page_id, float device_pixels_per_css_pixel)
{
    if (auto page = this->page(page_id); page.has_value())
        page->set_device_pixels_per_css_pixel(device_pixels_per_css_pixel);
}

void ConnectionFromClient::set_window_position(u64 page_id, Web::DevicePixelPoint position)
{
    if (auto page = this->page(page_id); page.has_value())
        page->set_window_position(position);
}

void ConnectionFromClient::set_window_size(u64 page_id, Web::DevicePixelSize size)
{
    if (auto page = this->page(page_id); page.has_value())
        page->set_window_size(size);
}

Messages::WebContentServer::GetLocalStorageEntriesResponse ConnectionFromClient::get_local_storage_entries(u64 page_id)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return OrderedHashMap<String, String> {};

    auto* document = page->page().top_level_browsing_context().active_document();
    auto local_storage = document->window()->local_storage().release_value_but_fixme_should_propagate_errors();
    return local_storage->map();
}

Messages::WebContentServer::GetSessionStorageEntriesResponse ConnectionFromClient::get_session_storage_entries(u64 page_id)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return OrderedHashMap<String, String> {};

    auto* document = page->page().top_level_browsing_context().active_document();
    auto session_storage = document->window()->session_storage().release_value_but_fixme_should_propagate_errors();
    return session_storage->map();
}

void ConnectionFromClient::handle_file_return(u64, i32 error, Optional<IPC::File> const& file, i32 request_id)
{
    auto file_request = m_requested_files.take(request_id);

    VERIFY(file_request.has_value());
    VERIFY(file_request.value().on_file_request_finish);

    file_request.value().on_file_request_finish(error != 0 ? Error::from_errno(error) : ErrorOr<i32> { file->take_fd() });
}

void ConnectionFromClient::request_file(u64 page_id, Web::FileRequest file_request)
{
    i32 const id = last_id++;

    auto path = file_request.path();
    m_requested_files.set(id, move(file_request));

    async_did_request_file(page_id, path, id);
}

void ConnectionFromClient::set_system_visibility_state(u64 page_id, bool visible)
{
    if (auto page = this->page(page_id); page.has_value()) {
        page->page().top_level_traversable()->set_system_visibility_state(
            visible
                ? Web::HTML::VisibilityState::Visible
                : Web::HTML::VisibilityState::Hidden);
    }
}

void ConnectionFromClient::js_console_input(u64 page_id, ByteString const& js_source)
{
    auto page = this->page(page_id);
    if (!page.has_value())
        return;

    page->js_console_input(js_source);
}

void ConnectionFromClient::run_javascript(u64 page_id, ByteString const& js_source)
{
    if (auto page = this->page(page_id); page.has_value())
        page->run_javascript(js_source);
}

void ConnectionFromClient::js_console_request_messages(u64 page_id, i32 start_index)
{
    if (auto page = this->page(page_id); page.has_value())
        page->js_console_request_messages(start_index);
}

void ConnectionFromClient::alert_closed(u64 page_id)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().alert_closed();
}

void ConnectionFromClient::confirm_closed(u64 page_id, bool accepted)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().confirm_closed(accepted);
}

void ConnectionFromClient::prompt_closed(u64 page_id, Optional<String> const& response)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().prompt_closed(response);
}

void ConnectionFromClient::color_picker_update(u64 page_id, Optional<Color> const& picked_color, Web::HTML::ColorPickerUpdateState const& state)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().color_picker_update(picked_color, state);
}

void ConnectionFromClient::file_picker_closed(u64 page_id, Vector<Web::HTML::SelectedFile> const& selected_files)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().file_picker_closed(const_cast<Vector<Web::HTML::SelectedFile>&>(selected_files));
}

void ConnectionFromClient::select_dropdown_closed(u64 page_id, Optional<u32> const& selected_item_id)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().select_dropdown_closed(selected_item_id);
}

void ConnectionFromClient::toggle_media_play_state(u64 page_id)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().toggle_media_play_state().release_value_but_fixme_should_propagate_errors();
}

void ConnectionFromClient::toggle_media_mute_state(u64 page_id)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().toggle_media_mute_state();
}

void ConnectionFromClient::toggle_media_loop_state(u64 page_id)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().toggle_media_loop_state().release_value_but_fixme_should_propagate_errors();
}

void ConnectionFromClient::toggle_media_controls_state(u64 page_id)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().toggle_media_controls_state().release_value_but_fixme_should_propagate_errors();
}

void ConnectionFromClient::toggle_page_mute_state(u64 page_id)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().toggle_page_mute_state();
}

void ConnectionFromClient::set_user_style(u64 page_id, String const& source)
{
    if (auto page = this->page(page_id); page.has_value())
        page->page().set_user_style(source);
}

void ConnectionFromClient::enable_inspector_prototype(u64)
{
    Web::HTML::Window::set_inspector_object_exposed(true);
}

}
