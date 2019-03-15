#include "IRCAppWindow.h"
#include "IRCClientWindow.h"
#include "IRCClientWindowListModel.h"
#include <LibGUI/GTableView.h>
#include <LibGUI/GBoxLayout.h>

IRCAppWindow::IRCAppWindow()
    : GWindow()
    , m_client("127.0.0.1", 6667)
{
    set_title(String::format("IRC Client: %s:%d", m_client.hostname().characters(), m_client.port()));
    set_rect(200, 200, 600, 400);
    setup_client();
    setup_widgets();
}

IRCAppWindow::~IRCAppWindow()
{
}

void IRCAppWindow::setup_client()
{
    m_client.on_connect = [this] {
        m_client.join_channel("#test");
    };

    m_client.on_join = [this] (const String& channel_name) {
        ensure_window(IRCClientWindow::Channel, channel_name);
    };

    m_client.on_query_message = [this] (const String& name) {
        // FIXME: Update query view.
    };

    m_client.on_channel_message = [this] (const String& channel_name) {
        // FIXME: Update channel view.
    };

    m_client.connect();
}

void IRCAppWindow::setup_widgets()
{
    auto* widget = new GWidget(nullptr);
    widget->set_fill_with_background_color(true);
    set_main_widget(widget);
    widget->set_layout(make<GBoxLayout>(Orientation::Horizontal));

    auto* subwindow_list = new GTableView(widget);
    subwindow_list->set_model(OwnPtr<IRCClientWindowListModel>(m_client.client_window_list_model()));
    subwindow_list->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    subwindow_list->set_preferred_size({ 120, 0 });

    m_subwindow_container = new GWidget(widget);
    m_subwindow_container->set_layout(make<GBoxLayout>(Orientation::Vertical));
    m_subwindow_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fill);

    create_subwindow(IRCClientWindow::Server, "Server");
}

IRCClientWindow& IRCAppWindow::create_subwindow(IRCClientWindow::Type type, const String& name)
{
    return *new IRCClientWindow(m_client, type, name, m_subwindow_container);
}

IRCClientWindow& IRCAppWindow::ensure_window(IRCClientWindow::Type type, const String& name)
{
    for (int i = 0; i < m_client.window_count(); ++i) {
        auto& window = m_client.window_at(i);
        if (window.name() == name)
            return window;
    }
    return create_subwindow(type, name);
}
