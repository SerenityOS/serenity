function (generate_libgl_implementation)
    set(LIBGL_INPUT_FOLDER "${CMAKE_CURRENT_SOURCE_DIR}")

    invoke_generator(
        "GLAPI.cpp"
        Lagom::GenerateGLAPIWrapper
        "${LIBGL_INPUT_FOLDER}/GLAPI.json"
        "GL/glapi.h"
        "GLAPI.cpp"
        arguments -j "${LIBGL_INPUT_FOLDER}/GLAPI.json"
    )

    install(FILES "${CMAKE_CURRENT_BINARY_DIR}/GL/glapi.h" DESTINATION "${CMAKE_INSTALL_INCLUDEDIR}/LibGL/GL/" OPTIONAL)
endfunction()
