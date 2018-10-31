#include <LibC/unistd.h>
#include <LibC/stdio.h>
#include <LibC/signal.h>
#include <AK/String.h>

static unsigned parseUInt(const String& str, bool& ok)
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

int main(int argc, char** argv)
{
    if (argc < 2) {
        printf("usage: kill <PID>\n");
        return 1;
    }
    bool ok;
    unsigned value = parseUInt(argv[1], ok);
    if (!ok) {
        printf("%s is not a valid PID\n", argv[1]);
        return 2;
    }

    kill((pid_t)value, SIGKILL);
    return 0;
}

