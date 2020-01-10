#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: chroot <path>\n");
        return 0;
    }

    if (chroot(argv[1]) < 0) {
        perror("chroot");
        return 1;
    }

    if (chdir("/") < 0) {
        perror("chdir(/)");
        return 1;
    }

    if (execl("/bin/Shell", "Shell", nullptr) < 0) {
        perror("execl");
        return 1;
    }

    return 0;
}
