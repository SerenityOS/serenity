/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PageHost.h"
#include "ConnectionFromClient.h"
#include "PageClient.h"
#include <LibGfx/ShareableBitmap.h>
#include <LibGfx/SystemTheme.h>
#include <LibWeb/CSS/SystemColor.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/PaintingCommandExecutorCPU.h>
#include <LibWeb/Painting/ViewportPaintable.h>
#include <LibWeb/Platform/Timer.h>
#include <WebContent/WebContentClientEndpoint.h>
#include <WebContent/WebDriverConnection.h>

#ifdef HAS_ACCELERATED_GRAPHICS
#    include <LibWeb/Painting/PaintingCommandExecutorGPU.h>
#endif

namespace WebContent {

PageHost::PageHost(ConnectionFromClient& client)
    : m_client(client)
{
    auto& first_page = create_page();
    first_page.page().set_top_level_traversable(Web::HTML::TraversableNavigable::create_a_fresh_top_level_traversable(first_page.page(), AK::URL("about:blank")).release_value_but_fixme_should_propagate_errors());
}

PageClient& PageHost::create_page()
{
    m_pages.set(m_next_id, make<PageClient>(*this, m_next_id));
    ++m_next_id;
    return *m_pages.get(m_next_id - 1).value();
}

PageHost::~PageHost() = default;

}
