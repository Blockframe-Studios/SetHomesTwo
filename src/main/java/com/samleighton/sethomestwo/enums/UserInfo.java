package com.samleighton.sethomestwo.enums;

public enum UserInfo {
    GET_PLAYER_HOMES_USAGE("Usage: /get-player-homes [playerName]"),
    BLACKLIST_USAGE("Usage: /blacklist <add|remove|list> [world]"),
    CREATE_HOME_USAGE("Usage: /create-home [name] [icon material, or d for the default icon] [description]. Omit the name and the home is called 'default'."),
    NO_HOMES("You have not setup any homes yet, you can use the /create-home command to create one."),
    NO_MAX_HOMES("There is no max number of homes."),
    NO_BLACKLISTED_DIMENSIONS("No dimensions are blacklisted"),
    MOVED_TO_SAFE_SPOT("Your home was not safe to stand in, so you were moved to the nearest safe spot."),
    MOVE_HOME_USAGE("Usage: /move-home <name>"),
    GO_PLAYER_HOME_USAGE("Usage: /go-player-home <player> <home>"),
    DELETE_PLAYER_HOME_USAGE("Usage: /delete-player-home <player> <home>"),
    MOVE_PLAYER_HOME_USAGE("Usage: /move-player-home <player> <home>");

    private final String value;

    UserInfo(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
