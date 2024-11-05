/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Noncopyable.h>
#include <LibDeviceTree/DeviceTree.h>

namespace Kernel::DeviceTree {

class Driver;
class Management;

class Device {
    AK_MAKE_NONCOPYABLE(Device);
    AK_MAKE_DEFAULT_MOVABLE(Device);

public:
    Device(::DeviceTree::DeviceTreeNodeView const& node, StringView node_name)
        : m_node(&node)
        , m_node_name(node_name)
    {
    }

    ::DeviceTree::DeviceTreeNodeView const& node() const { return *m_node; }
    StringView node_name() const { return m_node_name; }

    Driver const* driver() const { return m_driver; }
    void set_driver(Badge<Management>, Driver const& driver)
    {
        VERIFY(m_driver == nullptr);
        m_driver = &driver;
    }

private:
    // This needs to be a pointer for the class to be movable.
    ::DeviceTree::DeviceTreeNodeView const* m_node;
    StringView m_node_name;
    Driver const* m_driver { nullptr };
};

}
