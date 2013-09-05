package com.belerweb.weixin.mp;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class WeixinMessage {

  private int id;
  /**
   * 文本/表情/位置:1
   * 示例:{"id":0,"type":1,"fakeid":"12345","nick_name":"昵称","date_time":1378288316,"content":"我的位置:<br/>
   * http://url.cn/","source":"","msg_status":4,"has_reply":0,"refuse_reason":""}
   * 
   * 图片:2 示例:{"id":0,"type":2,"fakeid":"12345","nick_name":"昵称","date_time":1378288257,"source":"",
   * "msg_status":4,"has_reply":0,"refuse_reason":""}
   * 
   * 声音:3
   * 示例:{"id":0,"type":3,"fakeid":"12345","nick_name":"昵称","date_time":1378288228,"play_length":
   * 1859,"length":1092,"source":"","msg_status":4,"has_reply":0,"refuse_reason":""}
   * 
   * 视频:4
   * 示例:{"id":0,"type":4,"fakeid":"12345","nick_name":"昵称","date_time":1378288286,"play_length":
   * 1,"length":6983,"source":"","msg_status":4,"has_reply":0,"refuse_reason":""}
   */
  private int type;

  private String fakeId;
  private String nickName;
  private Date dateTime;
  private String source;
  private int msgStatus;
  private boolean hasReply;
  private String refuseReason;

  // 2
  private String content;

  // 3/4
  private int playLength;
  private int length;

  public WeixinMessage() {

  }

  public WeixinMessage(JSONObject json) throws MpException {
    try {
      this.id = json.getInt("id");
      this.type = json.getInt("type");
      this.fakeId = json.getString("fakeid");
      this.nickName = json.getString("nick_name");
      this.dateTime = new Date(json.getLong("date_time") * 1000);
      this.source = json.optString("source");
      this.msgStatus = json.getInt("msg_status");
      this.hasReply = json.getInt("has_reply") == 1;
      this.content = json.optString("content");
      this.playLength = json.optInt("play_length");
      this.length = json.optInt("length");
    } catch (JSONException e) {
      throw new MpException(json);
    }
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  public String getFakeId() {
    return fakeId;
  }

  public void setFakeId(String fakeId) {
    this.fakeId = fakeId;
  }

  public String getNickName() {
    return nickName;
  }

  public void setNickName(String nickName) {
    this.nickName = nickName;
  }

  public Date getDateTime() {
    return dateTime;
  }

  public void setDateTime(Date dateTime) {
    this.dateTime = dateTime;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public int getMsgStatus() {
    return msgStatus;
  }

  public void setMsgStatus(int msgStatus) {
    this.msgStatus = msgStatus;
  }

  public boolean isHasReply() {
    return hasReply;
  }

  public void setHasReply(boolean hasReply) {
    this.hasReply = hasReply;
  }

  public String getRefuseReason() {
    return refuseReason;
  }

  public void setRefuseReason(String refuseReason) {
    this.refuseReason = refuseReason;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public int getPlayLength() {
    return playLength;
  }

  public void setPlayLength(int playLength) {
    this.playLength = playLength;
  }

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }

}
