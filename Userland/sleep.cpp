#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <AK/AKString.h>

static unsigned parse_uint(const String& str, bool& ok)
{
    unsigned value = 0;
    for (size_t i = 0; i < str.length(); ++i) {
        if (str[i] < '0' || str[i] > '9') {
            ok = false;
            return 0;
        }
        value = value * 10;
        value += str[i] - '0';
    }
    ok = true;
    return value;
}

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
    unsigned secs = parse_uint(argv[1], ok);
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

