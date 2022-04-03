/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/JsonValue.h>
#include <AK/RefPtr.h>
#include <WebDriver/ConnectionFromClient.h>
#include <WebDriver/HttpError.h>
#include <unistd.h>

namespace WebDriver {
class WebDriverSession {
public:
    WebDriverSession(unsigned session_id);
    ~WebDriverSession();

    unsigned session_id() const { return m_id; }

    struct Window {
        String handle;
        bool is_open;
    };

    HashMap<String, NonnullOwnPtr<Window>>& get_window_handles() { return m_windows; }
    Optional<Window*> get_window_object() { return m_windows.get(m_current_window_handle); }
    String get_window() { return m_current_window_handle; }

    ErrorOr<void> start();
    ErrorOr<void> stop();
    ErrorOr<void, Variant<HttpError, Error>> delete_window();
    ErrorOr<JsonValue, HttpError> post_url(JsonValue url);
    ErrorOr<JsonValue, HttpError> get_title();

private:
    bool m_started = false;
    pid_t m_browser_pid = 0;
    unsigned m_id;
    HashMap<String, NonnullOwnPtr<Window>> m_windows;
    String m_current_window_handle;
    RefPtr<Core::LocalServer> m_local_server;
    RefPtr<ConnectionFromClient> m_browser_connection;
};
}
