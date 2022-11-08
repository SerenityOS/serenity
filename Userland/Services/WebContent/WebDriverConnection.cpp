/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/Vector.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Platform/Timer.h>
#include <WebContent/PageHost.h>
#include <WebContent/WebDriverConnection.h>

namespace WebContent {

ErrorOr<NonnullRefPtr<WebDriverConnection>> WebDriverConnection::connect(PageHost& page_host, String const& webdriver_ipc_path)
{
    dbgln_if(WEBDRIVER_DEBUG, "Trying to connect to {}", webdriver_ipc_path);
    auto socket = TRY(Core::Stream::LocalSocket::connect(webdriver_ipc_path));

    dbgln_if(WEBDRIVER_DEBUG, "Connected to WebDriver");
    return adopt_nonnull_ref_or_enomem(new (nothrow) WebDriverConnection(move(socket), page_host));
}

WebDriverConnection::WebDriverConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, PageHost& page_host)
    : IPC::ConnectionToServer<WebDriverClientEndpoint, WebDriverServerEndpoint>(*this, move(socket))
    , m_page_host(page_host)
{
}

void WebDriverConnection::set_is_webdriver_active(bool is_webdriver_active)
{
    m_page_host.set_is_webdriver_active(is_webdriver_active);
}

}
