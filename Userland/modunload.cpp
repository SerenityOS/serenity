#include <serenity.h>
#include <string.h>

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;
    const char* name = "FIXME";
    int rc = module_unload(name, strlen(name));
    if (rc < 0) {
        perror("module_unload");
        return 1;
    }
    return 0;
}
