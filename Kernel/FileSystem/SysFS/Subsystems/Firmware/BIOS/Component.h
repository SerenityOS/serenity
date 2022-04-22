/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Firmware/Directory.h>
#include <Kernel/KBuffer.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class BIOSSysFSComponent : public SysFSComponent {
public:
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const override;

protected:
    virtual ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const = 0;
    BIOSSysFSComponent();
};

}
