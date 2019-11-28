#include <serenity.h>
#include <string.h>

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;
    const char* path = "/TestModule.o";
    int rc = module_load(path, strlen(path));
    if (rc < 0) {
        perror("module_load");
        return 1;
    }
    return 0;
}
