set(SOURCES
    main.cpp
)

add_executable(GenerateUnicodeData ${SOURCES})
target_link_libraries(GenerateUnicodeData LagomCore)
