/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
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

DragOperation::DragOperation(Core::EventReceiver* parent)
    : Core::EventReceiver(parent)
{
}

DragOperation::Outcome DragOperation::exec()
{
    VERIFY(!s_current_drag_operation);
    VERIFY(!m_event_loop);
    VERIFY(m_mime_data);

    Gfx::ShareableBitmap drag_bitmap;
    if (m_mime_data->has_format("image/x-raw-bitmap"sv)) {
        auto data = m_mime_data->data("image/x-raw-bitmap"sv);
        auto bitmap = Gfx::Bitmap::create_from_serialized_byte_buffer(move(data)).release_value_but_fixme_should_propagate_errors();
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
    dbgln_if(DRAG_DEBUG, "{}: event loop returned with result {}", class_name(), result);
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

void DragOperation::set_text(ByteString const& text)
{
    if (!m_mime_data)
        m_mime_data = Core::MimeData::construct();
    m_mime_data->set_text(text);
}
void DragOperation::set_bitmap(Gfx::Bitmap const* bitmap)
{
    if (!m_mime_data)
        m_mime_data = Core::MimeData::construct();
    if (bitmap)
        m_mime_data->set_data("image/x-raw-bitmap"_string, bitmap->serialize_to_byte_buffer().release_value_but_fixme_should_propagate_errors());
}
void DragOperation::set_data(String const& data_type, ByteString const& data)
{
    if (!m_mime_data)
        m_mime_data = Core::MimeData::construct();
    m_mime_data->set_data(data_type, data.to_byte_buffer());
}

}
