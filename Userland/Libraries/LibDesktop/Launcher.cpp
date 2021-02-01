/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/JsonObject.h>
#include <AK/URL.h>
#include <LaunchServer/LaunchClientEndpoint.h>
#include <LaunchServer/LaunchServerEndpoint.h>
#include <LibDesktop/Launcher.h>
#include <LibIPC/ServerConnection.h>
#include <stdlib.h>

namespace Desktop {

auto Launcher::Details::from_details_str(const String& details_str) -> NonnullRefPtr<Details>
{
    auto details = adopt(*new Details);
    auto json = JsonValue::from_string(details_str);
    ASSERT(json.has_value());
    auto obj = json.value().as_object();
    details->executable = obj.get("executable").to_string();
    details->name = obj.get("name").to_string();
    if (auto type_value = obj.get_ptr("type")) {
        auto type_str = type_value->to_string();
        if (type_str == "app")
            details->launcher_type = LauncherType::Application;
        else if (type_str == "userpreferred")
            details->launcher_type = LauncherType::UserPreferred;
        else if (type_str == "userdefault")
            details->launcher_type = LauncherType::UserDefault;
    }
    return details;
}

class LaunchServerConnection : public IPC::ServerConnection<LaunchClientEndpoint, LaunchServerEndpoint>
    , public LaunchClientEndpoint {
    C_OBJECT(LaunchServerConnection)
public:
    virtual void handshake() override
    {
        send_sync<Messages::LaunchServer::Greet>();
    }

private:
    LaunchServerConnection()
        : IPC::ServerConnection<LaunchClientEndpoint, LaunchServerEndpoint>(*this, "/tmp/portal/launch")
    {
    }
    virtual void handle(const Messages::LaunchClient::Dummy&) override { }
};

static LaunchServerConnection& connection()
{
    static auto connection = LaunchServerConnection::construct();
    return connection;
}

bool Launcher::add_allowed_url(const URL& url)
{
    auto response = connection().send_sync<Messages::LaunchServer::AddAllowedURL>(url);
    if (!response) {
        dbgln("Launcher::add_allowed_url: Failed");
        return false;
    }
    return true;
}

bool Launcher::add_allowed_handler_with_any_url(const String& handler)
{
    auto response = connection().send_sync<Messages::LaunchServer::AddAllowedHandlerWithAnyURL>(handler);
    if (!response) {
        dbgln("Launcher::add_allowed_handler_with_any_url: Failed");
        return false;
    }
    return true;
}

bool Launcher::add_allowed_handler_with_only_specific_urls(const String& handler, const Vector<URL>& urls)
{
    auto response = connection().send_sync<Messages::LaunchServer::AddAllowedHandlerWithOnlySpecificURLs>(handler, urls);
    if (!response) {
        dbgln("Launcher::add_allowed_handler_with_only_specific_urls: Failed");
        return false;
    }
    return true;
}

bool Launcher::seal_allowlist()
{
    auto response = connection().send_sync<Messages::LaunchServer::SealAllowlist>();
    if (!response) {
        dbgln("Launcher::seal_allowlist: Failed");
        return false;
    }
    return true;
}

bool Launcher::open(const URL& url, const String& handler_name)
{
    return connection().send_sync<Messages::LaunchServer::OpenURL>(url, handler_name)->response();
}

bool Launcher::open(const URL& url, const Details& details)
{
    ASSERT(details.launcher_type != LauncherType::Application); // Launcher should not be used to execute arbitrary applications
    return open(url, details.executable);
}

Vector<String> Launcher::get_handlers_for_url(const URL& url)
{
    return connection().send_sync<Messages::LaunchServer::GetHandlersForURL>(url.to_string())->handlers();
}

auto Launcher::get_handlers_with_details_for_url(const URL& url) -> NonnullRefPtrVector<Details>
{
    auto details = connection().send_sync<Messages::LaunchServer::GetHandlersWithDetailsForURL>(url.to_string())->handlers_details();
    NonnullRefPtrVector<Details> handlers_with_details;
    for (auto& value : details) {
        handlers_with_details.append(Details::from_details_str(value));
    }
    return handlers_with_details;
}

}
