/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Net/MDIO.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel::MDIO {

namespace Clause22 {

ErrorOr<NonnullRefPtr<Process>> Interface::spawn_mdio_handling_task(u8 phy_id)
{
    auto status = read_phy_register<Status>(phy_id);
    if (!status.has_auto_negotiation_ability) {
        dmesgln("PHY doesn't support auto-negotiation. This is not supported.");
        return ENOTSUP;
    }

    auto [process, _] = TRY(Process::create_kernel_process("Ethernet PHY MDIO Handling Task"sv, [this, phy_id] { mdio_handling_thread(phy_id); }));
    return process;
}

void Interface::mdio_handling_thread(u8 phy_id)
{
    enum class State {
        Initial,
        ResetStarted,
        LinkDown,
        AutoNegotiationStarted,
        AutoNegotiationComplete,
        LinkUp,
    };

    State state = State::Initial;

    while (!Process::current().is_dying()) {
        switch (state) {
        case State::Initial: {
            // Reset the PHY.
            Control control {};
            control.reset = 1;
            write_phy_register<Control>(phy_id, control);

            state = State::ResetStarted;
            continue;
        }
        case State::ResetStarted: {
            auto control = read_phy_register<Control>(phy_id);
            if (!control.reset) {
                // This bit gets cleared when the PHY finished resetting.
                state = State::LinkDown;
                continue;
            }
            break;
        }
        case State::LinkDown: {
            // Start auto-negotiation with default settings, so without touching register 4.
            // FIXME: Is this always the right thing to do?

            auto control = read_phy_register<Control>(phy_id);
            control.auto_negotiation_enable = 1;
            control.restart_auto_negotiation = 1;
            write_phy_register<Control>(phy_id, control);

            state = State::AutoNegotiationStarted;
            continue;
        }
        case State::AutoNegotiationStarted: {
            auto status = read_phy_register<Status>(phy_id);
            if (status.auto_negotiation_process_completed) {
                state = State::AutoNegotiationComplete;
                continue;
            }
            break;
        }
        case State::AutoNegotiationComplete: {
            auto status = read_phy_register<Status>(phy_id);
            if (status.link_up) {
                state = State::LinkUp;
                on_phy_link_status_change(LinkStatus::Up);
                continue;
            }
            break;
        }
        case State::LinkUp:
            auto status = read_phy_register<Status>(phy_id);
            if (!status.link_up) {
                state = State::LinkDown;
                on_phy_link_status_change(LinkStatus::Down);
                continue;
            }
            break;
        }

        (void)Thread::current()->sleep(Duration::from_milliseconds(500));
    }

    Thread::current()->exit();
    VERIFY_NOT_REACHED();
}

}

}
