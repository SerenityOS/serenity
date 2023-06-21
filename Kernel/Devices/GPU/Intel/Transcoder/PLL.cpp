/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/GPU/Intel/Transcoder/PLL.h>

namespace Kernel::IntelGraphics {

static constexpr PLLMaxSettings g35limits {
    { 20'000'000, 400'000'000 },      // values in Hz, dot_clock
    { 1'400'000'000, 2'800'000'000 }, // values in Hz, VCO
    { 3, 8 },                         // n
    { 70, 120 },                      // m
    { 10, 20 },                       // m1
    { 5, 9 },                         // m2
    { 5, 80 },                        // p
    { 1, 8 },                         // p1
    { 5, 10 }                         // p2
};

PLLMaxSettings const& pll_max_settings_for_generation(Generation generation)
{
    switch (generation) {
    case Generation::Gen4:
        return g35limits;
    default:
        VERIFY_NOT_REACHED();
    }
}

static size_t find_absolute_difference(u64 target_frequency, u64 checked_frequency)
{
    if (target_frequency >= checked_frequency)
        return target_frequency - checked_frequency;
    return checked_frequency - target_frequency;
}

Optional<PLLSettings> create_pll_settings(Generation generation, u64 target_frequency, u64 reference_clock)
{
    PLLSettings settings {};
    PLLSettings best_settings {};
    auto& limits = pll_max_settings_for_generation(generation);
    // FIXME: Is this correct for all Intel Native graphics cards?
    settings.p2 = 10;
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Check PLL settings for ref clock of {} Hz, for target of {} Hz", reference_clock, target_frequency);
    u64 best_difference = 0xffffffff;
    for (settings.n = limits.n.min; settings.n <= limits.n.max; ++settings.n) {
        for (settings.m1 = limits.m1.max; settings.m1 >= limits.m1.min; --settings.m1) {
            for (settings.m2 = limits.m2.max; settings.m2 >= limits.m2.min; --settings.m2) {
                for (settings.p1 = limits.p1.max; settings.p1 >= limits.p1.min; --settings.p1) {
                    dbgln_if(INTEL_GRAPHICS_DEBUG, "Check PLL settings for {} {} {} {} {}", settings.n, settings.m1, settings.m2, settings.p1, settings.p2);
                    if (!check_pll_settings(settings, reference_clock, limits))
                        continue;
                    auto current_dot_clock = settings.compute_dot_clock(reference_clock);
                    if (current_dot_clock == target_frequency)
                        return settings;
                    auto difference = find_absolute_difference(target_frequency, current_dot_clock);
                    if (difference < best_difference && (current_dot_clock > target_frequency)) {
                        best_settings = settings;
                        best_difference = difference;
                    }
                }
            }
        }
    }
    if (best_settings.is_valid())
        return best_settings;
    return {};
}

bool check_pll_settings(PLLSettings const& settings, size_t reference_clock, PLLMaxSettings const& limits)
{
    if (settings.n < limits.n.min || settings.n > limits.n.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "N is invalid {}", settings.n);
        return false;
    }
    if (settings.m1 < limits.m1.min || settings.m1 > limits.m1.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "m1 is invalid {}", settings.m1);
        return false;
    }
    if (settings.m2 < limits.m2.min || settings.m2 > limits.m2.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "m2 is invalid {}", settings.m2);
        return false;
    }
    if (settings.p1 < limits.p1.min || settings.p1 > limits.p1.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "p1 is invalid {}", settings.p1);
        return false;
    }

    if (settings.m1 <= settings.m2) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "m2 is invalid {} as it is bigger than m1 {}", settings.m2, settings.m1);
        return false;
    }

    auto m = settings.compute_m();
    auto p = settings.compute_p();

    if (m < limits.m.min || m > limits.m.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "m invalid {}", m);
        return false;
    }
    if (p < limits.p.min || p > limits.p.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "p invalid {}", p);
        return false;
    }

    auto dot = settings.compute_dot_clock(reference_clock);
    auto vco = settings.compute_vco(reference_clock);

    if (dot < limits.dot_clock.min || dot > limits.dot_clock.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "Dot clock invalid {}", dot);
        return false;
    }
    if (vco < limits.vco.min || vco > limits.vco.max) {
        dbgln_if(INTEL_GRAPHICS_DEBUG, "VCO clock invalid {}", vco);
        return false;
    }
    return true;
}

}
