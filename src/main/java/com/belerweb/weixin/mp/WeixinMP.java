package com.belerweb.weixin.mp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;

/**
 * 微信公众平台
 */
public class WeixinMP {

  public static final String CONFIG_USERNAME = "weixin.mp.username";
  public static final String CONFIG_PASSWORD = "weixin.mp.password";

  private static final Map<String, WeixinMP> MP = new HashMap<String, WeixinMP>();
  private static final String MP_URI = "https://mp.weixin.qq.com";
  private static final String MP_URI_TOKEN = "https://api.weixin.qq.com/cgi-bin/token";
  private static final String MP_URI_LOGIN = MP_URI + "/cgi-bin/login?lang=zh_CN";
  private static final String MP_URI_USERS =
      MP_URI + "/cgi-bin/contactmanagepage?t=wxm-friend&lang=zh_CN&pageidx=0&type=0&groupid=0";
  private static final String MP_URI_INDEX = MP_URI + "/cgi-bin/indexpage?t=wxm-index&lang=zh_CN";
  private static final String MP_URI_INFO =
      MP_URI + "/cgi-bin/getcontactinfo?t=ajax-getcontactinfo&lang=zh_CN";
  private static final String MP_URI_GROUP =
      MP_URI + "/cgi-bin/modifygroup?t=ajax-friend-group&lang=zh_CN";
  private static final String MP_URI_PUTINTO_GROUP =
      MP_URI + "/cgi-bin/modifycontacts?action=modifycontacts&t=ajax-putinto-group";
  private static final String MP_URI_SEND =
      MP_URI + "/cgi-bin/singlesend?t=ajax-response&lang=zh_CN";
  private static final String MP_URI_UPLOAD =
      MP_URI + "/cgi-bin/uploadmaterial?cgi=uploadmaterial&t=iframe-uploadfile&lang=zh_CN";
  private static final String MP_URI_MODIFY_FILE =
      MP_URI + "/cgi-bin/modifyfile?lang=zh_CN&t=ajax-response";
  private static final String MP_URI_GET_MESSAGE =
      MP_URI + "/cgi-bin/getmessage?cgi=getmessage&t=ajax-message&ajax=1";

  private HttpClient httpClient;
  private String username;
  private String password;
  private String token;
  private long tokenTime;

  /**
   * 获取凭证
   */
  public AccessToken getAccessToken(String appid, String secret) throws MpException {
    return getAccessToken("client_credential", appid, secret);
  }

  /**
   * 获取凭证
   */
  public AccessToken getAccessToken(String grantType, String appid, String secret)
      throws MpException {
    GetMethod request = new GetMethod(MP_URI_TOKEN);
    NameValuePair[] params = new NameValuePair[3];
    params[0] = new NameValuePair("grant_type", grantType);
    params[1] = new NameValuePair("appid", appid);
    params[2] = new NameValuePair("secret", secret);
    request.setQueryString(params);
    return new AccessToken(toJsonObject(execute(request)));
  }

  private String execute(HttpMethod request) throws MpException {
    try {
      int status = httpClient.executeMethod(request);
      if (status != HttpStatus.SC_OK) {
        throw new MpException(MpException.DEFAULT_CODE, String.valueOf(status));
      }
      return request.getResponseBodyAsString();
    } catch (HttpException e) {
      throw new MpException(e);
    } catch (IOException e) {
      throw new MpException(e);
    }
  }

  private JSONObject toJsonObject(String json) throws MpException {
    try {
      return new JSONObject(json);
    } catch (JSONException e) {
      throw new MpException(e);
    }
  }

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

