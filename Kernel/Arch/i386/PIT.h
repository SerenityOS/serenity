#pragma once

#include <AK/Types.h>

#define TICKS_PER_SECOND 1000
/* Timer related ports */
#define TIMER0_CTL 0x40
#define TIMER1_CTL 0x41
#define TIMER2_CTL 0x42
#define PIT_CTL 0x43

/* Building blocks for PIT_CTL */
#define TIMER0_SELECT 0x00
#define TIMER1_SELECT 0x40
#define TIMER2_SELECT 0x80

#define MODE_COUNTDOWN 0x00
#define MODE_ONESHOT 0x02
#define MODE_RATE 0x04
#define MODE_SQUARE_WAVE 0x06

#define WRITE_WORD 0x30

#define BASE_FREQUENCY 1193182

namespace PIT {

void initialize();
u32 ticks_this_second();
u32 seconds_since_boot();

}
