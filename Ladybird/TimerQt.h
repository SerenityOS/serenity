/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Platform/Timer.h>

class QTimer;

namespace Ladybird {

class TimerQt final : public Web::Platform::Timer {
public:
    static NonnullRefPtr<TimerQt> create();

    virtual ~TimerQt();

    virtual void start() override;
    virtual void start(int interval_ms) override;
    virtual void restart() override;
    virtual void restart(int interval_ms) override;
    virtual void stop() override;

    virtual void set_active(bool) override;

    virtual bool is_active() const override;
    virtual int interval() const override;
    virtual void set_interval(int interval_ms) override;

    virtual bool is_single_shot() const override;
    virtual void set_single_shot(bool) override;

private:
    TimerQt();

    QTimer* m_timer { nullptr };
};

}
