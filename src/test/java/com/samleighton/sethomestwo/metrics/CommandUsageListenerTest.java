package com.samleighton.sethomestwo.metrics;

import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Map;

import static com.samleighton.sethomestwo.metrics.UsageCounters.Family.ALIAS;
import static com.samleighton.sethomestwo.metrics.UsageCounters.Family.COMMAND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandUsageListenerTest extends ServerTestBase {

    private Map<String, Integer> commands() {
        return plugin.getUsageCounters().snapshot(COMMAND);
    }

    private Map<String, Integer> aliases() {
        return plugin.getUsageCounters().snapshot(ALIAS);
    }

    private void playerTypes(String line) {
        PlayerMock player = addPlayer();
        server.getPluginManager().callEvent(new PlayerCommandPreprocessEvent(player, line));
    }

    @Test
    void aCanonicalCommandCountsUnderItsOwnName() {
        playerTypes("/go-home base");

        assertEquals(1, commands().get("go-home"));
        assertTrue(aliases().isEmpty(), "a canonical name is not an alias use");
    }

    @Test
    void aDispatchedCommandIsCounted() {
        PlayerMock player = addPlayer();
        player.performCommand("go-home base");

        assertEquals(1, commands().get("go-home"));
    }

    @Test
    void anAliasCountsOnlyUnderTheTypedAlias() {
        playerTypes("/sethome base");

        assertEquals(1, aliases().get("sethome"));
        assertTrue(commands().isEmpty(), "an alias use is not a use of the canonical name");
    }

    @Test
    void aCommandAndItsAliasAreCountedSeparately() {
        playerTypes("/create-home one");
        playerTypes("/sethome two");
        playerTypes("/sethome three");

        assertEquals(1, commands().get("create-home"));
        assertEquals(2, aliases().get("sethome"));
    }

    @Test
    void theNamespacedFormCountsOnceWithoutTheNamespace() {
        playerTypes("/sethomestwo:home base");

        assertEquals(1, aliases().get("home"));
        assertEquals(1, aliases().size());
        assertTrue(commands().isEmpty());
    }

    @Test
    void caseAndSurroundingSpacesDoNotMatter() {
        playerTypes("  /Go-Home   base ");

        assertEquals(1, commands().get("go-home"));
    }

    @Test
    void aForeignCommandCountsNothing() {
        playerTypes("/say hello");
        playerTypes("/notacommandatall");
        playerTypes("/");

        assertTrue(commands().isEmpty());
        assertTrue(aliases().isEmpty());
    }

    @Test
    void consoleCommandsCountToo() {
        server.getPluginManager().callEvent(new ServerCommandEvent(server.getConsoleSender(), "get-player-homes Steve"));

        assertEquals(1, commands().get("get-player-homes"));
    }

    @Test
    void theListenerNeverThrowsOnAnOddLine() {
        server.getPluginManager().callEvent(new ServerCommandEvent(server.getConsoleSender(), ""));
        server.getPluginManager().callEvent(new ServerCommandEvent(server.getConsoleSender(), "   "));

        assertTrue(commands().isEmpty());
    }
}
