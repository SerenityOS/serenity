#pragma once

#include <SharedGraphics/Rect.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <AK/AKString.h>
#include <AK/InlineLinkedList.h>
#include <AK/Lock.h>
#include "WSMessageReceiver.h"

class Process;

class WSWindow final : public WSMessageReceiver, public InlineLinkedListNode<WSWindow> {
    friend class WSWindowLocker;
public:
    WSWindow(Process&, int window_id);
    virtual ~WSWindow() override;

    int window_id() const { return m_window_id; }

    String title() const { return m_title; }
    void set_title(String&&);

    int x() const { return m_rect.x(); }
    int y() const { return m_rect.y(); }
    int width() const { return m_rect.width(); }
    int height() const { return m_rect.height(); }

    const Rect& rect() const { return m_rect; }
    void set_rect(const Rect&);
    void set_rect_without_repaint(const Rect& rect) { m_rect = rect; }

    Point position() const { return m_rect.location(); }
    void set_position(const Point& position) { set_rect({ position.x(), position.y(), width(), height() }); }
    void set_position_without_repaint(const Point& position) { set_rect_without_repaint({ position.x(), position.y(), width(), height() }); }

    virtual void event(WSMessage&) override;

    bool is_being_dragged() const { return m_is_being_dragged; }
    void set_is_being_dragged(bool b) { m_is_being_dragged = b; }

    GraphicsBitmap* backing() { return m_backing.ptr(); }

    pid_t pid() const { return m_pid; }

    // For InlineLinkedList.
    // FIXME: Maybe make a ListHashSet and then WSWindowManager can just use that.
    WSWindow* m_next { nullptr };
    WSWindow* m_prev { nullptr };

private:
    Lock m_lock;
    String m_title;
    Rect m_rect;
    bool m_is_being_dragged { false };

    RetainPtr<GraphicsBitmap> m_backing;
    Process& m_process;
    int m_window_id { -1 };
    pid_t m_pid { -1 };
};

class WSWindowLocker {
public:
    WSWindowLocker(WSWindow& window) : m_locker(window.m_lock) { }
    ~WSWindowLocker() { }
private:
    Locker m_locker;
};

