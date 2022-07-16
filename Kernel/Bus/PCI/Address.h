/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Try.h>
#include <Kernel/Bus/PCI/Definitions.h>

namespace Kernel::PCI {

struct Address {
public:
    Address() = default;
    Address(u32 domain)
        : m_domain(domain)
        , m_bus(0)
        , m_device(0)
        , m_function(0)
    {
    }
    Address(u32 domain, u8 bus, u8 device, u8 function)
        : m_domain(domain)
        , m_bus(bus)
        , m_device(device)
        , m_function(function)
    {
    }

    Address(Address const& address) = default;

    bool is_null() const { return !m_bus && !m_device && !m_function; }
    operator bool() const { return !is_null(); }

    // Disable default implementations that would use surprising integer promotion.
    bool operator<=(Address const&) const = delete;
    bool operator>=(Address const&) const = delete;
    bool operator<(Address const&) const = delete;
    bool operator>(Address const&) const = delete;

    bool operator==(Address const& other) const
    {
        if (this == &other)
            return true;
        return m_domain == other.m_domain && m_bus == other.m_bus && m_device == other.m_device && m_function == other.m_function;
    }
    bool operator!=(Address const& other) const
    {
        return !(*this == other);
    }

    u32 domain() const { return m_domain; }
    u8 bus() const { return m_bus; }
    u8 device() const { return m_device; }
    u8 function() const { return m_function; }

private:
    u32 m_domain { 0 };
    u8 m_bus { 0 };
    u8 m_device { 0 };
    u8 m_function { 0 };
};

}
