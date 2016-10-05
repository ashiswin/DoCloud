package com.devostrum.docloud.objects;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Master on 4/13/2014.
 */
public class Person implements Comparable, Parcelable {
    Bitmap profilePic;
    String name;
    String uid;
    String tagline;
    String email;

    public Bitmap getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(Bitmap profilePic) {
        this.profilePic = profilePic;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    Parcelable.Creator<Person> CREATOR = new Parcelable.Creator<Person>() {
        @Override
        public Person createFromParcel(Parcel parcel) {
            Person person = new Person();
            person.name = parcel.readString();
            person.uid = parcel.readString();
            person.email = parcel.readString();

            return person;
        }

        @Override
        public Person[] newArray(int i) {
            return new Person[i];
        }
    };

    @Override
    public int compareTo(Object o) {
        return name.compareTo(((Person) o).name);
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(uid);
        parcel.writeString(email);
    }
}
