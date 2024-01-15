/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/USB/USBDevice.h>

namespace Kernel::USB {

// USB 2.0 Specification Page 421 Table 11-16
// USB 3.2 Specification Page 440 Table 10-8.  Hub Class Request Codes
enum HubRequest : u8 {
    GET_STATUS = 0,
    CLEAR_FEATURE = 1,
    // 2 is reserved. aka GET_STATE
    SET_FEATURE = 3,
    // 4-5 are reserved.
    GET_DESCRIPTOR = 6,
    SET_DESCRIPTOR = 7,
    CLEAR_TT_BUFFER = 8, // USB 2.0 only
    RESET_TT = 9,        // USB 2.0 only
    GET_TT_STATE = 10,   // USB 2.0 only
    STOP_TT = 11,        // USB 2.0 only
    // USB 3.2:
    SET_HUB_DEPTH = 12,
    GET_PORT_ERR_COUNT = 13
};

// USB 2.0 Specification Pages 421-422 Table 11-17
// USB 3.2 Specification Page 441      Table 10-9.  Hub Class Feature Selectors
enum HubFeatureSelector : u8 {
    // Hub:
    C_HUB_LOCAL_POWER = 0,
    C_HUB_OVER_CURRENT = 1,
    // Port:
    PORT_CONNECTION = 0,
    PORT_ENABLE = 1,  // Maybe USB 2.0 only
    PORT_SUSPEND = 2, // Maybe USB 2.0 only
    PORT_OVER_CURRENT = 3,
    PORT_RESET = 4,
    PORT_LINK_STATE = 5,
    PORT_POWER = 8,
    PORT_LOW_SPEED = 9, // Maybe USB 2.0 only
    C_PORT_CONNECTION = 16,
    C_PORT_ENABLE = 17,  // Maybe USB 2.0 only
    C_PORT_SUSPEND = 18, // Maybe USB 2.0 only
    C_PORT_OVER_CURRENT = 19,
    C_PORT_RESET = 20,
    PORT_TEST = 21,      // USB 2.0 only
    PORT_INDICATOR = 22, // Maybe USB 2.0 only
    // USB 3.2:
    PORT_U1_TIMEOUT = 23,
    PORT_U2_TIMEOUT = 24,
    C_PORT_LINK_STATE = 25,
    C_PORT_CONFIG_ERROR = 26,
    PORT_REMOTE_WAKE_MASK = 27,
    BH_PORT_RESET = 28,
    C_BH_PORT_RESET = 29,
    FORCE_LINKPM_ACCEPT = 30,
};

// USB 2.0 Specification Section 11.24.2.{6,7}
// This is used to store both the hub status and port status, as they have the same layout.
struct [[gnu::packed]] HubStatus {
    u16 status { 0 }; // wHubStatus
    u16 change { 0 }; // wHubChange
};
static_assert(AssertSize<HubStatus, 4>());

static constexpr u16 HUB_STATUS_LOCAL_POWER_SOURCE = (1 << 0);
static constexpr u16 HUB_STATUS_OVER_CURRENT = (1 << 1);

static constexpr u16 HUB_STATUS_LOCAL_POWER_SOURCE_CHANGED = (1 << 0);
static constexpr u16 HUB_STATUS_OVER_CURRENT_CHANGED = (1 << 1);

static constexpr u16 PORT_STATUS_CURRENT_CONNECT_STATUS = (1 << 0);
static constexpr u16 PORT_STATUS_PORT_ENABLED = (1 << 1);
static constexpr u16 PORT_STATUS_SUSPEND = (1 << 2);
static constexpr u16 PORT_STATUS_OVER_CURRENT = (1 << 3);
static constexpr u16 PORT_STATUS_RESET = (1 << 4);
static constexpr u16 PORT_STATUS_PORT_POWER = (1 << 8);
static constexpr u16 PORT_STATUS_LOW_SPEED_DEVICE_ATTACHED = (1 << 9);
static constexpr u16 PORT_STATUS_HIGH_SPEED_DEVICE_ATTACHED = (1 << 10);
static constexpr u16 PORT_STATUS_PORT_STATUS_MODE = (1 << 11);
static constexpr u16 PORT_STATUS_PORT_INDICATOR_CONTROL = (1 << 12);

static constexpr u16 PORT_STATUS_CONNECT_STATUS_CHANGED = (1 << 0);
static constexpr u16 PORT_STATUS_PORT_ENABLED_CHANGED = (1 << 1);
static constexpr u16 PORT_STATUS_SUSPEND_CHANGED = (1 << 2);
static constexpr u16 PORT_STATUS_OVER_CURRENT_INDICATOR_CHANGED = (1 << 3);
static constexpr u16 PORT_STATUS_RESET_CHANGED = (1 << 4);

class Hub : public Device {
public:
    static ErrorOr<NonnullLockRefPtr<Hub>> try_create_root_hub(NonnullLockRefPtr<USBController>, DeviceSpeed);
    static ErrorOr<NonnullLockRefPtr<Hub>> try_create_from_device(Device const&);

    virtual ~Hub() override = default;

    ErrorOr<void> enumerate_and_power_on_hub();

    ErrorOr<void> get_port_status(u8, HubStatus&);
    ErrorOr<void> clear_port_feature(u8, HubFeatureSelector);
    ErrorOr<void> set_port_feature(u8, HubFeatureSelector);

    void check_for_port_updates();

private:
    // Root Hub constructor
    Hub(NonnullLockRefPtr<USBController>, DeviceSpeed, NonnullOwnPtr<ControlPipe> default_pipe);

    Hub(Device const&, NonnullOwnPtr<ControlPipe> default_pipe);

    USBHubDescriptor m_hub_descriptor {};

    Device::List m_children;

    void remove_children_from_sysfs();
};

}
