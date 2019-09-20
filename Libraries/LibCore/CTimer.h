#pragma once

#include <AK/Function.h>
#include <LibCore/CObject.h>
#include <LibCore/ObjectPtr.h>

class CTimer final : public CObject {
    C_OBJECT(CTimer)
public:
    static ObjectPtr<CTimer> create(CObject* parent = nullptr)
    {
        return new CTimer(parent);
    }

    static ObjectPtr<CTimer> create(int interval, Function<void()>&& timeout_handler, CObject* parent = nullptr)
    {
        return new CTimer(interval, move(timeout_handler), parent);
    }

    virtual ~CTimer() override;

    void start();
    void start(int interval);
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
    explicit CTimer(CObject* parent = nullptr);
    CTimer(int interval, Function<void()>&& timeout_handler, CObject* parent = nullptr);

    virtual void timer_event(CTimerEvent&) override;

    bool m_active { false };
    bool m_single_shot { false };
    bool m_interval_dirty { false };
    int m_interval { 0 };
};
