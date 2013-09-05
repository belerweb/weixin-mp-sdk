package com.belerweb.weixin.mp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.helper.StringUtil;

/**
 * 微信公众平台
 */
public class WeixinMP {

  public static final String CONFIG_USERNAME = "weixin.mp.username";
  public static final String CONFIG_PASSWORD = "weixin.mp.password";

  public static final int GROUP_DEFAULT = 0;// 未分组
  public static final int GROUP_BLACKLIST = 1;// 黑名单
  public static final int GROUP_ASTERISK = 2;// 星标组

  private static final Map<String, WeixinMP> MP = new HashMap<String, WeixinMP>();
  private static final String MP_URI = "https://mp.weixin.qq.com";
  private static final String MP_URI_TOKEN = "https://api.weixin.qq.com/cgi-bin/token";
  private static final String MP_URI_USERS =
      MP_URI + "/cgi-bin/contactmanagepage?t=wxm-friend&lang=zh_CN&pageidx=0&type=0&groupid=0";
  private static final String MP_URI_GROUP =
      MP_URI + "/cgi-bin/modifygroup?t=ajax-friend-group&lang=zh_CN";
  private static final String MP_URI_SEND =
      MP_URI + "/cgi-bin/singlesend?t=ajax-response&lang=zh_CN";
  private static final String MP_URI_UPLOAD =
      MP_URI + "/cgi-bin/uploadmaterial?cgi=uploadmaterial&t=iframe-uploadfile&lang=zh_CN";
  private static final String MP_URI_MODIFY_FILE =
      MP_URI + "/cgi-bin/modifyfile?lang=zh_CN&t=ajax-response";

  private HttpClient httpClient;
  private String username;
  private String password;
  private String token;
  private long tokenTime;

  /**
   * 私有构造函数，请通过init方法获取实例
   */
  private WeixinMP(String username, String password) {
    this.username = username;
    this.password = password;
    httpClient = new HttpClient();
    httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    httpClient.getParams().setParameter("http.protocol.single-cookie-header", true);
  }

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

  /**
   * 微信公众平台初始化
   */
  public static WeixinMP init(String username, String password) throws MpException {
    if (!MP.containsKey(username)) {
      WeixinMP mp = new WeixinMP(username, password);
      mp.login();
      MP.put(username, mp);
    }

    return MP.get(username);
  }

  /**
   * 检查Token是否过期
   */
  private void checkToken() throws MpException {
    if (token == null || (System.currentTimeMillis() - tokenTime) > 600000) {
      login();
    }
  }

  /**
   * 登录
   */
  private void login() throws MpException {
    PostMethod request = new PostMethod("https://mp.weixin.qq.com/cgi-bin/login");
    request.addParameter("X-Requested-With", "XMLHttpRequest");
    request.addParameter(new NameValuePair("lang", "zh_CN"));
    request.addParameter(new NameValuePair("f", "json"));
    request.addParameter(new NameValuePair("imgcode", ""));
    request.addParameter(new NameValuePair("username", username));
    request.addParameter(new NameValuePair("pwd", DigestUtils.md5Hex(password)));

    /**
     * 正确结果示例:
     * 
     * { "Ret": 302, "ErrMsg": "/cgi-bin/home?t=home/index&lang=zh_CN&token=1234567890",
     * "ShowVerifyCode": 0, "ErrCode": 0 }
     * 
     * 错误结果示例:
     * 
     * { "Ret": 400, "ErrMsg": "", "ShowVerifyCode": 0, "ErrCode": -3 } // 密码错误
     * 
     * { "Ret": 400, "ErrMsg": "", "ShowVerifyCode": 0, "ErrCode": -2 } // HTTP错误
     */
    try {
      JSONObject result = new JSONObject(execute(request));
      if (result.getInt("Ret") == 302) {
        Matcher matcher = Pattern.compile("token=(\\d+)").matcher(result.getString("ErrMsg"));
        if (matcher.find()) {
          token = matcher.group(1);
          tokenTime = new Date().getTime();
        }
      }
    } catch (JSONException e) {
      throw new MpException(e);
    }
  }

