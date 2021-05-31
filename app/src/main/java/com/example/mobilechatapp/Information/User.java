package com.example.mobilechatapp.Information;

import android.bluetooth.BluetoothDevice;

public class User {
    private String UserId;
    private String Username;
    private String ImageUrl;
    private String Email;
    private String City;
    private String Age;
    private String Gender;
    //private BluetoothDevice device;

    public String getGender() {
        return Gender;
    }

    public void setGender(String gender) {
        Gender = gender;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public String getCity() {
        return City;
    }

    public void setCity(String city) {
        City = city;
    }

    public String getAge() {
        return Age;
    }

    public void setAge(String age) {
        Age = age;
    }

    public User(){}

    public User(String Email, String ImageUrl , String UserId, String Username) {
        this.Email = Email;
        this.UserId = UserId;
        this.Username = Username;
        this.ImageUrl = ImageUrl;
    }

    /*public User(String id, BluetoothDevice device) {
        this.userId = id;
        this.device = device;
    }

    protected User(Parcel in) {
        userId = in.readString();
        device = in.readParcelable(BluetoothDevice.class.getClassLoader());
    }*/

   /* public BluetoothDevice getUserDevice() {
        return device;
    }*/

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        this.UserId = userId;
    }

    public String getUsername() {
        return Username;
    }

    public void setUsername(String Username) {
        this.Username = Username;
    }

    public String getImageUrl() {
        return ImageUrl;
    }

    public void setImageUrl(String ImageUrl) {
        this.ImageUrl = ImageUrl;
    }

    public boolean equals(com.example.mobilechatapp.Information.User user) {
        if ( user.getUserId().equals(UserId) /*&& user.getUserDevice().equals(device) */)
            return true;

        else
            return false;
    }

    public boolean sameMac(BluetoothDevice device) {
        return device != null && device.getAddress().equals(device.getAddress());
    }

    /*
    public String toString() {
        return id + ":" + device.getAddress();
    }
*/


}
