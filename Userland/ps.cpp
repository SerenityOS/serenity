#include <LibCore/CFile.h>
#include <LibCore/CProcessStatisticsReader.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;

    printf("PID TPG PGP SID UID  STATE        PPID NSCHED     FDS TTY   NAME\n");

    auto all_processes = CProcessStatisticsReader::get_all();

    for (const auto& it : all_processes) {
        const auto& proc = it.value;
        auto tty = proc.tty;

        if (tty.starts_with("/dev/"))
            tty = tty.characters() + 5;
        else
            tty = "n/a";

        printf("%-3u %-3u %-3u %-3u %-3u  %-11s  %-3u  %-9u  %-3u %-5s %s\n",
            proc.pid,
            proc.pgid,
            proc.pgp,
            proc.sid,
            proc.uid,
            proc.threads.first().state.characters(),
            proc.ppid,
            proc.threads.first().times_scheduled,
            proc.nfds,
            tty.characters(),
            proc.name.characters());
    }

    return 0;
}
