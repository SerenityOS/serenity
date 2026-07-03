/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/RefPtr.h>
#include <Kernel/Forward.h>

namespace Kernel::RPi {

class RP1;
class RP1GPIO;
class RP1PWM;

class RP1Fan {
public:
    ~RP1Fan();

    static ErrorOr<NonnullOwnPtr<RP1Fan>> create(RP1&, RP1GPIO&, RP1PWM& pwm1);

    ErrorOr<void> initialize_and_start_handler_process();

private:
    void fan_control_thread();

    RP1Fan(RP1&, RP1GPIO&, RP1PWM& pwm1);

    NonnullRefPtr<RP1> m_rp1;
    NonnullRefPtr<RP1GPIO> m_rp1_gpio;
    NonnullRefPtr<RP1PWM> m_rp1_pwm1;

    RefPtr<Process> m_control_thread_process;
};

}
