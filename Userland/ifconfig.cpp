#include <LibCore/CFile.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    UNUSED_PARAM(argc);
    UNUSED_PARAM(argv);

    CFile file("/proc/netadapters");
    if (!file.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Error: %s\n", file.error_string());
        return 1;
    }

    for (;;) {
        auto line = file.read_line(1024);
        if (line.is_null())
            break;
        auto parts = String::copy(line, Chomp).split(',');
        if (parts.size() < 4)
            continue;
        printf("%s:\n", parts[0].characters());
        printf("     mac: %s\n", parts[2].characters());
        printf("    ipv4: %s\n", parts[3].characters());
        printf("   class: %s\n", parts[1].characters());
        printf("\n");
    }

    return 0;
}
