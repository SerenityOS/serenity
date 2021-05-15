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
    static NonnullRefPtr<Timer> create_repeating(int interval_ms, Function<void()>&& timeout_handler, Object* parent = nullptr)
    {
        auto timer = adopt_ref(*new Timer(interval_ms, move(timeout_handler), parent));
        timer->stop();
        return timer;
    }
    static NonnullRefPtr<Timer> create_single_shot(int interval_ms, Function<void()>&& timeout_handler, Object* parent = nullptr)
    {
        auto timer = adopt_ref(*new Timer(interval_ms, move(timeout_handler), parent));
        timer->set_single_shot(true);
        timer->stop();
        return timer;
    }

    virtual ~Timer() override;

    void start();
    void start(int interval_ms);
    void restart();
    void restart(int interval_ms);
    void stop();

    bool is_active() const { return m_active; }
    int interval() const { return m_interval_ms; }
    void set_interval(int interval_ms)
    {
        if (m_interval_ms == interval_ms)
            return;
        m_interval_ms = interval_ms;
        m_interval_dirty = true;
    }

    bool is_single_shot() const { return m_single_shot; }
    void set_single_shot(bool single_shot) { m_single_shot = single_shot; }

    Function<void()> on_timeout;

private:
    explicit Timer(Object* parent = nullptr);
    Timer(int interval_ms, Function<void()>&& timeout_handler, Object* parent = nullptr);

    virtual void timer_event(TimerEvent&) override;

    bool m_active { false };
    bool m_single_shot { false };
    bool m_interval_dirty { false };
    int m_interval_ms { 0 };
};

}
