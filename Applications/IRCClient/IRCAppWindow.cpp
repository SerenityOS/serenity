#include "IRCAppWindow.h"
#include "IRCChannel.h"
#include "IRCWindow.h"
#include "IRCWindowListModel.h"
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GInputBox.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GStackWidget.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GToolBar.h>
#include <stdio.h>
#include <stdlib.h>

static IRCAppWindow* s_the;

IRCAppWindow& IRCAppWindow::the()
{
    return *s_the;
}

IRCAppWindow::IRCAppWindow()
{
    ASSERT(!s_the);
    s_the = this;

    set_icon(load_png("/res/icons/16x16/app-irc-client.png"));

    update_title();
    set_rect(200, 200, 600, 400);
    setup_actions();
    setup_menus();
    setup_widgets();

    setup_client();
}

IRCAppWindow::~IRCAppWindow()
{
}

void IRCAppWindow::update_title()
{
    set_title(String::format("IRC Client: %s@%s:%d", m_client.nickname().characters(), m_client.hostname().characters(), m_client.port()));
}

void IRCAppWindow::setup_client()
{
    m_client.aid_create_window = [this](void* owner, IRCWindow::Type type, const String& name) {
        return &create_window(owner, type, name);
    };
    m_client.aid_get_active_window = [this] {
        return static_cast<IRCWindow*>(m_container->active_widget());
    };
    m_client.aid_update_window_list = [this] {
        m_window_list->model()->update();
    };
    m_client.on_nickname_changed = [this](const String&) {
        update_title();
    };
    m_client.on_part_from_channel = [this](auto&) {
        update_part_action();
    };

    if (m_client.hostname().is_empty()) {
        auto input_box = GInputBox::construct("Enter server:", "Connect to server", this);
        auto result = input_box->exec();
        if (result == GInputBox::ExecCancel)
            ::exit(0);

        m_client.set_server(input_box->text_value(), 6667);
    }
    update_title();
    bool success = m_client.connect();
    ASSERT(success);
}

void IRCAppWindow::setup_actions()
{
    m_join_action = GAction::create("Join channel", GraphicsBitmap::load_from_file("/res/icons/16x16/irc-join.png"), [&](auto&) {
        auto input_box = GInputBox::construct("Enter channel name:", "Join channel", this);
        if (input_box->exec() == GInputBox::ExecOK && !input_box->text_value().is_empty())
            m_client.handle_join_action(input_box->text_value());
    });

    m_part_action = GAction::create("Part from channel", GraphicsBitmap::load_from_file("/res/icons/16x16/irc-part.png"), [this](auto&) {
        auto* window = m_client.current_window();
        if (!window || window->type() != IRCWindow::Type::Channel) {
            // FIXME: Perhaps this action should have been disabled instead of allowing us to activate it.
            return;
        }
        m_client.handle_part_action(window->channel().name());
    });

    m_whois_action = GAction::create("Whois user", GraphicsBitmap::load_from_file("/res/icons/16x16/irc-whois.png"), [&](auto&) {
        auto input_box = GInputBox::construct("Enter nickname:", "IRC WHOIS lookup", this);
        if (input_box->exec() == GInputBox::ExecOK && !input_box->text_value().is_empty())
            m_client.handle_whois_action(input_box->text_value());
    });

    m_open_query_action = GAction::create("Open query", GraphicsBitmap::load_from_file("/res/icons/16x16/irc-open-query.png"), [&](auto&) {
        auto input_box = GInputBox::construct("Enter nickname:", "Open IRC query with...", this);
        if (input_box->exec() == GInputBox::ExecOK && !input_box->text_value().is_empty())
            m_client.handle_open_query_action(input_box->text_value());
    });

    m_close_query_action = GAction::create("Close query", GraphicsBitmap::load_from_file("/res/icons/16x16/irc-close-query.png"), [](auto&) {
        printf("FIXME: Implement close-query action\n");
    });

    m_change_nick_action = GAction::create("Change nickname", GraphicsBitmap::load_from_file("/res/icons/16x16/irc-nick.png"), [this](auto&) {
        auto input_box = GInputBox::construct("Enter nickname:", "Change nickname", this);
        if (input_box->exec() == GInputBox::ExecOK && !input_box->text_value().is_empty())
            m_client.handle_change_nick_action(input_box->text_value());
    });
}

