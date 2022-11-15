/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibCore/Object.h>
#include <LibCore/Stream.h>
#include <LibWeb/WebDriver/Client.h>
#include <LibWeb/WebDriver/Error.h>
#include <LibWeb/WebDriver/Response.h>
#include <WebDriver/Session.h>

namespace WebDriver {

class Client final : public Web::WebDriver::Client {
    C_OBJECT_ABSTRACT(Client);

public:
    static ErrorOr<NonnullRefPtr<Client>> try_create(NonnullOwnPtr<Core::Stream::BufferedTCPSocket>, Core::Object* parent);
    virtual ~Client() override;

    void close_session(unsigned session_id);

private:
    Client(NonnullOwnPtr<Core::Stream::BufferedTCPSocket>, Core::Object* parent);

    ErrorOr<Session*, Web::WebDriver::Error> find_session_with_id(StringView session_id);
    ErrorOr<NonnullOwnPtr<Session>, Web::WebDriver::Error> take_session_with_id(StringView session_id);

    virtual Web::WebDriver::Response new_session(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response delete_session(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_status(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_timeouts(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response set_timeouts(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response navigate_to(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_current_url(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response back(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response forward(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response refresh(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_title(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_window_handle(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response close_window(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_window_handles(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_window_rect(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response set_window_rect(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response maximize_window(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response minimize_window(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response fullscreen_window(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response find_element(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response find_elements(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response find_element_from_element(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response find_elements_from_element(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_active_element(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response is_element_selected(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_element_attribute(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_element_property(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_element_css_value(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_element_text(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_element_tag_name(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_element_rect(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response is_element_enabled(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_source(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response execute_script(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response execute_async_script(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_all_cookies(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response get_named_cookie(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response add_cookie(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response delete_cookie(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response delete_all_cookies(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response take_screenshot(Web::WebDriver::Parameters parameters, JsonValue payload) override;
    virtual Web::WebDriver::Response take_element_screenshot(Web::WebDriver::Parameters parameters, JsonValue payload) override;

    static NonnullOwnPtrVector<Session> s_sessions;
    static Atomic<unsigned> s_next_session_id;
};

}
