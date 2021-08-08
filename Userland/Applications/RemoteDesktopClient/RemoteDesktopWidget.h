/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IPv4Address.h>
#include <AK/RefPtr.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibRemoteDesktop/RemoteDesktopRenderer.h>

namespace RemoteDesktopClient {

class RemoteDesktopWidget : public GUI::AbstractScrollableWidget
    , public RemoteDesktop::RendererCallbacks {
    C_OBJECT(RemoteDesktopWidget);

public:
    virtual ~RemoteDesktopWidget() override;

    bool connect(IPv4Address const&, u16);

    bool is_connected() const;
    u64 bytes_sent() const;
    u64 bytes_received() const;

    Function<void()> on_disconnect;

    virtual void did_scroll() override;
    virtual void invalidate_rects(Gfx::DisjointRectSet const&) override;
    virtual void set_surface_size(Gfx::IntSize const&) override;

    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;

private:
    RemoteDesktopWidget();

    virtual void paint_event(GUI::PaintEvent& event) override;

    RefPtr<RemoteDesktop::RemoteDesktopServerConnection> m_connection;
    OwnPtr<RemoteDesktop::Renderer> m_renderer;
};

}
