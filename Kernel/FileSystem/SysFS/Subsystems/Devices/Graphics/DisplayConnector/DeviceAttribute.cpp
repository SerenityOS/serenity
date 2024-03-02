/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/DisplayConnector/DeviceAttribute.h>
#include <Kernel/Sections.h>

namespace Kernel {

StringView DisplayConnectorAttributeSysFSComponent::name() const
{
    switch (m_type) {
    case Type::MutableModeSettingCapable:
        return "mutable_mode_setting_capable"sv;
    case Type::DoubleFrameBufferingCapable:
        return "double_framebuffering_capable"sv;
    case Type::FlushSupport:
        return "flush_support"sv;
    case Type::PartialFlushSupport:
        return "partial_flush_support"sv;
    case Type::RefreshRateSupport:
        return "refresh_rate_support"sv;
    case Type::EDID:
        return "edid"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

NonnullRefPtr<DisplayConnectorAttributeSysFSComponent> DisplayConnectorAttributeSysFSComponent::must_create(DisplayConnectorSysFSDirectory const& device_directory, Type type)
{
    return adopt_ref(*new (nothrow) DisplayConnectorAttributeSysFSComponent(device_directory, type));
}

DisplayConnectorAttributeSysFSComponent::DisplayConnectorAttributeSysFSComponent(DisplayConnectorSysFSDirectory const& device_directory, Type type)
    : SysFSComponent()
    , m_device(device_directory.device({}))
    , m_type(type)
{
}

ErrorOr<size_t> DisplayConnectorAttributeSysFSComponent::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    auto blob = TRY(try_to_generate_buffer());

    if ((size_t)offset >= blob->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(blob->data() + offset, nread));
    return nread;
}

ErrorOr<NonnullOwnPtr<KBuffer>> DisplayConnectorAttributeSysFSComponent::try_to_generate_buffer() const
{
    OwnPtr<KString> value;
    switch (m_type) {
    case Type::MutableModeSettingCapable:
        value = TRY(KString::formatted("{:d}", m_device->mutable_mode_setting_capable()));
        break;
    case Type::DoubleFrameBufferingCapable:
        value = TRY(KString::formatted("{:d}", m_device->double_framebuffering_capable()));
        break;
    case Type::FlushSupport:
        value = TRY(KString::formatted("{:d}", m_device->flush_support()));
        break;
    case Type::PartialFlushSupport:
        value = TRY(KString::formatted("{:d}", m_device->partial_flush_support()));
        break;
    case Type::RefreshRateSupport:
        value = TRY(KString::formatted("{:d}", m_device->refresh_rate_support()));
        break;
    case Type::EDID: {
        auto edid_buffer = TRY(m_device->get_edid());
        return KBuffer::try_create_with_bytes("SysFS DisplayConnectorAttributeSysFSComponent EDID buffer"sv, edid_buffer.bytes());
    }
    default:
        VERIFY_NOT_REACHED();
    }
    return KBuffer::try_create_with_bytes("SysFS DisplayConnectorAttributeSysFSComponent buffer"sv, value->view().bytes());
}
}
