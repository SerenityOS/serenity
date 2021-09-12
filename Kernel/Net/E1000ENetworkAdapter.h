/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Net/E1000NetworkAdapter.h>
#include <Kernel/Net/NetworkAdapter.h>

namespace Kernel {

class E1000ENetworkAdapter final
    : public E1000NetworkAdapter {
public:
    static RefPtr<E1000ENetworkAdapter> try_to_initialize(PCI::Address);

    virtual bool initialize() override;

    virtual ~E1000ENetworkAdapter() override;

    virtual StringView purpose() const override { return class_name(); }

private:
    E1000ENetworkAdapter(PCI::Address, u8 irq);

    virtual StringView class_name() const override { return "E1000ENetworkAdapter"sv; }

    virtual void detect_eeprom() override;
    virtual u32 read_eeprom(u8 address) override;
};
}
