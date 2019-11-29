#include <serenity.h>
#include <string.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: %s <module name>\n", argv[0]);
        return 0;
    }
    const char* name = argv[1];
    int rc = module_unload(name, strlen(name));
    if (rc < 0) {
        perror("module_unload");
        return 1;
    }
    return 0;
}
