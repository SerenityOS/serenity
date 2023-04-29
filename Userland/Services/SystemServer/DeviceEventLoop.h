/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DeviceNodeFamily.h"
#include <AK/ByteBuffer.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Notifier.h>
#include <LibIPC/MultiServer.h>

namespace SystemServer {

class DeviceEventLoop {
public:
    enum class MinorNumberAllocationType {
        SequentialUnlimited,
        SequentialLimited,
    };

    struct device_node_match {
        StringView permission_group;
        StringView family_type_literal;
        StringView path_pattern;
        bool block_device;
        MajorNumber major_number;
        MinorNumberAllocationType minor_number_allocation_type;
        MinorNumber minor_number_start;
        size_t minor_number_range_size;
        mode_t create_mode;
    };

    DeviceEventLoop(int devctl_fd);
    virtual ~DeviceEventLoop() = default;

    void drain_events_from_devctl();

private:
    DeviceNodeFamily* find_device_node_family(bool block_device, MajorNumber major_number, MinorNumber minor_number) const;
    ErrorOr<DeviceNodeFamily*> find_or_register_new_device_node_family(device_node_match const& match, bool block_device, MajorNumber major_number, MinorNumber minor_number);

    ErrorOr<void> register_new_device(bool block_device, MajorNumber major_number, MinorNumber minor_number);
    ErrorOr<void> unregister_device(bool block_device, MajorNumber major_number, MinorNumber minor_number);

    Vector<NonnullOwnPtr<DeviceNodeFamily>> m_device_node_families;
    int m_devctl_fd { -1 };
};

}
