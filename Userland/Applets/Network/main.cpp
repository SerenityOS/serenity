/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
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
#include <LibGUI/Process.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibMain/Main.h>

class NetworkWidget final : public GUI::ImageWidget {
    C_OBJECT_ABSTRACT(NetworkWidget)

public:
    static ErrorOr<NonnullRefPtr<NetworkWidget>> try_create(bool notifications)
    {
        NonnullRefPtr<Gfx::Bitmap> connected_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/network.png"sv));
        NonnullRefPtr<Gfx::Bitmap> disconnected_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/network-disconnected.png"sv));
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
        GUI::Process::spawn_or_show_error(window(), "/bin/SystemMonitor"sv, Array { "-t", "network" });
    }

    void update_widget()
    {
        auto adapter_info_or_error = get_adapter_info();
        if (adapter_info_or_error.is_error()) {
            dbgln("Couldn't update adapter info: {}", adapter_info_or_error.error());
            set_connected(false);
            m_adapter_info = "No network adapters"_string;
        } else {
            m_adapter_info = adapter_info_or_error.release_value();
        }

        set_tooltip(m_adapter_info);

        if (m_connected)
            NetworkWidget::set_bitmap(m_connected_icon);
        else
            NetworkWidget::set_bitmap(m_disconnected_icon);

        update();
    }

    void notify_on_connect()
    {
        if (!m_notifications)
            return;
        auto notification = GUI::Notification::construct();
        notification->set_title("Network"_string);
        notification->set_icon(m_connected_icon);
        notification->set_text("Network connected"_string);
        notification->show();
    }

    void notify_on_disconnect()
    {
        if (!m_notifications)
            return;
        auto notification = GUI::Notification::construct();
        notification->set_title("Network"_string);
        notification->set_icon(m_disconnected_icon);
        notification->set_text("Network disconnected"_string);
        notification->show();
    }

    void set_connected(bool connected)
    {
        if (m_connected != connected) {
            connected ? notify_on_connect() : notify_on_disconnect();
        }

        m_connected = connected;
    }

    ErrorOr<String> get_adapter_info()
    {
        auto file = TRY(Core::File::open("/sys/kernel/net/adapters"sv, Core::File::OpenMode::Read));
        auto file_contents = TRY(file->read_until_eof());
        auto json = TRY(JsonValue::from_string(file_contents));

        StringBuilder adapter_info;
        int connected_adapters = 0;
        json.as_array().for_each([&adapter_info, &connected_adapters](auto& value) {
            auto& if_object = value.as_object();
            auto ip_address = if_object.get_byte_string("ipv4_address"sv).value_or("no IP");
            auto ifname = if_object.get_byte_string("name"sv).value();
            auto link_up = if_object.get_bool("link_up"sv).value();
            auto link_speed = if_object.get_i32("link_speed"sv).value();

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
    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::unveil("/tmp/session/%sid/portal/notify", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/sys/kernel/net/adapters", "r"));
    TRY(Core::System::unveil("/bin/SystemMonitor", "x"));
    TRY(Core::System::unveil(nullptr, nullptr));

    bool display_notifications = false;
    StringView name;
    Core::ArgsParser args_parser;
    args_parser.add_option(display_notifications, "Display notifications", "display-notifications", 'd');
    args_parser.add_option(name, "Applet name used by WindowServer.ini to set the applet order", "name", 'n', "name");
    args_parser.parse(arguments);

    if (name.is_empty())
        name = "Network"sv;

    auto window = GUI::Window::construct();
    window->set_title(name);
    window->set_window_type(GUI::WindowType::Applet);
    window->set_has_alpha_channel(true);
    window->resize(16, 16);
    auto icon = TRY(NetworkWidget::try_create(display_notifications));
    window->set_main_widget(icon);
    icon->load_from_file("/res/icons/16x16/network.png"sv);
    window->resize(16, 16);
    window->show();

    return app->exec();
}
