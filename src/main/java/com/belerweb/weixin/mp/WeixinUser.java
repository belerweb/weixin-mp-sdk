package com.belerweb.weixin.mp;

import org.json.JSONException;
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
  private int groupId;

  public WeixinUser() {}

  public WeixinUser(JSONObject json) throws MpException {
    try {
      if (json.optString("id", null) != null) {
        this.fakeid = json.getString("id");
        this.nickname = json.getString("nick_name");
        this.reMarkName = json.getString("remark_name");
        this.groupId = json.getInt("group_id");
      } else {

        this.fakeid = json.getString("FakeId");
        this.nickname = json.getString("NickName");
        this.reMarkName = json.getString("ReMarkName");
        this.username = json.getString("Username");
        this.signature = json.getString("Signature");
        this.country = json.getString("Country");
        this.province = json.getString("Province");
        this.city = json.getString("City");
        this.sex = json.getString("Sex");
        this.groupId = json.getInt("GroupID");
      }
    } catch (JSONException e) {
      throw new MpException(e);
    }
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

  public int getGroupId() {
    return groupId;
  }

  public void setGroupId(int groupId) {
    this.groupId = groupId;
  }

}
