package com.samleighton.sethomestwo.support;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.mockbukkit.mockbukkit.exception.UnimplementedOperationException;
import org.opentest4j.AssertionFailedError;

/**
 * Turns MockBukkit's {@link UnimplementedOperationException} into a real test
 * failure instead of a silent skip.
 * <p>
 * MockBukkit's {@code UnimplementedOperationException} extends
 * {@link org.opentest4j.TestAbortedException}. JUnit treats any thrown
 * {@code TestAbortedException} as an assumption failure, not a test failure:
 * the test is reported SKIPPED and Maven still prints BUILD SUCCESS. That
 * means a test can walk off a cliff into an unimplemented corner of the
 * MockBukkit API - stop testing anything past that point - and the run stays
 * green. This has already happened twice in this project's history.
 * <p>
 * This extension intercepts that exception wherever JUnit would otherwise
 * treat it as an abort - inside {@code @Test} methods via
 * {@link TestExecutionExceptionHandler}, and inside {@code @BeforeEach} /
 * {@code @AfterEach} via {@link LifecycleMethodExecutionExceptionHandler} -
 * and rethrows it wrapped as an {@link AssertionFailedError} so it fails the
 * build. The lifecycle handler matters on its own: {@code MockBukkit.unmock()}
 * in {@code @AfterEach} runs the plugin's {@code onDisable}, which calls
 * {@code player.resetTitle()} for any player left mid-teleport, so teardown
 * itself can hit an unimplemented call.
 * <p>
 * The fix for a failure raised here is never to ignore it - it is a
 * documented override in {@link TestPlayer} that patches the specific gap.
 */
public class FailOnUnimplemented implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {

    private static final String MESSAGE =
            "MockBukkit reached an unimplemented operation and would normally abort "
                    + "this test as SKIPPED, which would leave the build green while "
                    + "testing nothing past this point. Fix this by adding a documented "
                    + "override for the missing call to TestPlayer - do not ignore it.";

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        throw translate(throwable);
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        throw translate(throwable);
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        throw translate(throwable);
    }

    private Throwable translate(Throwable throwable) {
        if (throwable instanceof UnimplementedOperationException) {
            return new AssertionFailedError(MESSAGE, throwable);
        }
        return throwable;
    }
}
