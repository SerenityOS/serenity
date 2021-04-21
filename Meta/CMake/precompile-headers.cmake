function(serenity_add_precompiled_header_to_target target header)
    if (PRECOMPILE_COMMON_HEADERS AND COMMAND target_precompile_headers)
        target_precompile_headers(${target} PRIVATE ${header})
    endif ()
endfunction()

function(serenity_add_ak_precompiled_headers_to_target target)
    serenity_add_precompiled_header_to_target(${target} ${CMAKE_SOURCE_DIR}/Meta/Precompile/AK.h)
endfunction()
