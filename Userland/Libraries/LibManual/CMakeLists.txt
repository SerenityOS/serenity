set(SOURCES
    Node.cpp
    PageNode.cpp
    SectionNode.cpp
)

serenity_lib(LibManual manual)
target_link_libraries(LibManual LibCore)
