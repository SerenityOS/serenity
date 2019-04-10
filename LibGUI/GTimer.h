#pragma once

#include <LibCore/CObject.h>
#include <AK/Function.h>

class GTimer final : public CObject {
public:
    explicit GTimer(CObject* parent = nullptr);
    virtual ~GTimer() override;

    void start();
    void start(int interval);
    void stop();

    bool is_active() const { return m_active; }
    int interval() const { return m_interval; }
    void set_interval(int interval) { m_interval = interval; }

    bool is_single_shot() const { return m_single_shot; }
    void set_single_shot(bool single_shot) { m_single_shot = single_shot; }

    Function<void()> on_timeout;

    virtual const char* class_name() const override { return "GTimer"; }

private:
    virtual void timer_event(CTimerEvent&) override;

    bool m_active { false };
    bool m_single_shot { false };
    int m_interval { 0 };
};
