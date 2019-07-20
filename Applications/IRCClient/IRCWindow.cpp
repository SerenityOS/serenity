#include "IRCWindow.h"
#include "IRCChannel.h"
#include "IRCChannelMemberListModel.h"
#include "IRCClient.h"
#include "IRCLogBufferModel.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GTextEditor.h>

IRCWindow::IRCWindow(IRCClient& client, void* owner, Type type, const String& name, GWidget* parent)
    : GWidget(parent)
    , m_client(client)
    , m_owner(owner)
    , m_type(type)
    , m_name(name)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));

    // Make a container for the log buffer view + (optional) member list.
    auto* container = new GSplitter(Orientation::Horizontal, this);

    m_table_view = new GTableView(container);
    m_table_view->set_headers_visible(false);
    m_table_view->set_font(Font::default_fixed_width_font());
    m_table_view->set_alternating_row_colors(false);

    if (m_type == Server) {
        m_table_view->set_column_hidden(IRCLogBufferModel::Column::Name, true);
    }

    if (m_type == Channel) {
        auto* member_view = new GTableView(container);
        member_view->set_headers_visible(false);
        member_view->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
        member_view->set_preferred_size(100, 0);
        member_view->set_alternating_row_colors(false);
        member_view->set_model(channel().member_model());
        member_view->set_activates_on_selection(true);
    }

    m_text_editor = new GTextEditor(GTextEditor::SingleLine, this);
    m_text_editor->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_text_editor->set_preferred_size(0, 19);
    m_text_editor->on_return_pressed = [this] {
        if (m_type == Channel)
            m_client.handle_user_input_in_channel(m_name, m_text_editor->text());
        else if (m_type == Query)
            m_client.handle_user_input_in_query(m_name, m_text_editor->text());
        else if (m_type == Server)
            m_client.handle_user_input_in_server(m_text_editor->text());
        m_text_editor->clear();
    };

    m_client.register_subwindow(*this);
}

IRCWindow::~IRCWindow()
{
    m_client.unregister_subwindow(*this);
}

void IRCWindow::set_log_buffer(const IRCLogBuffer& log_buffer)
{
    m_log_buffer = &log_buffer;
    m_table_view->set_model(log_buffer.model());
}

bool IRCWindow::is_active() const
{
    return m_client.current_window() == this;
}

void IRCWindow::did_add_message()
{
    if (!is_active()) {
        ++m_unread_count;
        m_client.aid_update_window_list();
        return;
    }
    m_table_view->scroll_to_bottom();
}

void IRCWindow::clear_unread_count()
{
    if (!m_unread_count)
        return;
    m_unread_count = 0;
    m_client.aid_update_window_list();
}

int IRCWindow::unread_count() const
{
    return m_unread_count;
}
