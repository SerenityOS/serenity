#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Random.h>
#include <Kernel/Devices/RandomDevice.h>

static u32 random32()
{
    if (g_cpu_supports_rdrand) {
        u32 value = 0;
        asm volatile(
            "1%=:\n"
            "rdrand %0\n"
            "jnc 1%=\n"
            : "=r"(value));
        return value;
    }
    // FIXME: This sucks lol
    static u32 next = 1;
    next = next * 1103515245 + 12345;
    return next;
}

void get_good_random_bytes(u8* buffer, size_t buffer_size)
{
    union {
        u8 bytes[4];
        u32 value;
    } u;
    size_t offset = 4;
    for (size_t i = 0; i < buffer_size; ++i) {
        if (offset >= 4) {
            u.value = random32();
            offset = 0;
        }
        buffer[i] = u.bytes[offset++];
    }
}

void get_fast_random_bytes(u8* buffer, size_t buffer_size)
{
    return get_good_random_bytes(buffer, buffer_size);
}
