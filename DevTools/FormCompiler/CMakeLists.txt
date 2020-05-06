set(SOURCES
    main.cpp
)

add_executable(FormCompiler ${SOURCES})
target_link_libraries(FormCompiler LagomCore)
