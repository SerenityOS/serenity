serenity_component(
    FileOperation
    TARGETS FileOperation
)

set(SOURCES
    main.cpp
)

serenity_bin(FileOperation)
target_link_libraries(FileOperation PRIVATE LibCore LibMain)
