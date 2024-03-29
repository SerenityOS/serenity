/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Firmware/EFI/EFI.h>

namespace Kernel::EFI {

// RISCV_EFI_BOOT_PROTOCOL: https://github.com/riscv-non-isa/riscv-uefi/releases/download/1.0.0/RISCV_UEFI_PROTOCOL-spec.pdf
struct RISCVBootProtocol {
    static constexpr GUID guid = { 0xccd15fec, 0x6f73, 0x4eec, { 0x83, 0x95, 0x3e, 0x69, 0xe4, 0xb9, 0x40, 0xbf } };

    using GetBootHartIDFn = EFIAPI Status (*)(RISCVBootProtocol*, FlatPtr* boot_hart_id);

    u64 revision;
    GetBootHartIDFn get_boot_hart_id;
};
static_assert(AssertSize<RISCVBootProtocol, 16>());

}
