/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TSC.h"
#include <AK/Singleton.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Library/LockRefPtr.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<TSC>> TSC::create(HardwareTimerBase& calibration_reference)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) TSC(calibration_reference));
}

bool TSC::is_supported_and_invariant()
{
    // FIXME: This only checks the current processor and assumes that the
    //        other have the same features.
    bool has_sufficient_tsc = Processor::current().has_feature(CPUFeature::TSC)
        && Processor::current().has_feature(CPUFeature::CONSTANT_TSC)
        && Processor::current().has_feature(CPUFeature::NONSTOP_TSC);

    if (!has_sufficient_tsc)
        dmesgln("TSC: TSC is unsupported or not invariant");

    return has_sufficient_tsc;
}

TSC::TSC(HardwareTimerBase& calibration_reference)
    : TimeSource(TimeSource::CounterSize::Bits64)
{
    // FIXME: Intel CPUs provide the frequency as a CPUID leaf, we could use that instead of doing the calibration.
    m_frequency = measure_frequency(calibration_reference, [this] { return read_counter(); }, CounterDirection::Up);
}

u64 TSC::read_counter_raw() const
{
    return read_tsc();
}

}
