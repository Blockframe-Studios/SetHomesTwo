package com.samleighton.sethomestwo.updates;

import com.samleighton.sethomestwo.support.ServerTestBase;
import com.samleighton.sethomestwo.support.TestPlayer;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest extends ServerTestBase {

    @BeforeEach
    void allowUpdateChecks() {
        // The plugin schedules its own check, against the real GitHub API, during
        // onEnable. Drop it before re-enabling the setting: the tests below drain
        // the scheduler, which would otherwise fire that real request.
        server.getScheduler().cancelTasks(plugin);
        plugin.getConfig().set("checkForUpdates", true);
    }

    private UpdateChecker checkerFor(String latestTag) {
        return new UpdateChecker(plugin, "1.2.0", () -> latestTag);
    }

    private TestPlayer playerAllowedToSeeNotices() {
        TestPlayer player = addPlayer();
        player.addAttachment(plugin, "sh2.update-notify", true);
        return player;
    }

    @Test
    void nothingIsAvailableBeforeTheCheckRuns() {
        assertNull(checkerFor("v1.3.0").getAvailableVersion());
    }

    @Test
    void newerReleaseBecomesAvailable() {
        UpdateChecker checker = checkerFor("v1.3.0");

        checker.checkNow();

        assertEquals("v1.3.0", checker.getAvailableVersion());
    }

    @Test
    void matchingReleaseLeavesNothingAvailable() {
        UpdateChecker checker = checkerFor("v1.2.0");

        checker.checkNow();

        assertNull(checker.getAvailableVersion());
    }

    @Test
    void olderReleaseLeavesNothingAvailable() {
        UpdateChecker checker = checkerFor("v1.1.0");

        checker.checkNow();

        assertNull(checker.getAvailableVersion());
    }

    @Test
    void failingSourceLeavesNothingAvailable() {
        UpdateChecker checker = new UpdateChecker(plugin, "1.2.0", () -> {
            throw new IOException("no route to host");
        });

        checker.checkNow();

        assertNull(checker.getAvailableVersion());
    }

    @Test
    void playerWithPermissionIsToldTheVersionAndWhereToGetIt() {
        UpdateChecker checker = checkerFor("v1.3.0");
        checker.checkNow();
        TestPlayer player = playerAllowedToSeeNotices();

        checker.notifyIfUpdateAvailable(player);

        assertTrue(player.nextMessage().contains("v1.3.0"));
        assertTrue(player.nextMessage().contains("github.com"));
    }

    @Test
    void playerWithoutPermissionIsNotNotified() {
        UpdateChecker checker = checkerFor("v1.3.0");
        checker.checkNow();
        TestPlayer player = addPlayer();

        checker.notifyIfUpdateAvailable(player);

        assertNull(player.nextMessage());
    }

    @Test
    void playerIsNotNotifiedWhenAlreadyUpToDate() {
        UpdateChecker checker = checkerFor("v1.2.0");
        checker.checkNow();
        TestPlayer player = playerAllowedToSeeNotices();

        checker.notifyIfUpdateAvailable(player);

        assertNull(player.nextMessage());
    }

    @Test
    void scheduledCheckRunsOffTheMainThread() {
        // The startup delay itself cannot be asserted here: MockBukkit's
        // waitAsyncTasksFinished drains every queued task regardless of whether
        // its delay has elapsed, so a test for it would pass without the delay.
        AtomicBoolean ranOnMainThread = new AtomicBoolean(true);
        UpdateChecker checker = new UpdateChecker(plugin, "1.2.0", () -> {
            ranOnMainThread.set(server.isPrimaryThread());
            return "v1.3.0";
        });

        checker.checkLater();
        server.getScheduler().waitAsyncTasksFinished();

        assertFalse(ranOnMainThread.get());
    }

    @Test
    void scheduledCheckRecordsTheResult() {
        UpdateChecker checker = checkerFor("v1.3.0");

        checker.checkLater();
        server.getScheduler().waitAsyncTasksFinished();

        assertEquals("v1.3.0", checker.getAvailableVersion());
    }

    @Test
    void scheduledCheckIsSkippedWhenTheServerOwnerTurnedItOff() {
        plugin.getConfig().set("checkForUpdates", false);
        AtomicInteger calls = new AtomicInteger();
        UpdateChecker checker = new UpdateChecker(plugin, "1.2.0", () -> {
            calls.incrementAndGet();
            return "v1.3.0";
        });

        checker.checkLater();
        server.getScheduler().waitAsyncTasksFinished();

        assertEquals(0, calls.get());
    }

    @Test
    void playerAlreadyOnlineIsNotifiedWhenTheCheckLands() {
        TestPlayer player = playerAllowedToSeeNotices();
        UpdateChecker checker = checkerFor("v1.3.0");

        checker.checkLater();
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performOneTick();

        assertTrue(player.nextMessage().contains("v1.3.0"));
        assertTrue(player.nextMessage().contains("github.com"));
    }

    @Test
    void playerAlreadyOnlineWithoutPermissionIsNotNotifiedWhenTheCheckLands() {
        TestPlayer player = addPlayer();
        UpdateChecker checker = checkerFor("v1.3.0");

        checker.checkLater();
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performOneTick();

        assertNull(player.nextMessage());
    }

    @Test
    void playerAlreadyOnlineIsNotNotifiedWhenAlreadyUpToDate() {
        TestPlayer player = playerAllowedToSeeNotices();
        UpdateChecker checker = checkerFor("v1.2.0");

        checker.checkLater();
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performOneTick();

        assertNull(player.nextMessage());
    }

    @Test
    void playersAlreadyOnlineAreNotifiedFromTheMainThread() {
        AtomicBoolean onMainThread = new AtomicBoolean();
        TestPlayer player = new TestPlayer(server, "MainThreadWatcher") {
            @Override
            public void sendMessage(String message) {
                onMainThread.set(Bukkit.isPrimaryThread());
                super.sendMessage(message);
            }
        };
        server.addPlayer(player);
        player.addAttachment(plugin, UpdateChecker.NOTIFY_PERMISSION, true);
        UpdateChecker checker = checkerFor("v1.3.0");

        checker.checkLater();
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performOneTick();

        assertTrue(onMainThread.get());
    }
}
