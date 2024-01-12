/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ConnectionToServer.h>
#include <LibWeb/HTML/ActivateTab.h>
#include <LibWeb/HTML/SelectItem.h>
#include <WebContent/WebContentClientEndpoint.h>
#include <WebContent/WebContentServerEndpoint.h>

namespace WebView {

class ViewImplementation;

class WebContentClient final
    : public IPC::ConnectionToServer<WebContentClientEndpoint, WebContentServerEndpoint>
    , public WebContentClientEndpoint {
    IPC_CLIENT_CONNECTION(WebContentClient, "/tmp/session/%sid/portal/webcontent"sv);

public:
    WebContentClient(NonnullOwnPtr<Core::LocalSocket>, ViewImplementation&);

    Function<void()> on_web_content_process_crash;

private:
    virtual void die() override;

    virtual void did_paint(Gfx::IntRect const&, i32) override;
    virtual void did_finish_loading(AK::URL const&) override;
    virtual void did_request_navigate_back() override;
    virtual void did_request_navigate_forward() override;
    virtual void did_request_refresh() override;
    virtual void did_request_cursor_change(i32) override;
    virtual void did_layout(Gfx::IntSize) override;
    virtual void did_change_title(ByteString const&) override;
    virtual void did_request_scroll(i32, i32) override;
    virtual void did_request_scroll_to(Gfx::IntPoint) override;
    virtual void did_enter_tooltip_area(Gfx::IntPoint, ByteString const&) override;
    virtual void did_leave_tooltip_area() override;
    virtual void did_hover_link(AK::URL const&) override;
    virtual void did_unhover_link() override;
    virtual void did_click_link(AK::URL const&, ByteString const&, unsigned) override;
    virtual void did_middle_click_link(AK::URL const&, ByteString const&, unsigned) override;
    virtual void did_start_loading(AK::URL const&, bool) override;
    virtual void did_request_context_menu(Gfx::IntPoint) override;
    virtual void did_request_link_context_menu(Gfx::IntPoint, AK::URL const&, ByteString const&, unsigned) override;
    virtual void did_request_image_context_menu(Gfx::IntPoint, AK::URL const&, ByteString const&, unsigned, Gfx::ShareableBitmap const&) override;
    virtual void did_request_media_context_menu(Gfx::IntPoint, ByteString const&, unsigned, Web::Page::MediaContextMenu const&) override;
    virtual void did_get_source(AK::URL const&, ByteString const&) override;
    virtual void did_inspect_dom_tree(ByteString const&) override;
    virtual void did_inspect_dom_node(bool has_style, ByteString const& computed_style, ByteString const& resolved_style, ByteString const& custom_properties, ByteString const& node_box_sizing, ByteString const& aria_properties_state) override;
    virtual void did_inspect_accessibility_tree(ByteString const&) override;
    virtual void did_get_hovered_node_id(i32 node_id) override;
    virtual void did_finish_editing_dom_node(Optional<i32> const& node_id) override;
    virtual void did_get_dom_node_html(String const& html) override;
    virtual void did_take_screenshot(Gfx::ShareableBitmap const& screenshot) override;
    virtual void did_output_js_console_message(i32 message_index) override;
    virtual void did_get_js_console_messages(i32 start_index, Vector<ByteString> const& message_types, Vector<ByteString> const& messages) override;
    virtual void did_change_favicon(Gfx::ShareableBitmap const&) override;
    virtual void did_request_alert(String const&) override;
    virtual void did_request_confirm(String const&) override;
    virtual void did_request_prompt(String const&, String const&) override;
    virtual void did_request_set_prompt_text(String const& message) override;
    virtual void did_request_accept_dialog() override;
    virtual void did_request_dismiss_dialog() override;
    virtual Messages::WebContentClient::DidRequestAllCookiesResponse did_request_all_cookies(AK::URL const&) override;
    virtual Messages::WebContentClient::DidRequestNamedCookieResponse did_request_named_cookie(AK::URL const&, ByteString const&) override;
    virtual Messages::WebContentClient::DidRequestCookieResponse did_request_cookie(AK::URL const&, u8) override;
    virtual void did_set_cookie(AK::URL const&, Web::Cookie::ParsedCookie const&, u8) override;
    virtual void did_update_cookie(Web::Cookie::Cookie const&) override;
    virtual Messages::WebContentClient::DidRequestNewTabResponse did_request_new_tab(Web::HTML::ActivateTab const& activate_tab) override;
    virtual void did_request_activate_tab() override;
    virtual void did_close_browsing_context() override;
    virtual void did_update_resource_count(i32 count_waiting) override;
    virtual void did_request_restore_window() override;
    virtual Messages::WebContentClient::DidRequestRepositionWindowResponse did_request_reposition_window(Gfx::IntPoint) override;
    virtual Messages::WebContentClient::DidRequestResizeWindowResponse did_request_resize_window(Gfx::IntSize) override;
    virtual Messages::WebContentClient::DidRequestMaximizeWindowResponse did_request_maximize_window() override;
    virtual Messages::WebContentClient::DidRequestMinimizeWindowResponse did_request_minimize_window() override;
    virtual Messages::WebContentClient::DidRequestFullscreenWindowResponse did_request_fullscreen_window() override;
    virtual void did_request_file(ByteString const& path, i32) override;
    virtual void did_request_color_picker(Color const& current_color) override;
    virtual void did_request_select_dropdown(Gfx::IntPoint content_position, i32 minimum_width, Vector<Web::HTML::SelectItem> const& items) override;
    virtual void did_finish_handling_input_event(bool event_was_accepted) override;
    virtual void did_finish_text_test() override;
    virtual void did_change_theme_color(Gfx::Color color) override;
    virtual void did_insert_clipboard_entry(String const& data, String const& presentation_style, String const& mime_type) override;
    virtual void inspector_did_load() override;
    virtual void inspector_did_select_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement::Type> const& pseudo_element) override;
    virtual void inspector_did_set_dom_node_text(i32 node_id, String const& text) override;
    virtual void inspector_did_set_dom_node_tag(i32 node_id, String const& tag) override;
    virtual void inspector_did_add_dom_node_attributes(i32 node_id, Vector<Attribute> const& attributes) override;
    virtual void inspector_did_replace_dom_node_attribute(i32 node_id, String const& name, Vector<Attribute> const& replacement_attributes) override;
    virtual void inspector_did_request_dom_tree_context_menu(i32 node_id, Gfx::IntPoint position, String const& type, Optional<String> const& tag, Optional<Attribute> const& attribute) override;
    virtual void inspector_did_execute_console_script(String const& script) override;
    virtual Messages::WebContentClient::RequestWorkerAgentResponse request_worker_agent() override;

    ViewImplementation& m_view;
};

}
