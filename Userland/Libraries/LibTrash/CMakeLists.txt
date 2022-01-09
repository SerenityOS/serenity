set(SOURCES
    Trash.cpp
)

serenity_lib(LibTrash trash)
target_link_libraries(LibTrash LibC LibCore)
