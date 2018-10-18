#include "Userspace.cpp"

using namespace Userspace;

extern "C" int elf_entry()
{
    int fd = open("/Banner.txt");
    char buf[2048];
    int nread = read(fd, buf, sizeof(buf));
    buf[nread] = '\0';
    return 0;
}
