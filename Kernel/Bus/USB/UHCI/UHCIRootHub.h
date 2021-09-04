/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <Kernel/API/KResult.h>
#include <Kernel/Bus/USB/USBHub.h>
#include <Kernel/Bus/USB/USBTransfer.h>

namespace Kernel::USB {

class UHCIController;

class UHCIRootHub {
public:
    static KResultOr<NonnullOwnPtr<UHCIRootHub>> try_create(NonnullRefPtr<UHCIController>);

    UHCIRootHub(NonnullRefPtr<UHCIController>);
    ~UHCIRootHub() = default;

    KResult setup(Badge<UHCIController>);

    u8 device_address() const { return m_hub->address(); }

    KResultOr<size_t> handle_control_transfer(Transfer& transfer);

    void check_for_port_updates() { m_hub->check_for_port_updates(); }

private:
    NonnullRefPtr<UHCIController> m_uhci_controller;
    RefPtr<Hub> m_hub;
};

}
