package com.samleighton.sethomestwo.datatypes;

import com.samleighton.sethomestwo.models.Home;
import org.apache.commons.lang3.SerializationUtils;
import com.samleighton.sethomestwo.metrics.Errors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class PersistentHome implements PersistentDataType<byte[], Home> {
    @Override
    public @NotNull Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Override
    public @NotNull Class<Home> getComplexType() {
        return Home.class;
    }

    @Override
    public byte @NotNull [] toPrimitive(@NotNull Home home, @NotNull PersistentDataAdapterContext persistentDataAdapterContext) {
        return SerializationUtils.serialize(home);
    }

    @Override
    public @NotNull Home fromPrimitive(byte @NotNull [] bytes, @NotNull PersistentDataAdapterContext persistentDataAdapterContext) {
        try {
            InputStream is = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(is);

            return (Home) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Bukkit.getLogger().severe("There was a problem deserializing a home.");
            Errors.count(Errors.ITEM_DATA);
            e.printStackTrace();
        }

        return new Home("", Material.WHITE_WOOL.name(), new Location(Bukkit.getWorlds().get(0), 0, 0, 0, 0.0f, 0.0f), "", "", "world");
    }
}
