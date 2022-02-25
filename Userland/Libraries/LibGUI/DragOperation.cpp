/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <LibCore/EventLoop.h>
#include <LibCore/MimeData.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/DragOperation.h>
#include <LibGfx/Bitmap.h>

namespace GUI {

static DragOperation* s_current_drag_operation;

DragOperation::DragOperation(Core::Object* parent)
    : Core::Object(parent)
{
}

DragOperation::~DragOperation()
{
}

DragOperation::Outcome DragOperation::exec()
{
    VERIFY(!s_current_drag_operation);
    VERIFY(!m_event_loop);
    VERIFY(m_mime_data);

    Gfx::ShareableBitmap drag_bitmap;
    if (m_mime_data->has_format("image/x-raw-bitmap")) {
        auto data = m_mime_data->data("image/x-raw-bitmap");
        auto bitmap = Gfx::Bitmap::try_create_from_serialized_byte_buffer(move(data)).release_value_but_fixme_should_propagate_errors();
        drag_bitmap = bitmap->to_shareable_bitmap();
    }

    auto started = ConnectionToWindowServer::the().start_drag(
        m_mime_data->text(),
        m_mime_data->all_data(),
        drag_bitmap);

    if (!started) {
        m_outcome = Outcome::Cancelled;
        return m_outcome;
    }

    s_current_drag_operation = this;
    m_event_loop = make<Core::EventLoop>();
    auto result = m_event_loop->exec();
    m_event_loop = nullptr;
    dbgln("{}: event loop returned with result {}", class_name(), result);
    remove_from_parent();
    s_current_drag_operation = nullptr;
    return m_outcome;
}

void DragOperation::done(Outcome outcome)
{
    VERIFY(m_outcome == Outcome::None);
    m_outcome = outcome;
    m_event_loop->quit(0);
}

void DragOperation::notify_accepted(Badge<ConnectionToWindowServer>)
{
    VERIFY(s_current_drag_operation);
    s_current_drag_operation->done(Outcome::Accepted);
}

void DragOperation::notify_cancelled(Badge<ConnectionToWindowServer>)
{
    if (s_current_drag_operation)
        s_current_drag_operation->done(Outcome::Cancelled);
}

void DragOperation::set_text(const String& text)
{
    if (!m_mime_data)
        m_mime_data = Core::MimeData::construct();
    m_mime_data->set_text(text);
}
void DragOperation::set_bitmap(const Gfx::Bitmap* bitmap)
{
    if (!m_mime_data)
        m_mime_data = Core::MimeData::construct();
    if (bitmap)
        m_mime_data->set_data("image/x-raw-bitmap", bitmap->serialize_to_byte_buffer());
}
void DragOperation::set_data(const String& data_type, const String& data)
{
    if (!m_mime_data)
        m_mime_data = Core::MimeData::construct();
    m_mime_data->set_data(data_type, data.to_byte_buffer());
}

}
