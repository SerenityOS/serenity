/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <LaunchServer/LaunchClientEndpoint.h>
#include <LaunchServer/LaunchServerEndpoint.h>
#include <LibDesktop/Launcher.h>
#include <LibIPC/ConnectionToServer.h>
#include <LibURL/URL.h>

namespace Desktop {

auto Launcher::Details::from_details_str(ByteString const& details_str) -> NonnullRefPtr<Details>
{
    auto details = adopt_ref(*new Details);
    auto json = JsonValue::from_string(details_str).release_value_but_fixme_should_propagate_errors();
    auto const& obj = json.as_object();
    details->executable = obj.get_byte_string("executable"sv).value_or({});
    details->name = obj.get_byte_string("name"sv).value_or({});

    obj.get_array("arguments"sv).value().for_each([&](JsonValue const& argument) {
        details->arguments.append(argument.as_string());
    });

    if (auto type_value = obj.get_byte_string("type"sv); type_value.has_value()) {
        auto const& type_str = type_value.value();
        if (type_str == "app")
            details->launcher_type = LauncherType::Application;
        else if (type_str == "userpreferred")
            details->launcher_type = LauncherType::UserPreferred;
        else if (type_str == "userdefault")
            details->launcher_type = LauncherType::UserDefault;
    }
    return details;
}

class ConnectionToLaunchServer final
    : public IPC::ConnectionToServer<LaunchClientEndpoint, LaunchServerEndpoint>
    , public LaunchClientEndpoint {
    IPC_CLIENT_CONNECTION(ConnectionToLaunchServer, "/tmp/session/%sid/portal/launch"sv)
private:
    ConnectionToLaunchServer(NonnullOwnPtr<Core::LocalSocket> socket)
        : IPC::ConnectionToServer<LaunchClientEndpoint, LaunchServerEndpoint>(*this, move(socket))
    {
    }
};

static ConnectionToLaunchServer& connection()
{
    static auto connection = ConnectionToLaunchServer::try_create().release_value_but_fixme_should_propagate_errors();
    return connection;
}

void Launcher::ensure_connection()
{
    [[maybe_unused]] auto& conn = connection();
}

ErrorOr<void> Launcher::add_allowed_url(URL::URL const& url)
{
    auto response_or_error = connection().try_add_allowed_url(url);
    if (response_or_error.is_error())
        return Error::from_string_literal("Launcher::add_allowed_url: Failed");
    return {};
}

ErrorOr<void> Launcher::add_allowed_handler_with_any_url(ByteString const& handler)
{
    auto response_or_error = connection().try_add_allowed_handler_with_any_url(handler);
    if (response_or_error.is_error())
        return Error::from_string_literal("Launcher::add_allowed_handler_with_any_url: Failed");
    return {};
}

ErrorOr<void> Launcher::add_allowed_handler_with_only_specific_urls(ByteString const& handler, Vector<URL::URL> const& urls)
{
    auto response_or_error = connection().try_add_allowed_handler_with_only_specific_urls(handler, urls);
    if (response_or_error.is_error())
        return Error::from_string_literal("Launcher::add_allowed_handler_with_only_specific_urls: Failed");
    return {};
}

ErrorOr<void> Launcher::seal_allowlist()
{
    auto response_or_error = connection().try_seal_allowlist();
    if (response_or_error.is_error())
        return Error::from_string_literal("Launcher::seal_allowlist: Failed");
    return {};
}

bool Launcher::open(const URL::URL& url, ByteString const& handler_name)
{
    return connection().open_url(url, handler_name);
}

bool Launcher::open(const URL::URL& url, Details const& details)
{
    VERIFY(details.launcher_type != LauncherType::Application); // Launcher should not be used to execute arbitrary applications
    return open(url, details.executable);
}

Vector<ByteString> Launcher::get_handlers_for_url(const URL::URL& url)
{
    return connection().get_handlers_for_url(url.to_byte_string());
}

auto Launcher::get_handlers_with_details_for_url(const URL::URL& url) -> Vector<NonnullRefPtr<Details>>
{
    auto details = connection().get_handlers_with_details_for_url(url.to_byte_string());
    Vector<NonnullRefPtr<Details>> handlers_with_details;
    for (auto& value : details) {
        handlers_with_details.append(Details::from_details_str(value));
    }
    return handlers_with_details;
}

}
