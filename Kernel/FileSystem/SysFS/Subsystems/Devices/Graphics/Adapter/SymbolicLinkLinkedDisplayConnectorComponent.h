/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/KString.h>

namespace Kernel {

class SysFSSymbolicLinkLinkedDisplayConnectorComponent final
    : public SysFSSymbolicLink
    , public Weakable<SysFSSymbolicLinkLinkedDisplayConnectorComponent> {
    friend class SysFSComponentRegistry;

public:
    static ErrorOr<NonnullRefPtr<SysFSSymbolicLinkLinkedDisplayConnectorComponent>> try_create(SysFSDirectory const& parent_directory, size_t display_connector_index, SysFSComponent const& pointed_component);
    virtual StringView name() const override { return m_symlink_name->view(); }

private:
    SysFSSymbolicLinkLinkedDisplayConnectorComponent(NonnullOwnPtr<KString>, SysFSDirectory const& parent_directory, SysFSComponent const& pointed_component);
    NonnullOwnPtr<KString> m_symlink_name;
};

}
