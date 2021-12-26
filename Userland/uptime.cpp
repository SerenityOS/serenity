#include <stdio.h>

int main(int, char**)
{
    FILE* fp = fopen("/proc/uptime", "r");
    if (!fp) {
        perror("fopen(/proc/uptime)");
        return 1;
    }

    char buffer[BUFSIZ];
    auto* p = fgets(buffer, sizeof(buffer), fp);
    if (!p) {
        perror("fgets");
        return 1;
    }

    unsigned seconds;
    sscanf(buffer, "%u", &seconds);

    printf("Up ");
    
    if (seconds / 86400 > 0) {
        printf("%d day%s, ", seconds / 86400, (seconds / 86400) == 1 ? "" : "s");
        seconds %= 86400;
    }
    
    if (seconds / 3600 > 0) {
        printf("%d hour%s, ", seconds / 3600, (seconds / 3600) == 1 ? "" : "s");
        seconds %= 3600;
    }
    
    if (seconds / 60 > 0) {
        printf("%d minute%s, ", seconds / 60, (seconds / 60) == 1 ? "" : "s");
        seconds %= 60;
    }
    
    printf("%d second%s\n", seconds, seconds == 1 ? "" : "s");

    fclose(fp);
    return 0;
}
