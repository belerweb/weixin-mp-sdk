package com.belerweb.weixin.mp;

import org.json.JSONObject;

public class MpException extends Exception {

  private static final long serialVersionUID = -5718852924251291897L;

  public static final int DEFAULT_CODE = -2;

  private int errcode = DEFAULT_CODE;
  private String errmsg;

  public MpException(String errormsg) {
    this.errmsg = errormsg;
  }

  public MpException(int errcode, String errormsg) {
    this.errcode = errcode;
    this.errmsg = errormsg;
  }

  public MpException(JSONObject json) {
    this.errcode = json.optInt("errcode", -2);
    this.errmsg = json.optString("errmsg");
  }

  public MpException(Throwable e) {
    super(e);
    this.errmsg = e.getMessage();
  }

  @Override
  public String getMessage() {
    return errcode + ":" + errmsg;
  }

  public int getErrcode() {
    return errcode;
  }

  public String getErrmsg() {
    return errmsg;
  }

}
