package com.samleighton.sethomestwo.support;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.BlacklistDao;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.models.Home;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Arrange database state directly, so tests of the GUI and commands do not have
 * to drive one feature in order to test another.
 */
public final class HomeFixtures {

    private HomeFixtures() {
    }

    /**
     * Build an unsaved home at the player's current location.
     */
    public static Home home(Player owner, String name) {
        return home(owner, name, owner.getLocation());
    }

    /**
     * Build an unsaved home at a given location.
     */
    public static Home home(Player owner, String name, Location location) {
        return new Home(
                owner.getUniqueId().toString(),
                Material.WHITE_WOOL.name(),
                location,
                name,
                null,
                Objects.requireNonNull(location.getWorld()).getEnvironment().toString()
        );
    }

    /**
     * Saves a home and returns it with its generated id, found via the highest
     * id for this player - relies on id being autoincrementing.
     */
    public static Home persist(Home home) {
        HomesDao dao = new HomesDao();

        if (!dao.save(home)) {
            throw new IllegalStateException("Fixture failed to save home " + home.getName());
        }

        List<Home> all = dao.getAll(UUID.fromString(home.getUUIDBelongingTo()));

        return all.stream()
                .max(Comparator.comparingInt(Home::getId))
                .orElseThrow(() -> new IllegalStateException("Saved home not found: " + home.getName()));
    }

    /**
     * Build and save a home at the player's current location.
     */
    public static Home persist(Player owner, String name) {
        return persist(home(owner, name));
    }

    /**
     * Blacklist a world. The blacklist table stores lowercased world names.
     */
    public static void blacklist(String worldName) {
        new BlacklistDao().save(worldName.toLowerCase());
    }

    /**
     * Make every write to the blacklist table fail while leaving reads working,
     * which is the state a command has to survive: it has already listed the
     * blacklist and only the insert or delete goes wrong.
     */
    public static void breakBlacklistWrites() {
        Connection connection = SetHomesTwo.instance().getConnectionManager().getConnection("homes");

        try (Statement statement = connection.createStatement()) {
            statement.execute("create trigger no_blacklist_insert before insert on blacklist"
                    + " begin select raise(abort, 'blacklist writes disabled'); end;");
            statement.execute("create trigger no_blacklist_delete before delete on blacklist"
                    + " begin select raise(abort, 'blacklist writes disabled'); end;");
        } catch (SQLException e) {
            throw new IllegalStateException("Fixture could not disable blacklist writes", e);
        }
    }
}
