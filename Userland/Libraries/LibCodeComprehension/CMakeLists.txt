set(SOURCES
    CodeComprehensionEngine.cpp
    FileDB.cpp
)

serenity_lib(LibCodeComprehension codecomprehension)

add_subdirectory(Cpp)
add_subdirectory(Shell)
