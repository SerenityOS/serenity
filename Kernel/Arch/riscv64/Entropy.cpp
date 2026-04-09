/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/riscv64/Entropy.h>

namespace Kernel::RISCV64 {

// https://docs.riscv.org/reference/isa/unpriv/scalar-crypto.html#crypto_scalar_es
ErrorOr<u16> poll_seed_csr_for_entropy()
{
    VERIFY(Processor::current().has_feature(CPUFeature::Zkr));

    for (;;) {
        auto seed_csr_value = RISCV64::CSR::SEED::read();
        if (seed_csr_value.OPST == RISCV64::CSR::SEED::Status::DEAD) {
            dmesgln("RISC-V seed CSR reported an unrecoverable self-test error!");
            return EIO;
        }

        if (seed_csr_value.OPST == RISCV64::CSR::SEED::Status::ES16)
            return static_cast<u16>(seed_csr_value.entropy);

        Processor::wait_check();
    }
}
}
