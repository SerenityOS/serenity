/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/RefPtr.h>

#include <LibHID/ReportDescriptorParser.h>

namespace Kernel::HID {

class Device;

class ApplicationCollectionDriver : public RefCounted<ApplicationCollectionDriver> {
public:
    virtual ~ApplicationCollectionDriver() = default;

    virtual ErrorOr<void> on_report(ReadonlyBytes) = 0;

protected:
    ApplicationCollectionDriver(Device const& device, ::HID::ApplicationCollection const& application_collection)
        : m_device(device)
        , m_application_collection(application_collection)
    {
    }

    Device const& m_device;
    ::HID::ApplicationCollection const& m_application_collection;

private:
    IntrusiveListNode<ApplicationCollectionDriver, NonnullRefPtr<ApplicationCollectionDriver>> m_list_node;

public:
    using List = IntrusiveList<&ApplicationCollectionDriver::m_list_node>;
};

}
