#include "IRCClientWindow.h"
#include "IRCClient.h"
#include "IRCLogBufferModel.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GTextBox.h>

IRCClientWindow::IRCClientWindow(IRCClient& client, Type type, const String& name, GWidget* parent)
    : GWidget(parent)
    , m_client(client)
    , m_type(type)
    , m_name(name)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    m_table_view = new GTableView(this);
    m_table_view->set_font(Font::default_fixed_width_font());

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
