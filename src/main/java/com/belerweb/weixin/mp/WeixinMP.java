package com.belerweb.weixin.mp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

/**
 * 微信公众平台
 */
public class WeixinMP {

  public static final String CONFIG_USERNAME = "weixin.mp.username";
  public static final String CONFIG_PASSWORD = "weixin.mp.password";

  private static final Map<String, WeixinMP> MP = new HashMap<String, WeixinMP>();
  private static final String MP_URI = "https://mp.weixin.qq.com";
  private static final String MP_URI_LOGIN = MP_URI + "/cgi-bin/login?lang=zh_CN";
  private static final String MP_URI_USERS =
      MP_URI + "/cgi-bin/contactmanagepage?t=wxm-friend&lang=zh_CN&pageidx=0&type=0&groupid=0";
  private static final String MP_URI_INDEX = MP_URI + "/cgi-bin/indexpage?t=wxm-index&lang=zh_CN";
  private static final String MP_URI_INFO =
      MP_URI + "/cgi-bin/getcontactinfo?t=ajax-getcontactinfo&lang=zh_CN";
  private static final String MP_URI_GROUP =
      MP_URI + "/cgi-bin/modifygroup?t=ajax-friend-group&lang=zh_CN";

  private HttpClient httpClient;
  private String username;
  private String password;
  private String token;
  private long tokenTime;

  /**
   * 获取 Integer.MAX_VALUE 用户
   */
  public List<WeixinUser> getUsers() {
    return getUsers(Integer.MAX_VALUE);
  }

