#include <AK/AKString.h>
#include <AK/Vector.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (argc < 2) {
        printf("usage: which <executable>\n");
        return 0;
    }

    char* filename = argv[1];

    String path = getenv("PATH");
    if (path.is_empty())
        path = "/bin:/usr/bin";

    auto parts = path.split(':');
    for (auto& part : parts) {
        auto candidate = String::format("%s/%s", part.characters(), filename);
        if(access(candidate.characters(), X_OK) == 0) {
            printf("%s\n", candidate.characters());
            return 0;
        }
    }

    return 1;
}
