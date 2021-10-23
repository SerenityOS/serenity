set(SOURCES
    BlockQuote.cpp
    CodeBlock.cpp
    CommentBlock.cpp
    ContainerBlock.cpp
    Document.cpp
    Heading.cpp
    HorizontalRule.cpp
    LineIterator.cpp
    List.cpp
    Paragraph.cpp
    Table.cpp
    Text.cpp
)

serenity_lib(LibMarkdown markdown)
target_link_libraries(LibMarkdown LibJS)
