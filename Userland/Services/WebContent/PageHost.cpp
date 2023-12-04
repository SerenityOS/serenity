/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <WebContent/ConnectionFromClient.h>
#include <WebContent/PageClient.h>
#include <WebContent/PageHost.h>
#include <WebContent/WebDriverConnection.h>

namespace WebContent {

PageHost::PageHost(ConnectionFromClient& client)
    : m_client(client)
{
    auto& first_page = create_page();
    first_page.page().set_top_level_traversable(Web::HTML::TraversableNavigable::create_a_fresh_top_level_traversable(first_page.page(), AK::URL("about:blank")).release_value_but_fixme_should_propagate_errors());
}

PageClient& PageHost::create_page()
{
    m_pages.set(m_next_id, PageClient::create(Web::Bindings::main_thread_vm(), *this, m_next_id));
    ++m_next_id;
    return *m_pages.get(m_next_id - 1).value();
}

PageHost::~PageHost() = default;

}
