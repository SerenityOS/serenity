#include <AK/String.h>
#include <LibCore/CFile.h>
#include <LibMarkdown/MDDocument.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char* argv[])
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    String name;
    String section;

    if (argc < 2 || argc > 3) {
        fprintf(stderr, "Usage:\t%s <name>\n\t%s <section <name>\n", argv[0], argv[0]);
        exit(1);
    }

    if (argc == 2) {
        name = argv[1];
    } else {
        section = argv[1];
        name = argv[2];
    }

    auto make_path = [&](String s) {
        return String::format("/usr/share/man/man%s/%s.md", s.characters(), name.characters());
    };
    if (section.is_null()) {
        String sections[] = {
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8"
        };
        for (auto& s : sections) {
            String path = make_path(s);
            if (access(path.characters(), R_OK) == 0) {
                section = s;
                break;
            }
        }
        if (section.is_null()) {
            fprintf(stderr, "No man page for %s\n", name.characters());
            exit(1);
        }
    }

    auto file = CFile::construct();
    file->set_filename(make_path(section));

    if (!file->open(CIODevice::OpenMode::ReadOnly)) {
        perror("Failed to open man page file");
        exit(1);
    }

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    dbg() << "Loading man page from " << file->filename();
    auto buffer = file->read_all();
    String source { (const char*)buffer.data(), (size_t)buffer.size() };

    printf("%s(%s)\t\tSerenity manual\n", name.characters(), section.characters());

    MDDocument document;
    bool success = document.parse(source);
    ASSERT(success);

    String rendered = document.render_for_terminal();
    printf("%s", rendered.characters());
}
