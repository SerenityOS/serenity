#include <serenity.h>
#include <string.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: %s <module.o>\n", argv[0]);
        return 0;
    }

    const char* path = argv[1];
    int rc = module_load(path, strlen(path));
    if (rc < 0) {
        perror("module_load");
        return 1;
    }
    return 0;
}
