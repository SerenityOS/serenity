/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/GPU/DisplayConnector.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Library/KString.h>

namespace Kernel {

class DisplayConnectorAttributeSysFSComponent;
class DisplayConnectorSysFSDirectory final : public SysFSDirectory {
public:
    static NonnullRefPtr<DisplayConnectorSysFSDirectory> create(SysFSDirectory const&, DisplayConnector const&);

    virtual StringView name() const override { return m_device_directory_name->view(); }

    DisplayConnector const& device(Badge<DisplayConnectorAttributeSysFSComponent>) const;

private:
    DisplayConnectorSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, SysFSDirectory const&, DisplayConnector const&);
    NonnullRefPtr<DisplayConnector> const m_device;
    NonnullOwnPtr<KString> m_device_directory_name;
};

}
