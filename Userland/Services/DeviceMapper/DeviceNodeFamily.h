/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "RegisteredDeviceNode.h"
#include <AK/Bitmap.h>
#include <AK/HashTable.h>
#include <AK/Types.h>
#include <Kernel/API/DeviceFileTypes.h>

namespace DeviceMapper {

class DeviceNodeFamily : public RefCounted<DeviceNodeFamily> {
public:
    DeviceNodeFamily(Bitmap devices_symbol_suffix_allocation_map, StringView literal_device_family, DeviceNodeType device_node_type, MajorNumber major)
        : m_literal_device_family(literal_device_family)
        , m_device_node_type(device_node_type)
        , m_major(major)
        , m_devices_symbol_suffix_allocation_map(move(devices_symbol_suffix_allocation_map))
    {
    }

    StringView literal_device_family() const { return m_literal_device_family; }
    MajorNumber major_number() const { return m_major; }
    DeviceNodeType device_node_type() const { return m_device_node_type; }

    HashTable<RegisteredDeviceNode>& registered_nodes() { return m_registered_nodes; }
    Bitmap& devices_symbol_suffix_allocation_map() { return m_devices_symbol_suffix_allocation_map; }
    Bitmap const& devices_symbol_suffix_allocation_map() const { return m_devices_symbol_suffix_allocation_map; }

private:
    StringView m_literal_device_family;
    DeviceNodeType m_device_node_type { DeviceNodeType::Character };
    MajorNumber m_major { 0 };

    HashTable<RegisteredDeviceNode> m_registered_nodes;
    Bitmap m_devices_symbol_suffix_allocation_map;
};

}
