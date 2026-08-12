package com.samleighton.sethomestwo.support;

import com.samleighton.sethomestwo.enums.DebugLevel;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import com.samleighton.sethomestwo.utils.ServerUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BootstrapTest extends ServerTestBase {

    @Test
    void firstServerReadsConfigAndDimensions() {
        assertTrue(plugin.isEnabled());
        assertEquals(DebugLevel.ERROR, ConfigUtil.getDebugLevel());

        List<String> dimensions = ServerUtil.getValidDimensions();
        assertEquals(List.of("world", "world_nether", "world_the_end"), dimensions);

        Map<String, String> map = ServerUtil.getDimensionsMap();
        assertEquals("world", map.get("NORMAL"));
        assertEquals("world_nether", map.get("NETHER"));
        assertEquals("world_the_end", map.get("THE_END"));
    }

    @Test
    void secondServerReadsItsOwnConfigAndDimensions() {
        assertTrue(plugin.isEnabled());
        assertEquals(DebugLevel.ERROR, ConfigUtil.getDebugLevel());

        // A cached FileConfiguration from an earlier server would still answer
        // here, so mutate this server's config and require the read to follow.
        plugin.getConfig().set("debugLevel", "info");
        assertEquals(DebugLevel.INFO, ConfigUtil.getDebugLevel());

        List<String> dimensions = ServerUtil.getValidDimensions();
        assertEquals(List.of("world", "world_nether", "world_the_end"), dimensions);
    }
}
