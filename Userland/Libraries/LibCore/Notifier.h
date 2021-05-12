/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibCore/Object.h>

namespace Core {

class AbstractNotifier : public Object {
    C_OBJECT_ABSTRACT(AbstractNotifier)
public:
    enum Event {
        None = 0,
        Read = 1,
        Write = 2,
        Exceptional = 4,
    };

    ~AbstractNotifier() override = default;

    virtual void set_enabled(bool) = 0;
    virtual void close() = 0;

    Function<void()> on_ready_to_read;
    Function<void()> on_ready_to_write;

protected:
    explicit AbstractNotifier(Object* parent)
        : Object(parent)
    {
    }
};

class Notifier : public AbstractNotifier {
    C_OBJECT(Notifier)
public:
    enum Event {
        None = 0,
        Read = 1,
        Write = 2,
        Exceptional = 4,
    };

    ~Notifier() override;

    void set_enabled(bool) override;

    void close() override;

    int fd() const { return m_fd; }
    unsigned event_mask() const { return m_event_mask; }
    void set_event_mask(unsigned event_mask) { m_event_mask = event_mask; }

    void event(Core::Event&) override;

private:
    Notifier(int fd, unsigned event_mask, Object* parent = nullptr);

    int m_fd { -1 };
    unsigned m_event_mask { 0 };
};

}
