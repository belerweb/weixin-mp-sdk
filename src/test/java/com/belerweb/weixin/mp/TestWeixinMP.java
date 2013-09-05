package com.belerweb.weixin.mp;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class TestWeixinMP {

  byte[] image =
      new byte[] {-119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 16, 0, 0,
          0, 16, 8, 4, 0, 0, 0, -75, -6, 55, -22, 0, 0, 0, 2, 98, 75, 71, 68, 0, -1, -121, -113,
          -52, -65, 0, 0, 0, 9, 112, 72, 89, 115, 0, 0, 0, 72, 0, 0, 0, 72, 0, 70, -55, 107, 62, 0,
          0, 0, 9, 118, 112, 65, 103, 0, 0, 0, 16, 0, 0, 0, 16, 0, 92, -58, -83, -61, 0, 0, 0, -38,
          73, 68, 65, 84, 40, -49, -123, -47, 61, 79, 2, 65, 16, -58, -15, -33, 30, -105, 64, -16,
          -123, -40, 64, -50, 68, 40, -82, -128, -60, -34, -17, -1, 13, 108, 13, -127, 16, 2, 36,
          106, 101, 52, 70, 94, -68, 91, -117, -69, 19, 105, 100, -118, 77, 102, -26, -1, -20, 62,
          51, 27, -94, -1, 35, 37, 28, -77, -98, 9, 102, -58, 54, -106, 16, -91, 127, -32, -127,
          -79, -114, -24, 94, -5, -28, -122, 58, 70, 114, 45, 27, 100, -24, -98, 2, 45, -71, 33,
          22, 102, -40, 25, -55, 20, -26, 10, 66, 20, -38, -58, -6, -66, -51, -83, 106, -39, -99,
          92, -22, -43, 83, -36, 39, -104, -56, 4, -21, -33, 54, 43, 107, 65, 102, 66, -126, 47,
          -17, 56, -100, 76, 119, 16, -67, -39, 86, 30, -90, -82, 60, 72, 112, -83, -117, 15, -97,
          18, -123, 71, -5, -58, 100, 81, -21, 110, 13, 5, 83, -117, 99, -75, 2, -102, 93, 69, -91,
          -96, 89, 110, -96, -14, 80, 37, 37, 74, -22, -77, 108, 68, -51, 19, -123, -66, -74, 27,
          17, 125, 29, -67, 26, -82, -127, -99, 103, 3, 23, 10, 91, 116, 93, -30, -91, -14, 16,
          -50, -3, 102, 114, -90, -17, 7, -103, 77, 60, -30, 26, 27, -14, 4, 0, 0, 0, 37, 116, 69,
          88, 116, 100, 97, 116, 101, 58, 99, 114, 101, 97, 116, 101, 0, 50, 48, 49, 48, 45, 48,
          50, 45, 49, 49, 84, 49, 49, 58, 53, 48, 58, 48, 56, 45, 48, 54, 58, 48, 48, -42, 16, 101,
          -5, 0, 0, 0, 37, 116, 69, 88, 116, 100, 97, 116, 101, 58, 109, 111, 100, 105, 102, 121,
          0, 50, 48, 48, 54, 45, 48, 53, 45, 48, 53, 84, 49, 51, 58, 50, 50, 58, 52, 48, 45, 48,
          53, 58, 48, 48, -65, -28, -2, 26, 0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126};

  String username =
      System.getProperty(WeixinMP.CONFIG_USERNAME, System.getenv(WeixinMP.CONFIG_USERNAME));
  String password =
      System.getProperty(WeixinMP.CONFIG_PASSWORD, System.getenv(WeixinMP.CONFIG_PASSWORD));

  @Test
  public void testGetAccessToken() throws MpException {
    WeixinMP mp = WeixinMP.init(username, password);
    String appid = System.getProperty("appid");
    String secret = System.getProperty("secret");
    AccessToken accessToken = mp.getAccessToken(appid, secret);
    System.out.println("Token:" + accessToken.getToken());
    System.out.println("Valid:" + accessToken.isValid());
  }

  @Test
  public void testGetMessage() throws MpException {
    WeixinMP mp = WeixinMP.init(username, password);
    List<WeixinMessage> messages = mp.getMessage(0, 20);
    for (WeixinMessage message : messages) {
      System.out.println("ID:" + message.getId());
      System.out.println("Type:" + message.getType());
      System.out.println("Date:" + message.getDateTime());
      System.out.println("Content:" + message.getContent());
    }
  }

  @Test
  public void testGetUser() throws MpException {
    WeixinMP mp = WeixinMP.init(username, password);
    List<WeixinUser> users = mp.getUsers();
    for (WeixinUser user : users) {
      System.out.println("FakeId:" + user.getFakeid());
      System.out.println("NickName:" + user.getNickname());
      System.out.println("ReMarkName:" + user.getReMarkName());
      System.out.println("Username:" + user.getUsername());
      System.out.println("Signature:" + user.getSignature());
      System.out.println("Country:" + user.getCountry());
      System.out.println("Province:" + user.getProvince());
      System.out.println("City:" + user.getCity());
      System.out.println("Sex:" + user.getSex());
      System.out.println("=======================================");
    }
  }

  @Test
  public void testAddGroup() throws MpException {
    WeixinMP mp = WeixinMP.init(username, password);
    mp.addGroup("测试组");
  }

  @Test
  public void testRenameGroup() throws MpException {
    WeixinMP mp = WeixinMP.init(username, password);
    mp.renameGroup("100", "测试组-修改");
  }

  @Test
  public void testDeleteGroup() throws MpException {
    WeixinMP mp = WeixinMP.init(username, password);
    mp.deleteGroup("100");
  }

  @Test
  public void testPutIntoGroup() throws MpException {
    List<String> fakeIds = new ArrayList<String>();
    fakeIds.add("25029755");
    fakeIds.add("24771975");
    fakeIds.add("2125943182");
    WeixinMP mp = WeixinMP.init(username, password);
    mp.putIntoGroup(fakeIds, "2");
  }

  @Test
  public void testSendText() throws MpException {
    WeixinMP mp = WeixinMP.init(username, password);
    mp.sendText("2125943182", "消息来自客户端/酷😭");
  }

  @Test
  public void testUploadImage() throws MpException {
    WeixinMP mp = WeixinMP.init(username, password);
    System.out.println("FileID:" + mp.uploadImage(image, "image/png"));
  }

  @Test
  public void testDeleteFile() throws MpException {
    WeixinMP mp = WeixinMP.init(username, password);
    mp.deleteFile("10000002");
  }

  @Test
  public void testSendImage() throws Exception {
    WeixinMP mp = WeixinMP.init(username, password);
    mp.sendImage("2125943182", image, "image/jpeg");
  }
}
