set(SOURCES
    CodeComprehensionEngine.cpp
    FileDB.cpp
)

serenity_lib(LibCodeComprehension codecomprehension)

serenity_add_subdirectory(Cpp)
serenity_add_subdirectory(Shell)
