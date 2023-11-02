/*
 * Copyright (c) 2023, Pankaj R <dev@pankajraghav.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel {
#if ARCH(X86_64)
u64 msi_address_register(u8 destination_id, bool redirection_hint, bool destination_mode);
u32 msi_data_register(u8 vector, bool level_trigger, bool assert);
u32 msix_vector_control_register(u32 vector_control, bool mask);
void msi_signal_eoi();
#elif ARCH(AARCH64) || ARCH(RISCV64)
[[maybe_unused]] static u64 msi_address_register([[maybe_unused]] u8 destination_id, [[maybe_unused]] bool redirection_hint, [[maybe_unused]] bool destination_mode)
{
    TODO_AARCH64();
    return 0;
}

[[maybe_unused]] static u32 msi_data_register([[maybe_unused]] u8 vector, [[maybe_unused]] bool level_trigger, [[maybe_unused]] bool assert)
{
    TODO_AARCH64();
    return 0;
}

[[maybe_unused]] static u32 msix_vector_control_register([[maybe_unused]] u32 vector_control, [[maybe_unused]] bool mask)
{
    TODO_AARCH64();
    return 0;
}

[[maybe_unused]] static void msi_signal_eoi()
{
    TODO_AARCH64();
    return;
}
#endif
}