  /**
   * 获取指定数量的用户
   */
  public List<WeixinUser> getUsers(int size) {
    preCheck();
    List<WeixinUser> result = new ArrayList<WeixinUser>();
    GetMethod get = new GetMethod(MP_URI_USERS + "&pagesize=" + size + "&token=" + token);
    addCommonHeader(get);
    get.addRequestHeader("Referer", MP_URI_INDEX + "&token=" + token);
    get.addRequestHeader("Accept",
        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    try {
      int status = httpClient.executeMethod(get);
      if (status != 200) {
        throw new RuntimeException("Status:" + status + "\n" + get.getResponseBodyAsString());
      }
      String response = get.getResponseBodyAsString();
      postCheck(response);
      JSONArray users =
          new JSONArray(Jsoup.parse(response).getElementById("json-friendList").html());
      for (int i = 0; i < users.length(); i++) {
        String fakeId = users.getJSONObject(i).getString("fakeId");
        WeixinUser user = getUser(fakeId);
        if (user == null) {
          // TODO
        } else {
          result.add(user);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * 通过fakeId获取用户
   */
  public WeixinUser getUser(String fakeId) {
    PostMethod post = new PostMethod(MP_URI_INFO + "&fakeid=" + fakeId);
    addCommonHeader(post);
    addAjaxHeader(post);
    addFormHeader(post);
    post.addRequestHeader("Referer", MP_URI_USERS + "&pagesize=10&token=" + token);
    post.addParameter("ajax", "1");
    post.addParameter("token", token);

    try {
      int status = httpClient.executeMethod(post);
      if (status != 200) {
        throw new RuntimeException("Status:" + status + "\n" + post.getResponseBodyAsString());
      }
      String response = post.getResponseBodyAsString();
      postCheck(response);
      return new WeixinUser(new JSONObject(response));
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * 增加分组
   */
  public String addGroup(String groupName) {
    return editGroup("add", null, groupName);
  }

  /**
   * 分组重命名
   */
  public String renameGroup(String groupId, String groupName) {
    return editGroup("rename", groupId, groupName);
  }

  private String editGroup(String action, String groupId, String groupName) {
    PostMethod post = new PostMethod(MP_URI_GROUP);
    addCommonHeader(post);
    addAjaxHeader(post);
    addFormHeader(post);
    post.addRequestHeader("Referer", MP_URI_USERS + "&pagesize=10&token=" + token);
    post.addParameter("ajax", "1");
    post.addParameter("token", token);
    post.addParameter("func", action);
    post.addParameter("name", groupName);
    if (groupId != null) {
      post.addParameter("id", groupId);
    }

    try {
      int status = httpClient.executeMethod(post);
      if (status != 200) {
        throw new RuntimeException("Status:" + status + "\n" + post.getResponseBodyAsString());
      }
      String response = post.getResponseBodyAsString();
      postCheck(response);
      return new JSONObject(response).getString("GroupId");
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * 删除分组
   */
  public void deleteGroup(String groupId) {
    PostMethod post = new PostMethod(MP_URI_GROUP);
    addCommonHeader(post);
    addAjaxHeader(post);
    addFormHeader(post);
    post.addRequestHeader("Referer", MP_URI_USERS + "&pagesize=10&token=" + token);
    post.addParameter("ajax", "1");
    post.addParameter("token", token);
    post.addParameter("func", "del");
    post.addParameter("id", groupId);

    try {
      int status = httpClient.executeMethod(post);
      if (status != 200) {
        throw new RuntimeException("Status:" + status + "\n" + post.getResponseBodyAsString());
      }
      postCheck(post.getResponseBodyAsString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private WeixinMP(String username, String password) {
    this.username = username;
    this.password = password;
    httpClient = new HttpClient();
    httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    httpClient.getParams().setParameter(HttpMethodParams.USER_AGENT,
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:22.0) Gecko/20100101 Firefox/22.0");
    httpClient.getParams().setParameter("http.protocol.single-cookie-header", true);
  }

  private void login() {
    GetMethod get = new GetMethod(MP_URI);
    addCommonHeader(get);
    try {
      int status = httpClient.executeMethod(get);
      if (status != 200) {
        throw new RuntimeException("Status:" + status + "\n" + get.getResponseBodyAsString());
      }
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    PostMethod post = new PostMethod(MP_URI_LOGIN);
    addCommonHeader(post);
    addAjaxHeader(post);
    addFormHeader(post);
    post.addRequestHeader("Referer", MP_URI);
    post.addRequestHeader("Accept", "application/json, text/javascript, */*; q=0.01");
    post.addParameter("username", username);
    post.addParameter("pwd", DigestUtils.md5Hex(password));
    post.addParameter("imgcode", "");
    post.addParameter("f", "json");
    try {
      int status = httpClient.executeMethod(post);
      if (status != 200) {
        throw new RuntimeException("Status:" + status + "\n" + get.getResponseBodyAsString());
      }

      String html = post.getResponseBodyAsString();
      JSONObject result = new JSONObject(html);
      if (result.getInt("Ret") == 302) {
        String tokenUrl = result.getString("ErrMsg");
        token = tokenUrl.substring(tokenUrl.lastIndexOf("&") + 7);
        tokenTime = System.currentTimeMillis();
      } else if (result.getInt("Ret") == 400) {
        if (result.getInt("ErrCode") == -3) {
          // 您输入的帐号或者密码不正确，请重新输入。
        } else if (result.getInt("ErrCode") == -4) {
          // 不存在该帐户
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void preCheck() {
    if (token == null || (System.currentTimeMillis() - tokenTime) > 600000) {
      login();
    }
  }

  private void postCheck(String response) {
    if (response.contains("登录超时") || response.contains("\"ret\":\"-20000\"")) {
      token = null;
      throw new RuntimeException("登录超时");
    }
  }

  private void addCommonHeader(HttpMethod request) {
    request.addRequestHeader("Accept-Language", "zh-cn;q=0.5");
    // request.addRequestHeader("Accept-Encoding", "gzip, deflate");
  }

  private void addAjaxHeader(HttpMethod request) {
    request.addRequestHeader("X-Requested-With", "XMLHttpRequest");
  }

  private void addFormHeader(HttpMethod request) {
    request.addRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
  }

  public static WeixinMP init(String username, String password) {
    if (!MP.containsKey(username)) {
      WeixinMP mp = new WeixinMP(username, password);
      mp.login();
      MP.put(username, mp);
    }

    return MP.get(username);
  }

}
