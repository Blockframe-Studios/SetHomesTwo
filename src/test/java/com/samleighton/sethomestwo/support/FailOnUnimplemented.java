package com.samleighton.sethomestwo.support;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.mockbukkit.mockbukkit.exception.UnimplementedOperationException;
import org.opentest4j.AssertionFailedError;

/**
 * Turns MockBukkit's {@link UnimplementedOperationException} into a real
 * failure instead of a silent skip: it extends TestAbortedException, which
 * JUnit reports as SKIPPED, leaving the build green.
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