  public void putIntoGroup(List<String> fakeIds, String groupId) {
    PostMethod post = new PostMethod(MP_URI_PUTINTO_GROUP);
    addCommonHeader(post);
    addAjaxHeader(post);
    addFormHeader(post);
    post.addRequestHeader("Referer", MP_URI_USERS + "&pagesize=10&token=" + token);
    post.addParameter("ajax", "1");
    post.addParameter("token", token);
    post.addParameter("contacttype", groupId);
    post.addParameter("tofakeidlist", StringUtil.join(fakeIds, "|"));

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

  public void sendText(String fakeId, String content) {
    PostMethod post = new PostMethod(MP_URI_SEND);
    addCommonHeader(post);
    addAjaxHeader(post);
    addFormHeader(post);
    post.addRequestHeader("Referer", MP_URI + "/cgi-bin/singlemsgpage?msgid=&source=&count&token="
        + token + "&fromfakeid=" + fakeId);
    post.addParameter("ajax", "1");
    post.addParameter("token", token);
    post.addParameter("error", "false");
    post.addParameter("imgcode", "");
    post.addParameter("tofakeid", fakeId);
    post.addParameter("content", content);
    post.addParameter("type", "1");

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

  public void sendImage(String fakeId, byte[] image, String type) {
    String fileid = uploadImage(image, type);
    PostMethod post = new PostMethod(MP_URI_SEND);
    addCommonHeader(post);
    addAjaxHeader(post);
    addFormHeader(post);
    post.addRequestHeader("Referer", MP_URI + "/cgi-bin/singlemsgpage?msgid=&source=&count&token="
        + token + "&fromfakeid=" + fakeId);
    post.addParameter("ajax", "1");
    post.addParameter("token", token);
    post.addParameter("error", "false");
    post.addParameter("imgcode", "");
    post.addParameter("tofakeid", fakeId);
    post.addParameter("type", "2");
    post.addParameter("fid", fileid);
    post.addParameter("fileid", fileid);

    try {
      int status = httpClient.executeMethod(post);
      if (status != 200) {
        throw new RuntimeException("Status:" + status + "\n" + post.getResponseBodyAsString());
      }
      postCheck(post.getResponseBodyAsString());
    } catch (Exception e) {
      e.printStackTrace();
    }

    deleteFile(fileid);
  }

  public String uploadImage(byte[] image, String type) {
    String formId = "file_from_" + System.currentTimeMillis();
    PostMethod post =
        new PostMethod(MP_URI_UPLOAD + "&type=2&token=" + token + "&formId=" + formId);
    addCommonHeader(post);
    post.addRequestHeader("Referer", MP_URI
        + "/cgi-bin/indexpage?lang=zh_CN&t=wxm-upload&type=2&token=" + token + "&formId=" + formId);
    try {
      Part[] parts = new Part[] {new ByteArrayPart(image, "uploadfile", type)};
      MultipartRequestEntity entry = new MultipartRequestEntity(parts, post.getParams());
      post.setRequestEntity(entry);

      int status = httpClient.executeMethod(post);
      String response = post.getResponseBodyAsString();
      if (status != 200) {
        throw new RuntimeException("Status:" + status + "\n" + response);
      }
      postCheck(response);
      Matcher matcher = Pattern.compile("'(\\d+)'").matcher(response);
      matcher.find();
      return matcher.group(1);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public void deleteFile(String fileid) {
    PostMethod post = new PostMethod(MP_URI_MODIFY_FILE + "&oper=del");
    addCommonHeader(post);
    addAjaxHeader(post);
    addFormHeader(post);
    post.addRequestHeader("Referer", MP_URI
        + "/cgi-bin/filemanagepage?t=wxm-file&lang=zh_CN&type=2&pagesize=10&pageidx=0&token="
        + token);
    post.addParameter("ajax", "1");
    post.addParameter("token", token);
    post.addParameter("fileid", fileid);

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

  public List<WeixinMessage> getMessage(Integer day, Integer count, Integer offset) {
    List<WeixinMessage> result = new ArrayList<WeixinMessage>();
    PostMethod post = new PostMethod(MP_URI_GET_MESSAGE);
    addCommonHeader(post);
    addAjaxHeader(post);
    addFormHeader(post);
    post.addRequestHeader("Referer", MP_URI
        + "/cgi-bin/filemanagepage?t=wxm-file&lang=zh_CN&type=2&pagesize=10&pageidx=0&token="
        + token);
    post.addParameter("token", token);
    post.addParameter("day", day == null ? "0" : day.toString());
    post.addParameter("count", count == null ? "50" : count.toString());
    post.addParameter("offset", offset == null ? "0" : offset.toString());

    try {
      int status = httpClient.executeMethod(post);
      if (status != 200) {
        throw new RuntimeException("Status:" + status + "\n" + post.getResponseBodyAsString());
      }
      String response = post.getResponseBodyAsString();
      postCheck(response);
      JSONArray messages = new JSONArray(response);
      for (int i = 0; i < messages.length(); i++) {
        result.add(new WeixinMessage(messages.getJSONObject(i)));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
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
