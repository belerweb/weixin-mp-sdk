package com.belerweb.weixin.mp;

import java.util.List;

import org.junit.Test;

public class TestWeixinMP {

  String username =
      System.getProperty(WeixinMP.CONFIG_USERNAME, System.getenv(WeixinMP.CONFIG_USERNAME));
  String password =
      System.getProperty(WeixinMP.CONFIG_PASSWORD, System.getenv(WeixinMP.CONFIG_PASSWORD));

  @Test
  public void testGetUser() {
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
  public void testAddGroup() {
    WeixinMP mp = WeixinMP.init(username, password);
    mp.addGroup("测试组");
  }

  @Test
  public void testRenameGroup() {
    WeixinMP mp = WeixinMP.init(username, password);
    mp.renameGroup("100", "测试组-修改");
  }

  @Test
  public void testDeleteGroup() {
    WeixinMP mp = WeixinMP.init(username, password);
    mp.deleteGroup("100");;
  }

}
