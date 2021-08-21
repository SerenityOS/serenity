/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArraySerializer.h>
#include <AK/JsonObjectSerializer.h>
#include <Kernel/Bus/USB/SysFSUSB.h>
#include <Kernel/KBufferBuilder.h>

namespace Kernel::USB {

static SysFSUSBBusDirectory* s_procfs_usb_bus_directory;

SysFSUSBDeviceInformation::SysFSUSBDeviceInformation(USB::Device& device)
    : SysFSComponent(String::number(device.address()))
    , m_device(device)
{
}

SysFSUSBDeviceInformation::~SysFSUSBDeviceInformation()
{
}

KResultOr<size_t> SysFSUSBDeviceInformation::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription*) const
{
    KBufferBuilder builder;
    JsonArraySerializer array { builder };

    auto obj = array.add_object();
    obj.add("usb_spec_compliance_bcd", m_device->device_descriptor().usb_spec_compliance_bcd);
    obj.add("device_class", m_device->device_descriptor().device_class);
    obj.add("device_sub_class", m_device->device_descriptor().device_sub_class);
    obj.add("device_protocol", m_device->device_descriptor().device_protocol);
    obj.add("max_packet_size", m_device->device_descriptor().max_packet_size);
    obj.add("vendor_id", m_device->device_descriptor().vendor_id);
    obj.add("product_id", m_device->device_descriptor().product_id);
    obj.add("device_release_bcd", m_device->device_descriptor().device_release_bcd);
    obj.add("manufacturer_id_descriptor_index", m_device->device_descriptor().manufacturer_id_descriptor_index);
    obj.add("product_string_descriptor_index", m_device->device_descriptor().product_string_descriptor_index);
    obj.add("serial_number_descriptor_index", m_device->device_descriptor().serial_number_descriptor_index);
    obj.add("num_configurations", m_device->device_descriptor().num_configurations);
    obj.finish();
    array.finish();

    auto data = builder.build();
    if (!data)
        return ENOMEM;

    ssize_t nread = min(static_cast<off_t>(data->size() - offset), static_cast<off_t>(count));
    if (!buffer.write(data->data() + offset, nread))
        return EFAULT;

    return nread;
}

KResult SysFSUSBBusDirectory::traverse_as_directory(unsigned fsid, Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    ScopedSpinlock lock(m_lock);
    // Note: if the parent directory is null, it means something bad happened as this should not happen for the USB directory.
    VERIFY(m_parent_directory);
    callback({ ".", { fsid, component_index() }, 0 });
    callback({ "..", { fsid, m_parent_directory->component_index() }, 0 });

    for (auto& device_node : m_device_nodes) {
        InodeIdentifier identifier = { fsid, device_node.component_index() };
        callback({ device_node.name(), identifier, 0 });
    }
    return KSuccess;
}

RefPtr<SysFSComponent> SysFSUSBBusDirectory::lookup(StringView name)
{
    ScopedSpinlock lock(m_lock);
    for (auto& device_node : m_device_nodes) {
        if (device_node.name() == name) {
            return device_node;
        }
    }
    return {};
}

RefPtr<SysFSUSBDeviceInformation> SysFSUSBBusDirectory::device_node_for(USB::Device& device)
{
    RefPtr<USB::Device> checked_device = device;
    for (auto& device_node : m_device_nodes) {
        if (device_node.device().ptr() == checked_device.ptr())
            return device_node;
    }
    return {};
}

void SysFSUSBBusDirectory::plug(USB::Device& new_device)
{
    ScopedSpinlock lock(m_lock);
    auto device_node = device_node_for(new_device);
    VERIFY(!device_node);
    m_device_nodes.append(SysFSUSBDeviceInformation::create(new_device));
}

void SysFSUSBBusDirectory::unplug(USB::Device& deleted_device)
{
    ScopedSpinlock lock(m_lock);
    auto device_node = device_node_for(deleted_device);
    VERIFY(device_node);
    device_node->m_list_node.remove();
}

SysFSUSBBusDirectory& SysFSUSBBusDirectory::the()
{
    VERIFY(s_procfs_usb_bus_directory);
    return *s_procfs_usb_bus_directory;
}

UNMAP_AFTER_INIT SysFSUSBBusDirectory::SysFSUSBBusDirectory(SysFSBusDirectory& buses_directory)
    : SysFSDirectory("usb"sv, buses_directory)
{
}

UNMAP_AFTER_INIT void SysFSUSBBusDirectory::initialize()
{
    auto directory = adopt_ref(*new SysFSUSBBusDirectory(SysFSComponentRegistry::the().buses_directory()));
    SysFSComponentRegistry::the().register_new_bus_directory(directory);
    s_procfs_usb_bus_directory = directory;
}

NonnullRefPtr<SysFSUSBDeviceInformation> SysFSUSBDeviceInformation::create(USB::Device& device)
{
    return adopt_ref(*new SysFSUSBDeviceInformation(device));
}

}
