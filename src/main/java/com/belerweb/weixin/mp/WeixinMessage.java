package com.belerweb.weixin.mp;

import java.util.Date;

import org.json.JSONObject;

public class WeixinMessage {

  private String id;
  private String type;
  private String fileId;
  private String hasReply;
  private String fakeId;
  private String nickName;
  private String remarkName;
  private Date dateTime;
  private String icon;
  private String content;
  private String playLength;
  private String length;
  private String source;
  private String starred;
  private String status;
  private String subtype;
  private String showType;
  private String desc;
  private String title;
  private String appName;
  private String contentUrl;

  public WeixinMessage() {}

  public WeixinMessage(JSONObject json) {
    this.id = json.optString("id");
    this.type = json.optString("type");
    this.fileId = json.optString("fileId");
    this.hasReply = json.optString("hasReply");
    this.fakeId = json.optString("fakeId");
    this.nickName = json.optString("nickName");
    this.remarkName = json.optString("remarkName");
    this.dateTime = new Date(json.optLong("dateTime"));
    this.icon = json.optString("icon");
    this.content = json.optString("content");
    this.playLength = json.optString("playLength");
    this.length = json.optString("length");
    this.source = json.optString("source");
    this.starred = json.optString("starred");
    this.status = json.optString("status");
    this.subtype = json.optString("subtype");
    this.showType = json.optString("showType");
    this.desc = json.optString("desc");
    this.title = json.optString("title");
    this.appName = json.optString("appName");
    this.contentUrl = json.optString("contentUrl");
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getFileId() {
    return fileId;
  }

  public void setFileId(String fileId) {
    this.fileId = fileId;
  }

  public String getHasReply() {
    return hasReply;
  }

  public void setHasReply(String hasReply) {
    this.hasReply = hasReply;
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

  public String getRemarkName() {
    return remarkName;
  }

  public void setRemarkName(String remarkName) {
    this.remarkName = remarkName;
  }

  public Date getDateTime() {
    return dateTime;
  }

  public void setDateTime(Date dateTime) {
    this.dateTime = dateTime;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getPlayLength() {
    return playLength;
  }

  public void setPlayLength(String playLength) {
    this.playLength = playLength;
  }

  public String getLength() {
    return length;
  }

  public void setLength(String length) {
    this.length = length;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getStarred() {
    return starred;
  }

  public void setStarred(String starred) {
    this.starred = starred;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getSubtype() {
    return subtype;
  }

  public void setSubtype(String subtype) {
    this.subtype = subtype;
  }

  public String getShowType() {
    return showType;
  }

  public void setShowType(String showType) {
    this.showType = showType;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getContentUrl() {
    return contentUrl;
  }

  public void setContentUrl(String contentUrl) {
    this.contentUrl = contentUrl;
  }

}
