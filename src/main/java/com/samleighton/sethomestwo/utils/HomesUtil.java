package com.samleighton.sethomestwo.utils;

import com.google.common.collect.Lists;
import com.samleighton.sethomestwo.dao.Dao;
import com.samleighton.sethomestwo.models.Home;

import java.util.List;
import java.util.UUID;

public class HomesUtil {

    /**
     * The name a home takes when created or requested without one. Matches the
     * name SetHomesV1Importer gives an imported v1 unnamed home, so an importing
     * server's homes line up with what a bare command reaches for.
     */
    public static final String DEFAULT_HOME_NAME = "default";

    public static List<String> getPlayerHomesNameOnly(Dao<Home> homesDao, UUID playerUUID){
        List<Home> playerHomes = homesDao.getAll(playerUUID);
        return Lists.transform(playerHomes, Home::getName);
    }

    public static int getPlayerHomesCount(Dao<Home> homesDao, UUID playerUUID) {
        return homesDao.getAll(playerUUID).size();
    }
}
