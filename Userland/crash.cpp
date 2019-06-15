#include <AK/AKString.h>
#include <stdio.h>
#include <stdlib.h>

static void print_usage_and_exit()
{
    printf("usage: crash -[sdia]\n");
    exit(0);
}

int main(int argc, char** argv)
{
    enum Mode {
        SegmentationViolation,
        DivisionByZero,
        IllegalInstruction,
        Abort
    };
    Mode mode = SegmentationViolation;

    if (argc != 2)
        print_usage_and_exit();

    if (String(argv[1]) == "-s")
        mode = SegmentationViolation;
    else if (String(argv[1]) == "-d")
        mode = DivisionByZero;
    else if (String(argv[1]) == "-i")
        mode = IllegalInstruction;
    else if (String(argv[1]) == "-a")
        mode = Abort;
    else
        print_usage_and_exit();

    if (mode == SegmentationViolation) {
        volatile int* crashme = nullptr;
        *crashme = 0xbeef;
        ASSERT_NOT_REACHED();
    }

    if (mode == DivisionByZero) {
        volatile int lala = 10;
        volatile int zero = 0;
        volatile int test = lala / zero;
        ASSERT_NOT_REACHED();
    }

    if (mode == IllegalInstruction) {
        asm volatile("ud2");
        ASSERT_NOT_REACHED();
    }

    if (mode == Abort) {
        abort();
        ASSERT_NOT_REACHED();
    }

    ASSERT_NOT_REACHED();
    return 0;
}
