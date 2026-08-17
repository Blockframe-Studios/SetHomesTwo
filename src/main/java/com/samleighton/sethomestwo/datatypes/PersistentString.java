package com.samleighton.sethomestwo.datatypes;

import org.apache.commons.lang3.SerializationUtils;
import com.samleighton.sethomestwo.metrics.Errors;
import org.bukkit.Bukkit;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;

public class PersistentString implements PersistentDataType<byte[], String> {
    @Override
    public @NotNull Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Override
    public @NotNull Class<String> getComplexType() {
        return String.class;
    }

    @Override
    public byte @NotNull [] toPrimitive(@NotNull String s, @NotNull PersistentDataAdapterContext persistentDataAdapterContext) {
        return SerializationUtils.serialize(s);
    }

    @Override
    public @NotNull String fromPrimitive(byte @NotNull [] bytes, @NotNull PersistentDataAdapterContext persistentDataAdapterContext) {
        try {
            InputStream is = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(is);

            return (String) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Bukkit.getLogger().severe("There was an issue deserializing a string.");
            Errors.count(Errors.ITEM_DATA);
            e.printStackTrace();
        }

        return Arrays.toString(new byte[0]);
    }
}
