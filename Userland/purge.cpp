#include <stdio.h>
#include <Kernel/Syscall.h>

int main(int, char**)
{
    int purged_page_count = syscall(SC_purge);
    printf("Purged page count: %d\n", purged_page_count);
    return 0;
}
