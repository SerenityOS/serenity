#include <AK/String.h>
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/CArgsParser.h>
#include <LibCore/CDirIterator.h>
#include <assert.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

bool copy_file_or_directory(String, String, bool);
bool copy_file(String, String, struct stat, int);
bool copy_directory(String, String);

int main(int argc, char** argv)
{
    CArgsParser args_parser("cp");
    args_parser.add_arg("r", "copy directories recursively");
    args_parser.add_required_single_value("source");
    args_parser.add_required_single_value("destination");

    CArgsParserResult args = args_parser.parse(argc, argv);
    Vector<String> values = args.get_single_values();
    if (values.size() == 0) {
        args_parser.print_usage();
        return 0;
    }
    bool recursion_allowed = args.is_present("r");
    String src_path = values[0];
    String dst_path = values[1];
    return copy_file_or_directory(src_path, dst_path, recursion_allowed) ? 0 : 1;
}

/**
 * Copy a file or directory to a new location. Returns true if successful, false 
 * otherwise. If there is an error, its description is output to stderr.
 * 
 * Directories should only be copied if recursion_allowed is set.
 */
bool copy_file_or_directory(String src_path, String dst_path, bool recursion_allowed)
{
    int src_fd = open(src_path.characters(), O_RDONLY);
    if (src_fd < 0) {
        perror("open src");
        return false;
    }

    struct stat src_stat;
    int rc = fstat(src_fd, &src_stat);
    if (rc < 0) {
        perror("stat src");
        return false;
    }

    if (S_ISDIR(src_stat.st_mode)) {
        if (!recursion_allowed) {
            fprintf(stderr, "cp: -r not specified; omitting directory '%s'\n", src_path.characters());
            return false;
        }
        return copy_directory(src_path, dst_path);
    }
    return copy_file(src_path, dst_path, src_stat, src_fd);
}

/**
 * Copy a source file to a destination file. Returns true if successful, false 
 * otherwise. If there is an error, its description is output to stderr.
 * 
 * To avoid repeated work, the source file's stat and file descriptor are required.
 */
bool copy_file(String src_path, String dst_path, struct stat src_stat, int src_fd)
{
    int dst_fd = creat(dst_path.characters(), 0666);
    if (dst_fd < 0) {
        if (errno != EISDIR) {
            perror("open dst");
            return false;
        }
        StringBuilder builder;
        builder.append(dst_path);
        builder.append('/');
        builder.append(FileSystemPath(src_path).basename());
        dst_path = builder.to_string();
        dst_fd = creat(dst_path.characters(), 0666);
        if (dst_fd < 0) {
            perror("open dst");
            return false;
        }
    }

    if (src_stat.st_size > 0) {
        if (ftruncate(dst_fd, src_stat.st_size) < 0) {
            perror("cp: ftruncate");
            return false;
        }
    }

    for (;;) {
        char buffer[32768];
        ssize_t nread = read(src_fd, buffer, sizeof(buffer));
        if (nread < 0) {
            perror("read src");
            return false;
        }
        if (nread == 0)
            break;
        ssize_t remaining_to_write = nread;
        char* bufptr = buffer;
        while (remaining_to_write) {
            ssize_t nwritten = write(dst_fd, bufptr, remaining_to_write);
            if (nwritten < 0) {
                perror("write dst");
                return false;
            }
            assert(nwritten > 0);
            remaining_to_write -= nwritten;
            bufptr += nwritten;
        }
    }

    auto my_umask = umask(0);
    umask(my_umask);
    int rc = fchmod(dst_fd, src_stat.st_mode & ~my_umask);
    if (rc < 0) {
        perror("fchmod dst");
        return false;
    }

    close(src_fd);
    close(dst_fd);
    return true;
}

/**
 * Copy the contents of a source directory into a destination directory.
 */
bool copy_directory(String src_path, String dst_path)
{
    int rc = mkdir(dst_path.characters(), 0755);
    if (rc < 0) {
        perror("cp: mkdir");
        return false;
    }
    CDirIterator di(src_path, CDirIterator::SkipDots);
    if (di.has_error()) {
        fprintf(stderr, "cp: CDirIterator: %s\n", di.error_string());
        return false;
    }
    while (di.has_next()) {
        String filename = di.next_path();
        bool is_copied = copy_file_or_directory(
            String::format("%s/%s", src_path.characters(), filename.characters()),
            String::format("%s/%s", dst_path.characters(), filename.characters()),
            true);
        if (!is_copied) {
            return false;
        }
    }
    return true;
}
