/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RemoteDesktopWidget.h"
#include <Applications/RemoteDesktopClient/RemoteDesktopClientGML.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/Timer.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Statusbar.h>
#include <stdio.h>
#include <unistd.h>

namespace RemoteDesktopClient {

class MainWidget : public GUI::Widget {
    C_OBJECT(MainWidget);

public:
    MainWidget()
        : m_status_timer(Core::Timer::create_repeating(
            1000, [this]() {
                status_timer_update();
            },
            this))
    {
        load_from_gml(remote_desktop_client_gml);

        m_remote_desktop_widget = find_descendant_of_type_named<RemoteDesktopWidget>("remote_desktop");
        VERIFY(m_remote_desktop_widget);

        m_status_bar = find_descendant_of_type_named<GUI::Statusbar>("status_bar");
        VERIFY(m_status_bar);

        auto icon_no_audio = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/audio-volume-zero.png").release_value();
        auto icon_audio = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/audio-volume-high.png").release_value();
        m_status_bar->set_icon(1, icon_no_audio);
        m_status_bar->set_fixed_width(1, icon_audio->width() + 2);

        m_time_elapsed.start();

        m_remote_desktop_widget->on_disconnect = [this]() {
            m_status_timer->stop();
            status_timer_update();
        };
    }

    bool connect(IPv4Address const& ipv4_address, u16 port)
    {
        m_status_bar->set_text("Connecting...");
        if (!m_remote_desktop_widget->connect(ipv4_address, port)) {
            warnln("Failed to connect");
            return false;
        }

        m_status_timer->start();
        return true;
    }

private:
    void status_timer_update()
    {
        if (m_remote_desktop_widget->is_connected()) {
            auto time_elapsed_ms = m_time_elapsed.elapsed();
            auto bytes_sent = m_remote_desktop_widget->bytes_sent();
            auto bytes_received = m_remote_desktop_widget->bytes_received();

            m_time_elapsed.start();

            auto delta_bytes_sent_per_second = time_elapsed_ms != 0 ? ((bytes_sent - m_last_bytes_sent) * 1000) / (u64)time_elapsed_ms : 0;
            auto delta_bytes_received_per_second = time_elapsed_ms != 0 ? ((bytes_received - m_last_bytes_received) * 1000) / (u64)time_elapsed_ms : 0;

            auto format_bytes_per_second = [](u64 bytes_per_second) {
                if (bytes_per_second >= 1024 * 1024)
                    return String::formatted("{} MiB/s", bytes_per_second / 1024 / 1024);
                else if (bytes_per_second >= 1024)
                    return String::formatted("{} KiB/s", bytes_per_second / 1024);
                return String::formatted("{} B/s", bytes_per_second);
            };

            m_status_bar->set_text(String::formatted("Tx: {} Rx: {}", format_bytes_per_second(delta_bytes_sent_per_second), format_bytes_per_second(delta_bytes_received_per_second)));
            m_last_bytes_sent = bytes_sent;
            m_last_bytes_received = bytes_received;
        } else {
            m_last_bytes_sent = m_last_bytes_received = 0;
            m_status_bar->set_text("Disconnected");
        }
    }

    RemoteDesktopWidget* m_remote_desktop_widget { nullptr };
    GUI::Statusbar* m_status_bar { nullptr };
    NonnullRefPtr<Core::Timer> m_status_timer;
    Core::ElapsedTimer m_time_elapsed;
    u64 m_last_bytes_sent { 0 };
    u64 m_last_bytes_received { 0 };
};

}

int main(int argc, char** argv)
{
    static constexpr u16 default_port = 3388;
    String connect_address;
    int port = default_port;
    String username;
    String password;

    Core::ArgsParser args_parser;
    args_parser.add_option(connect_address, "IP address to connect to", "connect", 'c', "connect");
    args_parser.add_option(port, "Port to connect on", "port", 'p', "port");
    args_parser.parse(argc, argv);

    auto ipv4_address = IPv4Address::from_string(connect_address);
    if (!ipv4_address.has_value()) {
        warnln("Invalid connect address: {}", connect_address);
        return 1;
    }

    if ((u16)port != port || (u16)port == 0) {
        warnln("Invalid port number: {}", port);
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd rpath inet", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto app_icon = GUI::Icon::default_icon("ladyball");
    auto window = GUI::Window::construct();
    window->set_title(String::formatted("RemoteDesktop - {}:{}", *ipv4_address, port));
    window->set_icon(app_icon.bitmap_for_size(32));
    auto main_widget = RemoteDesktopClient::MainWidget::construct();
    window->set_main_widget(main_widget);
    window->show();
    if (!main_widget->connect(ipv4_address.value(), port))
        return 1;
    return app->exec();
}
