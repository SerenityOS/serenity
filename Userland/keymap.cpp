#include <LibCore/ArgsParser.h>
#include <LibKeyboard/CharacterMap.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio setkeymap rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res/keymaps", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    const char* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "The mapping file to be used", "file");
    args_parser.parse(argc, argv);

    Keyboard::CharacterMap character_map(path);
    int rc = character_map.set_system_map();
    if (rc != 0)
        fprintf(stderr, "%s\n", strerror(-rc));

    return rc;
}
