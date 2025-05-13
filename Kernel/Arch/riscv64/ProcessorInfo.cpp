/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/riscv64/ProcessorInfo.h>
#include <Kernel/Arch/riscv64/SBI.h>

namespace Kernel {

ProcessorInfo::ProcessorInfo()
{
    if (!SBI::is_legacy()) {
        m_mvendorid = MUST(SBI::Base::get_mvendorid());
        m_marchid = MUST(SBI::Base::get_marchid());
        m_mimpid = MUST(SBI::Base::get_mimpid());
    }
}

}
