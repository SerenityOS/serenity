/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <Kernel/Devices/MajorNumberAllocation.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/DeviceMajorNumberAllocations.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSDeviceMajorNumberAllocations::SysFSDeviceMajorNumberAllocations(SysFSDirectory const& parent_directory, DeviceNodeType device_node_type)
    : SysFSGlobalInformation(parent_directory)
    , m_device_node_type(device_node_type)
{
    VERIFY(m_device_node_type == DeviceNodeType::Block || m_device_node_type == DeviceNodeType::Character);
}

StringView SysFSDeviceMajorNumberAllocations::name() const
{
    return m_device_node_type == DeviceNodeType::Character ? "chardev_major_allocs"sv : "blockdev_major_allocs"sv;
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSDeviceMajorNumberAllocations> SysFSDeviceMajorNumberAllocations::must_create(SysFSDirectory const& parent_directory, DeviceNodeType device_node_type)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSDeviceMajorNumberAllocations(parent_directory, device_node_type)).release_nonnull();
}

ErrorOr<void> SysFSDeviceMajorNumberAllocations::try_generate(KBufferBuilder& builder)
{
    VERIFY(m_device_node_type == DeviceNodeType::Block || m_device_node_type == DeviceNodeType::Character);
    auto array = TRY(JsonArraySerializer<>::try_create(builder));
    if (m_device_node_type == DeviceNodeType::Block) {
        for (auto& allocation : MajorAllocation::s_block_device_numbers) {
            auto major_number_allocation_object = TRY(array.add_object());
            TRY(major_number_allocation_object.add("allocated_number"sv, allocation.allocated_number.value()));
            TRY(major_number_allocation_object.add("family_name"sv, allocation.family_name));
            TRY(major_number_allocation_object.finish());
        }
    } else {
        for (auto& allocation : MajorAllocation::s_char_device_numbers) {
            auto major_number_allocation_object = TRY(array.add_object());
            TRY(major_number_allocation_object.add("allocated_number"sv, allocation.allocated_number.value()));
            TRY(major_number_allocation_object.add("family_name"sv, allocation.family_name));
            TRY(major_number_allocation_object.finish());
        }
    }

    TRY(array.finish());
    return {};
}

}
