#include <LibDraw/GraphicsBitmap.h>
#include <LibGUI/GDragOperation.h>
#include <LibGUI/GWindowServerConnection.h>

static GDragOperation* s_current_drag_operation;

GDragOperation::GDragOperation(CObject* parent)
    : CObject(parent)
{
}

GDragOperation::~GDragOperation()
{
}

GDragOperation::Outcome GDragOperation::exec()
{
    ASSERT(!s_current_drag_operation);
    ASSERT(!m_event_loop);

    int bitmap_id = -1;
    Size bitmap_size;
    RefPtr<GraphicsBitmap> shared_bitmap;
    if (m_bitmap) {
        shared_bitmap = m_bitmap->to_shareable_bitmap();
        shared_bitmap->shared_buffer()->share_with(GWindowServerConnection::the().server_pid());
        bitmap_id = shared_bitmap->shared_buffer_id();
        bitmap_size = shared_bitmap->size();
    }

    auto response = GWindowServerConnection::the().send_sync<WindowServer::StartDrag>(m_text, m_data_type, m_data, bitmap_id, bitmap_size);
    if (!response->started()) {
        m_outcome = Outcome::Cancelled;
        return m_outcome;
    }

    s_current_drag_operation = this;
    m_event_loop = make<CEventLoop>();
    auto result = m_event_loop->exec();
    m_event_loop = nullptr;
    dbgprintf("%s: event loop returned with result %d\n", class_name(), result);
    remove_from_parent();
    s_current_drag_operation = nullptr;
    return m_outcome;
}

void GDragOperation::done(Outcome outcome)
{
    ASSERT(m_outcome == Outcome::None);
    m_outcome = outcome;
    m_event_loop->quit(0);
}

void GDragOperation::notify_accepted(Badge<GWindowServerConnection>)
{
    ASSERT(s_current_drag_operation);
    s_current_drag_operation->done(Outcome::Accepted);
}

void GDragOperation::notify_cancelled(Badge<GWindowServerConnection>)
{
    ASSERT(s_current_drag_operation);
    s_current_drag_operation->done(Outcome::Cancelled);
}
