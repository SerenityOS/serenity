#include <AK/Assertions.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <LibCore/CFile.h>
#include <LibCore/CProcess.h>

static void check_for_test_mode()
{
    CFile f("/proc/cmdline");
    if (!f.open(CIODevice::ReadOnly)) {
        dbgprintf("Failed to read command line: %s\n", f.error_string());
        ASSERT(false);
    }
    const String cmd = String::copy(f.read_all());
    dbgprintf("Read command line: %s\n", cmd.characters());
    if (cmd.matches("*testmode=1*")) {
        // Eventually, we should run a test binary and wait for it to finish
        // before shutting down. But this is good enough for now.
        dbgprintf("Waiting for testmode shutdown...\n");
        sleep(5);
        dbgprintf("Shutting down due to testmode...\n");
        if (fork() == 0) {
            execl("/bin/shutdown", "/bin/shutdown", "-n", nullptr);
            ASSERT_NOT_REACHED();
        }
    } else {
        dbgprintf("Continuing normally\n");
    }
}

int main(int, char**)
{
    CProcess::start_detached("/bin/LookupServer", Vector<StringView>(), CProcess::Priority::Lowest);
    CProcess::start_detached("/bin/WindowServer", Vector<StringView>(), CProcess::Priority::Highest);
    CProcess::start_detached("/bin/Taskbar", Vector<StringView>(), CProcess::Priority::Highest);
    CProcess::start_detached("/bin/Terminal", Vector<StringView>());
    CProcess::start_detached("/bin/Launcher", Vector<StringView>(), CProcess::Priority::Highest);

    // This won't return if we're in test mode.
    check_for_test_mode();

    while (1) {
        sleep(1);
    }
}
