package com.devostrum.docloud.objects;

import android.graphics.Bitmap;

/**
 * Created by Master on 4/13/2014.
 */
public class Group {
    Bitmap groupPic;
    String groupName;
    String groupId;
    int numberOfItems;

    public Group() {
        numberOfItems = -1;
    }

    public Bitmap getGroupPic() {
        return groupPic;
    }

    public void setGroupPic(Bitmap groupPic) {
        this.groupPic = groupPic;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public int getNumberOfItems() {
        return numberOfItems;
    }

    public void setNumberOfItems(int numberOfItems) {
        this.numberOfItems = numberOfItems;
    }
}
