#include "Userspace.cpp"

using namespace Userspace;

extern "C" int elf_entry()
{
    int fd = open("/Banner.txt");
    char buf[2048];
    int nread = read(fd, buf, sizeof(buf));
    buf[nread] = '\0';
    for (int i = 0; i < nread; ++i) {
        putch(buf[i]);
    }
    return 0;
}
