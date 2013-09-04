package com.belerweb.weixin.mp;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class AccessToken {

  private String token;// 凭证
  private int expiresIn;// 凭证有效时间，单位：秒
  private Date time;// 凭证时间

  public AccessToken(JSONObject json) throws MpException {
    try {
      this.token = json.getString("access_token");
      this.expiresIn = json.getInt("expires_in");
      this.time = new Date();
    } catch (JSONException e) {
      throw new MpException(json);
    }
  }

  public String getToken() {
    return token;
  }

  public boolean isValid() {
    return (new Date().getTime() - time.getTime()) < expiresIn * 1000;
  }

}
