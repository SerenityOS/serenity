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
enum HubRequest : u8 {
    GET_STATUS = 0,
    CLEAR_FEATURE = 1,
    // 2 is reserved.
    SET_FEATURE = 3,
    // 4-5 are reserved.
    GET_DESCRIPTOR = 6,
    SET_DESCRIPTOR = 7,
    CLEAR_TT_BUFFER = 8,
    RESET_TT = 9,
    GET_TT_STATE = 10,
    STOP_TT = 11,
};

// USB 2.0 Specification Pages 421-422 Table 11-17
enum HubFeatureSelector : u8 {
    C_HUB_LOCAL_POWER = 0,
    C_HUB_OVER_CURRENT = 1,
    PORT_CONNECTION = 0,
    PORT_ENABLE = 1,
    PORT_SUSPEND = 2,
    PORT_OVER_CURRENT = 3,
    PORT_RESET = 4,
    PORT_POWER = 8,
    PORT_LOW_SPEED = 9,
    C_PORT_CONNECTION = 16,
    C_PORT_ENABLE = 17,
    C_PORT_SUSPEND = 18,
    C_PORT_OVER_CURRENT = 19,
    C_PORT_RESET = 20,
    PORT_TEST = 21,
    PORT_INDICATOR = 22,
    C_PORT_LINK_STATE = 25,
};

// USB 2.0 Specification Section 11.24.2.{6,7}
// This is used to store both the hub status and port status, as they have the same layout.
struct [[gnu::packed]] HubStatus {
    u16 status { 0 };
    u16 change { 0 };
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

static constexpr u16 SUPERSPEED_PORT_STATUS_POWER = (1 << 9);

static constexpr u16 PORT_STATUS_CONNECT_STATUS_CHANGED = (1 << 0);
static constexpr u16 PORT_STATUS_PORT_ENABLED_CHANGED = (1 << 1);
static constexpr u16 PORT_STATUS_SUSPEND_CHANGED = (1 << 2);
static constexpr u16 PORT_STATUS_OVER_CURRENT_INDICATOR_CHANGED = (1 << 3);
static constexpr u16 PORT_STATUS_RESET_CHANGED = (1 << 4);

class Hub : public Device {
public:
    static ErrorOr<NonnullLockRefPtr<Hub>> try_create_root_hub(NonnullLockRefPtr<USBController>, DeviceSpeed);
    static ErrorOr<NonnullLockRefPtr<Hub>> try_create_root_hub(NonnullLockRefPtr<USBController>, DeviceSpeed, u8 address, USBDeviceDescriptor const&);
    static ErrorOr<NonnullLockRefPtr<Hub>> try_create_from_device(Device const&);

    virtual ~Hub() override = default;

    ErrorOr<void> enumerate_and_power_on_hub();

    ErrorOr<void> get_port_status(u8, HubStatus&);
    ErrorOr<void> clear_port_feature(u8, HubFeatureSelector);
    ErrorOr<void> set_port_feature(u8, HubFeatureSelector);

    void check_for_port_updates();

private:
    // Root Hub constructor
    Hub(NonnullLockRefPtr<USBController>, DeviceSpeed);
    Hub(NonnullLockRefPtr<USBController>, DeviceSpeed, u8 address, USBDeviceDescriptor const&);

    Hub(Device const&);

    USBHubDescriptor m_hub_descriptor {};

    Device::List m_children;

    void remove_children_from_sysfs();
};

}
