#include <AK/String.h>
#include <LibCore/CFile.h>
#include <LibMarkdown/MDDocument.h>
#include <stdio.h>

int main(int argc, char* argv[])
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* file_name = nullptr;
    bool html = false;

    for (int i = 1; i < argc; i++)
        if (strcmp(argv[i], "--html") == 0)
            html = true;
        else
            file_name = argv[i];

    auto file = CFile::construct();;
    bool success;
    if (file_name == nullptr) {
        success = file->open(STDIN_FILENO, CIODevice::OpenMode::ReadOnly, CFile::ShouldCloseFileDescription::No);
    } else {
        file->set_filename(file_name);
        success = file->open(CIODevice::OpenMode::ReadOnly);
    }
    if (!success) {
        fprintf(stderr, "Error: %s\n", file->error_string());
        return 1;
    }

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto buffer = file->read_all();
    dbg() << "Read size " << buffer.size();

    String input { (const char*)buffer.data(), (size_t)buffer.size() };
    MDDocument document;
    success = document.parse(input);

    if (!success) {
        fprintf(stderr, "Error parsing\n");
        return 1;
    }

    String res = html ? document.render_to_html() : document.render_for_terminal();
    printf("%s", res.characters());
}
