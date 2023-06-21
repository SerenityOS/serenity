/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, Jesse Buhagiar <jesse.buhagiar@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArraySerializer.h>
#include <AK/JsonObjectSerializer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/USB/DeviceInformation.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<SysFSUSBDeviceInformation>> SysFSUSBDeviceInformation::create(USB::Device& device)
{
    auto device_name = TRY(KString::number(device.address()));
    return adopt_nonnull_ref_or_enomem(new (nothrow) SysFSUSBDeviceInformation(move(device_name), device));
}

SysFSUSBDeviceInformation::SysFSUSBDeviceInformation(NonnullOwnPtr<KString> device_name, USB::Device& device)
    : SysFSComponent()
    , m_device(device)
    , m_device_name(move(device_name))
{
}

SysFSUSBDeviceInformation::~SysFSUSBDeviceInformation() = default;

ErrorOr<void> SysFSUSBDeviceInformation::try_generate(KBufferBuilder& builder)
{
    VERIFY(m_lock.is_locked());
    auto array = TRY(JsonArraySerializer<>::try_create(builder));

    auto obj = TRY(array.add_object());
    TRY(obj.add("device_address"sv, m_device->address()));
    TRY(obj.add("usb_spec_compliance_bcd"sv, m_device->device_descriptor().usb_spec_compliance_bcd));
    TRY(obj.add("device_class"sv, m_device->device_descriptor().device_class));
    TRY(obj.add("device_sub_class"sv, m_device->device_descriptor().device_sub_class));
    TRY(obj.add("device_protocol"sv, m_device->device_descriptor().device_protocol));
    TRY(obj.add("max_packet_size"sv, m_device->device_descriptor().max_packet_size));
    TRY(obj.add("vendor_id"sv, m_device->device_descriptor().vendor_id));
    TRY(obj.add("product_id"sv, m_device->device_descriptor().product_id));
    TRY(obj.add("device_release_bcd"sv, m_device->device_descriptor().device_release_bcd));
    TRY(obj.add("manufacturer_id_descriptor_index"sv, m_device->device_descriptor().manufacturer_id_descriptor_index));
    TRY(obj.add("product_string_descriptor_index"sv, m_device->device_descriptor().product_string_descriptor_index));
    TRY(obj.add("serial_number_descriptor_index"sv, m_device->device_descriptor().serial_number_descriptor_index));
    TRY(obj.add("num_configurations"sv, m_device->device_descriptor().num_configurations));
    TRY(obj.add("length"sv, m_device->device_descriptor().descriptor_header.length));
    TRY(obj.add("descriptor_type"sv, m_device->device_descriptor().descriptor_header.descriptor_type));

    auto configuration_array = TRY(obj.add_array("configurations"sv));
    for (auto const& configuration : m_device->configurations()) {
        auto configuration_object = TRY(configuration_array.add_object());
        auto const& configuration_descriptor = configuration.descriptor();
        TRY(configuration_object.add("length"sv, configuration_descriptor.descriptor_header.length));
        TRY(configuration_object.add("descriptor_type"sv, configuration_descriptor.descriptor_header.descriptor_type));
        TRY(configuration_object.add("total_length"sv, configuration_descriptor.total_length));
        TRY(configuration_object.add("number_of_interfaces"sv, configuration_descriptor.number_of_interfaces));
        TRY(configuration_object.add("attributes_bitmap"sv, configuration_descriptor.attributes_bitmap));
        TRY(configuration_object.add("max_power"sv, configuration_descriptor.max_power_in_ma));

        auto interface_array = TRY(configuration_object.add_array("interfaces"sv));
        for (auto const& interface : configuration.interfaces()) {
            auto interface_object = TRY(interface_array.add_object());
            auto const& interface_descriptor = interface.descriptor();
            TRY(interface_object.add("length"sv, interface_descriptor.descriptor_header.length));
            TRY(interface_object.add("descriptor_type"sv, interface_descriptor.descriptor_header.descriptor_type));
            TRY(interface_object.add("interface_number"sv, interface_descriptor.interface_id));
            TRY(interface_object.add("alternate_setting"sv, interface_descriptor.alternate_setting));
            TRY(interface_object.add("num_endpoints"sv, interface_descriptor.number_of_endpoints));
            TRY(interface_object.add("interface_class_code"sv, interface_descriptor.interface_class_code));
            TRY(interface_object.add("interface_sub_class_code"sv, interface_descriptor.interface_sub_class_code));
            TRY(interface_object.add("interface_protocol"sv, interface_descriptor.interface_protocol));
            TRY(interface_object.add("interface_string_desc_index"sv, interface_descriptor.interface_string_descriptor_index));

            auto endpoint_array = TRY(interface_object.add_array("endpoints"sv));
            for (auto const& endpoint : interface.endpoints()) {
                auto endpoint_object = TRY(endpoint_array.add_object());
                TRY(endpoint_object.add("length"sv, endpoint.descriptor_header.length));
                TRY(endpoint_object.add("descriptor_length"sv, endpoint.descriptor_header.descriptor_type));
                TRY(endpoint_object.add("endpoint_address"sv, endpoint.endpoint_address));
                TRY(endpoint_object.add("attribute_bitmap"sv, endpoint.endpoint_attributes_bitmap));
                TRY(endpoint_object.add("max_packet_size"sv, endpoint.max_packet_size));
                TRY(endpoint_object.add("polling_interval"sv, endpoint.poll_interval_in_frames));
                TRY(endpoint_object.finish());
            }
            TRY(endpoint_array.finish());
            TRY(interface_object.finish());
        }
        TRY(interface_array.finish());
        TRY(configuration_object.finish());
    }
    TRY(configuration_array.finish());

    TRY(obj.finish());
    TRY(array.finish());
    return {};
}

ErrorOr<void> SysFSUSBDeviceInformation::refresh_data(OpenFileDescription& description) const
{
    MutexLocker lock(m_lock);
    auto& cached_data = description.data();
    if (!cached_data) {
        cached_data = TRY(adopt_nonnull_own_or_enomem(new (nothrow) SysFSInodeData));
    }
    auto builder = TRY(KBufferBuilder::try_create());
    TRY(const_cast<SysFSUSBDeviceInformation&>(*this).try_generate(builder));
    auto& typed_cached_data = static_cast<SysFSInodeData&>(*cached_data);
    typed_cached_data.buffer = builder.build();
    if (!typed_cached_data.buffer)
        return ENOMEM;
    return {};
}

ErrorOr<size_t> SysFSUSBDeviceInformation::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* description) const
{
    dbgln_if(SYSFS_DEBUG, "SysFSUSBDeviceInformation @ {}: read_bytes offset: {} count: {}", name(), offset, count);

    VERIFY(offset >= 0);
    VERIFY(buffer.user_or_kernel_ptr());

    if (!description)
        return Error::from_errno(EIO);

    MutexLocker locker(m_lock);

    if (!description->data()) {
        dbgln("SysFSUSBDeviceInformation: Do not have cached data!");
        return Error::from_errno(EIO);
    }

    auto& typed_cached_data = static_cast<SysFSInodeData&>(*description->data());
    auto& data_buffer = typed_cached_data.buffer;

    if (!data_buffer || (size_t)offset >= data_buffer->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(data_buffer->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(data_buffer->data() + offset, nread));
    return nread;
}

}
