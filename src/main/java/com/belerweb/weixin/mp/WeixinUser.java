package com.belerweb.weixin.mp;

import org.json.JSONObject;

public class WeixinUser {

  private String openid;
  private String fakeid;
  private String nickname;
  private String reMarkName;
  private String username;
  private String signature;
  private String country;
  private String province;
  private String city;
  private String sex;

  public WeixinUser() {}

  public WeixinUser(JSONObject json) {
    this.fakeid = json.optString("FakeId");
    this.nickname = json.optString("NickName");
    this.reMarkName = json.optString("ReMarkName");
    this.username = json.optString("Username");
    this.signature = json.optString("Signature");
    this.country = json.optString("Country");
    this.province = json.optString("Province");
    this.city = json.optString("City");
    this.sex = json.optString("Sex");
  }

  public String getOpenid() {
    return openid;
  }

  public void setOpenid(String openid) {
    this.openid = openid;
  }

  public String getFakeid() {
    return fakeid;
  }

  public void setFakeid(String fakeid) {
    this.fakeid = fakeid;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getReMarkName() {
    return reMarkName;
  }

  public void setReMarkName(String reMarkName) {
    this.reMarkName = reMarkName;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getProvince() {
    return province;
  }

  public void setProvince(String province) {
    this.province = province;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getSex() {
    return sex;
  }

  public void setSex(String sex) {
    this.sex = sex;
  }

}
