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
    struct DeviceNodeMatch {
        String permission_group;
        String family_type_literal;
        String path_pattern;
        DeviceNodeType device_node_type;
        MajorNumber major_number;
        Optional<MinorNumber> specific_minor_number;
        mode_t create_mode;
    };

    DeviceEventLoop(Vector<DeviceNodeMatch>, NonnullOwnPtr<Core::File>);
    virtual ~DeviceEventLoop() = default;

    ErrorOr<void> drain_events_from_devctl();

private:
    Optional<DeviceEventLoop::DeviceNodeMatch const&> device_node_family_to_match_type(DeviceNodeType device_node_type, MajorNumber major_number, MinorNumber minor_number);

    Optional<DeviceNodeFamily&> find_device_node_family(DeviceNodeType, MajorNumber major_number) const;
    ErrorOr<NonnullRefPtr<DeviceNodeFamily>> find_or_register_new_device_node_family(DeviceNodeMatch const& match, DeviceNodeType, MajorNumber major_number);

    ErrorOr<void> register_new_device(DeviceNodeType, MajorNumber major_number, MinorNumber minor_number);
    ErrorOr<void> unregister_device(DeviceNodeType, MajorNumber major_number, MinorNumber minor_number);

    ErrorOr<void> read_one_or_eof(DeviceEvent& event);

    Vector<NonnullRefPtr<DeviceNodeFamily>> m_device_node_families;
    NonnullOwnPtr<Core::File> const m_devctl_file;
    Vector<DeviceNodeMatch> m_matches;
};

}
