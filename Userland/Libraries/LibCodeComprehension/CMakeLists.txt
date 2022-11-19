set(SOURCES
    CodeComprehensionEngine.cpp
    FileDB.cpp
)

serenity_lib(LibCodeComprehension codecomprehension)
serenity_lib_static(LibCodeComprehensionStatic codecomprehension)

add_subdirectory(Cpp)
add_subdirectory(Shell)
