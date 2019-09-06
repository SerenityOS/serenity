#include <AK/String.h>
#include <signal.h>
#include <stdio.h>
#include <unistd.h>

void handle_sigint(int)
{
}

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: sleep <seconds>\n");
        return 1;
    }
    bool ok;
    unsigned secs = String(argv[1]).to_uint(ok);
    if (!ok) {
        fprintf(stderr, "Not a valid number of seconds: \"%s\"\n", argv[1]);
        return 1;
    }
    struct sigaction sa;
    memset(&sa, 0, sizeof(struct sigaction));
    sa.sa_handler = handle_sigint;
    sigaction(SIGINT, &sa, nullptr);
    unsigned remaining = sleep(secs);
    if (remaining) {
        printf("Sleep interrupted with %u seconds remaining.\n", remaining);
    }
    return 0;
}
