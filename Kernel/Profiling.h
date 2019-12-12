#pragma once

#include <AK/Function.h>
#include <AK/String.h>
#include <AK/Types.h>

class Process;

namespace Profiling {

constexpr size_t max_stack_frame_count = 30;

struct Sample {
    i32 pid;
    i32 tid;
    u64 timestamp;
    u32 frames[max_stack_frame_count];
    u32 offsets[max_stack_frame_count];
    String symbolicated_frames[max_stack_frame_count];
};

Sample& next_sample_slot();
void start(Process&);
void stop();
void for_each_sample(Function<void(Sample&)>);

}
