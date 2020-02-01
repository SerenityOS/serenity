set(SOURCES
    MDCodeBlock.cpp
    MDDocument.cpp
    MDHeading.cpp
    MDList.cpp
    MDParagraph.cpp
    MDText.cpp
)

serenity_lib(LibMarkdown markdown)
target_link_libraries(LibMarkdown LibC)
