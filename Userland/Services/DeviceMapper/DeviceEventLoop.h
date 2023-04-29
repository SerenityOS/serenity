/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DeviceNodeFamily.h"
#include <AK/ByteBuffer.h>
#include <Kernel/API/DeviceEvent.h>
#include <Kernel/API/DeviceFileTypes.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/Notifier.h>

namespace DeviceMapper {

class DeviceEventLoop {
public:
    enum class MinorNumberAllocationType {
        SequentialUnlimited,
        SequentialLimited,
    };

    enum class UnixDeviceType {
        BlockDevice,
        CharacterDevice,
    };

    struct DeviceNodeMatch {
        StringView permission_group;
        StringView family_type_literal;
        StringView path_pattern;
        DeviceNodeFamily::Type unix_device_type;
        MajorNumber major_number;
        MinorNumberAllocationType minor_number_allocation_type;
        MinorNumber minor_number_start;
        size_t minor_number_range_size;
        mode_t create_mode;
    };

    DeviceEventLoop(NonnullOwnPtr<Core::File>);
    virtual ~DeviceEventLoop() = default;

    ErrorOr<void> drain_events_from_devctl();

private:
    Optional<DeviceNodeFamily&> find_device_node_family(DeviceNodeFamily::Type unix_device_type, MajorNumber major_number, MinorNumber minor_number) const;
    ErrorOr<NonnullRefPtr<DeviceNodeFamily>> find_or_register_new_device_node_family(DeviceNodeMatch const& match, DeviceNodeFamily::Type unix_device_type, MajorNumber major_number, MinorNumber minor_number);

    ErrorOr<void> register_new_device(DeviceNodeFamily::Type unix_device_type, MajorNumber major_number, MinorNumber minor_number);
    ErrorOr<void> unregister_device(DeviceNodeFamily::Type unix_device_type, MajorNumber major_number, MinorNumber minor_number);

    ErrorOr<void> read_one_or_eof(DeviceEvent& event);

    Vector<NonnullRefPtr<DeviceNodeFamily>> m_device_node_families;
    NonnullOwnPtr<Core::File> const m_devctl_file;
};

}
