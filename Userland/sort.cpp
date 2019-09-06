#include <AK/String.h>
#include <AK/QuickSort.h>
#include <AK/Vector.h>
#include <stdio.h>
#include <stdlib.h>

int main(int argc, char** argv)
{
    UNUSED_PARAM(argc);
    UNUSED_PARAM(argv);

    Vector<String> lines;

    for (;;) {
        char buffer[BUFSIZ];
        auto* str = fgets(buffer, sizeof(buffer), stdin);
        if (!str)
            break;
        lines.append(buffer);
    }

    quick_sort(lines.begin(), lines.end(), [](auto& a, auto& b) {
        return strcmp(a.characters(), b.characters()) < 0;
    });

    for (auto& line : lines) {
        fputs(line.characters(), stdout);
    }

    return 0;
}
