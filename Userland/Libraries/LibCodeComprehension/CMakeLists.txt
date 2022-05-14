set(SOURCES
    CodeComprehensionEngine.cpp
    FileDB.cpp
)

serenity_lib(LibCodeComprehension codecomprehension)
target_link_libraries(LibCodeComprehension LibC)

add_subdirectory(Cpp)
add_subdirectory(Shell)