void IRCAppWindow::setup_menus()
{
    auto menubar = make<GMenuBar>();
    auto app_menu = GMenu::construct("IRC Client");
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        dbgprintf("Terminal: Quit menu activated!\n");
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto server_menu = GMenu::construct("Server");
    server_menu->add_action(*m_change_nick_action);
    server_menu->add_separator();
    server_menu->add_action(*m_join_action);
    server_menu->add_action(*m_part_action);
    server_menu->add_separator();
    server_menu->add_action(*m_whois_action);
    server_menu->add_action(*m_open_query_action);
    server_menu->add_action(*m_close_query_action);
    menubar->add_menu(move(server_menu));

    auto help_menu = GMenu::construct("Help");
    help_menu->add_action(GAction::create("About", [this](const GAction&) {
        GAboutDialog::show("IRC Client", load_png("/res/icons/32x32/app-irc-client.png"), this);
    }));
    menubar->add_menu(move(help_menu));

    GApplication::the().set_menubar(move(menubar));
}

void IRCAppWindow::setup_widgets()
{
    auto widget = GWidget::construct();
    set_main_widget(widget);
    widget->set_fill_with_background_color(true);
    widget->set_background_color(SystemColor::Window);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_spacing(0);

    auto toolbar = GToolBar::construct(widget);
    toolbar->set_has_frame(false);
    toolbar->add_action(*m_change_nick_action);
    toolbar->add_separator();
    toolbar->add_action(*m_join_action);
    toolbar->add_action(*m_part_action);
    toolbar->add_separator();
    toolbar->add_action(*m_whois_action);
    toolbar->add_action(*m_open_query_action);
    toolbar->add_action(*m_close_query_action);

    auto outer_container = GWidget::construct(widget.ptr());
    outer_container->set_layout(make<GBoxLayout>(Orientation::Vertical));
    outer_container->layout()->set_margins({ 2, 0, 2, 2 });

    auto horizontal_container = GSplitter::construct(Orientation::Horizontal, outer_container);

    m_window_list = GTableView::construct(horizontal_container);
    m_window_list->set_headers_visible(false);
    m_window_list->set_alternating_row_colors(false);
    m_window_list->set_size_columns_to_fit_content(true);
    m_window_list->set_model(m_client.client_window_list_model());
    m_window_list->set_activates_on_selection(true);
    m_window_list->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    m_window_list->set_preferred_size(100, 0);
    m_window_list->on_activation = [this](auto& index) {
        set_active_window(m_client.window_at(index.row()));
    };

    m_container = GStackWidget::construct(horizontal_container);
    m_container->on_active_widget_change = [this](auto*) {
        update_part_action();
    };

    create_window(&m_client, IRCWindow::Server, "Server");
}

void IRCAppWindow::set_active_window(IRCWindow& window)
{
    m_container->set_active_widget(&window);
    window.clear_unread_count();
    auto index = m_window_list->model()->index(m_client.window_index(window));
    m_window_list->selection().set(index);
}

void IRCAppWindow::update_part_action()
{
    auto* window = static_cast<IRCWindow*>(m_container->active_widget());
    bool is_open_channel = window && window->type() == IRCWindow::Type::Channel && window->channel().is_open();
    m_part_action->set_enabled(is_open_channel);
}

IRCWindow& IRCAppWindow::create_window(void* owner, IRCWindow::Type type, const String& name)
{
    return *new IRCWindow(m_client, owner, type, name, m_container);
}
