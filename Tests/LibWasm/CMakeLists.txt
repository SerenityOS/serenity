serenity_testjs_test(test-wasm.cpp test-wasm LIBS LibWasm)
install(TARGETS test-wasm RUNTIME DESTINATION bin OPTIONAL)
