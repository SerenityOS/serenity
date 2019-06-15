#include <stdio.h>
#include <sys/utsname.h>

int main(int argc, char** argv)
{
    utsname uts;
    int rc = uname(&uts);
    if (rc < 0) {
        perror("uname() failed");
        return 0;
    }
    bool flag_s = false;
    bool flag_n = false;
    bool flag_r = false;
    bool flag_m = false;
    if (argc == 1) {
        flag_s = true;
    } else {
        for (int i = 1; i < argc; ++i) {
            if (argv[i][0] == '-') {
                for (const char* o = &argv[i][1]; *o; ++o) {
                    switch (*o) {
                    case 's':
                        flag_s = true;
                        break;
                    case 'n':
                        flag_n = true;
                        break;
                    case 'r':
                        flag_r = true;
                        break;
                    case 'm':
                        flag_m = true;
                        break;
                    case 'a':
                        flag_s = flag_n = flag_r = flag_m = true;
                        break;
                    }
                }
            }
        }
    }
    if (!flag_s && !flag_n && !flag_r && !flag_m)
        flag_s = true;
    if (flag_s)
        printf("%s ", uts.sysname);
    if (flag_n)
        printf("%s ", uts.nodename);
    if (flag_r)
        printf("%s ", uts.release);
    if (flag_m)
        printf("%s ", uts.machine);
    printf("\n");
    return 0;
}
