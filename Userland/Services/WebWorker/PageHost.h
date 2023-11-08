/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Rect.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/PixelUnits.h>
#include <WebWorker/Forward.h>

namespace WebWorker {

class PageHost final : public Web::PageClient {
    AK_MAKE_NONCOPYABLE(PageHost);
    AK_MAKE_NONMOVABLE(PageHost);

public:
    static NonnullOwnPtr<PageHost> create(ConnectionFromClient& client) { return adopt_own(*new PageHost(client)); }
    virtual ~PageHost();

    virtual Web::Page& page() override;
    virtual Web::Page const& page() const override;
    virtual bool is_connection_open() const override;
    virtual Gfx::Palette palette() const override;
    virtual Web::DevicePixelRect screen_rect() const override;
    virtual double device_pixels_per_css_pixel() const override;
    virtual Web::CSS::PreferredColorScheme preferred_color_scheme() const override;
    virtual void paint(Web::DevicePixelRect const&, Gfx::Bitmap&) override;
    virtual void request_file(Web::FileRequest) override;

private:
    explicit PageHost(ConnectionFromClient&);

    void setup_palette();

    ConnectionFromClient& m_client;
    NonnullOwnPtr<Web::Page> m_page;
    RefPtr<Gfx::PaletteImpl> m_palette_impl;
};

}
