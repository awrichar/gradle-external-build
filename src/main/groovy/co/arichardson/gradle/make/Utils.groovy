package co.arichardson.gradle.make

/**
 * Created by andreric on 4/15/16.
 */
class Utils {
    static <T> T invokeWithContext(Closure<T> closure, Object context) {
        closure.delegate = context
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        return closure.call(context)
    }
}
