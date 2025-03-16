/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/Input/HID/ApplicationCollectionDriver.h>
#include <Kernel/Devices/Input/MouseDevice.h>

namespace Kernel::HID {

class MouseDriver final : public ApplicationCollectionDriver {
public:
    virtual ~MouseDriver() override;
    static ErrorOr<NonnullRefPtr<MouseDriver>> create(Device const&, ::HID::ApplicationCollection const&);

private:
    MouseDriver(Device const&, ::HID::ApplicationCollection const&, NonnullRefPtr<MouseDevice>);

    // ^ApplicationCollectionDriver
    virtual ErrorOr<void> on_report(ReadonlyBytes) override;

    NonnullRefPtr<MouseDevice> m_mouse_device;
};

}
