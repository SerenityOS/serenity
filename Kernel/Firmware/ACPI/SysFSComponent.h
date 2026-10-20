/*
 * Copyright (c) 2020-2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Library/KString.h>

#pragma once

namespace Kernel::ACPI {

class ACPISysFSComponent : public SysFSComponent {
public:
    static NonnullLockRefPtr<ACPISysFSComponent> create(StringView name, PhysicalAddress, size_t table_size);
    virtual StringView name() const override { return m_table_name->view(); }
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const override;

    virtual size_t size() const override final { return m_length; }

protected:
    ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const;
    ACPISysFSComponent(NonnullOwnPtr<KString> table_name, PhysicalAddress, size_t table_size);

    PhysicalAddress m_paddr;
    size_t m_length { 0 };
    NonnullOwnPtr<KString> m_table_name;
};

}
