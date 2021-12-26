#include <stdio.h>
#include <unistd.h>
#include <sys/stat.h>
#include <AK/AKString.h>
#include <AK/FileSystemPath.h>

int main(int argc, char** argv)
{
    if (argc != 3) {
        printf("usage: mv <old-path> <new-path>\n");
        return 1;
    }

    String old_path = argv[1];
    String new_path = argv[2];

    struct stat st;
    int rc = lstat(new_path.characters(), &st);
    if (rc == 0) {
        if (S_ISDIR(st.st_mode)) {
            auto old_basename = FileSystemPath(old_path).basename();
            new_path = String::format("%s/%s", new_path.characters(), old_basename.characters());
        }
    }

    rc = rename(old_path.characters(), new_path.characters());
    if (rc < 0) {
        perror("rename");
        return 1;
    }
    return 0;
}
