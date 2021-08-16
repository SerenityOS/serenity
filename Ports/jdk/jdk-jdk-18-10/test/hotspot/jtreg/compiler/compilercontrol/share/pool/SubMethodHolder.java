package compiler.compilercontrol.share.pool;

import jdk.test.lib.util.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * A helper class that creates executables and callables for internal classes
 * It's necessary to have this class to make all helper lambdas not contain
 * any of class names that could be used as a pattern (Internal*, *Klass*)
 */
public abstract class SubMethodHolder extends MethodHolder {
    @Override
    public List<Pair<Executable, Callable<?>>> getAllMethods() {
        List<Pair<Executable, Callable<?>>> pairs = new ArrayList<>();
        {
            Method method = getMethod(this, "method", Float.class);
            Pair<Executable, Callable<?>> pair = new Pair<>(method,
                    () -> method.invoke(this, 3.141592f));
            pairs.add(pair);
        }
        {
            Method method = getMethod(this, "methodDup");
            Pair<Executable, Callable<?>> pair = new Pair<>(method,
                    () -> method.invoke(this));
            pairs.add(pair);
        }
        {
            Method method = getMethod(this, "smethod", Integer.class);
            Pair<Executable, Callable<?>> pair = new Pair<>(method,
                    () -> method.invoke(this, 1024));
            pairs.add(pair);
        }
        try {
            Constructor constructor = this.getClass().getConstructor();
            Pair<Executable, Callable<?>> pair = new Pair<>(constructor,
                    constructor::newInstance);
            pairs.add(pair);
        } catch (NoSuchMethodException e) {
            throw new Error("TESTBUG: unable to get constructor");
        }
        return pairs;
    }
}
