set(SOURCES
    PageNode.cpp
    SectionNode.cpp
)

serenity_lib(LibManual manual)
target_link_libraries(LibManual PRIVATE LibCore)
