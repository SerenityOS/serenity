#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/Syscall.h>

int main(int argc, char** argv)
{
    if (argc < 2)
        return 1;

    int pid = atoi(argv[1]);
    int fd = systrace(pid);
    if (fd < 0) {
        perror("systrace");
        return 1;
    }

    for (;;) {
        dword call[5];
        int nread = read(fd, &call, sizeof(call));
        if (nread == 0)
            break;
        if (nread < 0) {
            perror("read");
            return 1;
        }
        ASSERT(nread == sizeof(call));
        printf("%s(%#x, %#x, %#x) = %#x\n", Syscall::to_string((Syscall::Function)call[0]), call[1], call[2], call[3], call[4]);
    }

    int rc = close(fd);
    if (rc < 0) {
        perror("close");
        return 1;
    }

    return 0;
}
