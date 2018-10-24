#include <LibC/stdio.h>
#include <LibC/unistd.h>
#include <LibC/dirent.h>

int main(int c, char** v)
{
    DIR* dirp = opendir("/");
    if (!dirp) {
        printf("opendir failed :(\n");
        return 1;
    }
    while (auto* de = readdir(dirp)) {
        printf("%s\n", de->d_name);

    }
    return 0;
}
