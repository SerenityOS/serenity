#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>

inline constexpr unsigned encoded_device(unsigned major, unsigned minor)
{
    return (minor & 0xff) | (major << 8) | ((minor & ~0xff) << 12);
}

static int usage()
{
    printf("usage: mknod <name> <c|b|p> <major> <minor>\n");
    return 0;
}

int main(int argc, char** argv)
{
    // FIXME: When invoked with type "p", no need for major/minor numbers.
    // FIXME: Add some kind of option for specifying the file permissions.
    if (argc != 5)
        return usage();

    const char* name = argv[1];
    mode_t mode = 0666;
    switch (argv[2][0]) {
    case 'c':
    case 'u':
        mode |= S_IFCHR;
        break;
    case 'b':
        mode |= S_IFBLK;
        break;
    case 'p':
        mode |= S_IFIFO;
        break;
    default:
        return usage();
    }

    int major = atoi(argv[3]);
    int minor = atoi(argv[4]);

    int rc = mknod(name, mode, encoded_device(major, minor));
    if (rc < 0) {
        perror("mknod");
        return 1;
    }
    return 0;
}
