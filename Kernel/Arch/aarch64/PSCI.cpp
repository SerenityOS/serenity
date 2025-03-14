/*
 * Copyright (c) 2025, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/PSCI.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>

// https://developer.arm.com/documentation/den0022/latest/
namespace Kernel::PSCI {

enum class Function : u32 {
    SystemOff = 0x8400'0008,
    SystemReset = 0x8400'0009,
};

static FlatPtr call(Function function, FlatPtr arg0, FlatPtr arg1, FlatPtr arg2)
{
    // 5.2.1 Register usage in arguments and return values
    // "For [PSCI] versions using 64-bit parameters, the arguments are passed in X0 to X3, with return values in X0."
    register FlatPtr x0 asm("x0") = static_cast<FlatPtr>(function);
    register FlatPtr x1 asm("x1") = arg0;
    register FlatPtr x2 asm("x2") = arg1;
    register FlatPtr x3 asm("x3") = arg2;

    // NOTE: x4-x17 are marked as clobbered since SMCCC 1.0 doesn't
    // require them to be preserved across SMC or HVC calls.
    asm volatile(
        "hvc #0"
        : "+r"(x0), "+r"(x1), "+r"(x2), "+r"(x3)
        :
        : "memory", "x4", "x5", "x6", "x7", "x8", "x9", "x10", "x11", "x12", "x13", "x14", "x15", "x16", "x17");

    return x0;
}

bool is_supported()
{
    // https://www.kernel.org/doc/Documentation/devicetree/bindings/arm/psci.txt
    // FIXME: Make this a DeviceTree::Driver, and don't assume that the psci node is a child of the root node.
    auto psci = DeviceTree::get().get_child("psci"sv);
    if (!psci.has_value())
        return false;

    // NOTE: We reject "arm,psci" on purpose, since these old devices don't have standardized function IDs.
    if (!psci->is_compatible_with("arm,psci-0.2"sv) && !psci->is_compatible_with("arm,psci-1.0"sv))
        return false;

    auto method = psci->get_property("method"sv);
    if (!method.has_value()) {
        dbgln("PSCI: No method property found");
        return false;
    }

    // Currently, we only support HVC (as opposed to SMC). While SMC is very similar to HVC, only the latter has been tested so far.
    if (method->as_string() != "hvc"sv)
        return false;

    return true;
}

void poweroff()
{
    call(Function::SystemOff, 0, 0, 0);
}

void reset()
{
    call(Function::SystemReset, 0, 0, 0);
}

}
