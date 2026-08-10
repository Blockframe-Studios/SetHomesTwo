package com.samleighton.sethomestwo.enums;

public enum UserError {
    /** Home Item restriction */
    INVALID_HOME_ITEM("This home item does not belong to you."),

    /** Max Home restriction */
    SET_MAX_HOMES_SINGULAR("Max Homes Type is singular. Usage: /set-max-homes [max number of homes]"),
    SET_MAX_HOMES_GROUPS("Max Homes Type is groups. Usage: /set-max-homes [group name] [max number of homes]"),
    MAX_HOMES("You have reached the maximum number of homes allowed."),

    /** Teleport restriction */
    TELEPORT_IS_BLACKLISTED("You cannot teleport to this home because the dimension it is in has been blacklisted."),
    DIMENSION_IS_BLACKLISTED("You cannot set a home in this dimension because it has been blacklisted."),
    MOVED_WHILE_TELEPORTING("Your teleport has been cancelled because you have moved."),
    ALREADY_TELEPORTING("You cannot teleport while already teleporting."),
    UNSAFE_HOME("Teleport cancelled: this home is not safe to stand in and no safe spot was found nearby."),

    /** Command Input Errors */
    DIMENSION_IS_NOT_BLACKLISTED("The %s dimension has not been blacklisted yet therefore you cannot remove it."),
    INVALID_DIMENSION("%s is not a valid dimension. Valid dimensions are (nether, overworld, end)."),
    DELETE_HOME_USAGE("Usage: /delete-home [name]"),
    INVALID_MATERIAL("The material you entered is not valid, please try a different one."),
    PLAYER_NOT_ONLINE("The player supplied is either not online or does not exist."),
    NO_HOMES("You have not created any homes yet. Use /create-home."),
    PLAYERS_ONLY("Only players may execute this command."),
    DIMENSION_ALREADY_BLACKLISTED("The %s dimension has already been blacklisted. You cannot add it again."),
    GROUP_DOES_NOT_EXIST("Group does not exist. Use /get-max-homes-groups to see all groups."),
    HOME_DOES_NOT_EXIST("The home '%s' does not exist.");


    private final String value;

    UserError(String message) {
        this.value = message;
    }

    public String getValue() {
        return value;
    }
}
