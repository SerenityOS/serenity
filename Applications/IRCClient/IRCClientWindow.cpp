#include "IRCClientWindow.h"
#include "IRCClient.h"
#include "IRCChannel.h"
#include "IRCChannelMemberListModel.h"
#include "IRCLogBufferModel.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GTextBox.h>

IRCClientWindow::IRCClientWindow(IRCClient& client, Type type, const String& name, GWidget* parent)
    : GWidget(parent)
    , m_client(client)
    , m_type(type)
    , m_name(name)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));

    // Make a container for the log buffer view + optional member list.
    GWidget* container = new GWidget(this);
    container->set_layout(make<GBoxLayout>(Orientation::Horizontal));

    m_table_view = new GTableView(container);
    m_table_view->set_headers_visible(false);
    m_table_view->set_font(Font::default_fixed_width_font());

    if (m_type == Channel) {
        auto* member_view = new GTableView(container);
        member_view->set_headers_visible(false);
        member_view->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
        member_view->set_preferred_size({ 100, 0 });
        member_view->set_model(OwnPtr<IRCChannelMemberListModel>(m_client.ensure_channel(m_name).member_model()));
    }

    m_text_editor = new GTextEditor(GTextEditor::SingleLine, this);
    m_text_editor->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_text_editor->set_preferred_size({ 0, 18 });
    m_text_editor->on_return_pressed = [this] (GTextEditor& editor) {
        if (m_type == Channel)
            m_client.handle_user_input_in_channel(m_name, editor.text());
        else if (m_type == Query)
            m_client.handle_user_input_in_query(m_name, editor.text());
        else if (m_type == Server)
            m_client.handle_user_input_in_server(editor.text());
        m_text_editor->clear();
    };

    m_client.register_subwindow(*this);
}

IRCClientWindow::~IRCClientWindow()
{
    m_client.unregister_subwindow(*this);
}

void IRCClientWindow::set_log_buffer(const IRCLogBuffer& log_buffer)
{
    m_log_buffer = &log_buffer;
    m_table_view->set_model(OwnPtr<IRCLogBufferModel>((IRCLogBufferModel*)log_buffer.model()));
}
