/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Notification.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibMain/Main.h>
#include <serenity.h>
#include <spawn.h>
#include <stdio.h>

class NetworkWidget final : public GUI::ImageWidget {
    C_OBJECT_ABSTRACT(NetworkWidget)

public:
    static ErrorOr<NonnullRefPtr<NetworkWidget>> try_create(bool notifications)
    {
        NonnullRefPtr<Gfx::Bitmap> connected_icon = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/network.png"));
        NonnullRefPtr<Gfx::Bitmap> disconnected_icon = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/network-disconnected.png"));
        return adopt_nonnull_ref_or_enomem(new (nothrow) NetworkWidget(notifications, move(connected_icon), move(disconnected_icon)));
    }

private:
    NetworkWidget(bool notifications, NonnullRefPtr<Gfx::Bitmap> connected_icon, NonnullRefPtr<Gfx::Bitmap> disconnected_icon)
        : m_connected_icon(move(connected_icon))
        , m_disconnected_icon(move(disconnected_icon))
    {
        m_notifications = notifications;
        update_widget();
        start_timer(5000);
    }

    virtual void timer_event(Core::TimerEvent&) override
    {
        update_widget();
    }

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() != GUI::MouseButton::Primary)
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
        if (!file->open(Core::OpenMode::ReadOnly)) {
            dbgln("Error: Could not open {}: {}", file->name(), file->error_string());
            return adapter_info.to_string();
        }

        auto file_contents = file->read_all();
        auto json = JsonValue::from_string(file_contents);

        if (json.is_error())
            return adapter_info.to_string();

        int connected_adapters = 0;
        json.value().as_array().for_each([&adapter_info, include_loopback, &connected_adapters](auto& value) {
            auto& if_object = value.as_object();
            auto ip_address = if_object.get("ipv4_address").as_string_or("no IP");
            auto ifname = if_object.get("name").to_string();
            auto link_up = if_object.get("link_up").as_bool();
            auto link_speed = if_object.get("link_speed").to_i32();

            if (!include_loopback)
                if (ifname == "loop")
                    return;
            if (ip_address != "null")
                connected_adapters++;

            if (!adapter_info.is_empty())
                adapter_info.append('\n');

            adapter_info.appendff("{}: {} ", ifname, ip_address);
            if (!link_up)
                adapter_info.appendff("(down)");
            else
                adapter_info.appendff("({} Mb/s)", link_speed);
        });

        // show connected icon so long as at least one adapter is connected
        connected_adapters ? set_connected(true) : set_connected(false);

        return adapter_info.to_string();
    }

    String m_adapter_info;
    bool m_connected = false;
    bool m_notifications = true;
    NonnullRefPtr<Gfx::Bitmap> m_connected_icon;
    NonnullRefPtr<Gfx::Bitmap> m_disconnected_icon;
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix proc exec"));
    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/portal/notify", "rw"));
    TRY(Core::System::unveil("/proc/net/adapters", "r"));
    TRY(Core::System::unveil("/bin/SystemMonitor", "x"));
    TRY(Core::System::unveil(nullptr, nullptr));

    bool display_notifications = false;
    const char* name = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_option(display_notifications, "Display notifications", "display-notifications", 'd');
    args_parser.add_option(name, "Applet name used by WindowServer.ini to set the applet order", "name", 'n', "name");
    args_parser.parse(arguments);

    if (name == nullptr)
        name = "Network";

    auto window = TRY(GUI::Window::try_create());
    window->set_title(name);
    window->set_window_type(GUI::WindowType::Applet);
    window->set_has_alpha_channel(true);
    window->resize(16, 16);
    auto icon = TRY(window->try_set_main_widget<NetworkWidget>(display_notifications));
    icon->load_from_file("/res/icons/16x16/network.png");
    window->resize(16, 16);
    window->show();

    return app->exec();
}
