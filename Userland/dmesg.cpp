#include <LibCore/CFile.h>
#include <assert.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;
    auto f = CFile::construct("/proc/dmesg");
    if (!f->open(CIODevice::ReadOnly)) {
        fprintf(stderr, "open: failed to open /proc/dmesg: %s\n", f->error_string());
        return 1;
    }
    const auto& b = f->read_all();
    for (auto i = 0; i < b.size(); ++i)
        putchar(b[i]);
    return 0;
}
