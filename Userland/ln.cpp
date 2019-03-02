#include <stdio.h>
#include <errno.h>
#include <unistd.h>
#include <getopt.h>
#include <stdlib.h>

[[noreturn]] static void print_usage_and_exit()
{
    printf("usage: ln [-s] <target> <link-path>\n");
    exit(0);
}

int main(int argc, char** argv)
{
    bool flag_symlink = false;
    int opt;
    while ((opt = getopt(argc, argv, "s")) != -1) {
        switch (opt) {
        case 's':
            flag_symlink = true;
            break;
        default:
            print_usage_and_exit();
        }
    }

    if ((optind + 1) >= argc)
        print_usage_and_exit();

    if (flag_symlink) {
        int rc = symlink(argv[optind], argv[optind + 1]);
        if (rc < 0) {
            perror("symlink");
            return 1;
        }
        return 0;
    }

    int rc = link(argv[1], argv[2]);
    if (rc < 0) {
        perror("link");
        return 1;
    }
    return 0;
}

