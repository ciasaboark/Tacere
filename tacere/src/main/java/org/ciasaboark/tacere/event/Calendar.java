/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.event;


public class Calendar {
    private final long id;
    private final String account_name;
    private final String display_name;
    private final String owner_name;
    private final int color;
    private boolean isSelected = false;

    public Calendar(long id, String account_name, String display_name, String owner_name, int color) {
        if (account_name == null || display_name == null || owner_name == null) {
            throw new IllegalArgumentException("can not initialize Calendar object will null values");
        }

        this.id = id;
        this.account_name = account_name;
        this.display_name = display_name;
        this.owner_name = owner_name;
        this.color = color;
    }


    public long getId() {
        return this.id;
    }

    public String getAccountName() {
        return this.account_name;
    }

    public String getDisplayName() {
        return this.display_name;
    }

    public String getOwnerName() {
        return this.owner_name;
    }

    public int getColor() {
        return color;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

}
