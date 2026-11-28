/*
 * Copyright (c) 2026, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullRefPtr.h>
#include <Kernel/Time/TimeSource.h>
#include <LibTest/TestCase.h>

class TestTimeSource final : public Kernel::TimeSource {
public:
    explicit TestTimeSource(CounterSize size)
        : TimeSource(size)
    {
    }

    virtual StringView model() const override { return "TestTimeSource"sv; }
    virtual Kernel::HardwareTimerType timer_type() const override { return Kernel::HardwareTimerType::HighPrecisionEventTimer; }
    virtual size_t frequency() const override { return 1000; }
    virtual u64 read_counter_raw() const override { return counter; }

    u64 counter { 0 };
};

TEST_CASE(repeated_reads_across_32_bit_wrap)
{
    auto source = adopt_ref(*new TestTimeSource(Kernel::TimeSource::CounterSize::Bits32));
    u64 seconds = 0;
    u32 ticks = 0;

    source->counter = 0xffff'fffe;
    source->update_time(seconds, ticks, false);

    source->counter = 1;
    EXPECT_EQ(source->read_counter(), 0x1'0000'0001ull);
    EXPECT_EQ(source->read_counter(), 0x1'0000'0001ull);

    auto query_seconds = seconds;
    auto query_ticks = ticks;
    EXPECT_EQ(source->update_time(query_seconds, query_ticks, true), 3'000'000ull);
    query_seconds = seconds;
    query_ticks = ticks;
    EXPECT_EQ(source->update_time(query_seconds, query_ticks, true), 3'000'000ull);
    EXPECT_EQ(source->update_time(seconds, ticks, false), 3'000'000ull);

    source->counter = 4;
    EXPECT_EQ(source->read_counter(), 0x1'0000'0004ull);
    EXPECT_EQ(source->update_time(seconds, ticks, false), 3'000'000ull);

    source->counter = 0xffff'ffff;
    source->update_time(seconds, ticks, false);
    source->counter = 0;
    EXPECT_EQ(source->read_counter(), 0x2'0000'0000ull);
    EXPECT_EQ(source->read_counter(), 0x2'0000'0000ull);
    EXPECT_EQ(source->update_time(seconds, ticks, false), 1'000'000ull);
}

TEST_CASE(repeated_reads_across_64_bit_wrap)
{
    auto source = adopt_ref(*new TestTimeSource(Kernel::TimeSource::CounterSize::Bits64));
    u64 seconds = 0;
    u32 ticks = 0;

    source->counter = 0xffff'ffff'ffff'fffeull;
    source->update_time(seconds, ticks, false);
    seconds = 10;
    ticks = 998;

    source->counter = 1;
    EXPECT_EQ(source->read_counter(), 1ull);
    EXPECT_EQ(source->read_counter(), 1ull);

    auto query_seconds = seconds;
    auto query_ticks = ticks;
    EXPECT_EQ(source->update_time(query_seconds, query_ticks, true), 3'000'000ull);
    EXPECT_EQ(query_seconds, 11ull);
    EXPECT_EQ(query_ticks, 1u);
    query_seconds = seconds;
    query_ticks = ticks;
    EXPECT_EQ(source->update_time(query_seconds, query_ticks, true), 3'000'000ull);
    EXPECT_EQ(query_seconds, 11ull);
    EXPECT_EQ(query_ticks, 1u);
    EXPECT_EQ(source->update_time(seconds, ticks, false), 3'000'000ull);
    EXPECT_EQ(seconds, 11ull);
    EXPECT_EQ(ticks, 1u);

    source->counter = 4;
    EXPECT_EQ(source->update_time(seconds, ticks, false), 3'000'000ull);
    EXPECT_EQ(seconds, 11ull);
    EXPECT_EQ(ticks, 4u);
}
