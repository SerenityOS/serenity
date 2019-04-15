#include <AK/FileSystemPath.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: basename <path>\n");
        return 1;
    }
    printf("%s\n", FileSystemPath(argv[1]).basename().characters());
    return 0;
}
