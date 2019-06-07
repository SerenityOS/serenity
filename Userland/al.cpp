#include <unistd.h>
#include <signal.h>
#include <stdio.h>

static volatile bool got_alarm = false;

int main(int c, char** v)
{
    unsigned ret = alarm(5);
    printf("alarm() with no alarm set: %u\n", ret);
    ret = alarm(2);
    printf("alarm() with an alarm(5) set: %u\n", ret);

    signal(SIGALRM, [] (int) {
        got_alarm = true;
    });
    printf("Entering infinite loop.\n");
    while (!got_alarm) {
    }
    printf("Oh, we got the alarm. Exiting :)\n");
    return 0;
}
