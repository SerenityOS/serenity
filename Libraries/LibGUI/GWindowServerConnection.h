#pragma once

#include <LibIPC/IServerConnection.h>
#include <WindowServer/WindowClientEndpoint.h>
#include <WindowServer/WindowServerEndpoint.h>

class GWindowServerConnection
    : public IServerConnection<WindowClientEndpoint, WindowServerEndpoint>
    , public WindowClientEndpoint {
    C_OBJECT(GWindowServerConnection)
public:
    GWindowServerConnection()
        : IServerConnection(*this, "/tmp/portal/window")
    {
        handshake();
    }

    virtual void handshake() override;
    static GWindowServerConnection& the();

private:
    virtual void handle(const WindowClient::Paint&) override;
    virtual void handle(const WindowClient::MouseMove&) override;
    virtual void handle(const WindowClient::MouseDown&) override;
    virtual void handle(const WindowClient::MouseDoubleClick&) override;
    virtual void handle(const WindowClient::MouseUp&) override;
    virtual void handle(const WindowClient::MouseWheel&) override;
    virtual void handle(const WindowClient::WindowEntered&) override;
    virtual void handle(const WindowClient::WindowLeft&) override;
    virtual void handle(const WindowClient::KeyDown&) override;
    virtual void handle(const WindowClient::KeyUp&) override;
    virtual void handle(const WindowClient::WindowActivated&) override;
    virtual void handle(const WindowClient::WindowDeactivated&) override;
    virtual void handle(const WindowClient::WindowCloseRequest&) override;
    virtual void handle(const WindowClient::WindowResized&) override;
    virtual void handle(const WindowClient::MenuItemActivated&) override;
    virtual void handle(const WindowClient::ScreenRectChanged&) override;
    virtual void handle(const WindowClient::ClipboardContentsChanged&) override;
    virtual void handle(const WindowClient::WM_WindowRemoved&) override;
    virtual void handle(const WindowClient::WM_WindowStateChanged&) override;
    virtual void handle(const WindowClient::WM_WindowIconBitmapChanged&) override;
    virtual void handle(const WindowClient::WM_WindowRectChanged&) override;
    virtual void handle(const WindowClient::AsyncSetWallpaperFinished&) override;
    virtual void handle(const WindowClient::DragDropped&) override;
    virtual void handle(const WindowClient::DragAccepted&) override;
    virtual void handle(const WindowClient::DragCancelled&) override;
    virtual void handle(const WindowClient::UpdateSystemTheme&) override;
};
