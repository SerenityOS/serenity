#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <assert.h>
#include <LibCore/CFile.h>

int main(int argc, char** argv)
{
    (void) argc;
    (void) argv;
    CFile f("/proc/dmesg");
    if (!f.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "open: failed to open /proc/dmesg: %s", f.error_string());
        return 1;
    }
    const auto& b = f.read_all();
    for (auto i = 0; i < b.size(); ++i)
        putchar(b[i]);
    return 0;
}
