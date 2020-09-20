set(SOURCES
    CodeBlock.cpp
    Document.cpp
    Heading.cpp
    List.cpp
    Paragraph.cpp
    Table.cpp
    Text.cpp
)

serenity_lib(LibMarkdown markdown)
target_link_libraries(LibMarkdown LibC)
