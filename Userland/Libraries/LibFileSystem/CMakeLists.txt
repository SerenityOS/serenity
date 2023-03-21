set(SOURCES
    FileSystem.cpp
)

serenity_lib(LibFileSystem filesystem)
target_link_libraries(LibFileSystem PRIVATE LibCore)
