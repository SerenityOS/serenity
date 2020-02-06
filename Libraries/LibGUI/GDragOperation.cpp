/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGfx/Bitmap.h>
#include <LibGUI/GDragOperation.h>
#include <LibGUI/GWindowServerConnection.h>

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
    ASSERT(!s_current_drag_operation);
    ASSERT(!m_event_loop);

    int bitmap_id = -1;
    Gfx::Size bitmap_size;
    RefPtr<Gfx::Bitmap> shared_bitmap;
    if (m_bitmap) {
        shared_bitmap = m_bitmap->to_shareable_bitmap();
        shared_bitmap->shared_buffer()->share_with(WindowServerConnection::the().server_pid());
        bitmap_id = shared_bitmap->shared_buffer_id();
        bitmap_size = shared_bitmap->size();
    }

    auto response = WindowServerConnection::the().send_sync<Messages::WindowServer::StartDrag>(m_text, m_data_type, m_data, bitmap_id, bitmap_size);
    if (!response->started()) {
        m_outcome = Outcome::Cancelled;
        return m_outcome;
    }

    s_current_drag_operation = this;
    m_event_loop = make<Core::EventLoop>();
    auto result = m_event_loop->exec();
    m_event_loop = nullptr;
    dbgprintf("%s: event loop returned with result %d\n", class_name(), result);
    remove_from_parent();
    s_current_drag_operation = nullptr;
    return m_outcome;
}

void DragOperation::done(Outcome outcome)
{
    ASSERT(m_outcome == Outcome::None);
    m_outcome = outcome;
    m_event_loop->quit(0);
}

void DragOperation::notify_accepted(Badge<WindowServerConnection>)
{
    ASSERT(s_current_drag_operation);
    s_current_drag_operation->done(Outcome::Accepted);
}

void DragOperation::notify_cancelled(Badge<WindowServerConnection>)
{
    ASSERT(s_current_drag_operation);
    s_current_drag_operation->done(Outcome::Cancelled);
}

}
