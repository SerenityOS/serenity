/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Enumerate.h>
#include <AK/Error.h>
#include <AK/RefPtr.h>
#include <Kernel/Arch/PowerState.h>
#include <Kernel/Arch/aarch64/RPi/Mailbox.h>
#include <Kernel/Arch/aarch64/RPi/RP1/Clocks.h>
#include <Kernel/Arch/aarch64/RPi/RP1/Fan.h>
#include <Kernel/Arch/aarch64/RPi/RP1/GPIO.h>
#include <Kernel/Arch/aarch64/RPi/RP1/PWM.h>
#include <Kernel/Arch/aarch64/RPi/RP1/RP1.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel::RPi {

struct TripPoint {
    enum class Type {
        ActiveCooling,
        Critical,
    };

    u32 temperature_in_millicelsius;
    u32 hysteresis;
    Type type;
    u8 fan_duty_cycle; // Duty cycles go from 0 to 255 in the devicetree binding.
};

// These values are all taken from the devicetree.

static constexpr auto TRIP_POINTS = to_array<TripPoint>({
    { 50'000, 5'000, TripPoint::Type::ActiveCooling, 75 },
    { 60'000, 5'000, TripPoint::Type::ActiveCooling, 125 },
    { 67'500, 5'000, TripPoint::Type::ActiveCooling, 175 },
    { 75'000, 5'000, TripPoint::Type::ActiveCooling, 250 },
    { 110'000, 0, TripPoint::Type::Critical, 250 },
});

static constexpr auto POLL_INTERVAL = Duration::from_seconds(1);
static constexpr auto FAN_PWM_PERIOD_NS = 41'566;
static constexpr auto FAN_PWM_CHANNEL = 3;
static constexpr auto FAN_GPIO_PIN = 45;
static constexpr auto FAN_SHOULD_INVERT_OUTPUT = RP1PWM::InvertOutput::Yes;

RP1Fan::~RP1Fan()
{
    if (m_control_thread_process) {
        m_control_thread_process->die();
        // Block until all threads exited to prevent UAF
        ErrorOr<siginfo_t> result = siginfo_t {};
        (void)Thread::current()->block<Thread::WaitBlocker>({}, WEXITED, m_control_thread_process.release_nonnull(), result);
    }
}

ErrorOr<NonnullOwnPtr<RP1Fan>> RP1Fan::create(RP1& rp1, RP1GPIO& gpio, RP1PWM& pwm1)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) RP1Fan(rp1, gpio, pwm1));
}

ErrorOr<void> RP1Fan::initialize_and_start_handler_process()
{
    m_rp1_gpio->set_pin_function(FAN_GPIO_PIN, 0); // Function 0 is PWM1.
    m_rp1_pwm1->set_up_channel(FAN_PWM_CHANNEL, FAN_PWM_PERIOD_NS, FAN_SHOULD_INVERT_OUTPUT);

    auto [process, _] = TRY(Process::create_kernel_process("RP1 Fan Control Task"sv, [this]() { fan_control_thread(); }));
    m_control_thread_process = move(process);

    return {};
}

class GetTemperatureMboxMessage : RPi::Mailbox::Message {
public:
    u32 temperature_id;
    u32 value;

    GetTemperatureMboxMessage()
        : RPi::Mailbox::Message(0x0003'0006, 8)
    {
        temperature_id = 0;
        value = 0;
    }
};

void Kernel::RPi::RP1Fan::fan_control_thread()
{
    ssize_t current_trip_index = -1;

    while (!Process::current().is_dying()) {
        struct {
            RPi::Mailbox::MessageHeader header;
            GetTemperatureMboxMessage get_temperature;
            RPi::Mailbox::MessageTail tail;
        } message_queue;

        if (!RPi::Mailbox::the().send_queue(&message_queue, sizeof(message_queue)) || message_queue.get_temperature.value == 0) {
            dbgln("Failed to get the RPi CPU temperature");
        } else {
            dbgln_if(RP1_DEBUG, "RPi CPU temperature: {}.{:03} °C", message_queue.get_temperature.value / 1000, message_queue.get_temperature.value % 1000);

            auto get_applicable_trip_index = [](u32 temperature_in_millicelsius) -> ssize_t {
                for (auto const& [i, trip] : enumerate(TRIP_POINTS)) {
                    if (temperature_in_millicelsius < trip.temperature_in_millicelsius)
                        return static_cast<ssize_t>(i) - 1;
                }

                return TRIP_POINTS.size() - 1;
            };

            auto new_trip_index = get_applicable_trip_index(message_queue.get_temperature.value);

            if (new_trip_index < current_trip_index) {
                VERIFY(current_trip_index >= 0);

                auto const& current_trip = TRIP_POINTS[current_trip_index];
                if (message_queue.get_temperature.value >= current_trip.temperature_in_millicelsius - current_trip.hysteresis) {
                    // New temperature is within hysteresis of the active trip point; keep using the current one.
                    new_trip_index = current_trip_index;
                }
            }

            if (new_trip_index != current_trip_index) {
                dbgln_if(RP1_DEBUG, "Trip point change: {} -> {}", current_trip_index, new_trip_index);
                current_trip_index = new_trip_index;

                if (new_trip_index == -1) {
                    m_rp1_pwm1->disable_channel(FAN_PWM_CHANNEL);
                } else {
                    auto const& new_trip = TRIP_POINTS[new_trip_index];
                    m_rp1_pwm1->enable_channel(FAN_PWM_CHANNEL, FAN_PWM_PERIOD_NS * new_trip.fan_duty_cycle / 256);

                    if (new_trip.type == TripPoint::Type::Critical) {
                        critical_dmesgln("Critical temperature reached! Performing emergency shutdown.");
                        arch_specific_poweroff(PowerOffOrRebootReason::SystemFailure);
                        Processor::halt();
                    }
                }
            }
        }

        (void)Thread::current()->sleep(POLL_INTERVAL);
    }

    Thread::current()->exit();
    VERIFY_NOT_REACHED();
}

RP1Fan::RP1Fan(RP1& rp1, RP1GPIO& gpio, RP1PWM& pwm1)
    : m_rp1(rp1)
    , m_rp1_gpio(gpio)
    , m_rp1_pwm1(pwm1)
{
}

}
