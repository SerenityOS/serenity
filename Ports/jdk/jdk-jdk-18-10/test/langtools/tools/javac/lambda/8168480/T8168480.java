/*
 * @test
 * @bug 8168480
 * @summary Speculative attribution of lambda causes NPE in Flow
 * @compile T8168480.java
 */

import java.util.function.Supplier;

class T8168480 {
    void f(Runnable r) { }
    void s(Supplier<Runnable> r) { }

    private void testVoid(boolean cond) {
        f(() ->
                new Runnable() {
                    public void run() {
                        switch (42) {
                            default:
                                break;
                        }
                    }
                }.run());

        f(() ->
                f(() -> {
                    switch (42) {
                        default:
                            break;
                    }
                }));

        f(() -> {
            if (cond) {
                new Runnable() {
                    public void run() {
                        switch (42) {
                            default:
                                break;
                        }
                    }
                }.run();
            } else {
                f(() -> {
                    switch (42) {
                        default:
                            break;
                    }
                });
            }
        });
    }

    private void testReturn(boolean cond) {
        s(() ->
                new Runnable() {
                    public void run() {
                        switch (42) {
                            default:
                                break;
                        }
                    }
                });

        s(() ->
                () -> {
                    switch (42) {
                        default:
                            break;
                    }
                });

        s(() -> {
            if (cond) {
                return new Runnable() {
                    public void run() {
                        switch (42) {
                            default:
                                break;
                        }
                    }
                };
            } else {
                return () -> {
                    switch (42) {
                        default:
                            break;
                    }
                };
            }
        });

        s(() -> {
            return cond ?
                new Runnable() {
                    public void run() {
                        switch (42) {
                            default:
                                break;
                        }
                    }
                } : () -> {
                    switch (42) {
                        default:
                            break;
                    }
                };
        });

        s(() -> cond ?
                new Runnable() {
                    public void run() {
                        switch (42) {
                            default:
                                break;
                        }
                    }
                } : () -> {
                    switch (42) {
                        default:
                            break;
                    }
                });
    }
}
