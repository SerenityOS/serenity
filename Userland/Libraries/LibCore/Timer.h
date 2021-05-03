/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibCore/Object.h>

namespace Core {

class Timer final : public Object {
    C_OBJECT(Timer);

public:
    static NonnullRefPtr<Timer> create_repeating(int interval, Function<void()>&& timeout_handler, Object* parent = nullptr)
    {
        auto timer = adopt(*new Timer(interval, move(timeout_handler), parent));
        timer->stop();
        return timer;
    }
    static NonnullRefPtr<Timer> create_single_shot(int interval, Function<void()>&& timeout_handler, Object* parent = nullptr)
    {
        auto timer = adopt(*new Timer(interval, move(timeout_handler), parent));
        timer->set_single_shot(true);
        timer->stop();
        return timer;
    }

    virtual ~Timer() override;

    void start();
    void start(int interval);
    void restart();
    void restart(int interval);
    void stop();

    bool is_active() const { return m_active; }
    int interval() const { return m_interval; }
    void set_interval(int interval)
    {
        if (m_interval == interval)
            return;
        m_interval = interval;
        m_interval_dirty = true;
    }

    bool is_single_shot() const { return m_single_shot; }
    void set_single_shot(bool single_shot) { m_single_shot = single_shot; }

    Function<void()> on_timeout;

private:
    explicit Timer(Object* parent = nullptr);
    Timer(int interval, Function<void()>&& timeout_handler, Object* parent = nullptr);

    virtual void timer_event(TimerEvent&) override;

    bool m_active { false };
    bool m_single_shot { false };
    bool m_interval_dirty { false };
    int m_interval { 0 };
};

}
