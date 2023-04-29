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
    enum class Type {
        BlockDevice,
        CharacterDevice,
    };

    DeviceNodeFamily(Bitmap devices_symbol_suffix_allocation_map, StringView literal_device_family, Type type, MajorNumber major, MinorNumber base_minor)
        : m_literal_device_family(literal_device_family)
        , m_type(type)
        , m_major(major)
        , m_base_minor(base_minor)
        , m_devices_symbol_suffix_allocation_map(move(devices_symbol_suffix_allocation_map))
    {
    }

    StringView literal_device_family() const { return m_literal_device_family; }
    MajorNumber major_number() const { return m_major; }
    MinorNumber base_minor_number() const { return m_base_minor; }
    Type type() const { return m_type; }

    HashTable<RegisteredDeviceNode>& registered_nodes() { return m_registered_nodes; }
    Bitmap& devices_symbol_suffix_allocation_map() { return m_devices_symbol_suffix_allocation_map; }
    Bitmap const& devices_symbol_suffix_allocation_map() const { return m_devices_symbol_suffix_allocation_map; }

private:
    StringView m_literal_device_family;
    Type m_type { Type::CharacterDevice };
    MajorNumber m_major { 0 };
    MinorNumber m_base_minor { 0 };

    HashTable<RegisteredDeviceNode> m_registered_nodes;
    Bitmap m_devices_symbol_suffix_allocation_map;
};

}
