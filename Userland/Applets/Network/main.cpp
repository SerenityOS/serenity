/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Notification.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <serenity.h>
#include <spawn.h>
#include <stdio.h>

class NetworkWidget final : public GUI::ImageWidget {
    C_OBJECT(NetworkWidget);

public:
    NetworkWidget(bool notifications)
    {
        m_notifications = notifications;
        update_widget();
        start_timer(5000);
    }

private:
    virtual void timer_event(Core::TimerEvent&) override
    {
        update_widget();
    }

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() != GUI::MouseButton::Left)
            return;

        pid_t child_pid;
        const char* argv[] = { "SystemMonitor", "-t", "network", nullptr };

        if ((errno = posix_spawn(&child_pid, "/bin/SystemMonitor", nullptr, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
            return;
        }

        if (disown(child_pid) < 0)
            perror("disown");
    }

    virtual void update_widget()
    {
        auto adapter_info = get_adapter_info();

        if (adapter_info == "") {
            set_connected(false);
            m_adapter_info = "No network adapters";
        } else {
            m_adapter_info = adapter_info;
        }

        set_tooltip(m_adapter_info);

        if (m_connected)
            NetworkWidget::set_bitmap(m_connected_icon);
        else
            NetworkWidget::set_bitmap(m_disconnected_icon);

        update();
    }

    virtual void notify_on_connect()
    {
        if (!m_notifications)
            return;
        auto notification = GUI::Notification::construct();
        notification->set_title("Network");
        notification->set_icon(m_connected_icon);
        notification->set_text("Network connected");
        notification->show();
    }

    virtual void notify_on_disconnect()
    {
        if (!m_notifications)
            return;
        auto notification = GUI::Notification::construct();
        notification->set_title("Network");
        notification->set_icon(m_disconnected_icon);
        notification->set_text("Network disconnected");
        notification->show();
    }

    virtual void set_connected(bool connected)
    {
        if (m_connected != connected) {
            connected ? notify_on_connect() : notify_on_disconnect();
        }

        m_connected = connected;
    }

    virtual String get_adapter_info(bool include_loopback = false)
    {
        StringBuilder adapter_info;

        auto file = Core::File::construct("/proc/net/adapters");
        if (!file->open(Core::IODevice::ReadOnly)) {
            fprintf(stderr, "Error: %s\n", file->error_string());
            return adapter_info.to_string();
        }

        auto file_contents = file->read_all();
        auto json = JsonValue::from_string(file_contents);

        if (!json.has_value())
            return adapter_info.to_string();

        int connected_adapters = 0;
        json.value().as_array().for_each([&adapter_info, include_loopback, &connected_adapters](auto& value) {
            auto if_object = value.as_object();
            auto ip_address = if_object.get("ipv4_address").to_string();
            auto ifname = if_object.get("name").to_string();

            if (!include_loopback)
                if (ifname == "loop0")
                    return;
            if (ip_address != "null")
                connected_adapters++;

            adapter_info.appendf("%s: %s\n", ifname.characters(), ip_address.characters());
        });

        // show connected icon so long as at least one adapter is connected
        connected_adapters ? set_connected(true) : set_connected(false);

        return adapter_info.to_string();
    }

    String m_adapter_info;
    bool m_connected = false;
    bool m_notifications = true;
    RefPtr<Gfx::Bitmap> m_connected_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/network.png");
    RefPtr<Gfx::Bitmap> m_disconnected_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/network-disconnected.png");
};

int main(int argc, char* argv[])
{
    if (pledge("stdio recvfd sendfd accept rpath unix cpath fattr unix proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd accept rpath unix proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/notify", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/proc/net/adapters", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/SystemMonitor", "x") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    bool display_notifications = false;
    const char* name = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_option(display_notifications, "Display notifications", "display-notifications", 'd');
    args_parser.add_option(name, "Applet name used by WindowServer.ini to set the applet order", "name", 'n', "name");
    args_parser.parse(argc, argv);

    if (name == nullptr)
        name = "Network";

    auto window = GUI::Window::construct();
    window->set_title(name);
    window->set_window_type(GUI::WindowType::Applet);
    window->set_has_alpha_channel(true);
    window->resize(16, 16);
    auto& icon = window->set_main_widget<NetworkWidget>(display_notifications);
    icon.load_from_file("/res/icons/16x16/network.png");
    window->resize(16, 16);
    window->show();

    return app->exec();
}
