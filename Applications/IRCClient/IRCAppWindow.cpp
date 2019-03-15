#include "IRCAppWindow.h"
#include "IRCClientWindow.h"
#include "IRCClientWindowListModel.h"
#include <LibGUI/GStackWidget.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GAction.h>
#include <stdio.h>

IRCAppWindow::IRCAppWindow()
    : GWindow()
    , m_client("127.0.0.1", 6667)
{
    set_title(String::format("IRC Client: %s@%s:%d", m_client.nickname().characters(), m_client.hostname().characters(), m_client.port()));
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

    m_client.on_channel_message = [this] (const String& channel_name) {
        ensure_window(IRCClientWindow::Channel, channel_name);
    };

    m_client.on_join = [this] (const String& channel_name) {
        ensure_window(IRCClientWindow::Channel, channel_name);
    };

    m_client.on_query_message = [this] (const String& name) {
        ensure_window(IRCClientWindow::Query, name);
    };

    m_client.connect();
}

void IRCAppWindow::setup_widgets()
{
    auto* widget = new GWidget(nullptr);
    set_main_widget(widget);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    printf("main_widget: %s{%p}\n", widget->class_name(), widget);

    auto join_action = GAction::create("Join channel", GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/16x16/irc-join.rgb", { 16, 16 }), [] (auto&) {
        printf("FIXME: Implement join action\n");
    });

    auto part_action = GAction::create("Part from channel", GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/16x16/irc-part.rgb", { 16, 16 }), [] (auto&) {
        printf("FIXME: Implement part action\n");
    });

    auto* toolbar = new GToolBar(widget);
    toolbar->add_action(join_action.copy_ref());
    toolbar->add_action(part_action.copy_ref());

    auto* horizontal_container = new GWidget(widget);
    printf("horizontal_widget: %s{%p}\n", horizontal_container->class_name(), horizontal_container);
    horizontal_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));

    auto* window_list = new GTableView(horizontal_container);
    window_list->set_headers_visible(false);
    window_list->set_alternating_row_colors(false);
    window_list->set_model(OwnPtr<IRCClientWindowListModel>(m_client.client_window_list_model()));
    window_list->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    window_list->set_preferred_size({ 120, 0 });
    m_client.client_window_list_model()->on_activation = [this] (IRCClientWindow& window) {
        m_container->set_active_widget(&window);
    };

    m_container = new GStackWidget(horizontal_container);
    printf("m_container: %s{%p}\n", ((GWidget*)m_container)->class_name(), m_container);

    create_subwindow(IRCClientWindow::Server, "Server");
}

IRCClientWindow& IRCAppWindow::create_subwindow(IRCClientWindow::Type type, const String& name)
{
    return *new IRCClientWindow(m_client, type, name, m_container);
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