  /**
   * 实时消息：全部消息
   */
  public List<WeixinMessage> getMessage(int offset, int count) throws MpException {
    checkToken();
    String url = "https://mp.weixin.qq.com/cgi-bin/message";
    GetMethod request = new GetMethod(url);
    NameValuePair[] params = new NameValuePair[5];
    params[0] = new NameValuePair("token", token);// 必须
    params[1] = new NameValuePair("lang", "zh_CN");// 必须
    params[2] = new NameValuePair("day", "7");// 全部消息
    params[3] = new NameValuePair("offset", String.valueOf(offset));
    params[4] = new NameValuePair("count", String.valueOf(count));
    request.setQueryString(params);
    List<WeixinMessage> messages = new ArrayList<WeixinMessage>();
    for (String line : execute(request).split("[\r\n]+")) {
      line = line.trim();
      if (line.startsWith("list : ({\"msg_item\":") && line.endsWith("}).msg_item")) {
        try {
          JSONArray array = new JSONArray(line.substring(20, line.length() - 11));
          for (int i = 0; i < array.length(); i++) {
            messages.add(new WeixinMessage(array.getJSONObject(i)));
          }
        } catch (JSONException e) {
          throw new MpException(e);
        }
        break;
      }
    }
    return messages;
  }

  /**
   * 用户列表
   */
  public List<WeixinUser> getUser(int groupId, int pageidx, int pagesize) throws MpException {
    checkToken();
    String url = "https://mp.weixin.qq.com/cgi-bin/contactmanage";
    GetMethod request = new GetMethod(url);
    NameValuePair[] params = new NameValuePair[6];
    params[0] = new NameValuePair("token", token);
    params[1] = new NameValuePair("lang", "zh_CN");
    params[2] = new NameValuePair("type", "0");
    params[3] = new NameValuePair("groupid", String.valueOf(groupId));
    params[4] = new NameValuePair("pageidx", String.valueOf(pageidx));
    params[5] = new NameValuePair("pagesize", String.valueOf(pagesize));
    request.setQueryString(params);
    List<WeixinUser> users = new ArrayList<WeixinUser>();
    for (String line : execute(request).split("[\r\n]+")) {
      line = line.trim();
      if (line.startsWith("friendsList : ({\"contacts\":") && line.endsWith("}).contacts,")) {
        try {
          JSONArray array = new JSONArray(line.substring(27, line.length() - 12));
          for (int i = 0; i < array.length(); i++) {
            users.add(new WeixinUser(array.getJSONObject(i)));
          }
        } catch (JSONException e) {
          throw new MpException(e);
        }
        break;
      }
    }
    return users;
  }

  /**
   * 通过FakeId获取指定用户信息
   */
  public WeixinUser getUser(String fakeId) throws MpException {
    checkToken();
    String url = "https://mp.weixin.qq.com/cgi-bin/getcontactinfo";
    PostMethod request = new PostMethod(url);
    request.addParameter("token", token);
    request.addParameter("lang", "zh_CN");
    request.addParameter("t", "ajax-getcontactinfo");
    request.addParameter("fakeid", fakeId);
    return new WeixinUser(toJsonObject(execute(request)));
  }

  /**
   * 将用户放入某个组内
   */
  public boolean putIntoGroup(List<String> fakeIds, int groupId) throws MpException {
    String url = "https://mp.weixin.qq.com/cgi-bin/modifycontacts";
    PostMethod request = new PostMethod(url);
    request.addParameter("token", token);
    request.addParameter("lang", "zh_CN");
    request.addParameter("t", "ajax-putinto-group");
    request.addParameter("action", "modifycontacts");
    request.addParameter("tofakeidlist", StringUtil.join(fakeIds, "|"));
    request.addParameter("contacttype", String.valueOf(groupId));
    return "0".equals(toJsonObject(execute(request)).optString("ret"));
  }

  private String execute(HttpMethod request) throws MpException {
    request.addRequestHeader("Pragma", "no-cache");
    request.addRequestHeader("Referer", "https://mp.weixin.qq.com/");
    request.addRequestHeader("User-Agent",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:23.0) Gecko/20100101 Firefox/23.0");
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

  private void postCheck(String response) {
    if (response.contains("登录超时") || response.contains("\"ret\":\"-20000\"")) {
      token = null;
      throw new RuntimeException("登录超时");
    }
  }

  private void addCommonHeader(HttpMethod request) {
    request.addRequestHeader("Acnguage", "zh-cn;q=0.5");
    // request.addRequestHeader("Accept-Encoding", "gzip, deflate");
  }

  private void addAjaxHeader(HttpMethod request) {
    request.addRequestHeader("X-Requested-With", "XMLHttpRequest");
  }

  private void addFormHeader(HttpMethod request) {
    request.addRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
  }

}
