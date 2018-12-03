#include "Document.h"
#include "Editor.h"
#include <stdio.h>

int main(int c, char** v)
{
    std::string file_to_open = "cuki.h";
    if (c > 1) {
        file_to_open = v[1];
    }
    auto document = Document::create_from_file(file_to_open);
    if (!document) {
        fprintf(stderr, "Failed to open file.\n");
        return 1;
    }
    Editor editor;
    editor.set_document(std::move(document));
    return editor.exec();
}
