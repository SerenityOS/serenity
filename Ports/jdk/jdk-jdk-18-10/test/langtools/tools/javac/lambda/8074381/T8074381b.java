/*
 * @test /nodynamiccopyright/
 * @bug 8074381
 * @summary java.lang.AssertionError during compiling
 * @compile/fail/ref=T8074381b.out -XDrawDiagnostics T8074381b.java
 */
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class T8074381b {

    @SuppressWarnings("unchecked")
    public Invocation resolve(Handler handler) {
        return new Invocation((t) -> handler.handle((String) t));
    }

    public static class Handler {
        public void handle(String s) {
            System.out.println(s);
        }
    }

    public static class Invocation<T> {
        public final ThrowingConsumer<T> consumer;

        public Invocation(final ThrowingConsumer<T> consumer) {
            this.consumer = consumer;
        }
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> extends BiConsumer<T,Consumer<Throwable>> {
        @Override
        default void accept(final T elem, final Consumer<Throwable> errorHandler) {
            try {
                acceptThrows(elem);
            } catch (final Throwable e) {
                errorHandler.accept(e);
            }
        }

        void acceptThrows(T elem) throws Throwable;
    }
}
