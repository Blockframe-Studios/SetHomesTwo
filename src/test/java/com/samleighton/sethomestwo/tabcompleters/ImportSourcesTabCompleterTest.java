package com.samleighton.sethomestwo.tabcompleters;

import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.command.PluginCommand;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportSourcesTabCompleterTest extends ServerTestBase {

    private List<String> complete(String... args) {
        PlayerMock player = addPlayer();
        PluginCommand command = Objects.requireNonNull(plugin.getCommand("import-homes"));

        return new ImportSourcesTabCompleter().onTabComplete(player, command, "import-homes", args);
    }

    @Test
    void theFirstArgumentOffersTheImportSources() {
        List<String> completions = complete("");

        assertTrue(completions.contains("sethomes"), completions.toString());
        assertTrue(completions.contains("essentialsx"), completions.toString());
    }

    @Test
    void theSecondArgumentOffersConfirm() {
        assertEquals(List.of("confirm"), complete("sethomes", ""));
    }

    @Test
    void beyondTheKnownArgumentsNothingIsOffered() {
        // Empty, not null. Bukkit falls back to suggesting online player names
        // when a completer returns null, which is meaningless for this command.
        assertEquals(List.of(), complete("sethomes", "confirm", ""));
    }
}
