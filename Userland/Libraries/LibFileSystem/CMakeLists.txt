set(SOURCES
    TempFile.cpp
)

if (WIN32)
    list(APPEND SOURCES
    FileSystemWindows.cpp
    )
else()
    list(APPEND SOURCES
        FileSystem.cpp
    )
endif()


serenity_lib(LibFileSystem filesystem)
target_link_libraries(LibFileSystem PRIVATE LibCore)
