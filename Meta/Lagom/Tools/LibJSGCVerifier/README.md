### LibJSGCVerifier

This is a simple Clang tool to validate certain behavior relating to LibJS's GC. It currently validates
two things:

- For all types wrapped by `GCPtr` or `NonnullGCPtr`, that the wrapped type inherits from `Cell`
- For all types not wrapped by `GCPtr` or `NonnullGCPtr`, that the wrapped type does not inherit from `Cell`
  (otherwise it should be wrapped).

This tool currently support being built with the Serenity Clang toolchain or the lagom Clang toolchain (in which case it won't be able to verify Serenity-only applications)

#### Building & Running with the Serenity toolchain
First build Serenity with the Clang toolchain for x86_64:
```bash
./Meta/serenity.sh build x86_64 Clang
```

Then build the tool with:
```bash
cmake -GNinja -B build
cmake --build build
```

Then run the tool with:
```bash
src/main.py -b <path to serenity>/Build
```

#### Building & Running with the Lagom toolchain
First build the Serenity lagom applications with:
```bash
./Meta/serenity.sh build lagom
```

Then build the tool with:
```bash
cmake -GNinja -DLAGOM_BUILD=ON -B build
cmake --build build
```

Then run the tool with:
```bash
src/main.py -l -b <path to serenity>/Build
```
