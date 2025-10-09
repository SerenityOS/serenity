/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/RISCVExtensionBitmask.h>
#include <Kernel/API/archctl_numbers.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$archctl(int option, FlatPtr arg1, FlatPtr arg2, FlatPtr arg3)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    switch (option) {
    case ARCHCTL_RISCV64_GET_CPU_INFO: {
        // https://github.com/riscv-non-isa/riscv-c-api-doc/blob/main/src/c-api.adoc#extension-bitmask
        // FIXME: Maybe handle systems with different mvendorids/marchids/mimpids and/or extensions per hart once we support SMP on riscv64.

        auto feature_bits_array_length = arg1;
        auto feature_bits_array_arg = static_cast<Userspace<unsigned long long*>>(arg2);
        auto cpu_model_arg = static_cast<Userspace<RISCVCPUModel*>>(arg3);

        RISCVCPUModel cpu_model = {
            .mvendorid = Processor::current().info().mvendorid(),
            .marchid = Processor::current().info().marchid(),
            .mimpid = Processor::current().info().mimpid(),
        };

        auto extension_bitmask = Processor::current().userspace_extension_bitmask();

        TRY(copy_to_user(cpu_model_arg, &cpu_model));
        TRY(copy_n_to_user(feature_bits_array_arg, extension_bitmask.data(), min(feature_bits_array_length, extension_bitmask.size())));

        return 0;
    }

    default:
        return EINVAL;
    }
}

}
