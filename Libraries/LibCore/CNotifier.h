#pragma once

#include <AK/Function.h>
#include "CObject.h"

class CNotifier : public CObject {
public:
    enum Event {
        None = 0,
        Read = 1,
        Write = 2,
        Exceptional = 4,
    };
    CNotifier(int fd, unsigned event_mask);
    ~CNotifier();

    void set_enabled(bool);

    Function<void()> on_ready_to_read;
    Function<void()> on_ready_to_write;

    int fd() const { return m_fd; }
    unsigned event_mask() const { return m_event_mask; }
    void set_event_mask(unsigned event_mask) { m_event_mask = event_mask; }

    const char* class_name() const override { return "CNotifier"; }
    void event(CEvent& event) override;

private:
    int m_fd { -1 };
    unsigned m_event_mask { 0 };
};
