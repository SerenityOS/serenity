/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <WebWorker/ConnectionFromClient.h>
#include <WebWorker/PageHost.h>

namespace WebWorker {

PageHost::~PageHost() = default;

Web::Page& PageHost::page()
{
    return *m_page;
}

Web::Page const& PageHost::page() const
{
    return *m_page;
}

Gfx::Palette PageHost::palette() const
{
    return Gfx::Palette(*m_palette_impl);
}

void PageHost::setup_palette()
{
    // FIXME: We don't actually need a palette :thonk:
    auto buffer_or_error = Core::AnonymousBuffer::create_with_size(sizeof(Gfx::SystemTheme));
    VERIFY(!buffer_or_error.is_error());
    auto buffer = buffer_or_error.release_value();
    auto* theme = buffer.data<Gfx::SystemTheme>();
    theme->color[to_underlying(Gfx::ColorRole::Window)] = Color::Magenta;
    theme->color[to_underlying(Gfx::ColorRole::WindowText)] = Color::Cyan;
    m_palette_impl = Gfx::PaletteImpl::create_with_anonymous_buffer(buffer);
}

bool PageHost::is_connection_open() const
{
    return m_client.is_open();
}

Web::DevicePixelRect PageHost::screen_rect() const
{
    return {};
}

double PageHost::device_pixels_per_css_pixel() const
{
    return 1.0;
}

Web::CSS::PreferredColorScheme PageHost::preferred_color_scheme() const
{
    return Web::CSS::PreferredColorScheme::Auto;
}

void PageHost::paint(Web::DevicePixelRect const&, Gfx::Bitmap&)
{
}

void PageHost::request_file(Web::FileRequest request)
{
    m_client.request_file(move(request));
}

PageHost::PageHost(ConnectionFromClient& client)
    : m_client(client)
    , m_page(make<Web::Page>(*this))
{
    setup_palette();
}

}
